"use client";

import { useEffect, useState } from "react";
import { UserCheck, UserX, Shield, Plus, X, Eye, EyeOff } from "lucide-react";
import DataTable, { StatusBadge } from "@/components/DataTable";
import { api } from "@/lib/api";

const roleFilters = ["ALL", "ADMIN", "STORE_OWNER", "CUSTOMER"];

export default function AdminUsersPage() {
  const [users, setUsers] = useState<any[]>([]);
  const [filter, setFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [selectedUser, setSelectedUser] = useState<any>(null);


  const [showInviteModal, setShowInviteModal] = useState(false);
  const [inviteForm, setInviteForm] = useState({
    fullName: "",
    email: "",
    password: "",
    confirmPassword: "",
    role: "ADMIN",
  });
  const [showPassword, setShowPassword] = useState(false);
  const [inviteError, setInviteError] = useState("");
  const [inviteSuccess, setInviteSuccess] = useState(false);
  const [inviteLoading, setInviteLoading] = useState(false);

  const loadUsers = async (role?: string) => {
    setLoading(true);
    try {
      const params: any = { size: 100 };
      if (role && role !== "ALL") params.role = role;
      const res: any = await api.getAllUsers(params);
      const list = Array.isArray(res) ? res : res?.content ?? res?.data ?? [];
      setUsers(
        list.map((u: any) => ({
          id: u.id,
          fullName: u.fullName ?? `${u.firstName ?? ""} ${u.lastName ?? ""}`.trim() ?? "User",
          email: u.email,
          roles: u.roles ?? [],
          status: u.status ?? "ACTIVE",
          createdAt: u.createdAt,
        }))
      );
    } catch {
      setUsers([
        { id: 1, fullName: "Amara Okafor", email: "amara@example.com", roles: ["STORE_OWNER"], status: "ACTIVE", createdAt: "2026-01-15T00:00:00Z" },
        { id: 2, fullName: "Tunde Bakare", email: "tunde@example.com", roles: ["STORE_OWNER"], status: "ACTIVE", createdAt: "2026-03-22T00:00:00Z" },
        { id: 3, fullName: "Nneka Eze", email: "nneka@example.com", roles: ["STORE_OWNER"], status: "ACTIVE", createdAt: "2025-11-08T00:00:00Z" },
        { id: 4, fullName: "Alice Johnson", email: "alice@example.com", roles: ["CUSTOMER"], status: "ACTIVE", createdAt: "2026-04-10T00:00:00Z" },
        { id: 5, fullName: "Bob Smith", email: "bob@example.com", roles: ["CUSTOMER"], status: "ACTIVE", createdAt: "2026-05-14T00:00:00Z" },
        { id: 6, fullName: "Admin User", email: "admin@meethybridhub.com", roles: ["ADMIN"], status: "ACTIVE", createdAt: "2025-08-01T00:00:00Z" },
        { id: 7, fullName: "Chidi Nwosu", email: "chidi@example.com", roles: ["STORE_OWNER"], status: "SUSPENDED", createdAt: "2026-08-01T00:00:00Z" },
      ]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadUsers(filter);
  }, [filter]);

  const handleStatusChange = async (userId: number, newStatus: string) => {
    try {
      await api.updateUserStatus(userId, newStatus);
      setUsers((prev) =>
        prev.map((u) => (u.id === userId ? { ...u, status: newStatus } : u))
      );
      setSelectedUser(null);
    } catch {
      alert("Failed to update user status.");
    }
  };

  const handleInviteAdmin = async (e: React.FormEvent) => {
    e.preventDefault();
    setInviteError("");

    if (inviteForm.password !== inviteForm.confirmPassword) {
      setInviteError("Passwords do not match");
      return;
    }
    if (inviteForm.password.length < 8) {
      setInviteError("Password must be at least 8 characters");
      return;
    }
    if (!inviteForm.fullName.trim()) {
      setInviteError("Full name is required");
      return;
    }

    setInviteLoading(true);
    try {
      const res = await api.createAdminUser({
        email: inviteForm.email,
        fullName: inviteForm.fullName.trim(),
        password: inviteForm.password,
        roles: [inviteForm.role],
      });

      // Add the new user to the list optimistically
      setUsers((prev) => [
        {
          id: res?.id ?? Date.now(),
          fullName: inviteForm.fullName.trim(),
          email: inviteForm.email,
          roles: [inviteForm.role],
          status: "ACTIVE",
          createdAt: new Date().toISOString(),
        },
        ...prev,
      ]);

      setInviteSuccess(true);
      setTimeout(() => {
        setShowInviteModal(false);
        setInviteSuccess(false);
        setInviteForm({ fullName: "", email: "", password: "", confirmPassword: "", role: "ADMIN" });
      }, 1500);
    } catch (err: any) {
      setInviteError(err.message || "Failed to create admin user. Is the backend running?");
    } finally {
      setInviteLoading(false);
    }
  };

  const resetInviteModal = () => {
    setShowInviteModal(false);
    setInviteError("");
    setInviteSuccess(false);
    setInviteForm({ fullName: "", email: "", password: "", confirmPassword: "", role: "ADMIN" });
    setShowPassword(false);
  };

  const columns = [
    {
      key: "fullName",
      label: "User",
      sortable: true,
      render: (v: string, row: any) => (
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 bg-gray-100 rounded-full flex items-center justify-center text-gray-600 text-xs font-bold">
            {v.split(" ").map((n: string) => n[0]).join("")}
          </div>
          <div>
            <p className="font-medium text-sm">{v}</p>
            <p className="text-xs text-gray-500">{row.email}</p>
          </div>
        </div>
      ),
    },
    {
      key: "roles",
      label: "Role",
      sortable: true,
      render: (v: string[]) =>
        v.map((r) => (
          <span
            key={r}
            className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium mr-1 ${
              r === "ADMIN" || r === "SUPER_ADMIN"
                ? "bg-red-100 text-red-700"
                : r === "STORE_OWNER"
                ? "bg-indigo-100 text-indigo-700"
                : "bg-gray-100 text-gray-600"
            }`}
          >
            {r.replace("ROLE_", "")}
          </span>
        )),
    },
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
    {
      key: "id",
      label: "",
      sortable: false,
      render: (_: number, row: any) => (
        <button
          onClick={(e) => { e.stopPropagation(); setSelectedUser(row); }}
          className="p-1.5 text-gray-400 hover:text-indigo-600 transition-colors"
        >
          <Shield className="w-4 h-4" />
        </button>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-gray-900">User Management</h2>
          <p className="text-sm text-gray-500 mt-1">Manage all users on the platform</p>
        </div>
        <button
          onClick={() => setShowInviteModal(true)}
          className="flex items-center gap-2 px-4 py-2.5 bg-red-600 text-white text-sm font-medium rounded-lg hover:bg-red-700 transition-colors"
        >
          <Plus className="w-4 h-4" />
          Invite Admin
        </button>
      </div>

      <div className="flex flex-wrap gap-2">
        {roleFilters.map((r) => (
          <button
            key={r}
            onClick={() => setFilter(r)}
            className={`px-3 py-1.5 text-xs font-medium rounded-full transition-colors ${
              filter === r
                ? "bg-red-600 text-white"
                : "bg-gray-100 text-gray-600 hover:bg-gray-200"
            }`}
          >
            {r.replace("_", " ")}
          </button>
        ))}
      </div>

      <div className="bg-white rounded-xl border border-gray-200 p-6">
        {loading ? (
          <div className="flex items-center justify-center py-12">
            <div className="animate-spin w-6 h-6 border-4 border-red-600 border-t-transparent rounded-full" />
          </div>
        ) : (
          <DataTable columns={columns} data={users} searchPlaceholder="Search users..." />
        )}
      </div>

      {/* Invite Admin Modal */}
      {showInviteModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl w-full max-w-lg mx-4 p-6">
            <div className="flex items-center justify-between mb-6">
              <div>
                <h3 className="text-lg font-semibold text-gray-900">Invite Admin User</h3>
                <p className="text-sm text-gray-500 mt-0.5">Create a new admin account with platform access</p>
              </div>
              <button
                onClick={resetInviteModal}
                className="p-1 text-gray-400 hover:text-gray-600"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {inviteSuccess ? (
              <div className="text-center py-8">
                <div className="w-14 h-14 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
                  <UserCheck className="w-7 h-7 text-green-600" />
                </div>
                <h4 className="font-semibold text-gray-900 mb-1">Admin Created!</h4>
                <p className="text-sm text-gray-500">
                  {inviteForm.fullName} has been added as an admin.
                </p>
              </div>
            ) : (
              <form onSubmit={handleInviteAdmin} className="space-y-4">
                {inviteError && (
                  <div className="bg-red-50 text-red-700 text-sm rounded-lg px-4 py-3">
                    {inviteError}
                  </div>
                )}

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Full Name <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    required
                    value={inviteForm.fullName}
                    onChange={(e) => setInviteForm({ ...inviteForm, fullName: e.target.value })}
                    className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-red-500 focus:border-red-500 outline-none"
                    placeholder="John Doe"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Email Address <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="email"
                    required
                    value={inviteForm.email}
                    onChange={(e) => setInviteForm({ ...inviteForm, email: e.target.value })}
                    className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-red-500 focus:border-red-500 outline-none"
                    placeholder="admin@meethybridhub.com"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Role <span className="text-red-500">*</span>
                  </label>
                  <div className="flex gap-3">
                    {["ADMIN", "SUPER_ADMIN"].map((role) => (
                      <button
                        key={role}
                        type="button"
                        onClick={() => setInviteForm({ ...inviteForm, role })}
                        className={`flex-1 py-2.5 text-sm font-medium rounded-lg border transition-colors ${
                          inviteForm.role === role
                            ? "border-red-600 bg-red-50 text-red-700"
                            : "border-gray-300 text-gray-600 hover:border-gray-400"
                        }`}
                      >
                        <Shield className="w-4 h-4 inline mr-1.5" />
                        {role.replace("_", " ")}
                      </button>
                    ))}
                  </div>
                  <p className="text-xs text-gray-500 mt-1.5">
                    {inviteForm.role === "SUPER_ADMIN"
                      ? "Full platform access including user management and configuration"
                      : "Can manage stores, disputes, and view analytics"}
                  </p>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Password <span className="text-red-500">*</span>
                  </label>
                  <div className="relative">
                    <input
                      type={showPassword ? "text" : "password"}
                      required
                      minLength={8}
                      value={inviteForm.password}
                      onChange={(e) => setInviteForm({ ...inviteForm, password: e.target.value })}
                      className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-red-500 focus:border-red-500 outline-none pr-10"
                      placeholder="At least 8 characters"
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword(!showPassword)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                    >
                      {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                    </button>
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Confirm Password <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="password"
                    required
                    value={inviteForm.confirmPassword}
                    onChange={(e) => setInviteForm({ ...inviteForm, confirmPassword: e.target.value })}
                    className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-red-500 focus:border-red-500 outline-none"
                    placeholder="Confirm password"
                  />
                </div>

                <div className="flex justify-end gap-3 pt-2">
                  <button
                    type="button"
                    onClick={resetInviteModal}
                    className="px-4 py-2.5 text-sm font-medium text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={inviteLoading}
                    className="flex items-center gap-2 px-5 py-2.5 text-sm font-medium text-white bg-red-600 rounded-lg hover:bg-red-700 disabled:opacity-50 transition-colors"
                  >
                    {inviteLoading ? (
                      <>
                        <div className="animate-spin w-4 h-4 border-2 border-white border-t-transparent rounded-full" />
                        Creating...
                      </>
                    ) : (
                      <>
                        <Plus className="w-4 h-4" />
                        Create Admin Account
                      </>
                    )}
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}

      {/* User Detail Modal */}
      {selectedUser && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl w-full max-w-md mx-4 p-6">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 bg-gray-100 rounded-full flex items-center justify-center text-gray-600 text-sm font-bold">
                  {selectedUser.fullName.split(" ").map((n: string) => n[0]).join("")}
                </div>
                <div>
                  <h3 className="text-lg font-semibold">{selectedUser.fullName}</h3>
                  <p className="text-xs text-gray-500">{selectedUser.email}</p>
                </div>
              </div>
              <button onClick={() => setSelectedUser(null)} className="text-gray-400 hover:text-gray-600 text-xl">
                ×
              </button>
            </div>

            <div className="space-y-3 text-sm mb-6">
              <div className="flex justify-between">
                <span className="text-gray-500">Roles</span>
                <div className="flex gap-1">
                  {selectedUser.roles.map((r: string) => (
                    <span
                      key={r}
                      className={`px-2 py-0.5 rounded-full text-xs font-medium ${
                        r === "ADMIN" || r === "SUPER_ADMIN"
                          ? "bg-red-100 text-red-700"
                          : r === "STORE_OWNER"
                          ? "bg-indigo-100 text-indigo-700"
                          : "bg-gray-100 text-gray-600"
                      }`}
                    >
                      {r.replace("ROLE_", "")}
                    </span>
                  ))}
                </div>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Status</span>
                <StatusBadge status={selectedUser.status} />
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Joined</span>
                <span>{new Date(selectedUser.createdAt).toLocaleDateString()}</span>
              </div>
            </div>

            <div className="flex gap-2">
              {selectedUser.status === "ACTIVE" ? (
                <button
                  onClick={() => handleStatusChange(selectedUser.id, "SUSPENDED")}
                  className="flex-1 flex items-center justify-center gap-2 py-2 bg-red-600 text-white text-sm font-medium rounded-lg hover:bg-red-700"
                >
                  <UserX className="w-4 h-4" /> Suspend User
                </button>
              ) : (
                <button
                  onClick={() => handleStatusChange(selectedUser.id, "ACTIVE")}
                  className="flex-1 flex items-center justify-center gap-2 py-2 bg-green-600 text-white text-sm font-medium rounded-lg hover:bg-green-700"
                >
                  <UserCheck className="w-4 h-4" /> Reactivate User
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
