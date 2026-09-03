"use client";

import Link from "next/link";
import { ShoppingBag } from "lucide-react";
import { useStore } from "@/components/StoreProvider";

export default function Footer() {
  const activeStore = useStore();
  return (
    <footer className="bg-gray-900 text-gray-400 mt-auto">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
          <div className="md:col-span-2">
            <div className="flex items-center gap-2 mb-4">
              <div className="w-8 h-8 bg-amber-600 rounded-lg flex items-center justify-center">
                <ShoppingBag className="w-5 h-5 text-white" />
              </div>
              <div>
                <span className="font-bold text-lg text-white block leading-none">{activeStore.name}</span>
                <span className="text-[9px] text-amber-400 font-medium tracking-wider uppercase">{activeStore.descriptor}</span>
              </div>
            </div>
            <p className="text-sm max-w-md leading-relaxed">
              {activeStore.description}
            </p>
          </div>

          <div>
            <h3 className="text-white font-semibold text-sm mb-4">Shop</h3>
            <ul className="space-y-2 text-sm">
              <li><Link href="/products" className="hover:text-white transition-colors">All products</Link></li>
              {activeStore.footerShopLinks.map((link) => <li key={link.href}><Link href={link.href} className="hover:text-white transition-colors">{link.label}</Link></li>)}
            </ul>
          </div>

          <div>
            <h3 className="text-white font-semibold text-sm mb-4">Company</h3>
            <ul className="space-y-2 text-sm">
              <li>
                <Link href="/about" className="hover:text-white transition-colors">
                  About Us
                </Link>
              </li>
              <li>
                <Link href="/about#contact" className="hover:text-white transition-colors">
                  Contact
                </Link>
              </li>
            </ul>
          </div>
        </div>

        <div className="border-t border-gray-800 mt-8 pt-8 text-center text-xs">
          <p>&copy; {new Date().getFullYear()} {activeStore.name}. All rights reserved.</p>
        </div>
      </div>
    </footer>
  );
}
