"use client";

import DataTable from "@/components/DataTable";
import { Mail, ShoppingBag } from "lucide-react";

const mockCustomers = [
  { id: 1, fullName: "Alice Johnson", email: "alice@example.com", orders: 12, totalSpent: 345000, joinedAt: "2026-01-15T00:00:00Z" },
  { id: 2, fullName: "Bob Smith", email: "bob@example.com", orders: 5, totalSpent: 125000, joinedAt: "2026-03-22T00:00:00Z" },
  { id: 3, fullName: "Carol White", email: "carol@example.com", orders: 23, totalSpent: 890000, joinedAt: "2025-11-08T00:00:00Z" },
  { id: 4, fullName: "David Brown", email: "david@example.com", orders: 3, totalSpent: 67000, joinedAt: "2026-07-01T00:00:00Z" },
  { id: 5, fullName: "Eva Martinez", email: "eva@example.com", orders: 8, totalSpent: 210000, joinedAt: "2026-05-14T00:00:00Z" },
];

const formatCurrency = (amount: number) =>
  new Intl.NumberFormat("en-NG", { style: "currency", currency: "NGN" }).format(amount);

export default function CustomersPage() {
  const columns = [
    {
      key: "fullName",
      label: "Customer",
      sortable: true,
      render: (v: string, row: any) => (
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 bg-indigo-100 rounded-full flex items-center justify-center text-indigo-600 text-xs font-bold">
            {v.split(" ").map((n) => n[0]).join("")}
          </div>
          <div>
            <p className="font-medium text-sm">{v}</p>
            <p className="text-xs text-gray-500">{row.email}</p>
          </div>
        </div>
      ),
    },
    {
      key: "orders",
      label: "Orders",
      sortable: true,
      render: (v: number) => (
        <span className="flex items-center gap-1">
          <ShoppingBag className="w-3 h-3 text-gray-400" /> {v}
        </span>
      ),
    },
    {
      key: "totalSpent",
      label: "Total Spent",
      sortable: true,
      render: (v: number) => formatCurrency(v),
    },
    {
      key: "joinedAt",
      label: "Joined",
      sortable: true,
      render: (v: string) => new Date(v).toLocaleDateString(),
    },
  ];

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-xl font-bold text-gray-900">Customers</h2>
        <p className="text-sm text-gray-500 mt-1">View your customer base and their purchase history</p>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 p-6">
        <DataTable columns={columns} data={mockCustomers} searchPlaceholder="Search customers..." />
      </div>
    </div>
  );
}
