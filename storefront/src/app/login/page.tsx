"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useStore } from "@/components/StoreProvider";
import { useAuth } from "@/components/AuthProvider";

export default function LoginPage() {
  const activeStore = useStore();
  const { login, isAuthenticated } = useAuth();
  const router = useRouter();
  const isDivinez = activeStore.slug === "divinez-signature";

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  if (isAuthenticated) {
    router.push("/account/orders");
    return null;
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      await login(email, password);
      router.push("/account/orders");
    } catch (err: any) {
      setError(err.message || "Invalid email or password");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={`min-h-[80vh] flex items-center justify-center px-4 py-16 ${isDivinez ? "" : ""}`}>
      <div className="w-full max-w-md">
        {/* Header */}
        <div className="text-center mb-10">
          <h1 className={`text-3xl font-serif font-bold mb-2 ${isDivinez ? "text-[#0B4A2B]" : "text-[#f7f1e8]"}`}>
            Welcome back
          </h1>
          <p className={`text-sm ${isDivinez ? "text-[#0B4A2B]/60" : "text-[#aa9a8b]"}`}>
            Sign in to your {activeStore.name} account
          </p>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className={`rounded-2xl p-8 ${isDivinez ? "bg-white border border-[#0B4A2B]/10 shadow-sm" : "bg-[#17110d] border border-[#3a2b20]"}`}>
          {error && (
            <div className={`mb-6 px-4 py-3 rounded-lg text-sm ${isDivinez ? "bg-red-50 text-red-700 border border-red-200" : "bg-red-900/20 text-red-300 border border-red-800"}`}>
              {error}
            </div>
          )}

          <div className="space-y-5">
            <div>
              <label className={`block text-sm font-medium mb-1.5 ${isDivinez ? "text-[#0B4A2B]/80" : "text-[#aa9a8b]"}`}>
                Email address
              </label>
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className={`w-full px-4 py-3 rounded-lg text-sm outline-none transition-colors ${
                  isDivinez
                    ? "bg-[#FAFAF8] border border-[#0B4A2B]/20 text-[#0B4A2B] placeholder-[#0B4A2B]/40 focus:border-[#0B4A2B]/50"
                    : "bg-[#100d0b] border border-[#3a2b20] text-[#f7f1e8] placeholder-[#aa9a8b]/50 focus:border-[#9a6842]"
                }`}
                placeholder="you@example.com"
              />
            </div>

            <div>
              <label className={`block text-sm font-medium mb-1.5 ${isDivinez ? "text-[#0B4A2B]/80" : "text-[#aa9a8b]"}`}>
                Password
              </label>
              <input
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className={`w-full px-4 py-3 rounded-lg text-sm outline-none transition-colors ${
                  isDivinez
                    ? "bg-[#FAFAF8] border border-[#0B4A2B]/20 text-[#0B4A2B] placeholder-[#0B4A2B]/40 focus:border-[#0B4A2B]/50"
                    : "bg-[#100d0b] border border-[#3a2b20] text-[#f7f1e8] placeholder-[#aa9a8b]/50 focus:border-[#9a6842]"
                }`}
                placeholder="••••••••"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className={`w-full mt-6 py-3 rounded-full text-sm font-medium transition-colors disabled:opacity-50 ${
              isDivinez
                ? "bg-[#0B4A2B] text-[#FAFAF8] hover:bg-[#07331D]"
                : "bg-[#9a6842] text-white hover:bg-[#8a5a36]"
            }`}
          >
            {loading ? "Signing in…" : "Sign in"}
          </button>

          <p className={`text-center text-sm mt-6 ${isDivinez ? "text-[#0B4A2B]/60" : "text-[#aa9a8b]"}`}>
            Don&apos;t have an account?{" "}
            <Link
              href="/register"
              className={`font-medium hover:underline ${isDivinez ? "text-[#0B4A2B]" : "text-[#e0a15d]"}`}
            >
              Create one
            </Link>
          </p>
        </form>
      </div>
    </div>
  );
}
