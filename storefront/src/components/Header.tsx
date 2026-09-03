"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useState } from "react";
import { Menu, X, ShoppingBag, Search, User } from "lucide-react";
import { useStore } from "@/components/StoreProvider";
import { useCart } from "@/components/CartProvider";
import { useAuth } from "@/components/AuthProvider";

export default function Header() {
  const activeStore = useStore();
  const { items } = useCart();
  const { isAuthenticated } = useAuth();
  const pathname = usePathname();
  const [mobileOpen, setMobileOpen] = useState(false);
  const isDivinez = activeStore.slug === "divinez-signature";

  return (
    <header className={`sticky top-0 z-50 backdrop-blur-md border-b ${
      isDivinez
        ? "bg-[#FAFAF8]/85 border-[#0B4A2B]/10"
        : "bg-[#100d0b]/85 border-[#3a2b20]"
    }`}>
      {/* Announcement Bar */}
      {isDivinez && (
        <div className="bg-[#0B4A2B] text-[#FAFAF8] text-xs font-medium py-2 text-center tracking-widest uppercase">
          {activeStore.descriptor}
        </div>
      )}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo */}
          <Link href="/" className="flex items-center gap-2">
            {isDivinez ? (
              <div className="leading-tight">
                <span className="font-serif font-bold text-2xl text-[#0B4A2B] tracking-tight">{activeStore.name}</span>
              </div>
            ) : (
              <>
                <div className="w-8 h-8 bg-[#9a6842] rounded-lg flex items-center justify-center">
                  <ShoppingBag className="w-5 h-5 text-white" />
                </div>
                <div className="leading-tight">
                  <span className="font-bold text-lg text-[#f7f1e8] block leading-none">{activeStore.name}</span>
                  <span className="text-[9px] text-[#d99a5a] font-medium tracking-wider uppercase">{activeStore.descriptor}</span>
                </div>
              </>
            )}
          </Link>

          {/* Desktop Nav */}
          <nav className="hidden md:flex items-center gap-8">
            {activeStore.navLinks.map((link) => (
              <Link
                key={link.href}
                href={link.href}
                className={`text-sm font-medium transition-colors ${
                  isDivinez
                    ? pathname === link.href
                      ? "text-[#0B4A2B]"
                      : "text-[#0B4A2B]/80 hover:text-[#0B4A2B]"
                    : pathname === link.href
                      ? "text-[#e0a15d]"
                      : "text-[#aa9a8b] hover:text-[#f7f1e8]"
                }`}
              >
                {link.label}
              </Link>
            ))}
          </nav>

          {/* Actions */}
          <div className="flex items-center gap-3">
            <Link href="/checkout" className={`relative p-2 transition-colors ${
              isDivinez ? "text-[#0B4A2B]/70 hover:text-[#0B4A2B]" : "text-[#aa9a8b] hover:text-[#f7f1e8]"
            }`} aria-label="Shopping bag">
              {items.length > 0 && <span className={`absolute -right-1 -top-1 min-w-4 h-4 px-1 rounded-full text-[10px] text-center ${
                isDivinez ? "bg-[#0B4A2B] text-[#FAFAF8]" : "bg-[#d99a5a] text-black"
              }`}>{items.length}</span>}
              <ShoppingBag className="w-5 h-5" />
            </Link>
            <button className={`p-2 transition-colors ${
              isDivinez ? "text-[#0B4A2B]/70 hover:text-[#0B4A2B]" : "text-[#aa9a8b] hover:text-[#f7f1e8]"
            }`}>
              <Search className="w-5 h-5" />
            </button>
            <Link
              href={isAuthenticated ? "/account/orders" : "/login"}
              className={`p-2 transition-colors ${
                isDivinez ? "text-[#0B4A2B]/70 hover:text-[#0B4A2B]" : "text-[#aa9a8b] hover:text-[#f7f1e8]"
              }`}
              aria-label={isAuthenticated ? "My account" : "Sign in"}
            >
              <User className="w-5 h-5" />
            </Link>
            <button
              onClick={() => setMobileOpen(!mobileOpen)}
              className={`md:hidden p-2 ${
                isDivinez ? "text-[#0B4A2B]/70 hover:text-[#0B4A2B]" : "text-[#aa9a8b] hover:text-[#f7f1e8]"
              }`}
            >
              {mobileOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
            </button>
          </div>
        </div>
      </div>

      {/* Mobile Nav */}
      {mobileOpen && (
        <div className={`md:hidden border-t ${
          isDivinez ? "border-[#0B4A2B]/10 bg-white" : "border-[#3a2b20] bg-[#17110d]"
        }`}>
          <nav className="px-4 py-4 space-y-2">
            {activeStore.navLinks.map((link) => (
              <Link
                key={link.href}
                href={link.href}
                onClick={() => setMobileOpen(false)}
                className={`block px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
                  isDivinez
                    ? pathname === link.href
                      ? "bg-[#0B4A2B]/10 text-[#0B4A2B]"
                      : "text-[#0B4A2B]/80 hover:bg-[#0B4A2B]/5"
                    : pathname === link.href
                      ? "bg-[#352316] text-[#e0a15d]"
                      : "text-[#aa9a8b] hover:bg-[#241912]"
                }`}
              >
                {link.label}
              </Link>
            ))}
            {/* Account link in mobile menu */}
            <div className={`pt-2 mt-2 border-t ${isDivinez ? "border-[#0B4A2B]/10" : "border-[#3a2b20]"}`}>
              <Link
                href={isAuthenticated ? "/account/orders" : "/login"}
                onClick={() => setMobileOpen(false)}
                className={`block px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
                  isDivinez
                    ? (pathname.startsWith("/account") || pathname === "/login" || pathname === "/register"
                        ? "bg-[#0B4A2B]/10 text-[#0B4A2B]"
                        : "text-[#0B4A2B]/80 hover:bg-[#0B4A2B]/5")
                    : (pathname.startsWith("/account") || pathname === "/login" || pathname === "/register"
                        ? "bg-[#352316] text-[#e0a15d]"
                        : "text-[#aa9a8b] hover:bg-[#241912]")
                }`}
              >
                {isAuthenticated ? "My Orders" : "Sign in"}
              </Link>
            </div>
          </nav>
        </div>
      )}
    </header>
  );
}
