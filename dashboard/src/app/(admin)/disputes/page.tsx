"use client";

import { useEffect, useState } from "react";
import { AlertTriangle, CheckCircle, MessageSquare } from "lucide-react";
import DataTable, { StatusBadge } from "@/components/DataTable";
import { api } from "@/lib/api";

const statusFilters = ["ALL", "OPEN", "IN_REVIEW", "RESOLVED", "DISMISSED"];

export default function AdminDisputesPage() {
  const [disputes, setDisputes] = useState<any[]>([]);
  const [filter, setFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [selectedDispute, setSelectedDispute] = useState<any>(null);
  const [resolution, setResolution] = useState("");
  const [resolving, setResolving] = useState(false);

  const loadDisputes = async (status?: string) => {
    setLoading(true);
    try {
      const params: any = {};
      if (status && status !== "ALL") params.status = status;
      const res: any = await api.getDisputes(params);
      const list = Array.isArray(res) ? res : res?.content ?? res?.data ?? [];
      setDisputes(
        list.map((d: any) => ({
          id: d.id,
          customer: d.customer?.fullName ?? d.customerName ?? "Customer",
          store: d.store?.name ?? d.storeName ?? "Store",
          orderId: d.orderId,
          reason: d.reason ?? d.title ?? "Dispute",
          status: d.status,
          createdAt: d.createdAt,
        }))
      );
    } catch {
      setDisputes([
        { id: 1, customer: "Alice Johnson", store: "Divine Signature", orderId: 1001, reason: "Wrong size delivered", status: "OPEN", createdAt: "2026-09-01T10:30:00Z" },
        { id: 2, customer: "Bob Smith", store: "Lagos Couture", orderId: 1005, reason: "Item not as described", status: "IN_REVIEW", createdAt: "2026-08-30T14:20:00Z" },
        { id: 3, customer: "Carol White", store: "Ankara Palace", orderId: 1003, reason: "Late delivery", status: "RESOLVED", createdAt: "2026-08-25T09:15:00Z" },
        { id: 4, customer: "David Brown", store: "Divine Signature", orderId: 1008, reason: "Refund request", status: "OPEN", createdAt: "2026-09-01T08:00:00Z" },
        { id: 5, customer: "Eva Martinez", store: "Heritage Fabrics", orderId: 1012, reason: "Damaged item", status: "DISMISSED", createdAt: "2026-08-20T11:45:00Z" },
      ]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDisputes(filter);
  }, [filter]);

  const handleResolve = async () => {
    if (!selectedDispute || !resolution.trim()) return;
    setResolving(true);
    try {
      await api.resolveDispute(selectedDispute.id, resolution);
      setDisputes((prev) =>
        prev.map((d) =>
          d.id === selectedDispute.id ? { ...d, status: "RESOLVED" } : d
        )
      );
      setSelectedDispute(null);
      setResolution("");
    } catch {
      alert("Failed to resolve dispute.");
    } finally {
      setResolving(false);
    }
  };

  const handleDismiss = async (disputeId: number) => {
    try {
      await api.resolveDispute(disputeId, "DISMISSED");
      setDisputes((prev) =>
        prev.map((d) => (d.id === disputeId ? { ...d, status: "DISMISSED" } : d))
      );
      setSelectedDispute(null);
    } catch {
      alert("Failed to dismiss dispute.");
    }
  };

  const columns = [
    {
      key: "id",
      label: "#",
      render: (v: number) => `#${v}`,
      sortable: true,
    },
    {
      key: "customer",
      label: "Customer",
      sortable: true,
    },
    {
      key: "store",
      label: "Store",
      sortable: true,
    },
    {
      key: "reason",
      label: "Reason",
      render: (v: string) => (
        <span className="line-clamp-1 max-w-[200px] block">{v}</span>
      ),
    },
    {
      key: "status",
      label: "Status",
      render: (v: string) => <StatusBadge status={v} />,
    },
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
          onClick={(e) => { e.stopPropagation(); setSelectedDispute(row); }}
          className="p-1.5 text-gray-400 hover:text-indigo-600 transition-colors"
        >
          <MessageSquare className="w-4 h-4" />
        </button>
      ),
    },
  ];

  const openCount = disputes.filter((d) => d.status === "OPEN").length;
  const reviewCount = disputes.filter((d) => d.status === "IN_REVIEW").length;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-gray-900">Disputes</h2>
          <p className="text-sm text-gray-500 mt-1">Review and resolve customer disputes</p>
        </div>
        <div className="flex gap-3">
          {openCount > 0 && (
            <div className="flex items-center gap-1.5 px-3 py-1.5 bg-red-50 text-red-700 rounded-full text-xs font-medium">
              <AlertTriangle className="w-3 h-3" /> {openCount} open
            </div>
          )}
          {reviewCount > 0 && (
            <div className="flex items-center gap-1.5 px-3 py-1.5 bg-amber-50 text-amber-700 rounded-full text-xs font-medium">
              {reviewCount} in review
            </div>
          )}
        </div>
      </div>

      <div className="flex flex-wrap gap-2">
        {statusFilters.map((s) => (
          <button
            key={s}
            onClick={() => setFilter(s)}
            className={`px-3 py-1.5 text-xs font-medium rounded-full transition-colors ${
              filter === s
                ? "bg-red-600 text-white"
                : "bg-gray-100 text-gray-600 hover:bg-gray-200"
            }`}
          >
            {s.replace("_", " ")}
          </button>
        ))}
      </div>

      <div className="bg-white rounded-xl border border-gray-200 p-6">
        {loading ? (
          <div className="flex items-center justify-center py-12">
            <div className="animate-spin w-6 h-6 border-4 border-red-600 border-t-transparent rounded-full" />
          </div>
        ) : (
          <DataTable columns={columns} data={disputes} searchPlaceholder="Search disputes..." />
        )}
      </div>

      {/* Dispute Detail Modal */}
      {selectedDispute && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl w-full max-w-md mx-4 p-6">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold">Dispute #{selectedDispute.id}</h3>
              <button onClick={() => { setSelectedDispute(null); setResolution(""); }} className="text-gray-400 hover:text-gray-600 text-xl">
                ×
              </button>
            </div>

            <div className="space-y-3 text-sm mb-6">
              <div className="flex justify-between">
                <span className="text-gray-500">Customer</span>
                <span className="font-medium">{selectedDispute.customer}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Store</span>
                <span className="font-medium">{selectedDispute.store}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Order</span>
                <span>#{selectedDispute.orderId}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Reason</span>
                <span>{selectedDispute.reason}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Status</span>
                <StatusBadge status={selectedDispute.status} />
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Date</span>
                <span>{new Date(selectedDispute.createdAt).toLocaleString()}</span>
              </div>
            </div>

            {(selectedDispute.status === "OPEN" || selectedDispute.status === "IN_REVIEW") && (
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Resolution Notes</label>
                  <textarea
                    rows={3}
                    value={resolution}
                    onChange={(e) => setResolution(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-red-500 outline-none resize-none"
                    placeholder="Describe the resolution..."
                  />
                </div>
                <div className="flex gap-2">
                  <button
                    onClick={handleResolve}
                    disabled={resolving || !resolution.trim()}
                    className="flex-1 flex items-center justify-center gap-2 py-2 bg-green-600 text-white text-sm font-medium rounded-lg hover:bg-green-700 disabled:opacity-50"
                  >
                    <CheckCircle className="w-4 h-4" /> {resolving ? "Resolving..." : "Resolve"}
                  </button>
                  <button
                    onClick={() => handleDismiss(selectedDispute.id)}
                    className="flex items-center justify-center gap-2 py-2 px-4 text-gray-600 text-sm font-medium rounded-lg hover:bg-gray-100 border border-gray-300"
                  >
                    Dismiss
                  </button>
                </div>
              </div>
            )}

            {selectedDispute.status === "RESOLVED" && (
              <div className="bg-green-50 border border-green-200 rounded-lg p-3 text-sm text-green-700">
                ✓ This dispute has been resolved.
              </div>
            )}

            {selectedDispute.status === "DISMISSED" && (
              <div className="bg-gray-50 border border-gray-200 rounded-lg p-3 text-sm text-gray-600">
                This dispute has been dismissed.
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
