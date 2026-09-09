"use client";

import { useEffect, useState } from "react";
import {
  TrendingUp,
  Users,
  ShoppingCart,
  DollarSign,
} from "lucide-react";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
} from "recharts";
import StatsCard from "@/components/StatsCard";
import { api } from "@/lib/api";

const fallbackBarData = [
  { month: "Jan", orders: 320, revenue: 1200000 },
  { month: "Feb", orders: 480, revenue: 1800000 },
  { month: "Mar", orders: 610, revenue: 2500000 },
  { month: "Apr", orders: 750, revenue: 3100000 },
  { month: "May", orders: 680, revenue: 2800000 },
  { month: "Jun", orders: 890, revenue: 4200000 },
  { month: "Jul", orders: 820, revenue: 3800000 },
];

const fallbackPieData = [
  { name: "Dresses", value: 35 },
  { name: "Traditional", value: 28 },
  { name: "Kids", value: 15 },
  { name: "Outerwear", value: 12 },
  { name: "Accessories", value: 10 },
];

const COLORS = ["#6366f1", "#8b5cf6", "#ec4899", "#f59e0b", "#10b981"];

export default function AdminAnalyticsPage() {
  const [analytics, setAnalytics] = useState<any>(null);
  const [barData, setBarData] = useState(fallbackBarData);
  const [pieData, setPieData] = useState(fallbackPieData);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function load() {
      try {
        const res = await api.getPlatformAnalytics();
        if (res) {
          setAnalytics(res);
          if (res.monthlyData?.length > 0) setBarData(res.monthlyData);
          if (res.categoryBreakdown?.length > 0) setPieData(res.categoryBreakdown);
        }
      } catch {

      } finally {
        setLoading(false);
      }
    }
    load();
  }, []);

  const formatCurrency = (amount: number) =>
    new Intl.NumberFormat("en-NG", { style: "currency", currency: "NGN" }).format(amount);

  if (loading) {
    return (
      <div className="space-y-6">
        <div>
          <h2 className="text-xl font-bold text-gray-900">Platform Analytics</h2>
          <p className="text-sm text-gray-500 mt-1">Detailed metrics and insights</p>
        </div>
        <div className="flex items-center justify-center py-20">
          <div className="animate-spin w-8 h-8 border-4 border-red-600 border-t-transparent rounded-full" />
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-xl font-bold text-gray-900">Platform Analytics</h2>
        <p className="text-sm text-gray-500 mt-1">Detailed metrics and performance insights</p>
      </div>

      {}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatsCard
          title="Monthly Revenue"
          value={formatCurrency(analytics?.monthlyRevenue ?? 3800000)}
          change="+18.2% vs last month"
          changeType="up"
          icon={DollarSign}
          iconColor="text-green-600"
        />
        <StatsCard
          title="Monthly Orders"
          value={analytics?.monthlyOrders ?? 820}
          change="+12.5% vs last month"
          changeType="up"
          icon={ShoppingCart}
          iconColor="text-blue-600"
        />
        <StatsCard
          title="New Users"
          value={analytics?.newUsers ?? 156}
          change="+8.3% vs last month"
          changeType="up"
          icon={Users}
          iconColor="text-purple-600"
        />
        <StatsCard
          title="Avg Order Value"
          value={formatCurrency(analytics?.avgOrderValue ?? 4634)}
          change="-2.1% vs last month"
          changeType="down"
          icon={TrendingUp}
          iconColor="text-indigo-600"
        />
      </div>

      {}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {}
        <div className="lg:col-span-2 bg-white rounded-xl border border-gray-200 p-6">
          <h3 className="text-sm font-semibold text-gray-900 mb-4">Revenue & Orders Trend</h3>
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={barData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
              <XAxis dataKey="month" stroke="#9ca3af" fontSize={12} />
              <YAxis yAxisId="left" stroke="#9ca3af" fontSize={12} />
              <YAxis yAxisId="right" orientation="right" stroke="#9ca3af" fontSize={12} />
              <Tooltip
                contentStyle={{ borderRadius: "8px", border: "1px solid #e5e7eb" }}
                formatter={(value: any, name: any) => [
                  name === "revenue" ? formatCurrency(Number(value)) : value,
                  name === "revenue" ? "Revenue" : "Orders",
                ]}
              />
              <Bar yAxisId="left" dataKey="revenue" fill="#6366f1" radius={[4, 4, 0, 0]} />
              <Bar yAxisId="right" dataKey="orders" fill="#c7d2fe" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>

        {}
        <div className="bg-white rounded-xl border border-gray-200 p-6">
          <h3 className="text-sm font-semibold text-gray-900 mb-4">Sales by Category</h3>
          <ResponsiveContainer width="100%" height={200}>
            <PieChart>
              <Pie
                data={pieData}
                cx="50%"
                cy="50%"
                innerRadius={50}
                outerRadius={80}
                paddingAngle={4}
                dataKey="value"
              >
                {pieData.map((_, index) => (
                  <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                ))}
              </Pie>
              <Tooltip
                contentStyle={{ borderRadius: "8px", border: "1px solid #e5e7eb" }}
                formatter={(value: any) => [`${value}%`, "Share"]}
              />
            </PieChart>
          </ResponsiveContainer>
          <div className="space-y-2 mt-4">
            {pieData.map((item, i) => (
              <div key={item.name} className="flex items-center justify-between text-sm">
                <div className="flex items-center gap-2">
                  <div className="w-3 h-3 rounded-full" style={{ backgroundColor: COLORS[i] }} />
                  <span className="text-gray-600">{item.name}</span>
                </div>
                <span className="font-medium">{item.value}%</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Top Stores */}
      <div className="bg-white rounded-xl border border-gray-200 p-6">
        <h3 className="text-sm font-semibold text-gray-900 mb-4">Top Performing Stores</h3>
        <div className="overflow-x-auto border border-gray-200 rounded-lg">
          <table className="w-full">
            <thead className="bg-gray-50 border-b border-gray-200">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">#</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Store</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Orders</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Revenue</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Rating</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {(analytics?.topStores ?? [
                { name: "Ankara Palace", orders: 342, revenue: 8500000, rating: 4.9 },
                { name: "Divine Signature", orders: 287, revenue: 6200000, rating: 4.8 },
                { name: "Lagos Couture", orders: 234, revenue: 5100000, rating: 4.7 },
                { name: "Heritage Fabrics", orders: 198, revenue: 4300000, rating: 4.6 },
                { name: "Urban Threads", orders: 156, revenue: 3200000, rating: 4.5 },
              ]).map((store: any, i: number) => (
                <tr key={i} className="hover:bg-gray-50">
                  <td className="px-4 py-3 text-sm font-medium text-gray-900">{i + 1}</td>
                  <td className="px-4 py-3 text-sm font-medium text-gray-900">{store.name}</td>
                  <td className="px-4 py-3 text-sm text-gray-600">{store.orders}</td>
                  <td className="px-4 py-3 text-sm text-gray-900 font-medium">{formatCurrency(store.revenue)}</td>
                  <td className="px-4 py-3 text-sm text-amber-600 font-medium">⭐ {store.rating}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
