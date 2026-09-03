"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Store, Eye, EyeOff } from "lucide-react";
import { api } from "@/lib/api";

export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      const res = await api.login(email, password);
      api.setToken(res.accessToken);
      router.push("/");
    } catch (err: any) {
      setError(err.message || "Invalid credentials");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#100d0b] flex flex-col justify-center py-12 px-4 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md">
        <div className="flex justify-center">
          <div className="w-12 h-12 bg-[#9a6842] rounded-xl flex items-center justify-center">
            <Store className="w-7 h-7 text-white" />
          </div>
        </div>
        <p className="mt-8 text-center text-[11px] tracking-[0.2em] uppercase text-[#9d8b7b]">MeethybridHub Admin</p>
        <h2 className="mt-3 text-center text-3xl font-bold text-[#f7f1e8]">
          Sign in to your dashboard
        </h2>
        <p className="mt-2 text-center text-sm text-[#9d8b7b]">
          Or{" "}
          <Link href="/register" className="text-[#c28a58] hover:text-[#e0b181] font-medium">
            create a new account
          </Link>
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-[#211712] py-8 px-6 rounded-xl border border-[#3a2b20]">
          <form onSubmit={handleSubmit} className="space-y-5">
            {error && (
              <div className="bg-[#3a1e1b] text-[#e6a49b] text-sm rounded-lg px-4 py-3 border border-[#69332c]">{error}</div>
            )}

            <div>
              <label htmlFor="email" className="block text-sm font-medium text-[#c5b5a6]">
                Email address
              </label>
              <input
                id="email"
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="mt-1 block w-full px-3 py-2.5 bg-[#17110d] border border-[#4a3424] rounded-lg text-sm text-[#f7f1e8] placeholder:text-[#756457] focus:ring-1 focus:ring-[#9a6842] focus:border-[#9a6842] outline-none"
                placeholder="you@example.com"
              />
            </div>

            <div>
              <label htmlFor="password" className="block text-sm font-medium text-[#c5b5a6]">
                Password
              </label>
              <div className="relative mt-1">
                <input
                  id="password"
                  type={showPassword ? "text" : "password"}
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="block w-full px-3 py-2.5 bg-[#17110d] border border-[#4a3424] rounded-lg text-sm text-[#f7f1e8] placeholder:text-[#756457] focus:ring-1 focus:ring-[#9a6842] focus:border-[#9a6842] outline-none pr-10"
                  placeholder="••••••••"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-[#9d8b7b] hover:text-[#f7f1e8]"
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full flex justify-center py-2.5 px-4 bg-[#9a6842] text-[#fff8ef] text-sm font-medium rounded-lg hover:bg-[#aa7650] focus:ring-1 focus:ring-[#c28a58] focus:ring-offset-2 focus:ring-offset-[#211712] disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              {loading ? "Signing in..." : "Sign in"}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
