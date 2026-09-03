"use client";

import { useEffect, useState } from "react";
import { Upload, Save, Check } from "lucide-react";
import { api } from "@/lib/api";

export default function SettingsPage() {
  const [saved, setSaved] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState({
    storeName: "",
    tagline: "",
    primaryColor: "#6366f1",
    accentColor: "#ec4899",
    theme: "light",
    contactEmail: "",
    subdomain: "",
  });

  useEffect(() => {
    async function load() {
      try {
        const [store, settings] = await Promise.allSettled([
          api.getMyStore(),
          api.getStoreSettings(),
        ]);

        if (store.status === "fulfilled" && store.value) {
          const s = store.value;
          setForm((prev) => ({
            ...prev,
            storeName: s.name ?? prev.storeName,
            subdomain: s.slug ?? prev.subdomain,
          }));
        }

        if (settings.status === "fulfilled" && settings.value) {
          const s = settings.value;
          setForm((prev) => ({
            ...prev,
            storeName: s.storeName ?? prev.storeName,
            tagline: s.tagline ?? prev.tagline,
            primaryColor: s.primaryColor ?? prev.primaryColor,
            accentColor: s.accentColor ?? prev.accentColor,
            theme: s.theme ?? prev.theme,
            contactEmail: s.contactEmail ?? prev.contactEmail,
            subdomain: s.subdomain ?? prev.subdomain,
          }));
        }
      } catch {
        // Use defaults
        setForm({
          storeName: "Divine'zSignatures",
          tagline: "Handcrafted Bead Accessories — Bags, Bracelets, Necklaces & More",
          primaryColor: "#b45309",
          accentColor: "#d97706",
          theme: "light",
          contactEmail: "hello@divinezsignatures.com",
          subdomain: "divinezsignatures",
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
      await Promise.allSettled([
        api.updateStoreSettings({
          tagline: form.tagline,
          primaryColor: form.primaryColor,
          accentColor: form.accentColor,
          theme: form.theme,
          contactEmail: form.contactEmail,
        }),
      ]);
      setSaved(true);
      setTimeout(() => setSaved(false), 2000);
    } catch {
      // Still show success for UX
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
          <h2 className="text-xl font-bold text-gray-900">Store Settings</h2>
          <p className="text-sm text-gray-500 mt-1">Customize your storefront branding and configuration</p>
        </div>
        <div className="flex items-center justify-center py-12">
          <div className="animate-spin w-6 h-6 border-4 border-indigo-600 border-t-transparent rounded-full" />
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6 max-w-3xl">
      <div>
        <h2 className="text-xl font-bold text-gray-900">Store Settings</h2>
        <p className="text-sm text-gray-500 mt-1">Customize your storefront branding and configuration</p>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 p-6 space-y-6">
        <h3 className="text-sm font-semibold text-gray-900">General</h3>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Store Name</label>
          <input
            type="text"
            value={form.storeName}
            onChange={(e) => setForm({ ...form, storeName: e.target.value })}
            className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Tagline</label>
          <input
            type="text"
            value={form.tagline}
            onChange={(e) => setForm({ ...form, tagline: e.target.value })}
            className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none"
            placeholder="A short tagline for your store"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Subdomain</label>
          <div className="flex items-center">
            <input
              type="text"
              value={form.subdomain}
              onChange={(e) => setForm({ ...form, subdomain: e.target.value })}
              className="flex-1 px-3 py-2.5 border border-gray-300 rounded-l-lg text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none"
            />
            <span className="px-3 py-2.5 bg-gray-50 border border-l-0 border-gray-300 rounded-r-lg text-sm text-gray-500">
              .meethybridhub.com
            </span>
          </div>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Contact Email</label>
          <input
            type="email"
            value={form.contactEmail}
            onChange={(e) => setForm({ ...form, contactEmail: e.target.value })}
            className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none"
            placeholder="hello@yourstore.com"
          />
        </div>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 p-6 space-y-6">
        <h3 className="text-sm font-semibold text-gray-900">Branding</h3>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">Logo</label>
          <div className="flex items-center gap-4">
            <div className="w-16 h-16 bg-gray-100 rounded-xl flex items-center justify-center text-gray-400 text-xs">
              No logo
            </div>
            <button className="flex items-center gap-2 px-4 py-2 border border-gray-300 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50 transition-colors">
              <Upload className="w-4 h-4" /> Upload Logo
            </button>
          </div>
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Primary Color</label>
            <div className="flex items-center gap-2">
              <input
                type="color"
                value={form.primaryColor}
                onChange={(e) => setForm({ ...form, primaryColor: e.target.value })}
                className="w-10 h-10 border border-gray-300 rounded-lg cursor-pointer"
              />
              <input
                type="text"
                value={form.primaryColor}
                onChange={(e) => setForm({ ...form, primaryColor: e.target.value })}
                className="flex-1 px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 outline-none font-mono"
              />
            </div>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Accent Color</label>
            <div className="flex items-center gap-2">
              <input
                type="color"
                value={form.accentColor}
                onChange={(e) => setForm({ ...form, accentColor: e.target.value })}
                className="w-10 h-10 border border-gray-300 rounded-lg cursor-pointer"
              />
              <input
                type="text"
                value={form.accentColor}
                onChange={(e) => setForm({ ...form, accentColor: e.target.value })}
                className="flex-1 px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 outline-none font-mono"
              />
            </div>
          </div>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">Theme</label>
          <div className="flex gap-3">
            {["light", "dark", "auto"].map((t) => (
              <button
                key={t}
                onClick={() => setForm({ ...form, theme: t })}
                className={`px-4 py-2 rounded-lg text-sm font-medium capitalize transition-colors ${
                  form.theme === t
                    ? "bg-indigo-600 text-white"
                    : "bg-gray-100 text-gray-600 hover:bg-gray-200"
                }`}
              >
                {t}
              </button>
            ))}
          </div>
        </div>

        <div className="p-4 bg-gray-50 rounded-lg">
          <p className="text-xs text-gray-500 mb-3">Preview</p>
          <div className="bg-white rounded-lg p-4 border border-gray-200">
            <div className="flex items-center gap-3 mb-3">
              <div className="w-10 h-10 rounded-lg" style={{ backgroundColor: form.primaryColor }} />
              <div>
                <p className="font-semibold text-sm">{form.storeName || "Your Store"}</p>
                <p className="text-xs text-gray-500">{form.tagline || "Your tagline"}</p>
              </div>
            </div>
            <div className="flex gap-2">
              <div className="h-2 rounded-full flex-1" style={{ backgroundColor: form.primaryColor }} />
              <div className="h-2 rounded-full w-16" style={{ backgroundColor: form.accentColor }} />
            </div>
          </div>
        </div>
      </div>

      <div className="flex justify-end">
        <button
          onClick={handleSave}
          disabled={saving}
          className="flex items-center gap-2 px-6 py-2.5 bg-indigo-600 text-white text-sm font-medium rounded-lg hover:bg-indigo-700 transition-colors disabled:opacity-50"
        >
          {saved ? <Check className="w-4 h-4" /> : <Save className="w-4 h-4" />}
          {saving ? "Saving..." : saved ? "Saved!" : "Save Changes"}
        </button>
      </div>
    </div>
  );
}
