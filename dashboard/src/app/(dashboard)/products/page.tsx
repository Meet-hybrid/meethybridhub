"use client";

import { useEffect, useState } from "react";
import { Plus, Pencil, Trash2, X } from "lucide-react";
import DataTable, { StatusBadge } from "@/components/DataTable";
import { api } from "@/lib/api";

export default function ProductsPage() {
  const [products, setProducts] = useState<any[]>([]);
  const [showModal, setShowModal] = useState(false);
  const [editingProduct, setEditingProduct] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [form, setForm] = useState({ name: "", description: "", price: "", category: "" });

  const loadProducts = async () => {
    setLoading(true);
    try {
      const res = await api.getProducts({ size: 100 });
      const list = Array.isArray(res) ? res : res?.content ?? res?.data ?? [];
      setProducts(
        list.map((p: any) => ({
          id: p.id,
          name: p.name,
          category: p.category?.name ?? "Uncategorized",
          price: p.price,
          stock: p.variants?.reduce((sum: number, v: any) => sum + (v.stock ?? 0), 0) ?? 0,
          status: p.status,
        }))
      );
    } catch {

      setProducts([
        { id: 1, name: "Classic Ankara Gown", category: "Dresses", price: 25000, stock: 45, status: "PUBLISHED" },
        { id: 2, name: "Mens Agbada Set", category: "Traditional", price: 45000, stock: 12, status: "PUBLISHED" },
        { id: 3, name: "Kids Birthday Outfit", category: "Kids", price: 15000, stock: 30, status: "DRAFT" },
        { id: 4, name: "Wedding Guest Dress", category: "Dresses", price: 35000, stock: 8, status: "PUBLISHED" },
        { id: 5, name: "Casual Denim Jacket", category: "Outerwear", price: 18000, stock: 0, status: "ARCHIVED" },
      ]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadProducts();
  }, []);

  const openCreate = () => {
    setEditingProduct(null);
    setForm({ name: "", description: "", price: "", category: "" });
    setShowModal(true);
  };

  const openEdit = (product: any) => {
    setEditingProduct(product);
    setForm({
      name: product.name,
      description: "",
      price: String(product.price),
      category: product.category?.toLowerCase() ?? "",
    });
    setShowModal(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const data = {
      name: form.name,
      description: form.description,
      price: Number(form.price),
      category: form.category || undefined,
    };

    try {
      if (editingProduct) {
        await api.updateProduct(editingProduct.id, data);
      } else {
        await api.createProduct(data);
      }
      setShowModal(false);
      loadProducts();
    } catch {
      alert("Failed to save product. Is the backend running?");
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm("Are you sure you want to delete this product?")) return;
    try {
      await api.deleteProduct(id);
      loadProducts();
    } catch {
      alert("Failed to delete product.");
    }
  };

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
          <button
            onClick={(e) => { e.stopPropagation(); openEdit(row); }}
            className="p-1.5 text-gray-400 hover:text-indigo-600 transition-colors"
          >
            <Pencil className="w-4 h-4" />
          </button>
          <button
            onClick={(e) => { e.stopPropagation(); handleDelete(row.id); }}
            className="p-1.5 text-gray-400 hover:text-red-600 transition-colors"
          >
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
          onClick={openCreate}
          className="flex items-center gap-2 px-4 py-2.5 bg-indigo-600 text-white text-sm font-medium rounded-lg hover:bg-indigo-700 transition-colors"
        >
          <Plus className="w-4 h-4" />
          Add Product
        </button>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 p-6">
        {loading ? (
          <div className="flex items-center justify-center py-12">
            <div className="animate-spin w-6 h-6 border-4 border-indigo-600 border-t-transparent rounded-full" />
          </div>
        ) : (
          <DataTable columns={columns} data={products} searchPlaceholder="Search products..." />
        )}
      </div>

      {showModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl w-full max-w-lg mx-4 p-6">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold">{editingProduct ? "Edit Product" : "Add Product"}</h3>
              <button onClick={() => setShowModal(false)} className="p-1 text-gray-400 hover:text-gray-600">
                <X className="w-5 h-5" />
              </button>
            </div>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Name</label>
                <input
                  type="text"
                  required
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
                    required
                    min="0"
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
                  {editingProduct ? "Save Changes" : "Create Product"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
