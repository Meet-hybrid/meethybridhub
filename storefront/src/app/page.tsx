"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { ArrowRight, Star, Truck, Shield, Palette } from "lucide-react";
import { storefrontApi } from "@/lib/api";
import { useStore } from "@/components/StoreProvider";

export default function HomePage() {
  const activeStore = useStore();
  const [products, setProducts] = useState<any[]>([]);
  const [categories, setCategories] = useState<any[]>([]);

  useEffect(() => {
    async function load() {
      try {
        const [productsRes, categoriesRes] = await Promise.allSettled([
          storefrontApi.getProducts(activeStore.slug, { size: 8 }),
          storefrontApi.getCategories(activeStore.slug),
        ]);

        if (productsRes.status === "fulfilled" && productsRes.value) {
          const val: any = productsRes.value;
          const list = Array.isArray(val) ? val : val?.content ?? val?.data ?? [];
          setProducts(list);
        }

        if (categoriesRes.status === "fulfilled" && categoriesRes.value) {
          const val: any = categoriesRes.value;
          const list = Array.isArray(val) ? val : val?.content ?? val?.data ?? [];
          setCategories(list);
        }
      } catch {
        // Use static fallback data
      }
    }
    load();
  }, []);

  const formatPrice = (price: number) =>
    new Intl.NumberFormat("en-NG", { style: "currency", currency: "NGN" }).format(price);

  const displayCategories = categories.length > 0 ? categories : activeStore.categories;

  const displayProducts = products.length > 0
    ? products.slice(0, 8)
    : activeStore.slug === "divinez-signature" ? [
        { id: 1, name: "Crystal Charm Bracelet", price: 8500, category: { name: "Bracelets" }, description: "A hand-finished bracelet with a soft shimmer" },
        { id: 2, name: "Pearl Layered Necklace", price: 15000, category: { name: "Necklaces" }, description: "Layered pearls with an easy, luminous drape" },
        { id: 3, name: "Gold Waist Bead Set", price: 12000, category: { name: "Waist Beads" }, description: "Gold-toned beads made for your own rhythm" },
        { id: 4, name: "Beaded Evening Bag", price: 28000, category: { name: "Beaded Bags" }, description: "A small statement for evenings worth dressing for" },
        { id: 5, name: "Natural Stone Anklet", price: 9000, category: { name: "Anklets" }, description: "Natural stones and a quiet, wearable finish" },
        { id: 6, name: "Seed Bead Waist Set", price: 9500, category: { name: "Waist Beads" }, description: "Colorful seed beads, made to order" },
        { id: 7, name: "Wooden Bead Bracelet", price: 6000, category: { name: "Bracelets" }, description: "Natural wooden beads with an unisex profile" },
        { id: 8, name: "Beadwork Gift Set", price: 22000, category: { name: "Sets & Gifts" }, description: "A considered trio, ready to give" },
      ] : [
        { id: 1, name: "The Relaxed Overshirt", price: 42000, category: { name: "Clothing" }, description: "A structured cotton overshirt with an easy fit" },
        { id: 2, name: "Low-Top Leather Sneaker", price: 68000, category: { name: "Sneakers" }, description: "Clean lines, soft leather, everyday comfort" },
        { id: 3, name: "Wide Leg Trouser", price: 38000, category: { name: "Clothing" }, description: "A tailored trouser with a relaxed drape" },
        { id: 4, name: "Canvas Court Shoe", price: 32000, category: { name: "Shoes" }, description: "A dependable everyday pair in natural canvas" },
        { id: 5, name: "Heavyweight Everyday Tee", price: 18000, category: { name: "Essentials" }, description: "Dense cotton jersey, cut for a clean fit" },
        { id: 6, name: "Structured Crossbody", price: 46000, category: { name: "Accessories" }, description: "A compact carry-all for daily essentials" },
        { id: 7, name: "Washed Utility Jacket", price: 55000, category: { name: "Clothing" }, description: "Layer-friendly utility jacket with considered details" },
        { id: 8, name: "Suede Runner", price: 72000, category: { name: "Sneakers" }, description: "Low-profile suede runner with a grounded palette" },
      ];

  return (
    <div className="min-h-screen flex flex-col">
      {/* Hero Section */}
      <section className="relative bg-[#241914] text-white overflow-hidden border-b border-[#3a2b20]">
        <div className="pointer-events-none absolute right-[8%] top-16 hidden md:block float-slow">
          <div className="h-44 w-44 rounded-full border border-[#8a6548]/50" />
        </div>
        <div className="pointer-events-none absolute right-[17%] bottom-12 hidden lg:block float-slower">
          <span className="block border border-[#8a6548]/40 px-4 py-2 text-[10px] tracking-[0.25em] uppercase text-[#a98361] rotate-[-8deg]">The daily edit</span>
        </div>
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20 md:py-28 relative">
          <div className="max-w-2xl">
            <p className="text-[#c28a58] font-medium text-sm tracking-widest uppercase mb-4">{activeStore.heroLabel}</p>
            <h1 className="text-4xl md:text-5xl lg:text-6xl font-bold leading-tight">
              {activeStore.heroTitle}
              <br />
              <span className="text-[#c28a58]">{activeStore.heroAccent}</span>
            </h1>
            <p className="mt-6 text-lg text-[#c5b5a6] max-w-lg leading-relaxed">
              {activeStore.heroBody}
            </p>
            <div className="mt-8 flex flex-wrap gap-4">
              <Link
                href="/products"
                className="inline-flex items-center gap-2 px-6 py-3 bg-[#e9dfd4] text-[#241914] font-semibold rounded-full hover:bg-white transition-colors"
              >
                Shop the edit <ArrowRight className="w-4 h-4" />
              </Link>
              <Link
                href="/custom-order"
                className="inline-flex items-center gap-2 px-6 py-3 border-2 border-white/40 text-white font-semibold rounded-full hover:bg-white/10 transition-colors"
              >
                {activeStore.slug === "divinez-signature" ? "Request a custom piece" : "Build your personal edit"}
              </Link>
            </div>
          </div>
        </div>
      </section>

      {/* Features Strip */}
      <section className="bg-[#17110d] border-b border-[#3a2b20]">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {activeStore.features.map((feature, index) => {
              const Icon = [Truck, Palette, Shield][index];
              return (
              <div key={feature.title} className="flex items-center gap-4">
                <div className="w-10 h-10 bg-[#352316] rounded-full flex items-center justify-center shrink-0">
                  <Icon className="w-5 h-5 text-[#e0a15d]" />
                </div>
                <div>
                  <p className="font-semibold text-sm text-[#f7f1e8]">{feature.title}</p>
                  <p className="text-xs text-[#aa9a8b]">{feature.description}</p>
                </div>
              </div>
              );
            })}
          </div>
        </div>
      </section>

      {/* Categories */}
      <section className="py-16">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between mb-8">
            <div>
              <h2 className="text-2xl font-bold text-[#f7f1e8]">Shop by category</h2>
              <p className="text-sm text-[#aa9a8b] mt-1">{activeStore.description}</p>
            </div>
          </div>
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
            {displayCategories.map((cat: any) => (
              <Link
                key={cat.id}
                href={`/products?category=${cat.name?.toLowerCase() ?? ""}`}
                className="group relative bg-[#1b1511] rounded-2xl p-5 text-center hover:bg-[#241c17] transition-colors border border-[#3a2b20]"
              >
                <div className="w-12 h-12 bg-[#2a2019] rounded-xl flex items-center justify-center mx-auto mb-3 group-hover:bg-[#36291f] transition-colors">
                  <span className="text-xs font-semibold tracking-[0.2em] text-[#c28a58]">{String(cat.name ?? "").slice(0, 2).toUpperCase()}</span>
                </div>
                <h3 className="font-semibold text-[#f7f1e8] text-sm">{cat.name}</h3>
                {cat.description && (
                  <p className="text-[10px] text-[#aa9a8b] mt-1 line-clamp-2">{cat.description}</p>
                )}
              </Link>
            ))}
          </div>
        </div>
      </section>

      {/* Featured Products */}
      <section className="py-16 bg-[#17110d] border-y border-[#2d2119]">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between mb-8">
            <div>
              <h2 className="text-2xl font-bold text-[#f7f1e8]">Featured Pieces</h2>
              <p className="text-sm text-[#aa9a8b] mt-1">The pieces we keep coming back to</p>
            </div>
            <Link
              href="/products"
              className="text-sm font-medium text-[#e0a15d] hover:text-[#f2bd7d]"
            >
              View all →
            </Link>
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
            {displayProducts.map((product: any) => (
              <Link
                key={product.id}
                href={`/products/${product.id}`}
                className="group bg-[#211712] rounded-2xl border border-[#3a2b20] overflow-hidden hover:border-[#60432d] transition-colors"
              >
                <div className="aspect-[3/4] bg-[#2a2019] flex items-center justify-center">
                  <span className="text-xs font-semibold tracking-[0.24em] uppercase text-[#8f6f53]">
                    {product.category?.name ?? activeStore.shortName}
                  </span>
                </div>
                <div className="p-4">
                  <p className="text-xs text-[#d99a5a] font-medium mb-1">
                    {product.category?.name ?? "General"}
                  </p>
                  <h3 className="font-semibold text-[#f7f1e8] group-hover:text-[#e0a15d] transition-colors line-clamp-1">
                    {product.name}
                  </h3>
                  <div className="flex items-center justify-between mt-2">
                    <p className="text-lg font-bold text-[#f7f1e8]">
                      {formatPrice(product.price)}
                    </p>
                    <div className="flex items-center gap-0.5">
                      <Star className="w-3 h-3 text-[#c28a58]" />
                      <span className="text-xs text-[#9d8b7b]">4.9</span>
                    </div>
                  </div>
                </div>
              </Link>
            ))}
          </div>
        </div>
      </section>

      {/* Custom Order CTA */}
      <section className="py-16">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="bg-[#2a2019] border border-[#4a3424] rounded-3xl p-8 md:p-12 text-white text-center">
            <h2 className="text-3xl font-bold mb-4">Find your everyday uniform</h2>
            <p className="text-[#c5b5a6] max-w-lg mx-auto mb-8">
              Start with the essentials, then make them yours. Explore the edit or speak with us
              about finding the right piece.
            </p>
            <Link
              href="/custom-order"
              className="inline-flex items-center gap-2 px-8 py-4 bg-[#e9dfd4] text-[#241914] font-bold rounded-full hover:bg-white transition-colors text-lg"
            >
              Explore the collection <ArrowRight className="w-5 h-5" />
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
}
