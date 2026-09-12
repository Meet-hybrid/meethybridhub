"use client";

import { useEffect, useState } from "react";
import {
  ScrollText,
  Store,
  Users,
  AlertTriangle,
  Settings,
  Shield,
  Filter,
  Download,
  Eye,
  Calendar,
} from "lucide-react";
import { api } from "@/lib/api";

interface AuditLogEntry {
  id: number;
  action: string;
  actor: string;
  actorRole: string;
  targetType: string;
  targetId: number;
  targetName: string;
  details: string;
  ipAddress: string;
  createdAt: string;
}

const actionFilters = [
  "ALL",
  "USER_CREATED",
  "USER_SUSPENDED",
  "USER_ACTIVATED",
  "STORE_APPROVED",
  "STORE_SUSPENDED",
  "STORE_REACTIVATED",
  "DISPUTE_RESOLVED",
  "DISPUTE_DISMISSED",
  "CONFIG_UPDATED",
  "ROLE_CHANGED",
];

const fallbackLogs: AuditLogEntry[] = [
  { id: 1, action: "USER_CREATED", actor: "Admin User", actorRole: "SUPER_ADMIN", targetType: "USER", targetId: 8, targetName: "newadmin@meethybridhub.com", details: "Created admin account with role ADMIN", ipAddress: "192.168.1.100", createdAt: "2026-09-02T14:30:00Z" },
  { id: 2, action: "STORE_APPROVED", actor: "Admin User", actorRole: "ADMIN", targetType: "STORE", targetId: 4, targetName: "Urban Threads", details: "Approved store registration", ipAddress: "192.168.1.100", createdAt: "2026-09-02T11:00:00Z" },
  { id: 3, action: "DISPUTE_RESOLVED", actor: "Admin User", actorRole: "ADMIN", targetType: "DISPUTE", targetId: 3, targetName: "Dispute #3", details: "Resolved dispute — refund of ₦35,000 issued to customer Carol White", ipAddress: "192.168.1.100", createdAt: "2026-09-01T16:00:00Z" },
  { id: 4, action: "CONFIG_UPDATED", actor: "Admin User", actorRole: "SUPER_ADMIN", targetType: "PLATFORM", targetId: 0, targetName: "Platform Config", details: "Updated commission rate from 5% to 6%", ipAddress: "192.168.1.100", createdAt: "2026-09-01T10:00:00Z" },
  { id: 5, action: "STORE_SUSPENDED", actor: "Admin User", actorRole: "ADMIN", targetType: "STORE", targetId: 5, targetName: "Heritage Fabrics", details: "Suspended store for policy violation", ipAddress: "192.168.1.100", createdAt: "2026-08-30T09:30:00Z" },
  { id: 6, action: "USER_SUSPENDED", actor: "Admin User", actorRole: "ADMIN", targetType: "USER", targetId: 7, targetName: "Chidi Nwosu", details: "Suspended user account", ipAddress: "192.168.1.100", createdAt: "2026-08-28T14:00:00Z" },
  { id: 7, action: "DISPUTE_DISMISSED", actor: "Admin User", actorRole: "ADMIN", targetType: "DISPUTE", targetId: 5, targetName: "Dispute #5", details: "Dismissed dispute — insufficient evidence provided", ipAddress: "192.168.1.100", createdAt: "2026-08-26T11:45:00Z" },
  { id: 8, action: "ROLE_CHANGED", actor: "Super Admin", actorRole: "SUPER_ADMIN", targetType: "USER", targetId: 6, targetName: "Admin User", details: "Changed role from STORE_OWNER to ADMIN", ipAddress: "10.0.0.1", createdAt: "2026-08-25T09:00:00Z" },
  { id: 9, action: "STORE_APPROVED", actor: "Admin User", actorRole: "ADMIN", targetType: "STORE", targetId: 3, targetName: "Ankara Palace", details: "Approved store registration", ipAddress: "192.168.1.100", createdAt: "2026-08-20T10:30:00Z" },
  { id: 10, action: "CONFIG_UPDATED", actor: "Super Admin", actorRole: "SUPER_ADMIN", targetType: "PLATFORM", targetId: 0, targetName: "Platform Config", details: "Enabled maintenance mode", ipAddress: "10.0.0.1", createdAt: "2026-08-15T08:00:00Z" },
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

function getActionIcon(action: string) {
  if (action.includes("USER")) return <Users className="w-4 h-4" />;
  if (action.includes("STORE")) return <Store className="w-4 h-4" />;
  if (action.includes("DISPUTE")) return <AlertTriangle className="w-4 h-4" />;
  if (action.includes("CONFIG")) return <Settings className="w-4 h-4" />;
  if (action.includes("ROLE")) return <Shield className="w-4 h-4" />;
  return <ScrollText className="w-4 h-4" />;
}

function getActionColor(action: string) {
  if (action.includes("CREATED") || action.includes("APPROVED") || action.includes("ACTIVATED") || action.includes("REACTIVATED"))
    return "bg-green-100 text-green-700";
  if (action.includes("SUSPENDED"))
    return "bg-red-100 text-red-700";
  if (action.includes("RESOLVED"))
    return "bg-blue-100 text-blue-700";
  if (action.includes("DISMISSED"))
    return "bg-gray-100 text-gray-600";
  if (action.includes("UPDATED") || action.includes("CHANGED"))
    return "bg-amber-100 text-amber-700";
  return "bg-indigo-100 text-indigo-700";
}

function formatAction(action: string) {
  return action
    .replace(/_/g, " ")
    .toLowerCase()
    .replace(/\b\w/g, (c) => c.toUpperCase());
}

export default function AdminAuditLogPage() {
  const [logs, setLogs] = useState<AuditLogEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionFilter, setActionFilter] = useState("ALL");
  const [dateFrom, setDateFrom] = useState("");
  const [dateTo, setDateTo] = useState("");
  const [selectedLog, setSelectedLog] = useState<AuditLogEntry | null>(null);

  const loadLogs = async () => {
    setLoading(true);
    try {
      const params: any = { size: 100 };
      if (actionFilter !== "ALL") params.action = actionFilter;
      if (dateFrom) params.from = dateFrom;
      if (dateTo) params.to = dateTo;
      const res: any = await api.getAuditLogs(params);
      const list = Array.isArray(res) ? res : res?.content ?? res?.data ?? [];
      setLogs(
        list.map((l: any) => ({
          id: l.id,
          action: l.action,
          actor: l.actor?.fullName ?? l.actorName ?? "System",
          actorRole: l.actor?.roles?.[0] ?? l.actorRole ?? "ADMIN",
          targetType: l.targetType,
          targetId: l.targetId,
          targetName: l.targetName ?? `#${l.targetId}`,
          details: l.details ?? "",
          ipAddress: l.ipAddress ?? "—",
          createdAt: l.createdAt,
        }))
      );
    } catch {
      setLogs(fallbackLogs);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadLogs();
  }, [actionFilter, dateFrom, dateTo]);

  const filtered = logs;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-gray-900">Audit Log</h2>
          <p className="text-sm text-gray-500 mt-1">
            Track all admin actions across the platform
            {logs.length > 0 && (
              <span className="ml-2 text-gray-400">· {logs.length} entries</span>
            )}
          </p>
        </div>
        <button className="flex items-center gap-2 px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200 transition-colors">
          <Download className="w-4 h-4" />
          Export CSV
        </button>
      </div>

      {/* Filters */}
      <div className="bg-white rounded-xl border border-gray-200 p-4">
        <div className="flex flex-wrap items-center gap-4">
          <div className="flex items-center gap-2">
            <Filter className="w-4 h-4 text-gray-400" />
            <span className="text-xs font-medium text-gray-500 uppercase">Action</span>
          </div>
          <div className="flex flex-wrap gap-2 flex-1">
            {actionFilters.map((f) => (
              <button
                key={f}
                onClick={() => setActionFilter(f)}
                className={`px-3 py-1.5 text-xs font-medium rounded-full transition-colors ${
                  actionFilter === f
                    ? "bg-red-600 text-white"
                    : "bg-gray-100 text-gray-600 hover:bg-gray-200"
                }`}
              >
                {f === "ALL" ? "All Actions" : formatAction(f)}
              </button>
            ))}
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-4 mt-3 pt-3 border-t border-gray-100">
          <div className="flex items-center gap-2">
            <Calendar className="w-4 h-4 text-gray-400" />
            <span className="text-xs font-medium text-gray-500 uppercase">Date Range</span>
          </div>
          <div className="flex items-center gap-2">
            <input
              type="date"
              value={dateFrom}
              onChange={(e) => setDateFrom(e.target.value)}
              className="px-3 py-1.5 border border-gray-300 rounded-lg text-xs focus:ring-2 focus:ring-red-500 outline-none"
            />
            <span className="text-xs text-gray-400">to</span>
            <input
              type="date"
              value={dateTo}
              onChange={(e) => setDateTo(e.target.value)}
              className="px-3 py-1.5 border border-gray-300 rounded-lg text-xs focus:ring-2 focus:ring-red-500 outline-none"
            />
          </div>
          {(dateFrom || dateTo) && (
            <button
              onClick={() => { setDateFrom(""); setDateTo(""); }}
              className="text-xs text-red-600 hover:text-red-500 font-medium"
            >
              Clear dates
            </button>
          )}
        </div>
      </div>

      {/* Log Table */}
      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center py-16">
            <div className="animate-spin w-6 h-6 border-4 border-red-600 border-t-transparent rounded-full" />
          </div>
        ) : filtered.length === 0 ? (
          <div className="text-center py-16">
            <ScrollText className="w-10 h-10 text-gray-300 mx-auto mb-3" />
            <p className="text-gray-500 font-medium">No audit log entries</p>
            <p className="text-sm text-gray-400 mt-1">Actions will appear here as admins make changes</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Action</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Actor</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Target</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Details</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Time</th>
                  <th className="px-4 py-3" />
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {filtered.map((log) => (
                  <tr
                    key={log.id}
                    className="hover:bg-gray-50 cursor-pointer transition-colors"
                    onClick={() => setSelectedLog(log)}
                  >
                    <td className="px-4 py-3">
                      <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium ${getActionColor(log.action)}`}>
                        {getActionIcon(log.action)}
                        {formatAction(log.action)}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <div>
                        <p className="text-sm font-medium text-gray-900">{log.actor}</p>
                        <p className="text-xs text-gray-500">{log.actorRole.replace("ROLE_", "")}</p>
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <div>
                        <p className="text-sm text-gray-900">{log.targetName}</p>
                        <p className="text-xs text-gray-500">{log.targetType} #{log.targetId}</p>
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <p className="text-sm text-gray-600 max-w-xs truncate">{log.details}</p>
                    </td>
                    <td className="px-4 py-3">
                      <div>
                        <p className="text-sm text-gray-900">{timeAgo(log.createdAt)}</p>
                        <p className="text-xs text-gray-400">{new Date(log.createdAt).toLocaleString()}</p>
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <button className="p-1.5 text-gray-400 hover:text-indigo-600 transition-colors">
                        <Eye className="w-4 h-4" />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Detail Modal */}
      {selectedLog && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl w-full max-w-lg mx-4 p-6">
            <div className="flex items-center justify-between mb-6">
              <div className="flex items-center gap-3">
                <div className={`w-10 h-10 rounded-full flex items-center justify-center ${getActionColor(selectedLog.action)}`}>
                  {getActionIcon(selectedLog.action)}
                </div>
                <div>
                  <h3 className="text-lg font-semibold text-gray-900">Audit Log Entry</h3>
                  <p className="text-xs text-gray-500">#{selectedLog.id}</p>
                </div>
              </div>
              <button
                onClick={() => setSelectedLog(null)}
                className="text-gray-400 hover:text-gray-600 text-xl"
              >
                ×
              </button>
            </div>

            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-medium text-gray-500 uppercase mb-1">Action</label>
                  <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium ${getActionColor(selectedLog.action)}`}>
                    {getActionIcon(selectedLog.action)}
                    {formatAction(selectedLog.action)}
                  </span>
                </div>
                <div>
                  <label className="block text-xs font-medium text-gray-500 uppercase mb-1">Timestamp</label>
                  <p className="text-sm text-gray-900">{new Date(selectedLog.createdAt).toLocaleString()}</p>
                </div>
              </div>

              <div className="border-t border-gray-100 pt-4">
                <label className="block text-xs font-medium text-gray-500 uppercase mb-1">Performed By</label>
                <div className="flex items-center gap-2">
                  <div className="w-7 h-7 bg-gray-100 rounded-full flex items-center justify-center text-gray-600 text-xs font-bold">
                    {selectedLog.actor.split(" ").map((n: string) => n[0]).join("")}
                  </div>
                  <div>
                    <p className="text-sm font-medium text-gray-900">{selectedLog.actor}</p>
                    <p className="text-xs text-gray-500">{selectedLog.actorRole.replace("ROLE_", "")}</p>
                  </div>
                </div>
              </div>

              <div className="border-t border-gray-100 pt-4">
                <label className="block text-xs font-medium text-gray-500 uppercase mb-1">Target</label>
                <p className="text-sm text-gray-900">
                  <span className="font-medium">{selectedLog.targetName}</span>
                  <span className="text-gray-500 ml-2">({selectedLog.targetType} #{selectedLog.targetId})</span>
                </p>
              </div>

              <div className="border-t border-gray-100 pt-4">
                <label className="block text-xs font-medium text-gray-500 uppercase mb-1">Details</label>
                <p className="text-sm text-gray-700 bg-gray-50 rounded-lg p-3">{selectedLog.details}</p>
              </div>

              <div className="border-t border-gray-100 pt-4">
                <label className="block text-xs font-medium text-gray-500 uppercase mb-1">IP Address</label>
                <p className="text-sm text-gray-900 font-mono">{selectedLog.ipAddress}</p>
              </div>
            </div>

            <div className="mt-6 flex justify-end">
              <button
                onClick={() => setSelectedLog(null)}
                className="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
