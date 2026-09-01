"use client";

import { useState } from "react";
import { Plus, Pencil, Trash2, X } from "lucide-react";
import DataTable, { StatusBadge } from "@/components/DataTable";

const mockProducts = [
  { id: 1, name: "Classic Ankara Gown", category: "Dresses", price: 25000, stock: 45, status: "PUBLISHED" },
  { id: 2, name: "Mens Agbada Set", category: "Traditional", price: 45000, stock: 12, status: "PUBLISHED" },
  { id: 3, name: "Kids Birthday Outfit", category: "Kids", price: 15000, stock: 30, status: "DRAFT" },
  { id: 4, name: "Wedding Guest Dress", category: "Dresses", price: 35000, stock: 8, status: "PUBLISHED" },
  { id: 5, name: "Casual Denim Jacket", category: "Outerwear", price: 18000, stock: 0, status: "ARCHIVED" },
];

export default function ProductsPage() {
  const [showModal, setShowModal] = useState(false);
  const [form, setForm] = useState({ name: "", description: "", price: "", category: "" });

  const columns = [
    { key: "name", label: "Product", sortable: true },
    { key: "category", label: "Category", sortable: true },
    {
      key: "price",
      label: "Price",
      sortable: true,
      render: (v: number) =>
        new Intl.NumberFormat("en-NG", { style: "currency", currency: "NGN" }).format(v),
    },
    {
      key: "stock",
      label: "Stock",
      sortable: true,
      render: (v: number) => (
        <span className={v === 0 ? "text-red-600 font-medium" : v < 10 ? "text-amber-600" : ""}>
          {v}
        </span>
      ),
    },
    {
      key: "status",
      label: "Status",
      render: (v: string) => <StatusBadge status={v} />,
    },
    {
      key: "id",
      label: "Actions",
      sortable: false,
      render: (_: number, row: any) => (
        <div className="flex items-center gap-2">
          <button className="p-1.5 text-gray-400 hover:text-indigo-600 transition-colors">
            <Pencil className="w-4 h-4" />
          </button>
          <button className="p-1.5 text-gray-400 hover:text-red-600 transition-colors">
            <Trash2 className="w-4 h-4" />
          </button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-gray-900">Products</h2>
          <p className="text-sm text-gray-500 mt-1">Manage your product catalog</p>
        </div>
        <button
          onClick={() => setShowModal(true)}
          className="flex items-center gap-2 px-4 py-2.5 bg-indigo-600 text-white text-sm font-medium rounded-lg hover:bg-indigo-700 transition-colors"
        >
          <Plus className="w-4 h-4" />
          Add Product
        </button>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 p-6">
        <DataTable columns={columns} data={mockProducts} searchPlaceholder="Search products..." />
      </div>

      {showModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl w-full max-w-lg mx-4 p-6">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold">Add Product</h3>
              <button onClick={() => setShowModal(false)} className="p-1 text-gray-400 hover:text-gray-600">
                <X className="w-5 h-5" />
              </button>
            </div>
            <form className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Name</label>
                <input
                  type="text"
                  value={form.name}
                  onChange={(e) => setForm({ ...form, name: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none"
                  placeholder="Product name"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
                <textarea
                  rows={3}
                  value={form.description}
                  onChange={(e) => setForm({ ...form, description: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none resize-none"
                  placeholder="Product description"
                />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Price (NGN)</label>
                  <input
                    type="number"
                    value={form.price}
                    onChange={(e) => setForm({ ...form, price: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none"
                    placeholder="0"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Category</label>
                  <select
                    value={form.category}
                    onChange={(e) => setForm({ ...form, category: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none"
                  >
                    <option value="">Select category</option>
                    <option value="dresses">Dresses</option>
                    <option value="traditional">Traditional</option>
                    <option value="kids">Kids</option>
                    <option value="outerwear">Outerwear</option>
                    <option value="accessories">Accessories</option>
                  </select>
                </div>
              </div>
              <div className="flex justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 text-sm font-medium text-white bg-indigo-600 rounded-lg hover:bg-indigo-700"
                >
                  Create Product
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
