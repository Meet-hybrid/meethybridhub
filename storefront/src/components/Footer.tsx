"use client";

import Link from "next/link";
import { useStore } from "@/components/StoreProvider";

export default function Footer() {
  const activeStore = useStore();
  const isDivinez = activeStore.slug === "divinez-signature";

  if (isDivinez) {
    return (
      <footer className="bg-[#0B4A2B] text-[#FAFAF8] mt-auto">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
            <div className="md:col-span-2">
              <p className="text-xs text-[#FAFAF8]/60 tracking-[0.18em] uppercase mb-1">{activeStore.parentBrand}</p>
              <h2 className="font-serif font-bold text-2xl text-[#FAFAF8] tracking-tight mb-4">{activeStore.subBrand}</h2>
              <p className="text-sm text-[#FAFAF8]/80 max-w-md leading-relaxed">
                {activeStore.description}
              </p>
              <p className="mt-3 text-xs text-[#FAFAF8]/60">{activeStore.subBrandDescriptor}</p>
              <div className="mt-4 flex items-center gap-2">
                <span className="text-xs text-[#FAFAF8]/60 tracking-widest uppercase">Handcrafted Beadwork · Made in Abuja</span>
              </div>
              <div className="mt-5 flex flex-wrap gap-x-5 gap-y-2 text-sm">
                <a href="https://wa.me/2348034704909" target="_blank" rel="noreferrer" className="text-[#FAFAF8]/80 hover:text-[#FAFAF8] transition-colors">📲 WhatsApp</a>
                <a href="https://www.instagram.com/beads_bydivine?igsh=M2l5ZzcybXVldnNn" target="_blank" rel="noreferrer" className="text-[#FAFAF8]/80 hover:text-[#FAFAF8] transition-colors">📸 Instagram</a>
                <a href="https://www.facebook.com/share/1DWr8Z45Q7/?mibextid=wwXIfr" target="_blank" rel="noreferrer" className="text-[#FAFAF8]/80 hover:text-[#FAFAF8] transition-colors">Facebook</a>
              </div>
            </div>

            <div>
              <h3 className="text-[#FAFAF8] font-semibold text-sm mb-4">Shop</h3>
              <ul className="space-y-2 text-sm">
                <li><Link href="/products" className="text-[#FAFAF8]/80 hover:text-[#FAFAF8] transition-colors">All products</Link></li>
                {activeStore.footerShopLinks.map((link) => <li key={link.href}><Link href={link.href} className="text-[#FAFAF8]/80 hover:text-[#FAFAF8] transition-colors">{link.label}</Link></li>)}
              </ul>
            </div>

            <div>
              <h3 className="text-[#FAFAF8] font-semibold text-sm mb-4">About</h3>
              <ul className="space-y-2 text-sm">
                <li><Link href="/about" className="text-[#FAFAF8]/80 hover:text-[#FAFAF8] transition-colors">Our Story</Link></li>
                <li><Link href="/about#contact" className="text-[#FAFAF8]/80 hover:text-[#FAFAF8] transition-colors">Contact</Link></li>
                <li><Link href="/custom-order" className="text-[#FAFAF8]/80 hover:text-[#FAFAF8] transition-colors">Custom Orders</Link></li>
              </ul>
            </div>
          </div>

          <div className="border-t border-[#FAFAF8]/20 mt-8 pt-8 flex flex-col md:flex-row items-center justify-between gap-4">
            <p className="text-xs text-[#FAFAF8]/60">&copy; {new Date().getFullYear()} {activeStore.name}. All rights reserved.</p>
            <p className="text-xs text-[#FAFAF8]/60">Handcrafted with love in Abuja, Nigeria</p>
          </div>
        </div>
      </footer>
    );
  }

  return (
    <footer className="bg-gray-900 text-gray-400 mt-auto">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
          <div className="md:col-span-2">
            <div className="flex items-center gap-2 mb-4">
              <div className="w-8 h-8 bg-amber-600 rounded-lg flex items-center justify-center">
                <span className="text-white font-bold text-sm">M</span>
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
              <li><Link href="/about" className="hover:text-white transition-colors">About Us</Link></li>
              <li><Link href="/about#contact" className="hover:text-white transition-colors">Contact</Link></li>
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
