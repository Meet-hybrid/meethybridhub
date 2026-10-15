"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { ArrowLeft, Package, Clock, CheckCircle, XCircle, Truck } from "lucide-react";
import { useStore } from "@/components/StoreProvider";
import { useAuth } from "@/components/AuthProvider";
import { storefrontApi } from "@/lib/api";

interface OrderItem {
  id: number;
  variantId: number;
  sku: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
}

interface Order {
  id: number;
  orderNumber: string;
  status: string;
  totalAmount: number;
  customerEmail: string;
  shippingAddress: string;
  billingAddress: string | null;
  notes: string | null;
  items: OrderItem[];
  createdAt: string;
}

const statusConfig: Record<string, { label: string; icon: any; description: string }> = {
  PENDING: { label: "Pending", icon: Clock, description: "Your order has been received and is awaiting confirmation." },
  CONFIRMED: { label: "Confirmed", icon: CheckCircle, description: "Your order has been confirmed and is being prepared." },
  PROCESSING: { label: "Processing", icon: Package, description: "Your order is currently being processed." },
  SHIPPED: { label: "Shipped", icon: Truck, description: "Your order is on its way to you." },
  DELIVERED: { label: "Delivered", icon: CheckCircle, description: "Your order has been delivered." },
  CANCELLED: { label: "Cancelled", icon: XCircle, description: "This order has been cancelled." },
};

export default function OrderDetailPage() {
  const params = useParams();
  const activeStore = useStore();
  const { isAuthenticated, isLoading: authLoading } = useAuth();
  const router = useRouter();
  const isDivinez = activeStore.slug === "divinez-signature";

  const [order, setOrder] = useState<Order | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!authLoading && !isAuthenticated) {
      router.push("/login");
    }
  }, [isAuthenticated, authLoading, router]);

  useEffect(() => {
    if (!isAuthenticated || !params.id) return;
    setLoading(true);
    storefrontApi
      .getOrder(Number(params.id))
      .then(setOrder)
      .catch((err) => setError(err.message || "Failed to load order"))
      .finally(() => setLoading(false));
  }, [isAuthenticated, params.id]);

  if (authLoading || !isAuthenticated) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center">
        <div className={`animate-pulse text-sm ${isDivinez ? "text-[#0B4A2B]/50" : "text-[#aa9a8b]"}`}>Loading…</div>
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
      {/* Back link */}
      <Link
        href="/account/orders"
        className={`inline-flex items-center gap-1.5 text-sm font-medium mb-8 transition-colors ${
          isDivinez ? "text-[#0B4A2B]/60 hover:text-[#0B4A2B]" : "text-[#aa9a8b] hover:text-[#f7f1e8]"
        }`}
      >
        <ArrowLeft className="w-4 h-4" />
        Back to orders
      </Link>

      {/* Loading */}
      {loading && (
        <div className={`animate-pulse rounded-2xl p-8 ${isDivinez ? "bg-white border border-[#0B4A2B]/10" : "bg-[#17110d] border border-[#3a2b20]"}`}>
          <div className={`h-6 w-40 rounded mb-4 ${isDivinez ? "bg-[#0B4A2B]/10" : "bg-[#3a2b20]"}`} />
          <div className={`h-4 w-64 rounded mb-2 ${isDivinez ? "bg-[#0B4A2B]/5" : "bg-[#3a2b20]/60"}`} />
          <div className={`h-4 w-32 rounded ${isDivinez ? "bg-[#0B4A2B]/5" : "bg-[#3a2b20]/60"}`} />
        </div>
      )}

      {/* Error */}
      {error && (
        <div className={`px-4 py-3 rounded-lg text-sm ${isDivinez ? "bg-red-50 text-red-700" : "bg-red-900/20 text-red-300"}`}>
          {error}
        </div>
      )}

      {/* Order details */}
      {order && (
        <div className="space-y-6">
          {/* Order header */}
          <div className={`rounded-2xl p-6 ${isDivinez ? "bg-white border border-[#0B4A2B]/10" : "bg-[#17110d] border border-[#3a2b20]"}`}>
            <div className="flex items-start justify-between mb-4">
              <div>
                <h1 className={`text-xl font-serif font-bold ${isDivinez ? "text-[#0B4A2B]" : "text-[#f7f1e8]"}`}>
                  Order #{order.orderNumber}
                </h1>
                <p className={`text-sm mt-1 ${isDivinez ? "text-[#0B4A2B]/60" : "text-[#aa9a8b]"}`}>
                  Placed on {new Date(order.createdAt).toLocaleDateString("en-NG", { year: "numeric", month: "long", day: "numeric" })}
                </p>
              </div>
              <span className={`text-lg font-bold ${isDivinez ? "text-[#0B4A2B]" : "text-[#f7f1e8]"}`}>
                ₦{order.totalAmount.toLocaleString()}
              </span>
            </div>

            {/* Status */}
            {(() => {
              const status = statusConfig[order.status] || statusConfig.PENDING;
              const StatusIcon = status.icon;
              return (
                <div className={`flex items-start gap-3 p-4 rounded-xl ${
                  isDivinez ? "bg-[#FAFAF8]" : "bg-[#100d0b]"
                }`}>
                  <StatusIcon className={`w-5 h-5 mt-0.5 flex-shrink-0 ${
                    order.status === "CANCELLED"
                      ? isDivinez ? "text-red-500" : "text-red-400"
                      : order.status === "DELIVERED"
                        ? isDivinez ? "text-green-600" : "text-green-400"
                        : isDivinez ? "text-[#0B4A2B]" : "text-[#e0a15d]"
                  }`} />
                  <div>
                    <p className={`text-sm font-medium ${isDivinez ? "text-[#0B4A2B]" : "text-[#f7f1e8]"}`}>
                      {status.label}
                    </p>
                    <p className={`text-xs mt-0.5 ${isDivinez ? "text-[#0B4A2B]/60" : "text-[#aa9a8b]"}`}>
                      {status.description}
                    </p>
                  </div>
                </div>
              );
            })()}
          </div>

          {/* Items */}
          <div className={`rounded-2xl p-6 ${isDivinez ? "bg-white border border-[#0B4A2B]/10" : "bg-[#17110d] border border-[#3a2b20]"}`}>
            <h2 className={`text-sm font-semibold mb-4 ${isDivinez ? "text-[#0B4A2B]" : "text-[#f7f1e8]"}`}>
              Items
            </h2>
            <div className="space-y-4">
              {order.items.map((item) => (
                <div key={item.id} className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className={`w-10 h-10 rounded-lg flex items-center justify-center text-xs font-bold ${
                      isDivinez ? "bg-[#0B4A2B]/10 text-[#0B4A2B]/60" : "bg-[#3a2b20] text-[#aa9a8b]"
                    }`}>
                      {item.quantity}×
                    </div>
                    <div>
                      <p className={`text-sm font-medium ${isDivinez ? "text-[#0B4A2B]" : "text-[#f7f1e8]"}`}>
                        {item.productName}
                      </p>
                      <p className={`text-xs ${isDivinez ? "text-[#0B4A2B]/50" : "text-[#aa9a8b]/60"}`}>
                        {item.sku}
                      </p>
                    </div>
                  </div>
                  <span className={`text-sm font-medium ${isDivinez ? "text-[#0B4A2B]" : "text-[#f7f1e8]"}`}>
                    ₦{item.totalPrice.toLocaleString()}
                  </span>
                </div>
              ))}
            </div>
          </div>

          {/* Shipping */}
          <div className={`rounded-2xl p-6 ${isDivinez ? "bg-white border border-[#0B4A2B]/10" : "bg-[#17110d] border border-[#3a2b20]"}`}>
            <h2 className={`text-sm font-semibold mb-3 ${isDivinez ? "text-[#0B4A2B]" : "text-[#f7f1e8]"}`}>
              Shipping address
            </h2>
            <p className={`text-sm leading-relaxed ${isDivinez ? "text-[#0B4A2B]/70" : "text-[#aa9a8b]"}`}>
              {order.shippingAddress}
            </p>
          </div>
        </div>
      )}
    </div>
  );
}
