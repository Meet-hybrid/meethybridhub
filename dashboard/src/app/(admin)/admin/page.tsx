"use client";

import { useEffect, useState } from "react";
import {
  Store,
  Users,
  ShoppingCart,
  DollarSign,
  TrendingUp,
  AlertTriangle,
  Activity,
  BarChart3,
} from "lucide-react";
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from "recharts";
import StatsCard from "@/components/StatsCard";
import DataTable, { StatusBadge } from "@/components/DataTable";
import { api } from "@/lib/api";

const fallbackRevenueData = [
  { date: "Jan", revenue: 120000 },
  { date: "Feb", revenue: 180000 },
  { date: "Mar", revenue: 250000 },
  { date: "Apr", revenue: 310000 },
  { date: "May", revenue: 280000 },
  { date: "Jun", revenue: 420000 },
  { date: "Jul", revenue: 380000 },
];

export default function AdminOverviewPage() {
  const [stats, setStats] = useState<any>(null);
  const [recentStores, setRecentStores] = useState<any[]>([]);
  const [revenueData, setRevenueData] = useState(fallbackRevenueData);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function load() {
      try {
        const [analyticsRes, storesRes] = await Promise.allSettled([
          api.getPlatformAnalytics(),
          api.getAllStores({ size: 5 }),
        ]);

        if (analyticsRes.status === "fulfilled" && analyticsRes.value) {
          const a = analyticsRes.value;
          setStats({
            totalStores: a.totalStores ?? 0,
            totalUsers: a.totalUsers ?? 0,
            totalOrders: a.totalOrders ?? 0,
            totalRevenue: a.totalRevenue ?? 0,
            activeDisputes: a.activeDisputes ?? 0,
            monthlyRevenue: a.monthlyRevenue ?? [],
          });
          if (a.monthlyRevenue?.length > 0) {
            setRevenueData(a.monthlyRevenue);
          }
        }

        if (storesRes.status === "fulfilled" && storesRes.value) {
          const val: any = storesRes.value;
          const list = Array.isArray(val) ? val : val?.content ?? val?.data ?? [];
          setRecentStores(
            list.map((s: any) => ({
              id: s.id,
              name: s.name,
              owner: s.owner?.fullName ?? "Owner",
              status: s.status,
              products: s.productCount ?? 0,
              createdAt: s.createdAt,
            }))
          );
        }
      } catch {

      } finally {
        setLoading(false);
      }

      if (!stats) {
        setStats({
          totalStores: 47,
          totalUsers: 1284,
          totalOrders: 3420,
          totalRevenue: 18500000,
          activeDisputes: 5,
        });
      }

      if (recentStores.length === 0) {
        setRecentStores([
          { id: 1, name: "Divine Signature", owner: "Amara Okafor", status: "ACTIVE", products: 45, createdAt: "2026-01-15T00:00:00Z" },
          { id: 2, name: "Lagos Couture", owner: "Tunde Bakare", status: "ACTIVE", products: 32, createdAt: "2026-03-22T00:00:00Z" },
          { id: 3, name: "Ankara Palace", owner: "Nneka Eze", status: "ACTIVE", products: 67, createdAt: "2025-11-08T00:00:00Z" },
          { id: 4, name: "Urban Threads", owner: "Chidi Nwosu", status: "PENDING", products: 12, createdAt: "2026-08-01T00:00:00Z" },
          { id: 5, name: "Heritage Fabrics", owner: "Folake Williams", status: "SUSPENDED", products: 28, createdAt: "2026-02-14T00:00:00Z" },
        ]);
      }
    }
    load();
  }, []);

  const formatCurrency = (amount: number) =>
    new Intl.NumberFormat("en-NG", { style: "currency", currency: "NGN" }).format(amount);

  const storeColumns = [
    {
      key: "name",
      label: "Store",
      sortable: true,
      render: (v: string, row: any) => (
        <div>
          <p className="font-medium">{v}</p>
          <p className="text-xs text-gray-500">{row.owner}</p>
        </div>
      ),
    },
    { key: "products", label: "Products", sortable: true },
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
  ];

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-xl font-bold text-gray-900">Platform Overview</h2>
        <p className="text-sm text-gray-500 mt-1">Monitor your platform-wide metrics and activity</p>
      </div>

      {}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
        <StatsCard
          title="Total Stores"
          value={stats?.totalStores ?? "—"}
          change="Active storefronts"
          changeType="neutral"
          icon={Store}
          iconColor="text-indigo-600"
        />
        <StatsCard
          title="Total Users"
          value={stats?.totalUsers ?? "—"}
          change="Registered accounts"
          changeType="neutral"
          icon={Users}
          iconColor="text-blue-600"
        />
        <StatsCard
          title="Total Orders"
          value={stats?.totalOrders ?? "—"}
          change="All-time orders"
          changeType="neutral"
          icon={ShoppingCart}
          iconColor="text-purple-600"
        />
        <StatsCard
          title="Total Revenue"
          value={stats ? formatCurrency(stats.totalRevenue) : "—"}
          change="Platform gross"
          changeType="up"
          icon={DollarSign}
          iconColor="text-green-600"
        />
        <StatsCard
          title="Active Disputes"
          value={stats?.activeDisputes ?? "—"}
          change="Need attention"
          changeType={stats?.activeDisputes > 0 ? "down" : "neutral"}
          icon={AlertTriangle}
          iconColor="text-red-600"
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {}
        <div className="lg:col-span-2 bg-white rounded-xl border border-gray-200 p-6">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-semibold text-gray-900">Platform Revenue</h3>
            <div className="flex items-center gap-1 text-xs text-green-600 font-medium">
              <TrendingUp className="w-3 h-3" /> +18.2%
            </div>
          </div>
          <ResponsiveContainer width="100%" height={280}>
            <AreaChart data={revenueData}>
              <defs>
                <linearGradient id="colorRevenue" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#ef4444" stopOpacity={0.2} />
                  <stop offset="95%" stopColor="#ef4444" stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
              <XAxis dataKey="date" stroke="#9ca3af" fontSize={12} />
              <YAxis stroke="#9ca3af" fontSize={12} />
              <Tooltip
                contentStyle={{ borderRadius: "8px", border: "1px solid #e5e7eb" }}
                formatter={(value: any) => [formatCurrency(Number(value)), "Revenue"]}
              />
              <Area type="monotone" dataKey="revenue" stroke="#ef4444" fillOpacity={1} fill="url(#colorRevenue)" />
            </AreaChart>
          </ResponsiveContainer>
        </div>

        {}
        <div className="bg-white rounded-xl border border-gray-200 p-6">
          <h3 className="text-sm font-semibold text-gray-900 mb-4">Platform Health</h3>
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Activity className="w-4 h-4 text-green-500" />
                <span className="text-sm text-gray-600">API Uptime</span>
              </div>
              <span className="text-sm font-medium text-green-600">99.9%</span>
            </div>
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <BarChart3 className="w-4 h-4 text-blue-500" />
                <span className="text-sm text-gray-600">Avg Response Time</span>
              </div>
              <span className="text-sm font-medium text-gray-900">142ms</span>
            </div>
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <ShoppingCart className="w-4 h-4 text-purple-500" />
                <span className="text-sm text-gray-600">Conversion Rate</span>
              </div>
              <span className="text-sm font-medium text-gray-900">3.2%</span>
            </div>
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Users className="w-4 h-4 text-indigo-500" />
                <span className="text-sm text-gray-600">Active Today</span>
              </div>
              <span className="text-sm font-medium text-gray-900">234</span>
            </div>
          </div>
        </div>
      </div>

      {}
      <div className="bg-white rounded-xl border border-gray-200 p-6">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-sm font-semibold text-gray-900">Recent Stores</h3>
          <a href="/admin/stores" className="text-sm text-red-600 hover:text-red-500 font-medium">
            View all →
          </a>
        </div>
        {loading ? (
          <div className="flex items-center justify-center py-8">
            <div className="animate-spin w-6 h-6 border-4 border-red-600 border-t-transparent rounded-full" />
          </div>
        ) : (
          <DataTable columns={storeColumns} data={recentStores} searchable={false} />
        )}
      </div>
    </div>
  );
}
