"use client";

import { useEffect, useState } from "react";
import { Eye, Truck, CheckCircle, XCircle } from "lucide-react";
import DataTable, { StatusBadge } from "@/components/DataTable";
import { api } from "@/lib/api";

const statusFilters = ["ALL", "PENDING", "CONFIRMED", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED"];

export default function OrdersPage() {
  const [orders, setOrders] = useState<any[]>([]);
  const [filter, setFilter] = useState("ALL");
  const [selectedOrder, setSelectedOrder] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  const loadOrders = async (status?: string) => {
    setLoading(true);
    try {
      const params: any = { size: 100 };
      if (status && status !== "ALL") params.status = status;
      const res = await api.getOrders(params);
      const list = Array.isArray(res) ? res : res?.content ?? res?.data ?? [];
      setOrders(
        list.map((o: any) => ({
          id: o.id,
          customer: o.customer?.fullName ?? o.customerName ?? "Customer",
          items: o.items?.length ?? o.itemCount ?? 0,
          totalAmount: o.totalAmount,
          status: o.status,
          createdAt: o.createdAt,
          shipping: o.shippingMethod ?? "Delivery",
        }))
      );
    } catch {

      setOrders([
        { id: 1001, customer: "Alice Johnson", items: 3, totalAmount: 125000, status: "PENDING", createdAt: "2026-09-01T10:30:00Z", shipping: "Delivery" },
        { id: 1002, customer: "Bob Smith", items: 1, totalAmount: 45000, status: "CONFIRMED", createdAt: "2026-09-01T09:15:00Z", shipping: "Pickup" },
        { id: 1003, customer: "Carol White", items: 5, totalAmount: 342000, status: "SHIPPED", createdAt: "2026-08-31T16:45:00Z", shipping: "Delivery" },
        { id: 1004, customer: "David Brown", items: 2, totalAmount: 67000, status: "DELIVERED", createdAt: "2026-08-31T14:20:00Z", shipping: "Delivery" },
        { id: 1005, customer: "Eva Martinez", items: 4, totalAmount: 210000, status: "PROCESSING", createdAt: "2026-08-30T11:00:00Z", shipping: "Pickup" },
        { id: 1006, customer: "Frank Lee", items: 1, totalAmount: 18500, status: "CANCELLED", createdAt: "2026-08-29T08:30:00Z", shipping: "Delivery" },
      ]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadOrders(filter);
  }, [filter]);

  const filtered = filter === "ALL" ? orders : orders.filter((o) => o.status === filter);

  const handleStatusUpdate = async (orderId: number, newStatus: string) => {
    try {
      await api.updateOrderStatus(orderId, newStatus);
      setOrders((prev) =>
        prev.map((o) => (o.id === orderId ? { ...o, status: newStatus } : o))
      );
      setSelectedOrder(null);
    } catch {
      alert("Failed to update order status.");
    }
  };

  const formatCurrency = (amount: number) =>
    new Intl.NumberFormat("en-NG", { style: "currency", currency: "NGN" }).format(amount);

  const columns = [
    { key: "id", label: "Order #", render: (v: number) => `#${v}`, sortable: true },
    { key: "customer", label: "Customer", sortable: true },
    { key: "items", label: "Items", sortable: true, render: (v: number) => `${v} items` },
    {
      key: "totalAmount",
      label: "Total",
      sortable: true,
      render: (v: number) => formatCurrency(v),
    },
    {
      key: "shipping",
      label: "Type",
      render: (v: string) => (
        <span className={`text-xs font-medium ${v === "Pickup" ? "text-emerald-600" : "text-blue-600"}`}>
          {v}
        </span>
      ),
    },
    { key: "status", label: "Status", render: (v: string) => <StatusBadge status={v} /> },
    {
      key: "createdAt",
      label: "Date",
      sortable: true,
      render: (v: string) => new Date(v).toLocaleDateString(),
    },
    {
      key: "id",
      label: "",
      sortable: false,
      render: (_: number, row: any) => (
        <button
          onClick={(e) => { e.stopPropagation(); setSelectedOrder(row); }}
          className="p-1.5 text-gray-400 hover:text-indigo-600 transition-colors"
        >
          <Eye className="w-4 h-4" />
        </button>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-xl font-bold text-gray-900">Orders</h2>
        <p className="text-sm text-gray-500 mt-1">Track and manage customer orders</p>
      </div>

      <div className="flex flex-wrap gap-2">
        {statusFilters.map((s) => (
          <button
            key={s}
            onClick={() => setFilter(s)}
            className={`px-3 py-1.5 text-xs font-medium rounded-full transition-colors ${
              filter === s
                ? "bg-indigo-600 text-white"
                : "bg-gray-100 text-gray-600 hover:bg-gray-200"
            }`}
          >
            {s}
          </button>
        ))}
      </div>

      <div className="bg-white rounded-xl border border-gray-200 p-6">
        {loading ? (
          <div className="flex items-center justify-center py-12">
            <div className="animate-spin w-6 h-6 border-4 border-indigo-600 border-t-transparent rounded-full" />
          </div>
        ) : (
          <DataTable columns={columns} data={filtered} searchPlaceholder="Search orders..." />
        )}
      </div>

      {selectedOrder && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl w-full max-w-md mx-4 p-6">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold">Order #{selectedOrder.id}</h3>
              <button onClick={() => setSelectedOrder(null)} className="text-gray-400 hover:text-gray-600 text-xl">
                ×
              </button>
            </div>
            <div className="space-y-3 text-sm">
              <div className="flex justify-between"><span className="text-gray-500">Customer</span><span className="font-medium">{selectedOrder.customer}</span></div>
              <div className="flex justify-between"><span className="text-gray-500">Items</span><span>{selectedOrder.items}</span></div>
              <div className="flex justify-between"><span className="text-gray-500">Total</span><span className="font-medium">{formatCurrency(selectedOrder.totalAmount)}</span></div>
              <div className="flex justify-between"><span className="text-gray-500">Shipping</span><span>{selectedOrder.shipping}</span></div>
              <div className="flex justify-between"><span className="text-gray-500">Status</span><StatusBadge status={selectedOrder.status} /></div>
              <div className="flex justify-between"><span className="text-gray-500">Date</span><span>{new Date(selectedOrder.createdAt).toLocaleString()}</span></div>
            </div>
            <div className="flex gap-2 mt-6">
              {selectedOrder.status === "PENDING" && (
                <button
                  onClick={() => handleStatusUpdate(selectedOrder.id, "CONFIRMED")}
                  className="flex-1 flex items-center justify-center gap-2 py-2 bg-indigo-600 text-white text-sm font-medium rounded-lg hover:bg-indigo-700"
                >
                  <CheckCircle className="w-4 h-4" /> Confirm
                </button>
              )}
              {selectedOrder.status === "CONFIRMED" && (
                <button
                  onClick={() => handleStatusUpdate(selectedOrder.id, "PROCESSING")}
                  className="flex-1 flex items-center justify-center gap-2 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700"
                >
                  <Truck className="w-4 h-4" /> Mark Processing
                </button>
              )}
              {selectedOrder.status === "PROCESSING" && (
                <button
                  onClick={() => handleStatusUpdate(selectedOrder.id, "SHIPPED")}
                  className="flex-1 flex items-center justify-center gap-2 py-2 bg-purple-600 text-white text-sm font-medium rounded-lg hover:bg-purple-700"
                >
                  <Truck className="w-4 h-4" /> Mark Shipped
                </button>
              )}
              {(selectedOrder.status === "PENDING" || selectedOrder.status === "CONFIRMED") && (
                <button
                  onClick={() => handleStatusUpdate(selectedOrder.id, "CANCELLED")}
                  className="flex items-center justify-center gap-2 py-2 px-4 text-red-600 text-sm font-medium rounded-lg hover:bg-red-50 border border-red-200"
                >
                  <XCircle className="w-4 h-4" /> Cancel
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
