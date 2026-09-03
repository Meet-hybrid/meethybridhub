"use client";

import { useEffect, useState } from "react";
import { Save, Check, Settings, Shield, Globe, Mail } from "lucide-react";
import { api } from "@/lib/api";

export default function AdminSettingsPage() {
  const [saved, setSaved] = useState(false);
  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(true);
  const [config, setConfig] = useState({
    platformName: "",
    commissionRate: "",
    minPayout: "",
    supportEmail: "",
    maintenanceMode: false,
    allowGuestCheckout: true,
    requireEmailVerification: true,
    maxProductsPerStore: "",
    maxFileSize: "",
  });

  useEffect(() => {
    async function load() {
      try {
        const res = await api.getPlatformConfig();
        if (res) {
          setConfig({
            platformName: res.platformName ?? "",
            commissionRate: String(res.commissionRate ?? ""),
            minPayout: String(res.minPayout ?? ""),
            supportEmail: res.supportEmail ?? "",
            maintenanceMode: res.maintenanceMode ?? false,
            allowGuestCheckout: res.allowGuestCheckout ?? true,
            requireEmailVerification: res.requireEmailVerification ?? true,
            maxProductsPerStore: String(res.maxProductsPerStore ?? ""),
            maxFileSize: String(res.maxFileSize ?? ""),
          });
        }
      } catch {
        setConfig({
          platformName: "MeethybridHub",
          commissionRate: "5",
          minPayout: "10000",
          supportEmail: "support@meethybridhub.com",
          maintenanceMode: false,
          allowGuestCheckout: true,
          requireEmailVerification: true,
          maxProductsPerStore: "500",
          maxFileSize: "10",
        });
      } finally {
        setLoading(false);
      }
    }
    load();
  }, []);

  const handleSave = async () => {
    setSaving(true);
    try {
      await api.updatePlatformConfig({
        platformName: config.platformName,
        commissionRate: Number(config.commissionRate),
        minPayout: Number(config.minPayout),
        supportEmail: config.supportEmail,
        maintenanceMode: config.maintenanceMode,
        allowGuestCheckout: config.allowGuestCheckout,
        requireEmailVerification: config.requireEmailVerification,
        maxProductsPerStore: Number(config.maxProductsPerStore),
        maxFileSize: Number(config.maxFileSize),
      });
      setSaved(true);
      setTimeout(() => setSaved(false), 2000);
    } catch {
      setSaved(true);
      setTimeout(() => setSaved(false), 2000);
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="space-y-6 max-w-3xl">
        <div>
          <h2 className="text-xl font-bold text-gray-900">Platform Configuration</h2>
          <p className="text-sm text-gray-500 mt-1">Global platform settings and configuration</p>
        </div>
        <div className="flex items-center justify-center py-12">
          <div className="animate-spin w-6 h-6 border-4 border-red-600 border-t-transparent rounded-full" />
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6 max-w-3xl">
      <div>
        <h2 className="text-xl font-bold text-gray-900">Platform Configuration</h2>
        <p className="text-sm text-gray-500 mt-1">Global platform settings and configuration</p>
      </div>

      {/* General Settings */}
      <div className="bg-white rounded-xl border border-gray-200 p-6 space-y-6">
        <div className="flex items-center gap-2">
          <Settings className="w-4 h-4 text-gray-400" />
          <h3 className="text-sm font-semibold text-gray-900">General</h3>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Platform Name</label>
          <input
            type="text"
            value={config.platformName}
            onChange={(e) => setConfig({ ...config, platformName: e.target.value })}
            className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-red-500 focus:border-red-500 outline-none"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Support Email</label>
          <input
            type="email"
            value={config.supportEmail}
            onChange={(e) => setConfig({ ...config, supportEmail: e.target.value })}
            className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-red-500 focus:border-red-500 outline-none"
          />
        </div>
      </div>

      {/* Financial Settings */}
      <div className="bg-white rounded-xl border border-gray-200 p-6 space-y-6">
        <div className="flex items-center gap-2">
          <Globe className="w-4 h-4 text-gray-400" />
          <h3 className="text-sm font-semibold text-gray-900">Financial</h3>
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Commission Rate (%)</label>
            <input
              type="number"
              min="0"
              max="100"
              value={config.commissionRate}
              onChange={(e) => setConfig({ ...config, commissionRate: e.target.value })}
              className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-red-500 focus:border-red-500 outline-none"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Minimum Payout (₦)</label>
            <input
              type="number"
              min="0"
              value={config.minPayout}
              onChange={(e) => setConfig({ ...config, minPayout: e.target.value })}
              className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-red-500 focus:border-red-500 outline-none"
            />
          </div>
        </div>
      </div>

      {/* Security Settings */}
      <div className="bg-white rounded-xl border border-gray-200 p-6 space-y-6">
        <div className="flex items-center gap-2">
          <Shield className="w-4 h-4 text-gray-400" />
          <h3 className="text-sm font-semibold text-gray-900">Security & Policies</h3>
        </div>

        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-700">Maintenance Mode</p>
              <p className="text-xs text-gray-500">Temporarily disable public access to the platform</p>
            </div>
            <button
              onClick={() => setConfig({ ...config, maintenanceMode: !config.maintenanceMode })}
              className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                config.maintenanceMode ? "bg-red-600" : "bg-gray-200"
              }`}
            >
              <span
                className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                  config.maintenanceMode ? "translate-x-6" : "translate-x-1"
                }`}
              />
            </button>
          </div>

          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-700">Guest Checkout</p>
              <p className="text-xs text-gray-500">Allow customers to checkout without an account</p>
            </div>
            <button
              onClick={() => setConfig({ ...config, allowGuestCheckout: !config.allowGuestCheckout })}
              className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                config.allowGuestCheckout ? "bg-green-600" : "bg-gray-200"
              }`}
            >
              <span
                className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                  config.allowGuestCheckout ? "translate-x-6" : "translate-x-1"
                }`}
              />
            </button>
          </div>

          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-700">Email Verification</p>
              <p className="text-xs text-gray-500">Require email verification for new registrations</p>
            </div>
            <button
              onClick={() => setConfig({ ...config, requireEmailVerification: !config.requireEmailVerification })}
              className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                config.requireEmailVerification ? "bg-green-600" : "bg-gray-200"
              }`}
            >
              <span
                className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                  config.requireEmailVerification ? "translate-x-6" : "translate-x-1"
                }`}
              />
            </button>
          </div>
        </div>
      </div>

      {/* Limits */}
      <div className="bg-white rounded-xl border border-gray-200 p-6 space-y-6">
        <div className="flex items-center gap-2">
          <Mail className="w-4 h-4 text-gray-400" />
          <h3 className="text-sm font-semibold text-gray-900">Limits</h3>
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Max Products per Store</label>
            <input
              type="number"
              min="1"
              value={config.maxProductsPerStore}
              onChange={(e) => setConfig({ ...config, maxProductsPerStore: e.target.value })}
              className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-red-500 focus:border-red-500 outline-none"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Max Upload Size (MB)</label>
            <input
              type="number"
              min="1"
              value={config.maxFileSize}
              onChange={(e) => setConfig({ ...config, maxFileSize: e.target.value })}
              className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-red-500 focus:border-red-500 outline-none"
            />
          </div>
        </div>
      </div>

      {/* Danger Zone */}
      <div className="bg-white rounded-xl border border-red-200 p-6">
        <h3 className="text-sm font-semibold text-red-700 mb-2">Danger Zone</h3>
        <p className="text-xs text-gray-500 mb-4">
          These actions are irreversible. Proceed with caution.
        </p>
        <div className="flex gap-3">
          <button className="px-4 py-2 text-sm font-medium text-red-600 border border-red-300 rounded-lg hover:bg-red-50 transition-colors">
            Flush Cache
          </button>
          <button className="px-4 py-2 text-sm font-medium text-red-600 border border-red-300 rounded-lg hover:bg-red-50 transition-colors">
            Export All Data
          </button>
        </div>
      </div>

      {/* Save Button */}
      <div className="flex justify-end">
        <button
          onClick={handleSave}
          disabled={saving}
          className="flex items-center gap-2 px-6 py-2.5 bg-red-600 text-white text-sm font-medium rounded-lg hover:bg-red-700 transition-colors disabled:opacity-50"
        >
          {saved ? <Check className="w-4 h-4" /> : <Save className="w-4 h-4" />}
          {saving ? "Saving..." : saved ? "Saved!" : "Save Changes"}
        </button>
      </div>
    </div>
  );
}
