"use client";

import { FormEvent, useState } from "react";
import Link from "next/link";
import { useCart } from "@/components/CartProvider";
import { storefrontApi } from "@/lib/api";
import { useStore } from "@/components/StoreProvider";

export default function CheckoutPage() {
  const store = useStore();
  const { items, total, removeItem, clear } = useCart();
  const [payment, setPayment] = useState<"FULL" | "INSTALLMENT">("FULL");
  const [installments, setInstallments] = useState(3);
  const [form, setForm] = useState({ email: "", address: "", notes: "" });
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);
  const money = (value: number) => new Intl.NumberFormat("en-NG", { style: "currency", currency: "NGN" }).format(value);
  const isDivinez = store.slug === "divinez-signature";

  async function submit(event: FormEvent) {
    event.preventDefault(); setMessage(""); setLoading(true);
    try {
      const order = await storefrontApi.createOrder({ customerEmail: form.email, shippingAddress: form.address, notes: form.notes, items: items.map(item => ({ variantId: item.variantId, quantity: item.quantity })) });
      if (payment === "INSTALLMENT") {
        const plan = await storefrontApi.createInstallmentPlan(order.id, installments);
        setMessage(`Order ${order.orderNumber} created. Your ${plan.installmentCount}-part plan is ${money(plan.installmentAmount)} per payment.`);
      } else setMessage(`Order ${order.orderNumber} created. Continue to payment to complete your purchase.`);
      clear();
    } catch (error) { setMessage(error instanceof Error ? error.message : "Checkout could not be completed."); }
    finally { setLoading(false); }
  }

  if (!items.length && !message) {
    return (
      <div className={`max-w-2xl mx-auto px-4 py-20 text-center ${isDivinez ? "text-[#0B4A2B]" : "text-white"}`}>
        <h1 className="text-3xl font-bold">Your bag is empty</h1>
        <Link href="/products" className={`inline-block mt-6 ${isDivinez ? "text-[#0B4A2B] hover:text-[#0B4A2B]/80" : "text-amber-400"}`}>Continue shopping</Link>
      </div>
    );
  }

  if (isDivinez) {
    return (
      <div className="max-w-5xl mx-auto px-4 sm:px-6 py-12 text-[#0B4A2B]">
        <h1 className="text-3xl font-serif font-bold">Checkout · {store.name}</h1>
        {message && (
          <div className="mt-6 border border-[#0B4A2B]/20 bg-[#0B4A2B]/5 p-4 rounded-xl text-[#0B4A2B]">{message}</div>
        )}
        {items.length > 0 && (
          <div className="grid lg:grid-cols-[1fr_380px] gap-10 mt-10">
            <form onSubmit={submit} className="space-y-5">
              <div>
                <label className="block text-sm font-medium mb-2">Email</label>
                <input required type="email" value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} className="w-full p-3 rounded-lg border border-[#0B4A2B]/20 focus:ring-2 focus:ring-[#0B4A2B]/20 outline-none" />
              </div>
              <div>
                <label className="block text-sm font-medium mb-2">Delivery address</label>
                <textarea required value={form.address} onChange={e => setForm({ ...form, address: e.target.value })} className="w-full p-3 rounded-lg border border-[#0B4A2B]/20 focus:ring-2 focus:ring-[#0B4A2B]/20 outline-none" rows={4} />
              </div>
              <div>
                <label className="block text-sm font-medium mb-2">Order notes</label>
                <textarea value={form.notes} onChange={e => setForm({ ...form, notes: e.target.value })} className="w-full p-3 rounded-lg border border-[#0B4A2B]/20 focus:ring-2 focus:ring-[#0B4A2B]/20 outline-none" rows={3} />
              </div>
              <fieldset>
                <legend className="text-lg font-semibold mb-3">Payment option</legend>
                <label className="block border border-[#0B4A2B]/20 p-4 rounded-lg mb-3 cursor-pointer hover:bg-[#0B4A2B]/5">
                  <input type="radio" checked={payment === "FULL"} onChange={() => setPayment("FULL")} /> <span className="ml-2">Pay in full</span>
                </label>
                <label className="block border border-[#0B4A2B]/20 p-4 rounded-lg cursor-pointer hover:bg-[#0B4A2B]/5">
                  <input type="radio" checked={payment === "INSTALLMENT"} onChange={() => setPayment("INSTALLMENT")} /> <span className="ml-2">Pay in installments</span>
                  {payment === "INSTALLMENT" && <span className="block mt-3 text-sm text-[#0B4A2B]/60">Choose a schedule (account required).</span>}
                </label>
                {payment === "INSTALLMENT" && (
                  <select value={installments} onChange={e => setInstallments(Number(e.target.value))} className="mt-3 p-3 rounded-lg border border-[#0B4A2B]/20 w-full">
                    {[2,3,4,6,12].map(count => <option key={count} value={count}>{count} payments · {money(total / count)} each</option>)}
                  </select>
                )}
              </fieldset>
              <button disabled={loading} className="w-full py-3 rounded-full bg-[#0B4A2B] text-[#FAFAF8] font-semibold hover:bg-[#07331D] transition-colors disabled:opacity-50">
                {loading ? "Processing…" : payment === "INSTALLMENT" ? "Create installment plan" : "Place order"}
              </button>
            </form>
            <aside className="bg-white border border-[#0B4A2B]/10 rounded-xl p-5 h-fit">
              <h2 className="font-semibold mb-4">Order summary</h2>
              {items.map(item => (
                <div key={item.variantId} className="flex justify-between gap-4 py-3 border-b border-[#0B4A2B]/10 text-sm">
                  <span>{item.name} × {item.quantity}<button type="button" onClick={() => removeItem(item.variantId)} className="block text-xs text-[#0B4A2B]/60 mt-1 hover:text-[#0B4A2B]">Remove</button></span>
                  <span>{money(item.price * item.quantity)}</span>
                </div>
              ))}
              <div className="flex justify-between font-bold pt-4">
                <span>Total</span>
                <span>{money(total)}</span>
              </div>
            </aside>
          </div>
        )}
      </div>
    );
  }

  // MeethybridHub (default dark theme)
  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 py-12 text-white">
      <h1 className="text-3xl font-bold">Checkout · {store.name}</h1>
      {message && (
        <div className="mt-6 border border-green-700 bg-green-950/40 p-4 rounded-xl">{message}</div>
      )}
      {items.length > 0 && (
        <div className="grid lg:grid-cols-[1fr_380px] gap-10 mt-10">
          <form onSubmit={submit} className="space-y-5">
            <div>
              <label className="block text-sm mb-2">Email</label>
              <input required type="email" value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} className="w-full p-3 rounded-lg bg-white text-black" />
            </div>
            <div>
              <label className="block text-sm mb-2">Delivery address</label>
              <textarea required value={form.address} onChange={e => setForm({ ...form, address: e.target.value })} className="w-full p-3 rounded-lg bg-white text-black" rows={4} />
            </div>
            <div>
              <label className="block text-sm mb-2">Order notes</label>
              <textarea value={form.notes} onChange={e => setForm({ ...form, notes: e.target.value })} className="w-full p-3 rounded-lg bg-white text-black" rows={3} />
            </div>
            <fieldset>
              <legend className="text-lg font-semibold mb-3">Payment option</legend>
              <label className="block border border-white/20 p-4 rounded-lg mb-3">
                <input type="radio" checked={payment === "FULL"} onChange={() => setPayment("FULL")} /> <span className="ml-2">Pay in full</span>
              </label>
              <label className="block border border-white/20 p-4 rounded-lg">
                <input type="radio" checked={payment === "INSTALLMENT"} onChange={() => setPayment("INSTALLMENT")} /> <span className="ml-2">Pay in installments</span>
                {payment === "INSTALLMENT" && <span className="block mt-3 text-sm text-zinc-300">Choose a schedule (account required).</span>}
              </label>
              {payment === "INSTALLMENT" && (
                <select value={installments} onChange={e => setInstallments(Number(e.target.value))} className="mt-3 p-3 rounded-lg bg-white text-black">
                  {[2,3,4,6,12].map(count => <option key={count} value={count}>{count} payments · {money(total / count)} each</option>)}
                </select>
              )}
            </fieldset>
            <button disabled={loading} className="w-full py-3 rounded-full bg-amber-600 font-semibold disabled:opacity-50">
              {loading ? "Processing…" : payment === "INSTALLMENT" ? "Create installment plan" : "Place order"}
            </button>
          </form>
          <aside className="bg-white/5 border border-white/10 rounded-xl p-5 h-fit">
            <h2 className="font-semibold mb-4">Order summary</h2>
            {items.map(item => (
              <div key={item.variantId} className="flex justify-between gap-4 py-3 border-b border-white/10 text-sm">
                <span>{item.name} × {item.quantity}<button type="button" onClick={() => removeItem(item.variantId)} className="block text-xs text-amber-400 mt-1">Remove</button></span>
                <span>{money(item.price * item.quantity)}</span>
              </div>
            ))}
            <div className="flex justify-between font-bold pt-4">
              <span>Total</span>
              <span>{money(total)}</span>
            </div>
          </aside>
        </div>
      )}
    </div>
  );
}
