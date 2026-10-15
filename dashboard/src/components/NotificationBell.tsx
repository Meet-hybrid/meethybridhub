"use client";

import { useEffect, useState, useRef } from "react";
import { Bell, Store, AlertTriangle, CheckCircle } from "lucide-react";
import { api } from "@/lib/api";

interface Notification {
  id: number;
  type: string;
  title: string;
  message: string;
  read: boolean;
  createdAt: string;
}

const fallbackNotifications: Notification[] = [
  {
    id: 1,
    type: "STORE_REGISTRATION",
    title: "New Store Registration",
    message: "Urban Threads has requested to join the platform",
    read: false,
    createdAt: "2026-09-02T09:00:00Z",
  },
  {
    id: 2,
    type: "DISPUTE_OPENED",
    title: "New Dispute Filed",
    message: "Alice Johnson filed a dispute against Divine Signature (Order #1001)",
    read: false,
    createdAt: "2026-09-01T14:30:00Z",
  },
  {
    id: 3,
    type: "STORE_REGISTRATION",
    title: "New Store Registration",
    message: "Fresh Fabrics has requested to join the platform",
    read: true,
    createdAt: "2026-08-30T11:00:00Z",
  },
  {
    id: 4,
    type: "DISPUTE_OPENED",
    title: "New Dispute Filed",
    message: "Bob Smith filed a dispute against Lagos Couture (Order #1005)",
    read: true,
    createdAt: "2026-08-28T09:15:00Z",
  },
];

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

export default function NotificationBell() {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  const loadUnreadCount = async () => {
    try {
      const res: any = await api.getUnreadCount();
      setUnreadCount(typeof res === "number" ? res : res?.count ?? 0);
    } catch {
      setUnreadCount(2);
    }
  };

  const loadNotifications = async () => {
    setLoading(true);
    try {
      const res: any = await api.getNotifications({ size: 10 });
      const list = Array.isArray(res) ? res : res?.content ?? res?.data ?? [];
      setNotifications(
        list.map((n: any) => ({
          id: n.id,
          type: n.type,
          title: n.title,
          message: n.message,
          read: n.read ?? false,
          createdAt: n.createdAt,
        }))
      );
    } catch {
      setNotifications(fallbackNotifications);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadUnreadCount();
    // Poll for new notifications every 30 seconds
    const interval = setInterval(loadUnreadCount, 30000);
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    if (open) {
      loadNotifications();
    }
  }, [open]);

  // Close dropdown on outside click
  useEffect(() => {
    function handleClick(e: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    if (open) {
      document.addEventListener("mousedown", handleClick);
      return () => document.removeEventListener("mousedown", handleClick);
    }
  }, [open]);

  const handleMarkRead = async (id: number) => {
    try {
      await api.markNotificationRead(id);
    } catch {
      // Optimistic update
    }
    setNotifications((prev) =>
      prev.map((n) => (n.id === id ? { ...n, read: true } : n))
    );
    setUnreadCount((prev) => Math.max(0, prev - 1));
  };

  const handleMarkAllRead = async () => {
    try {
      await api.markAllNotificationsRead();
    } catch {
      // Optimistic update
    }
    setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
    setUnreadCount(0);
  };

  const getIcon = (type: string) => {
    switch (type) {
      case "STORE_REGISTRATION":
        return <Store className="w-4 h-4 text-indigo-600" />;
      case "DISPUTE_OPENED":
        return <AlertTriangle className="w-4 h-4 text-amber-600" />;
      case "DISPUTE_RESOLVED":
        return <CheckCircle className="w-4 h-4 text-green-600" />;
      default:
        return <Bell className="w-4 h-4 text-gray-600" />;
    }
  };

  return (
    <div className="relative" ref={dropdownRef}>
      <button
        onClick={() => setOpen(!open)}
        className="relative p-2 text-gray-500 hover:text-gray-700 transition-colors"
      >
        <Bell className="w-5 h-5" />
        {unreadCount > 0 && (
          <span className="absolute -top-0.5 -right-0.5 min-w-[18px] h-[18px] flex items-center justify-center bg-red-500 text-white text-[10px] font-bold rounded-full px-1">
            {unreadCount > 99 ? "99+" : unreadCount}
          </span>
        )}
      </button>

      {open && (
        <div className="absolute right-0 mt-2 w-96 bg-white rounded-xl shadow-lg border border-gray-200 z-50 overflow-hidden">
          <div className="flex items-center justify-between px-4 py-3 border-b border-gray-100">
            <h3 className="font-semibold text-sm text-gray-900">Notifications</h3>
            {unreadCount > 0 && (
              <button
                onClick={handleMarkAllRead}
                className="text-xs text-indigo-600 hover:text-indigo-500 font-medium"
              >
                Mark all read
              </button>
            )}
          </div>

          <div className="max-h-96 overflow-y-auto">
            {loading ? (
              <div className="flex items-center justify-center py-8">
                <div className="animate-spin w-5 h-5 border-4 border-indigo-600 border-t-transparent rounded-full" />
              </div>
            ) : notifications.length === 0 ? (
              <div className="text-center py-8 text-sm text-gray-500">
                No notifications yet
              </div>
            ) : (
              notifications.map((notif) => (
                <div
                  key={notif.id}
                  onClick={() => !notif.read && handleMarkRead(notif.id)}
                  className={`flex items-start gap-3 px-4 py-3 border-b border-gray-50 cursor-pointer transition-colors ${
                    notif.read
                      ? "bg-white hover:bg-gray-50"
                      : "bg-indigo-50/50 hover:bg-indigo-50"
                  }`}
                >
                  <div className="mt-0.5 shrink-0">{getIcon(notif.type)}</div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <p className={`text-sm font-medium ${notif.read ? "text-gray-700" : "text-gray-900"}`}>
                        {notif.title}
                      </p>
                      {!notif.read && (
                        <span className="w-2 h-2 bg-indigo-500 rounded-full shrink-0" />
                      )}
                    </div>
                    <p className="text-xs text-gray-500 mt-0.5 line-clamp-2">{notif.message}</p>
                    <p className="text-xs text-gray-400 mt-1">{timeAgo(notif.createdAt)}</p>
                  </div>
                </div>
              ))
            )}
          </div>

          <div className="px-4 py-2.5 border-t border-gray-100 bg-gray-50">
            <a
              href="/admin/notifications"
              onClick={() => setOpen(false)}
              className="block text-center text-xs font-medium text-indigo-600 hover:text-indigo-500"
            >
              View all notifications →
            </a>
          </div>
        </div>
      )}
    </div>
  );
}
