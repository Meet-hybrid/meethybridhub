"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Package, ChevronRight, LogOut, Clock, CheckCircle, XCircle, Truck } from "lucide-react";
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
  storeId: number;
  customerId: number;
  orderNumber: string;
  status: string;
  totalAmount: number;
  customerEmail: string;
  shippingAddress: string;
  items: OrderItem[];
  createdAt: string;
}

const statusConfig: Record<string, { label: string; icon: any; color: string; divinezColor: string }> = {
  PENDING: { label: "Pending", icon: Clock, color: "text-amber-400 bg-amber-400/10", divinezColor: "text-amber-600 bg-amber-50" },
  CONFIRMED: { label: "Confirmed", icon: CheckCircle, color: "text-blue-400 bg-blue-400/10", divinezColor: "text-blue-600 bg-blue-50" },
  PROCESSING: { label: "Processing", icon: Package, color: "text-purple-400 bg-purple-400/10", divinezColor: "text-purple-600 bg-purple-50" },
  SHIPPED: { label: "Shipped", icon: Truck, color: "text-cyan-400 bg-cyan-400/10", divinezColor: "text-cyan-600 bg-cyan-50" },
  DELIVERED: { label: "Delivered", icon: CheckCircle, color: "text-green-400 bg-green-400/10", divinezColor: "text-green-600 bg-green-50" },
  CANCELLED: { label: "Cancelled", icon: XCircle, color: "text-red-400 bg-red-400/10", divinezColor: "text-red-600 bg-red-50" },
};

export default function OrdersPage() {
  const activeStore = useStore();
  const { isAuthenticated, isLoading: authLoading, logout } = useAuth();
  const router = useRouter();
  const isDivinez = activeStore.slug === "divinez-signature";

  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!authLoading && !isAuthenticated) {
      router.push("/login");
    }
  }, [isAuthenticated, authLoading, router]);

  useEffect(() => {
    if (!isAuthenticated) return;
    setLoading(true);
    storefrontApi
      .getOrders(0, 20)
      .then((res) => setOrders(res.content || []))
      .catch((err) => setError(err.message || "Failed to load orders"))
      .finally(() => setLoading(false));
  }, [isAuthenticated]);

  const handleLogout = async () => {
    await logout();
    router.push("/");
  };

  if (authLoading || !isAuthenticated) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center">
        <div className={`animate-pulse text-sm ${isDivinez ? "text-[#0B4A2B]/50" : "text-[#aa9a8b]"}`}>Loading…</div>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
      {/* Header */}
      <div className="flex items-center justify-between mb-10">
        <div>
          <h1 className={`text-2xl font-serif font-bold ${isDivinez ? "text-[#0B4A2B]" : "text-[#f7f1e8]"}`}>
            My Orders
          </h1>
          <p className={`text-sm mt-1 ${isDivinez ? "text-[#0B4A2B]/60" : "text-[#aa9a8b]"}`}>
            Track your recent purchases
          </p>
        </div>
        <button
          onClick={handleLogout}
          className={`flex items-center gap-2 text-sm font-medium px-4 py-2 rounded-full transition-colors ${
            isDivinez
              ? "text-[#0B4A2B]/70 hover:text-[#0B4A2B] hover:bg-[#0B4A2B]/5"
              : "text-[#aa9a8b] hover:text-[#f7f1e8] hover:bg-white/5"
          }`}
        >
          <LogOut className="w-4 h-4" />
          Sign out
        </button>
      </div>

      {/* Error */}
      {error && (
        <div className={`mb-6 px-4 py-3 rounded-lg text-sm ${isDivinez ? "bg-red-50 text-red-700" : "bg-red-900/20 text-red-300"}`}>
          {error}
        </div>
      )}

      {/* Loading */}
      {loading && (
        <div className="space-y-4">
          {[1, 2, 3].map((i) => (
            <div
              key={i}
              className={`animate-pulse rounded-xl p-6 ${isDivinez ? "bg-white border border-[#0B4A2B]/10" : "bg-[#17110d] border border-[#3a2b20]"}`}
            >
              <div className={`h-4 w-32 rounded mb-3 ${isDivinez ? "bg-[#0B4A2B]/10" : "bg-[#3a2b20]"}`} />
              <div className={`h-3 w-48 rounded mb-2 ${isDivinez ? "bg-[#0B4A2B]/5" : "bg-[#3a2b20]/60"}`} />
              <div className={`h-3 w-24 rounded ${isDivinez ? "bg-[#0B4A2B]/5" : "bg-[#3a2b20]/60"}`} />
            </div>
          ))}
        </div>
      )}

      {/* Empty state */}
      {!loading && orders.length === 0 && !error && (
        <div className={`text-center py-20 rounded-2xl ${isDivinez ? "bg-white border border-[#0B4A2B]/10" : "bg-[#17110d] border border-[#3a2b20]"}`}>
          <Package className={`w-12 h-12 mx-auto mb-4 ${isDivinez ? "text-[#0B4A2B]/30" : "text-[#3a2b20]"}`} />
          <h3 className={`text-lg font-medium mb-2 ${isDivinez ? "text-[#0B4A2B]" : "text-[#f7f1e8]"}`}>
            No orders yet
          </h3>
          <p className={`text-sm mb-6 ${isDivinez ? "text-[#0B4A2B]/60" : "text-[#aa9a8b]"}`}>
            When you place an order, it will appear here.
          </p>
          <Link
            href="/products"
            className={`inline-flex items-center gap-2 px-6 py-3 rounded-full text-sm font-medium transition-colors ${
              isDivinez
                ? "bg-[#0B4A2B] text-[#FAFAF8] hover:bg-[#07331D]"
                : "bg-[#9a6842] text-white hover:bg-[#8a5a36]"
            }`}
          >
            Start shopping
          </Link>
        </div>
      )}

      {/* Orders list */}
      {!loading && orders.length > 0 && (
        <div className="space-y-4">
          {orders.map((order) => {
            const status = statusConfig[order.status] || statusConfig.PENDING;
            const StatusIcon = status.icon;
            return (
              <Link
                key={order.id}
                href={`/account/orders/${order.id}`}
                className={`block rounded-xl p-6 transition-all ${
                  isDivinez
                    ? "bg-white border border-[#0B4A2B]/10 hover:border-[#0B4A2B]/25 hover:shadow-sm"
                    : "bg-[#17110d] border border-[#3a2b20] hover:border-[#4a3b30]"
                }`}
              >
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <div className="flex items-center gap-3 mb-2">
                      <span className={`text-sm font-semibold ${isDivinez ? "text-[#0B4A2B]" : "text-[#f7f1e8]"}`}>
                        #{order.orderNumber}
                      </span>
                      <span className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium ${
                        isDivinez ? status.divinezColor : status.color
                      }`}>
                        <StatusIcon className="w-3 h-3" />
                        {status.label}
                      </span>
                    </div>
                    <p className={`text-sm mb-1 ${isDivinez ? "text-[#0B4A2B]/60" : "text-[#aa9a8b]"}`}>
                      {order.items.length} item{order.items.length !== 1 ? "s" : ""} · {new Date(order.createdAt).toLocaleDateString("en-NG", { year: "numeric", month: "short", day: "numeric" })}
                    </p>
                    <div className={`flex flex-wrap gap-1 mt-2 ${isDivinez ? "text-[#0B4A2B]/50" : "text-[#aa9a8b]/60"}`}>
                      {order.items.slice(0, 3).map((item) => (
                        <span key={item.id} className="text-xs">
                          {item.productName}{item.quantity > 1 ? ` ×${item.quantity}` : ""}
                        </span>
                      ))}
                      {order.items.length > 3 && (
                        <span className="text-xs">+{order.items.length - 3} more</span>
                      )}
                    </div>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className={`text-sm font-semibold ${isDivinez ? "text-[#0B4A2B]" : "text-[#f7f1e8]"}`}>
                      ₦{order.totalAmount.toLocaleString()}
                    </span>
                    <ChevronRight className={`w-4 h-4 ${isDivinez ? "text-[#0B4A2B]/30" : "text-[#3a2b20]"}`} />
                  </div>
                </div>
              </Link>
            );
          })}
        </div>
      )}
    </div>
  );
}
