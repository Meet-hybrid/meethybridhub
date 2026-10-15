"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  LayoutDashboard,
  Store,
  Users,
  AlertTriangle,
  Settings,
  Shield,
  ChevronLeft,
  ChevronRight,
  BarChart3,
  Bell,
  ScrollText,
} from "lucide-react";
import { useState } from "react";

const adminNavItems = [
  { href: "/admin", label: "Overview", icon: LayoutDashboard },
  { href: "/admin/analytics", label: "Analytics", icon: BarChart3 },
  { href: "/admin/stores", label: "Stores", icon: Store },
  { href: "/admin/users", label: "Users", icon: Users },
  { href: "/admin/disputes", label: "Disputes", icon: AlertTriangle },
  { href: "/admin/notifications", label: "Notifications", icon: Bell },
  { href: "/admin/audit-log", label: "Audit Log", icon: ScrollText },
  { href: "/admin/settings", label: "Platform Config", icon: Settings },
];

export default function AdminSidebar() {
  const pathname = usePathname();
  const [collapsed, setCollapsed] = useState(false);

  return (
    <aside
      className={`bg-gray-950 text-white flex flex-col transition-all duration-300 ${
        collapsed ? "w-16" : "w-64"
      }`}
    >
      <div className="flex items-center gap-2 px-4 h-16 border-b border-gray-800">
        <Shield className="w-6 h-6 text-red-400 shrink-0" />
        {!collapsed && <span className="font-bold text-lg truncate">Admin Panel</span>}
      </div>

      <nav className="flex-1 py-4 space-y-1 px-2">
        {adminNavItems.map((item) => {
          const isActive =
            item.href === "/admin"
              ? pathname === "/admin"
              : pathname.startsWith(item.href);
          return (
            <Link
              key={item.href}
              href={item.href}
              className={`flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                isActive
                  ? "bg-red-600 text-white"
                  : "text-gray-400 hover:text-white hover:bg-gray-800"
              }`}
              title={collapsed ? item.label : undefined}
            >
              <item.icon className="w-5 h-5 shrink-0" />
              {!collapsed && <span>{item.label}</span>}
            </Link>
          );
        })}
      </nav>

      <div className="px-2 pb-2">
        <Link
          href="/"
          className={`flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors text-gray-400 hover:text-white hover:bg-gray-800`}
          title={collapsed ? "Store Dashboard" : undefined}
        >
          <LayoutDashboard className="w-5 h-5 shrink-0" />
          {!collapsed && <span>Store Dashboard</span>}
        </Link>
      </div>

      <button
        onClick={() => setCollapsed(!collapsed)}
        className="flex items-center justify-center h-12 border-t border-gray-800 text-gray-400 hover:text-white hover:bg-gray-800 transition-colors"
      >
        {collapsed ? <ChevronRight className="w-5 h-5" /> : <ChevronLeft className="w-5 h-5" />}
      </button>
    </aside>
  );
}
