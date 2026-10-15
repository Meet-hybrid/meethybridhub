"use client";

import { useState } from "react";
import { ArrowRight, CheckCircle, MessageSquare, DollarSign, Clock, Send } from "lucide-react";
import { storefrontApi } from "@/lib/api";
import { useStore } from "@/components/StoreProvider";

export default function CustomOrderPage() {
  const activeStore = useStore();
  const isDivinez = activeStore.slug === "divinez-signature";
  const [submitted, setSubmitted] = useState(false);
  const [loading, setLoading] = useState(false);
  const [form, setForm] = useState({
    title: "",
    description: "",
    budgetMin: "",
    budgetMax: "",
  });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      await storefrontApi.submitCustomOrder({
        title: form.title,
        description: form.description,
        budgetMin: form.budgetMin ? Number(form.budgetMin) : undefined,
        budgetMax: form.budgetMax ? Number(form.budgetMax) : undefined,
      });
      setSubmitted(true);
    } catch {
      setSubmitted(true);
    } finally {
      setLoading(false);
    }
  };

  const formatPrice = (price: number) =>
    new Intl.NumberFormat("en-NG", { style: "currency", currency: "NGN" }).format(price);

  if (submitted) {
    return (
      <div className={`max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-16 text-center ${isDivinez ? "text-[#0B4A2B]" : "text-gray-900"}`}>
        <div className={`w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-6 ${isDivinez ? "bg-[#0B4A2B]/10" : "bg-green-100"}`}>
          <CheckCircle className={`w-8 h-8 ${isDivinez ? "text-[#0B4A2B]" : "text-green-600"}`} />
        </div>
        <h1 className="text-3xl font-bold mb-4">Request Submitted!</h1>
        <p className={`mb-8 ${isDivinez ? "text-[#0B4A2B]/70" : "text-gray-600"}`}>
          Your custom order request has been sent to our designers. You&apos;ll receive a quote
          and can discuss the details through our messaging system.
        </p>
        <div className={`rounded-2xl p-6 text-left mb-8 ${isDivinez ? "bg-[#0B4A2B]/5 border border-[#0B4A2B]/10" : "bg-gray-50"}`}>
          <h3 className="font-semibold mb-3">What happens next?</h3>
          <div className="space-y-3">
            {[
              { icon: Clock, text: "A designer reviews your request within 24 hours" },
              { icon: DollarSign, text: "You receive a detailed quote with pricing" },
              { icon: MessageSquare, text: "Discuss details and finalize the design" },
              { icon: ArrowRight, text: "Once approved, production begins" },
            ].map((step, i) => (
              <div key={i} className="flex items-center gap-3">
                <div className={`w-8 h-8 rounded-full flex items-center justify-center shrink-0 ${isDivinez ? "bg-[#0B4A2B]/10" : "bg-amber-100"}`}>
                  <step.icon className={`w-4 h-4 ${isDivinez ? "text-[#0B4A2B]" : "text-amber-600"}`} />
                </div>
                <span className={`text-sm ${isDivinez ? "text-[#0B4A2B]/70" : "text-gray-600"}`}>{step.text}</span>
              </div>
            ))}
          </div>
        </div>
        <a
          href="/products"
          className={`inline-flex items-center gap-2 px-6 py-3 font-semibold rounded-full transition-colors ${
            isDivinez
              ? "bg-[#0B4A2B] text-[#FAFAF8] hover:bg-[#07331D]"
              : "bg-amber-600 text-white hover:bg-amber-700"
          }`}
        >
          Continue Shopping
        </a>
      </div>
    );
  }

  if (isDivinez) {
    return (
      <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-12 text-[#0B4A2B]">
        {/* Header */}
        <div className="text-center mb-12">
          <span className="text-[#0B4A2B]/70 uppercase tracking-widest text-xs font-semibold">Bespoke Service</span>
          <h1 className="text-3xl font-serif font-bold text-[#0B4A2B] mt-1">Custom Order Request</h1>
          <p className="text-[#0B4A2B]/70 mt-3 max-w-lg mx-auto">
            Describe your dream piece and our artisans will bring it to life.
            Get a personalized quote within 24 hours.
          </p>
        </div>

        {/* How it works */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-12">
          {[
            { icon: MessageSquare, title: "1. Describe", desc: "Tell us what you want" },
            { icon: DollarSign, title: "2. Get Quote", desc: "Receive pricing & timeline" },
            { icon: CheckCircle, title: "3. Create", desc: "We bring it to life" },
          ].map((step) => (
            <div key={step.title} className="text-center p-4">
              <div className="w-12 h-12 bg-[#0B4A2B]/10 rounded-full flex items-center justify-center mx-auto mb-3">
                <step.icon className="w-6 h-6 text-[#0B4A2B]" />
              </div>
              <h3 className="font-semibold text-sm">{step.title}</h3>
              <p className="text-xs text-[#0B4A2B]/60 mt-1">{step.desc}</p>
            </div>
          ))}
        </div>

        {/* Form */}
        <div className="bg-white rounded-2xl border border-[#0B4A2B]/10 p-8 shadow-sm">
          <form onSubmit={handleSubmit} className="space-y-6">
            <div>
              <label className="block text-sm font-medium mb-1">
                What do you want? <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                required
                value={form.title}
                onChange={(e) => setForm({ ...form, title: e.target.value })}
                className="w-full px-4 py-3 border border-[#0B4A2B]/20 rounded-xl text-sm focus:ring-2 focus:ring-[#0B4A2B]/20 focus:border-[#0B4A2B]/40 outline-none"
                placeholder="e.g., Custom crystal bracelet with emerald beads"
              />
            </div>

            <div>
              <label className="block text-sm font-medium mb-1">
                Description <span className="text-red-500">*</span>
              </label>
              <textarea
                required
                rows={5}
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
                className="w-full px-4 py-3 border border-[#0B4A2B]/20 rounded-xl text-sm focus:ring-2 focus:ring-[#0B4A2B]/20 focus:border-[#0B4A2B]/40 outline-none resize-none"
                placeholder="Describe the design in detail — bead colors, patterns, length, size, any reference images you'd like to share..."
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium mb-1">
                  Minimum Budget (₦)
                </label>
                <input
                  type="number"
                  min="0"
                  value={form.budgetMin}
                  onChange={(e) => setForm({ ...form, budgetMin: e.target.value })}
                  className="w-full px-4 py-3 border border-[#0B4A2B]/20 rounded-xl text-sm focus:ring-2 focus:ring-[#0B4A2B]/20 focus:border-[#0B4A2B]/40 outline-none"
                  placeholder="10,000"
                />
              </div>
              <div>
                <label className="block text-sm font-medium mb-1">
                  Maximum Budget (₦)
                </label>
                <input
                  type="number"
                  min="0"
                  value={form.budgetMax}
                  onChange={(e) => setForm({ ...form, budgetMax: e.target.value })}
                  className="w-full px-4 py-3 border border-[#0B4A2B]/20 rounded-xl text-sm focus:ring-2 focus:ring-[#0B4A2B]/20 focus:border-[#0B4A2B]/40 outline-none"
                  placeholder="50,000"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full flex items-center justify-center gap-2 px-6 py-3.5 bg-[#0B4A2B] text-[#FAFAF8] font-semibold rounded-full hover:bg-[#07331D] transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? (
                <>
                  <div className="animate-spin w-4 h-4 border-2 border-white border-t-transparent rounded-full" />
                  Submitting...
                </>
              ) : (
                <>
                  <Send className="w-4 h-4" />
                  Submit Request
                </>
              )}
            </button>
          </form>
        </div>
      </div>
    );
  }

  // MeethybridHub (default gray/amber theme)
  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
      {/* Header */}
      <div className="text-center mb-12">
        <h1 className="text-3xl font-bold text-gray-900">Custom Order Request</h1>
        <p className="text-gray-500 mt-3 max-w-lg mx-auto">
          Describe your dream outfit and our designers will bring it to life.
          Get a personalized quote within 24 hours.
        </p>
      </div>

      {/* How it works */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-12">
        {[
          { icon: MessageSquare, title: "1. Describe", desc: "Tell us what you want" },
          { icon: DollarSign, title: "2. Get Quote", desc: "Receive pricing & timeline" },
          { icon: CheckCircle, title: "3. Create", desc: "We bring it to life" },
        ].map((step) => (
          <div key={step.title} className="text-center p-4">
            <div className="w-12 h-12 bg-amber-100 rounded-full flex items-center justify-center mx-auto mb-3">
              <step.icon className="w-6 h-6 text-amber-600" />
            </div>
            <h3 className="font-semibold text-gray-900 text-sm">{step.title}</h3>
            <p className="text-xs text-gray-500 mt-1">{step.desc}</p>
          </div>
        ))}
      </div>

      {/* Form */}
      <div className="bg-white rounded-2xl border border-gray-200 p-8 shadow-sm">
        <form onSubmit={handleSubmit} className="space-y-6">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              What do you want? <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              required
              value={form.title}
              onChange={(e) => setForm({ ...form, title: e.target.value })}
              className="w-full px-4 py-3 border border-gray-300 rounded-xl text-sm focus:ring-2 focus:ring-amber-500 focus:border-amber-500 outline-none"
              placeholder="e.g., Custom wedding gown with lace details"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Description <span className="text-red-500">*</span>
            </label>
            <textarea
              required
              rows={5}
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
              className="w-full px-4 py-3 border border-gray-300 rounded-xl text-sm focus:ring-2 focus:ring-amber-500 focus:border-amber-500 outline-none resize-none"
              placeholder="Describe the design in detail — fabric preferences, colors, patterns, measurements, any reference images you'd like to share..."
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Minimum Budget (₦)
              </label>
              <input
                type="number"
                min="0"
                value={form.budgetMin}
                onChange={(e) => setForm({ ...form, budgetMin: e.target.value })}
                className="w-full px-4 py-3 border border-gray-300 rounded-xl text-sm focus:ring-2 focus:ring-amber-500 focus:border-amber-500 outline-none"
                placeholder="10,000"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Maximum Budget (₦)
              </label>
              <input
                type="number"
                min="0"
                value={form.budgetMax}
                onChange={(e) => setForm({ ...form, budgetMax: e.target.value })}
                className="w-full px-4 py-3 border border-gray-300 rounded-xl text-sm focus:ring-2 focus:ring-amber-500 focus:border-amber-500 outline-none"
                placeholder="50,000"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full flex items-center justify-center gap-2 px-6 py-3.5 bg-amber-600 text-white font-semibold rounded-full hover:bg-amber-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {loading ? (
              <>
                <div className="animate-spin w-4 h-4 border-2 border-white border-t-transparent rounded-full" />
                Submitting...
              </>
            ) : (
              <>
                <Send className="w-4 h-4" />
                Submit Request
              </>
            )}
          </button>
        </form>
      </div>
    </div>
  );
}
