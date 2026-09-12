"use client";

import { useEffect, useState } from "react";
import {
  Bell,
  Store,
  AlertTriangle,
  CheckCircle,
  Filter,
  CheckCheck,
} from "lucide-react";
import { api } from "@/lib/api";

interface Notification {
  id: number;
  type: string;
  title: string;
  message: string;
  read: boolean;
  createdAt: string;
  link?: string;
}

const fallbackNotifications: Notification[] = [
  { id: 1, type: "STORE_REGISTRATION", title: "New Store Registration", message: "Urban Threads has requested to join the platform. Review their application and approve or reject.", read: false, createdAt: "2026-09-02T09:00:00Z", link: "/admin/stores" },
  { id: 2, type: "DISPUTE_OPENED", title: "New Dispute Filed", message: "Alice Johnson filed a dispute against Divine Signature for Order #1001 — Wrong size delivered.", read: false, createdAt: "2026-09-01T14:30:00Z", link: "/admin/disputes" },
  { id: 3, type: "STORE_REGISTRATION", title: "New Store Registration", message: "Fresh Fabrics has requested to join the platform. Review their application.", read: false, createdAt: "2026-08-30T11:00:00Z", link: "/admin/stores" },
  { id: 4, type: "DISPUTE_OPENED", title: "New Dispute Filed", message: "Bob Smith filed a dispute against Lagos Couture for Order #1005 — Item not as described.", read: true, createdAt: "2026-08-28T09:15:00Z", link: "/admin/disputes" },
  { id: 5, type: "DISPUTE_RESOLVED", title: "Dispute Resolved", message: "Dispute #3 (Carol White vs Ankara Palace) has been resolved. Refund of ₦35,000 issued.", read: true, createdAt: "2026-08-26T16:00:00Z", link: "/admin/disputes" },
  { id: 6, type: "STORE_REGISTRATION", title: "Store Registration Approved", message: "Heritage Fabrics registration has been approved and is now active on the platform.", read: true, createdAt: "2026-08-25T10:30:00Z", link: "/admin/stores" },
  { id: 7, type: "DISPUTE_OPENED", title: "New Dispute Filed", message: "Eva Martinez filed a dispute against Heritage Fabrics for Order #1012 — Damaged item.", read: true, createdAt: "2026-08-20T11:45:00Z", link: "/admin/disputes" },
  { id: 8, type: "DISPUTE_RESOLVED", title: "Dispute Dismissed", message: "Dispute #5 (Eva Martinez vs Heritage Fabrics) has been dismissed — insufficient evidence.", read: true, createdAt: "2026-08-19T14:00:00Z", link: "/admin/disputes" },
];

const typeFilters = ["ALL", "STORE_REGISTRATION", "DISPUTE_OPENED", "DISPUTE_RESOLVED"];
const readFilters = ["ALL", "UNREAD", "READ"];

function timeAgo(dateStr: string): string {
  const now = new Date();
  const date = new Date(dateStr);
  const diffMs = now.getTime() - date.getTime();
  const diffMin = Math.floor(diffMs / 60000);
  const diffHr = Math.floor(diffMin / 60);
  const diffDay = Math.floor(diffHr / 24);

  if (diffMin < 1) return "Just now";
  if (diffMin < 60) return `${diffMin}m ago`;
  if (diffHr < 24) return `${diffHr}h ago`;
  if (diffDay < 7) return `${diffDay}d ago`;
  return date.toLocaleDateString();
}

export default function AdminNotificationsPage() {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(true);
  const [typeFilter, setTypeFilter] = useState("ALL");
  const [readFilter, setReadFilter] = useState("ALL");

  const loadNotifications = async () => {
    setLoading(true);
    try {
      const res: any = await api.getNotifications({ size: 50 });
      const list = Array.isArray(res) ? res : res?.content ?? res?.data ?? [];
      setNotifications(
        list.map((n: any) => ({
          id: n.id,
          type: n.type,
          title: n.title,
          message: n.message,
          read: n.read ?? false,
          createdAt: n.createdAt,
          link: n.link,
        }))
      );
    } catch {
      setNotifications(fallbackNotifications);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadNotifications();
  }, []);

  const filtered = notifications.filter((n) => {
    if (typeFilter !== "ALL" && n.type !== typeFilter) return false;
    if (readFilter === "UNREAD" && n.read) return false;
    if (readFilter === "READ" && !n.read) return false;
    return true;
  });

  const unreadCount = notifications.filter((n) => !n.read).length;

  const handleMarkRead = async (id: number) => {
    try {
      await api.markNotificationRead(id);
    } catch {
      // Optimistic
    }
    setNotifications((prev) =>
      prev.map((n) => (n.id === id ? { ...n, read: true } : n))
    );
  };

  const handleMarkAllRead = async () => {
    try {
      await api.markAllNotificationsRead();
    } catch {
      // Optimistic
    }
    setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
  };

  const getIcon = (type: string) => {
    switch (type) {
      case "STORE_REGISTRATION":
        return (
          <div className="w-9 h-9 bg-indigo-100 rounded-full flex items-center justify-center shrink-0">
            <Store className="w-4 h-4 text-indigo-600" />
          </div>
        );
      case "DISPUTE_OPENED":
        return (
          <div className="w-9 h-9 bg-amber-100 rounded-full flex items-center justify-center shrink-0">
            <AlertTriangle className="w-4 h-4 text-amber-600" />
          </div>
        );
      case "DISPUTE_RESOLVED":
        return (
          <div className="w-9 h-9 bg-green-100 rounded-full flex items-center justify-center shrink-0">
            <CheckCircle className="w-4 h-4 text-green-600" />
          </div>
        );
      default:
        return (
          <div className="w-9 h-9 bg-gray-100 rounded-full flex items-center justify-center shrink-0">
            <Bell className="w-4 h-4 text-gray-600" />
          </div>
        );
    }
  };

  const getTypeLabel = (type: string) => {
    switch (type) {
      case "STORE_REGISTRATION":
        return "Store Registration";
      case "DISPUTE_OPENED":
        return "Dispute Filed";
      case "DISPUTE_RESOLVED":
        return "Dispute Resolved";
      default:
        return type;
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-gray-900">Notifications</h2>
          <p className="text-sm text-gray-500 mt-1">
            Stay updated on platform activity
            {unreadCount > 0 && (
              <span className="ml-2 inline-flex items-center px-2 py-0.5 bg-red-100 text-red-700 rounded-full text-xs font-medium">
                {unreadCount} unread
              </span>
            )}
          </p>
        </div>
        {unreadCount > 0 && (
          <button
            onClick={handleMarkAllRead}
            className="flex items-center gap-1.5 px-4 py-2 text-sm font-medium text-indigo-600 bg-indigo-50 rounded-lg hover:bg-indigo-100 transition-colors"
          >
            <CheckCheck className="w-4 h-4" />
            Mark all read
          </button>
        )}
      </div>

      {/* Filters */}
      <div className="flex flex-wrap items-center gap-4">
        <div className="flex items-center gap-2">
          <Filter className="w-4 h-4 text-gray-400" />
          <span className="text-xs font-medium text-gray-500 uppercase">Type</span>
        </div>
        <div className="flex flex-wrap gap-2">
          {typeFilters.map((f) => (
            <button
              key={f}
              onClick={() => setTypeFilter(f)}
              className={`px-3 py-1.5 text-xs font-medium rounded-full transition-colors ${
                typeFilter === f
                  ? "bg-red-600 text-white"
                  : "bg-gray-100 text-gray-600 hover:bg-gray-200"
              }`}
            >
              {f === "ALL" ? "All" : getTypeLabel(f)}
            </button>
          ))}
        </div>

        <div className="w-px h-6 bg-gray-200" />

        <div className="flex items-center gap-2">
          <span className="text-xs font-medium text-gray-500 uppercase">Status</span>
        </div>
        <div className="flex gap-2">
          {readFilters.map((f) => (
            <button
              key={f}
              onClick={() => setReadFilter(f)}
              className={`px-3 py-1.5 text-xs font-medium rounded-full transition-colors ${
                readFilter === f
                  ? "bg-red-600 text-white"
                  : "bg-gray-100 text-gray-600 hover:bg-gray-200"
              }`}
            >
              {f}
            </button>
          ))}
        </div>
      </div>

      {/* Notifications List */}
      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center py-16">
            <div className="animate-spin w-6 h-6 border-4 border-red-600 border-t-transparent rounded-full" />
          </div>
        ) : filtered.length === 0 ? (
          <div className="text-center py-16">
            <Bell className="w-10 h-10 text-gray-300 mx-auto mb-3" />
            <p className="text-gray-500 font-medium">No notifications</p>
            <p className="text-sm text-gray-400 mt-1">
              {typeFilter !== "ALL" || readFilter !== "ALL"
                ? "Try adjusting your filters"
                : "You're all caught up!"}
            </p>
          </div>
        ) : (
          <div className="divide-y divide-gray-100">
            {filtered.map((notif) => (
              <div
                key={notif.id}
                onClick={() => !notif.read && handleMarkRead(notif.id)}
                className={`flex items-start gap-4 px-5 py-4 cursor-pointer transition-colors ${
                  notif.read
                    ? "bg-white hover:bg-gray-50"
                    : "bg-indigo-50/40 hover:bg-indigo-50"
                }`}
              >
                {getIcon(notif.type)}
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-0.5">
                    <h4 className={`text-sm font-semibold ${notif.read ? "text-gray-700" : "text-gray-900"}`}>
                      {notif.title}
                    </h4>
                    {!notif.read && (
                      <span className="w-2 h-2 bg-indigo-500 rounded-full shrink-0" />
                    )}
                    <span className="text-xs text-gray-400 ml-auto shrink-0">
                      {timeAgo(notif.createdAt)}
                    </span>
                  </div>
                  <p className="text-sm text-gray-500 line-clamp-2">{notif.message}</p>
                  {notif.link && (
                    <a
                      href={notif.link}
                      className="inline-block mt-1.5 text-xs font-medium text-indigo-600 hover:text-indigo-500"
                    >
                      View details →
                    </a>
                  )}
                </div>
                {!notif.read && (
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      handleMarkRead(notif.id);
                    }}
                    className="shrink-0 p-1 text-gray-400 hover:text-indigo-600 transition-colors"
                    title="Mark as read"
                  >
                    <CheckCheck className="w-4 h-4" />
                  </button>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
