"use client";

"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useState } from "react";
import { Menu, X, ShoppingBag, Search, User } from "lucide-react";
import { useStore } from "@/components/StoreProvider";
import { useCart } from "@/components/CartProvider";

export default function Header() {
  const activeStore = useStore();
  const { items } = useCart();
  const pathname = usePathname();
  const [mobileOpen, setMobileOpen] = useState(false);

  return (
    <header className="sticky top-0 z-50 bg-[#100d0b]/85 backdrop-blur-md border-b border-[#3a2b20]">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo */}
          <Link href="/" className="flex items-center gap-2">
            <div className="w-8 h-8 bg-[#9a6842] rounded-lg flex items-center justify-center">
              <ShoppingBag className="w-5 h-5 text-white" />
            </div>
            <div className="leading-tight">
              <span className="font-bold text-lg text-[#f7f1e8] block leading-none">{activeStore.name}</span>
              <span className="text-[9px] text-[#d99a5a] font-medium tracking-wider uppercase">{activeStore.descriptor}</span>
            </div>
          </Link>

          {/* Desktop Nav */}
          <nav className="hidden md:flex items-center gap-8">
            {activeStore.navLinks.map((link) => (
              <Link
                key={link.href}
                href={link.href}
                className={`text-sm font-medium transition-colors ${
                  pathname === link.href
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
            <Link href="/checkout" className="relative p-2 text-[#aa9a8b] hover:text-[#f7f1e8] transition-colors" aria-label="Shopping bag">
              {items.length > 0 && <span className="absolute -right-1 -top-1 min-w-4 h-4 px-1 rounded-full bg-[#d99a5a] text-[10px] text-black text-center">{items.length}</span>}
              <ShoppingBag className="w-5 h-5" />
            </Link>
            <button className="p-2 text-[#aa9a8b] hover:text-[#f7f1e8] transition-colors">
              <Search className="w-5 h-5" />
            </button>
            <button className="p-2 text-[#aa9a8b] hover:text-[#f7f1e8] transition-colors">
              <User className="w-5 h-5" />
            </button>
            <button
              onClick={() => setMobileOpen(!mobileOpen)}
              className="md:hidden p-2 text-[#aa9a8b] hover:text-[#f7f1e8]"
            >
              {mobileOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
            </button>
          </div>
        </div>
      </div>

      {/* Mobile Nav */}
      {mobileOpen && (
        <div className="md:hidden border-t border-[#3a2b20] bg-[#17110d]">
          <nav className="px-4 py-4 space-y-2">
            {activeStore.navLinks.map((link) => (
              <Link
                key={link.href}
                href={link.href}
                onClick={() => setMobileOpen(false)}
                className={`block px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
                  pathname === link.href
                    ? "bg-[#352316] text-[#e0a15d]"
                    : "text-[#aa9a8b] hover:bg-[#241912]"
                }`}
              >
                {link.label}
              </Link>
            ))}
          </nav>
        </div>
      )}
    </header>
  );
}
