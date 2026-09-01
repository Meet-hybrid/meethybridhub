"use client";

import { useEffect, useState } from "react";
import { DollarSign, ShoppingCart, Package, Users, TrendingUp, Clock } from "lucide-react";
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from "recharts";
import StatsCard from "@/components/StatsCard";
import DataTable, { StatusBadge } from "@/components/DataTable";
import { api } from "@/lib/api";

const mockRevenueData = [
  { date: "Mon", revenue: 4200 },
  { date: "Tue", revenue: 3800 },
  { date: "Wed", revenue: 5100 },
  { date: "Thu", revenue: 4700 },
  { date: "Fri", revenue: 6200 },
  { date: "Sat", revenue: 7800 },
  { date: "Sun", revenue: 5900 },
];

export default function OverviewPage() {
  const [stats, setStats] = useState<any>(null);
  const [orders, setOrders] = useState<any[]>([]);

  useEffect(() => {
    // Use mock data for now — replace with API calls when backend is running
    setStats({
      totalOrders: 342,
      totalRevenue: 45892,
      totalProducts: 87,
      totalCustomers: 1234,
      pendingOrders: 18,
    });
    setOrders([
      { id: 1001, customer: "Alice Johnson", status: "PENDING", totalAmount: 12500, createdAt: "2026-09-01T10:30:00Z" },
      { id: 1002, customer: "Bob Smith", status: "CONFIRMED", totalAmount: 8900, createdAt: "2026-09-01T09:15:00Z" },
      { id: 1003, customer: "Carol White", status: "SHIPPED", totalAmount: 34200, createdAt: "2026-08-31T16:45:00Z" },
      { id: 1004, customer: "David Brown", status: "DELIVERED", totalAmount: 6700, createdAt: "2026-08-31T14:20:00Z" },
      { id: 1005, customer: "Eva Martinez", status: "PROCESSING", totalAmount: 21000, createdAt: "2026-08-30T11:00:00Z" },
    ]);
  }, []);

  const formatCurrency = (amount: number) =>
    new Intl.NumberFormat("en-NG", { style: "currency", currency: "NGN" }).format(amount);

  const orderColumns = [
    { key: "id", label: "Order #", render: (v: number) => `#${v}` },
    { key: "customer", label: "Customer" },
    {
      key: "status",
      label: "Status",
      render: (v: string) => <StatusBadge status={v} />,
    },
    {
      key: "totalAmount",
      label: "Total",
      render: (v: number) => formatCurrency(v),
    },
    {
      key: "createdAt",
      label: "Date",
      render: (v: string) => new Date(v).toLocaleDateString(),
    },
  ];

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatsCard
          title="Total Revenue"
          value={stats ? formatCurrency(stats.totalRevenue) : "—"}
          change="+12.5% from last month"
          changeType="up"
          icon={DollarSign}
          iconColor="text-green-600"
        />
        <StatsCard
          title="Total Orders"
          value={stats?.totalOrders ?? "—"}
          change="+8.2% from last month"
          changeType="up"
          icon={ShoppingCart}
          iconColor="text-blue-600"
        />
        <StatsCard
          title="Products"
          value={stats?.totalProducts ?? "—"}
          change="+3 new this week"
          changeType="neutral"
          icon={Package}
          iconColor="text-purple-600"
        />
        <StatsCard
          title="Customers"
          value={stats?.totalCustomers ?? "—"}
          change="+56 this month"
          changeType="up"
          icon={Users}
          iconColor="text-indigo-600"
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 bg-white rounded-xl border border-gray-200 p-6">
          <h3 className="text-sm font-semibold text-gray-900 mb-4">Revenue This Week</h3>
          <ResponsiveContainer width="100%" height={280}>
            <AreaChart data={mockRevenueData}>
              <defs>
                <linearGradient id="colorRevenue" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#6366f1" stopOpacity={0.2} />
                  <stop offset="95%" stopColor="#6366f1" stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
              <XAxis dataKey="date" stroke="#9ca3af" fontSize={12} />
              <YAxis stroke="#9ca3af" fontSize={12} />
              <Tooltip
                contentStyle={{ borderRadius: "8px", border: "1px solid #e5e7eb" }}
                formatter={(value: any) => [formatCurrency(Number(value)), "Revenue"]}
              />
              <Area type="monotone" dataKey="revenue" stroke="#6366f1" fillOpacity={1} fill="url(#colorRevenue)" />
            </AreaChart>
          </ResponsiveContainer>
        </div>

        <div className="bg-white rounded-xl border border-gray-200 p-6">
          <h3 className="text-sm font-semibold text-gray-900 mb-4">Quick Actions</h3>
          <div className="space-y-3">
            {[
              { label: "Add new product", href: "/products", color: "bg-indigo-50 text-indigo-700" },
              { label: "View pending orders", href: "/orders", color: "bg-amber-50 text-amber-700" },
              { label: "Review custom orders", href: "/custom-orders", color: "bg-emerald-50 text-emerald-700" },
              { label: "Update store branding", href: "/settings", color: "bg-purple-50 text-purple-700" },
            ].map((action) => (
              <a
                key={action.href}
                href={action.href}
                className={`block px-4 py-3 rounded-lg text-sm font-medium ${action.color} hover:opacity-80 transition-opacity`}
              >
                {action.label}
              </a>
            ))}
          </div>

          {stats?.pendingOrders > 0 && (
            <div className="mt-4 p-3 bg-amber-50 border border-amber-200 rounded-lg">
              <div className="flex items-center gap-2 text-amber-700 text-sm">
                <Clock className="w-4 h-4" />
                <span className="font-medium">{stats.pendingOrders} pending orders</span>
              </div>
            </div>
          )}
        </div>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 p-6">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-sm font-semibold text-gray-900">Recent Orders</h3>
          <a href="/orders" className="text-sm text-indigo-600 hover:text-indigo-500 font-medium">
            View all →
          </a>
        </div>
        <DataTable columns={orderColumns} data={orders} searchable={false} />
      </div>
    </div>
  );
}
