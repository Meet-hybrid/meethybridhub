"use client";

import { Suspense, useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Search, Star, Heart } from "lucide-react";
import { storefrontApi } from "@/lib/api";
import { useStore } from "@/components/StoreProvider";

function ProductsContent() {
  const activeStore = useStore();
  const searchParams = useSearchParams();
  const initialCategory = searchParams.get("category") ?? "";
  const initialSearch = searchParams.get("search") ?? "";

  const [products, setProducts] = useState<any[]>([]);
  const [categories, setCategories] = useState<any[]>([]);
  const [selectedCategory, setSelectedCategory] = useState(initialCategory);
  const [searchQuery, setSearchQuery] = useState(initialSearch);
  const [loading, setLoading] = useState(true);

  const categoryEmojis: Record<string, string> = {
    Bracelets: "📿",
    Necklaces: "💎",
    Anklets: "🦶",
    "Waist Beads": "✨",
    Bags: "👜",
    Sets: "🎁",
  };

  const loadProducts = useCallback(async () => {
    setLoading(true);
    try {
      const params: any = { size: 50 };
      if (selectedCategory) params.category = selectedCategory;
      if (searchQuery) params.search = searchQuery;
      const res = await storefrontApi.getProducts(activeStore.slug, params);
      const list = Array.isArray(res) ? res : res?.content ?? res?.data ?? [];
      setProducts(list.filter((p: any) => p.status === "PUBLISHED"));
    } catch {
      setProducts(activeStore.slug === "divinez-signature" ? [
        { id: 1, name: "Crystal Charm Bracelet", price: 8500, category: { name: "Bracelets" }, description: "Elegant crystal beaded bracelet" },
        { id: 2, name: "Pearl Layered Necklace", price: 15000, category: { name: "Necklaces" }, description: "Multi-layer pearl bead necklace" },
        { id: 3, name: "Ankara Beaded Bag", price: 25000, category: { name: "Bags" }, description: "Handmade beaded clutch with Ankara print" },
        { id: 4, name: "Gold Waist Bead Set", price: 12000, category: { name: "Waist Beads" }, description: "Traditional gold-toned waist beads" },
        { id: 5, name: "Seed Bead Anklet", price: 5500, category: { name: "Anklets" }, description: "Delicate seed bead anklet" },
        { id: 6, name: "Crystal Choker Set", price: 18000, category: { name: "Sets" }, description: "Matching choker & bracelet set" },
        { id: 7, name: "Wooden Bead Bracelet", price: 6000, category: { name: "Bracelets" }, description: "Natural wooden bead bracelet — unisex" },
        { id: 8, name: "Beaded Statement Necklace", price: 22000, category: { name: "Necklaces" }, description: "Bold beaded statement piece" },
        { id: 9, name: "Seed Bead Waist Set", price: 9500, category: { name: "Waist Beads" }, description: "Colorful seed bead waist set" },
        { id: 10, name: "Beaded Crossbody Bag", price: 30000, category: { name: "Bags" }, description: "Full beaded crossbody bag" },
        { id: 11, name: "Men's Wooden Bracelet", price: 7000, category: { name: "Bracelets" }, description: "Masculine wooden & stone bead bracelet" },
        { id: 12, name: "Anklet & Bracelet Set", price: 11000, category: { name: "Sets" }, description: "Matching anklet and bracelet combo" },
      ] : [
        { id: 1, name: "The Relaxed Overshirt", price: 42000, category: { name: "Clothing" }, description: "A structured cotton overshirt with an easy fit" },
        { id: 2, name: "Low-Top Leather Sneaker", price: 68000, category: { name: "Sneakers" }, description: "Clean lines, soft leather, everyday comfort" },
        { id: 3, name: "Wide Leg Trouser", price: 38000, category: { name: "Clothing" }, description: "A tailored trouser with a relaxed drape" },
        { id: 4, name: "Canvas Court Shoe", price: 32000, category: { name: "Shoes" }, description: "A dependable everyday pair in natural canvas" },
        { id: 5, name: "Heavyweight Everyday Tee", price: 18000, category: { name: "Essentials" }, description: "Dense cotton jersey, cut for a clean fit" },
        { id: 6, name: "Structured Crossbody", price: 46000, category: { name: "Accessories" }, description: "A compact carry-all for daily essentials" },
      ]);
    } finally {
      setLoading(false);
    }
  }, [activeStore.slug, searchQuery, selectedCategory]);

  useEffect(() => {
    async function loadCategories() {
      try {
        const res: any = await storefrontApi.getCategories(activeStore.slug);
        const list = Array.isArray(res) ? res : res?.content ?? res?.data ?? [];
        setCategories(list);
      } catch {
        setCategories(activeStore.categories.map((category, id) => ({ id, name: category.name })));
      }
    }
    loadCategories();
  }, [activeStore.categories, activeStore.slug]);

  useEffect(() => {
    loadProducts();
  }, [loadProducts]);

  const handleSearch = () => {
    loadProducts();
  };

  const filteredProducts = products.filter((p) => {
    if (selectedCategory && p.category?.name?.toLowerCase() !== selectedCategory.toLowerCase()) return false;
    if (searchQuery && !p.name.toLowerCase().includes(searchQuery.toLowerCase())) return false;
    return true;
  });

  const formatPrice = (price: number) =>
    new Intl.NumberFormat("en-NG", { style: "currency", currency: "NGN" }).format(price);

  const displayCategories = categories.length > 0 ? categories : activeStore.categories.map((category, id) => ({ id, name: category.name }));
  const isDivinez = activeStore.slug === "divinez-signature";

  return (
    <div className={`max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 ${
      isDivinez ? "text-[#0B4A2B]" : "text-gray-900"
    }`}>
      {/* Page Header */}
      <div className="mb-8">
        {isDivinez ? (
          <>
            <span className="text-[#0B4A2B]/70 uppercase tracking-widest text-xs font-semibold">Our Collection</span>
            <h1 className="text-3xl font-serif font-bold text-[#0B4A2B] mt-1">Shop All</h1>
            <p className="text-[#0B4A2B]/70 mt-2">{activeStore.description}</p>
          </>
        ) : (
          <>
            <h1 className="text-3xl font-bold text-gray-900">Shop</h1>
            <p className="text-gray-500 mt-2">{activeStore.description}</p>
          </>
        )}
      </div>

      {/* Filters */}
      <div className="flex flex-col sm:flex-row gap-4 mb-8">
        <div className="relative flex-1">
          <Search className={`absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 ${
            isDivinez ? "text-[#0B4A2B]/40" : "text-gray-400"
          }`} />
          <input
            type="text"
            placeholder={`Search ${activeStore.shortName.toLowerCase()} products...`}
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && handleSearch()}
            className={`w-full pl-10 pr-4 py-2.5 border rounded-full text-sm outline-none ${
              isDivinez
                ? "border-[#0B4A2B]/20 focus:ring-2 focus:ring-[#0B4A2B]/20 focus:border-[#0B4A2B]/40"
                : "border-gray-300 focus:ring-2 focus:ring-amber-500 focus:border-amber-500"
            }`}
          />
        </div>
        <div className="flex flex-wrap gap-2">
          <button
            onClick={() => setSelectedCategory("")}
            className={`px-4 py-2 text-sm font-medium rounded-full transition-colors ${
              isDivinez
                ? !selectedCategory
                  ? "bg-[#0B4A2B] text-[#FAFAF8]"
                  : "bg-[#0B4A2B]/10 text-[#0B4A2B] hover:bg-[#0B4A2B]/15"
                : !selectedCategory
                  ? "bg-amber-600 text-white"
                  : "bg-gray-100 text-gray-600 hover:bg-gray-200"
            }`}
          >
            All
          </button>
          {displayCategories.map((cat: any) => (
            <button
              key={cat.id}
              onClick={() => setSelectedCategory(cat.name)}
              className={`px-4 py-2 text-sm font-medium rounded-full transition-colors ${
                isDivinez
                  ? selectedCategory === cat.name
                    ? "bg-[#0B4A2B] text-[#FAFAF8]"
                    : "bg-[#0B4A2B]/10 text-[#0B4A2B] hover:bg-[#0B4A2B]/15"
                  : selectedCategory === cat.name
                    ? "bg-amber-600 text-white"
                    : "bg-gray-100 text-gray-600 hover:bg-gray-200"
              }`}
            >
              {categoryEmojis[cat.name] ?? "📿"} {cat.name}
            </button>
          ))}
        </div>
      </div>

      {/* Products Grid */}
      {loading ? (
        <div className="flex items-center justify-center py-20">
          <div className={`animate-spin w-8 h-8 border-4 border-t-transparent rounded-full ${
            isDivinez ? "border-[#0B4A2B]" : "border-amber-600"
          }`} />
        </div>
      ) : filteredProducts.length === 0 ? (
        <div className="text-center py-20">
          <p className={`text-lg ${isDivinez ? "text-[#0B4A2B]/60" : "text-gray-500"}`}>No products found</p>
          <p className={`text-sm mt-1 ${isDivinez ? "text-[#0B4A2B]/40" : "text-gray-400"}`}>Try adjusting your filters or search terms</p>
        </div>
      ) : isDivinez ? (
        /* DivinezSignature Product Grid */
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          {filteredProducts.map((product: any) => (
            <Link
              key={product.id}
              href={`/products/${product.id}`}
              className="group bg-[#FAFAF8] rounded-2xl border border-[#0B4A2B]/10 overflow-hidden hover:shadow-lg transition-all"
            >
              <div className="aspect-[3/4] bg-[#0B4A2B]/5 flex items-center justify-center relative">
                <span className="text-xs font-semibold tracking-[0.24em] uppercase text-[#0B4A2B]/30">
                  {product.category?.name ?? activeStore.shortName}
                </span>
                <div className="absolute top-3 right-3">
                  <button className="w-8 h-8 rounded-full bg-white/80 flex items-center justify-center text-[#0B4A2B]/50 hover:text-[#0B4A2B] hover:bg-white transition-colors">
                    <Heart className="w-4 h-4" />
                  </button>
                </div>
              </div>
              <div className="p-4">
                <p className="text-xs text-[#0B4A2B]/60 font-medium mb-1">
                  {product.category?.name ?? "General"}
                </p>
                <h3 className="font-semibold text-[#0B4A2B] group-hover:text-[#0B4A2B]/80 transition-colors line-clamp-1">
                  {product.name}
                </h3>
                {product.description && (
                  <p className="text-xs text-[#0B4A2B]/50 mt-1 line-clamp-2">{product.description}</p>
                )}
                <div className="flex items-center justify-between mt-3">
                  <p className="text-lg font-bold text-[#0B4A2B]">
                    {formatPrice(product.price)}
                  </p>
                  <div className="flex items-center gap-0.5">
                    <Star className="w-3 h-3 text-[#0B4A2B]" />
                    <span className="text-xs text-[#0B4A2B]/50">4.9</span>
                  </div>
                </div>
              </div>
            </Link>
          ))}
        </div>
      ) : (
        /* MeethybridHub Product Grid (default) */
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          {filteredProducts.map((product: any) => (
            <Link
              key={product.id}
              href={`/products/${product.id}`}
              className="group bg-white rounded-2xl border border-gray-100 overflow-hidden hover:shadow-lg transition-all"
            >
              <div className="aspect-[3/4] bg-gradient-to-br from-amber-100 to-orange-100 flex items-center justify-center">
                <span className="text-5xl opacity-60">
                  {categoryEmojis[product.category?.name] ?? "📿"}
                </span>
              </div>
              <div className="p-4">
                <p className="text-xs text-amber-600 font-medium mb-1">
                  {product.category?.name ?? "General"}
                </p>
                <h3 className="font-semibold text-gray-900 group-hover:text-amber-600 transition-colors line-clamp-1">
                  {product.name}
                </h3>
                {product.description && (
                  <p className="text-xs text-gray-500 mt-1 line-clamp-2">{product.description}</p>
                )}
                <div className="flex items-center justify-between mt-3">
                  <p className="text-lg font-bold text-gray-900">
                    {formatPrice(product.price)}
                  </p>
                  <div className="flex items-center gap-0.5">
                    <Star className="w-3 h-3 text-amber-400 fill-amber-400" />
                    <span className="text-xs text-gray-500">4.9</span>
                  </div>
                </div>
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}

export default function ProductsPage() {
  return <Suspense fallback={<div className="min-h-[50vh]" />}><ProductsContent /></Suspense>;
}
