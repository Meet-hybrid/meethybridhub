"use client";

import { useEffect, useState } from "react";
import { Store, Pause, Play, Ban, Eye } from "lucide-react";
import DataTable, { StatusBadge } from "@/components/DataTable";
import { api } from "@/lib/api";

const statusFilters = ["ALL", "ACTIVE", "PENDING", "SUSPENDED"];

export default function AdminStoresPage() {
  const [stores, setStores] = useState<any[]>([]);
  const [filter, setFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [selectedStore, setSelectedStore] = useState<any>(null);

  const loadStores = async (status?: string) => {
    setLoading(true);
    try {
      const params: any = { size: 100 };
      if (status && status !== "ALL") params.status = status;
      const res: any = await api.getAllStores(params);
      const list = Array.isArray(res) ? res : res?.content ?? res?.data ?? [];
      setStores(
        list.map((s: any) => ({
          id: s.id,
          name: s.name,
          slug: s.slug,
          owner: s.owner?.fullName ?? "Owner",
          status: s.status,
          products: s.productCount ?? 0,
          orders: s.orderCount ?? 0,
          createdAt: s.createdAt,
        }))
      );
    } catch {
      setStores([
        { id: 1, name: "Divine Signature", slug: "divine-signature", owner: "Amara Okafor", status: "ACTIVE", products: 45, orders: 287, createdAt: "2026-01-15T00:00:00Z" },
        { id: 2, name: "Lagos Couture", slug: "lagos-couture", owner: "Tunde Bakare", status: "ACTIVE", products: 32, orders: 234, createdAt: "2026-03-22T00:00:00Z" },
        { id: 3, name: "Ankara Palace", slug: "ankara-palace", owner: "Nneka Eze", status: "ACTIVE", products: 67, orders: 342, createdAt: "2025-11-08T00:00:00Z" },
        { id: 4, name: "Urban Threads", slug: "urban-threads", owner: "Chidi Nwosu", status: "PENDING", products: 12, orders: 0, createdAt: "2026-08-01T00:00:00Z" },
        { id: 5, name: "Heritage Fabrics", slug: "heritage-fabrics", owner: "Folake Williams", status: "SUSPENDED", products: 28, orders: 198, createdAt: "2026-02-14T00:00:00Z" },
      ]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadStores(filter);
  }, [filter]);

  const handleStatusChange = async (storeId: number, newStatus: string) => {
    try {
      await api.updateStoreStatus(storeId, newStatus);
      setStores((prev) =>
        prev.map((s) => (s.id === storeId ? { ...s, status: newStatus } : s))
      );
      setSelectedStore(null);
    } catch {
      alert("Failed to update store status.");
    }
  };

  const columns = [
    {
      key: "name",
      label: "Store",
      sortable: true,
      render: (v: string, row: any) => (
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 bg-indigo-100 rounded-lg flex items-center justify-center">
            <Store className="w-4 h-4 text-indigo-600" />
          </div>
          <div>
            <p className="font-medium text-sm">{v}</p>
            <p className="text-xs text-gray-500">{row.slug}.meethybridhub.com</p>
          </div>
        </div>
      ),
    },
    { key: "owner", label: "Owner", sortable: true },
    { key: "products", label: "Products", sortable: true },
    { key: "orders", label: "Orders", sortable: true },
    {
      key: "status",
      label: "Status",
      render: (v: string) => <StatusBadge status={v} />,
    },
    {
      key: "createdAt",
      label: "Joined",
      sortable: true,
      render: (v: string) => new Date(v).toLocaleDateString(),
    },
    {
      key: "id",
      label: "",
      sortable: false,
      render: (_: number, row: any) => (
        <button
          onClick={(e) => { e.stopPropagation(); setSelectedStore(row); }}
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
        <h2 className="text-xl font-bold text-gray-900">Store Management</h2>
        <p className="text-sm text-gray-500 mt-1">Manage all stores on the platform</p>
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
            {s}
          </button>
        ))}
      </div>

      <div className="bg-white rounded-xl border border-gray-200 p-6">
        {loading ? (
          <div className="flex items-center justify-center py-12">
            <div className="animate-spin w-6 h-6 border-4 border-red-600 border-t-transparent rounded-full" />
          </div>
        ) : (
          <DataTable columns={columns} data={stores} searchPlaceholder="Search stores..." />
        )}
      </div>

      {/* Store Detail Modal */}
      {selectedStore && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl w-full max-w-md mx-4 p-6">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 bg-indigo-100 rounded-lg flex items-center justify-center">
                  <Store className="w-5 h-5 text-indigo-600" />
                </div>
                <div>
                  <h3 className="text-lg font-semibold">{selectedStore.name}</h3>
                  <p className="text-xs text-gray-500">{selectedStore.slug}.meethybridhub.com</p>
                </div>
              </div>
              <button onClick={() => setSelectedStore(null)} className="text-gray-400 hover:text-gray-600 text-xl">
                ×
              </button>
            </div>

            <div className="space-y-3 text-sm mb-6">
              <div className="flex justify-between">
                <span className="text-gray-500">Owner</span>
                <span className="font-medium">{selectedStore.owner}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Products</span>
                <span>{selectedStore.products}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Orders</span>
                <span>{selectedStore.orders}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Status</span>
                <StatusBadge status={selectedStore.status} />
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Joined</span>
                <span>{new Date(selectedStore.createdAt).toLocaleDateString()}</span>
              </div>
            </div>

            <div className="flex gap-2">
              {selectedStore.status === "PENDING" && (
                <button
                  onClick={() => handleStatusChange(selectedStore.id, "ACTIVE")}
                  className="flex-1 flex items-center justify-center gap-2 py-2 bg-green-600 text-white text-sm font-medium rounded-lg hover:bg-green-700"
                >
                  <Play className="w-4 h-4" /> Approve
                </button>
              )}
              {selectedStore.status === "ACTIVE" && (
                <button
                  onClick={() => handleStatusChange(selectedStore.id, "SUSPENDED")}
                  className="flex-1 flex items-center justify-center gap-2 py-2 bg-amber-600 text-white text-sm font-medium rounded-lg hover:bg-amber-700"
                >
                  <Pause className="w-4 h-4" /> Suspend
                </button>
              )}
              {selectedStore.status === "SUSPENDED" && (
                <button
                  onClick={() => handleStatusChange(selectedStore.id, "ACTIVE")}
                  className="flex-1 flex items-center justify-center gap-2 py-2 bg-green-600 text-white text-sm font-medium rounded-lg hover:bg-green-700"
                >
                  <Play className="w-4 h-4" /> Reactivate
                </button>
              )}
              {selectedStore.status !== "SUSPENDED" && (
                <button
                  onClick={() => handleStatusChange(selectedStore.id, "SUSPENDED")}
                  className="flex items-center justify-center gap-2 py-2 px-4 text-red-600 text-sm font-medium rounded-lg hover:bg-red-50 border border-red-200"
                >
                  <Ban className="w-4 h-4" /> Suspend
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
