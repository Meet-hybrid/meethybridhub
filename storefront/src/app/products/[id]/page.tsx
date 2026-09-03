"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import { Star, ChevronLeft, ShoppingBag, Heart } from "lucide-react";
import { storefrontApi } from "@/lib/api";
import { useStore } from "@/components/StoreProvider";
import { useCart } from "@/components/CartProvider";

export default function ProductDetailPage() {
  const activeStore = useStore();
  const { addItem } = useCart();
  const params = useParams();
  const productId = Number(params.id);

  const [product, setProduct] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [selectedVariant, setSelectedVariant] = useState<any>(null);
  const [selectedSize, setSelectedSize] = useState<string>("");
  const [selectedColor, setSelectedColor] = useState<string>("");

  useEffect(() => {
    async function load() {
      try {
        const res = await storefrontApi.getProduct(activeStore.slug, productId);
        setProduct(res);
        if (res?.variants?.length > 0) {
          setSelectedVariant(res.variants[0]);
          setSelectedSize(res.variants[0].size ?? "");
          setSelectedColor(res.variants[0].color ?? "");
        }
      } catch {
        setProduct(activeStore.slug === "divinez-signature" ? {
          id: productId,
          name: "Crystal Charm Bracelet",
          description: "An elegant crystal beaded bracelet handcrafted with precision. Features a mix of clear and amber crystals on a durable elastic cord. Perfect for everyday wear or special occasions. Unisex design.",
          price: 8500,
          category: { name: "Bracelets" },
          variants: [
            { id: 1, sku: "CB-001-S", size: "S", color: "Clear", price: 8500, stock: 10 },
            { id: 2, sku: "CB-001-M", size: "M", color: "Clear", price: 8500, stock: 15 },
            { id: 3, sku: "CB-001-L", size: "L", color: "Clear", price: 8500, stock: 8 },
            { id: 4, sku: "CB-001-S-A", size: "S", color: "Amber", price: 8500, stock: 12 },
            { id: 5, sku: "CB-001-M-A", size: "M", color: "Amber", price: 8500, stock: 10 },
          ],
          images: [],
        } : {
          id: productId,
          name: "The Relaxed Overshirt",
          description: "A structured cotton overshirt with an easy fit, considered details, and enough room for everyday layering.",
          price: 42000,
          category: { name: "Clothing" },
          variants: [
            { id: 1, sku: "RO-001-M", size: "M", color: "Stone", price: 42000, stock: 8 },
            { id: 2, sku: "RO-001-L", size: "L", color: "Stone", price: 42000, stock: 6 },
            { id: 3, sku: "RO-001-XL", size: "XL", color: "Stone", price: 42000, stock: 4 },
          ],
          images: [],
        });
      } finally {
        setLoading(false);
      }
    }
    load();
  }, [productId]);

  useEffect(() => {
    if (product?.variants) {
      const match = product.variants.find(
        (v: any) => v.size === selectedSize && v.color === selectedColor
      );
      if (match) setSelectedVariant(match);
    }
  }, [selectedSize, selectedColor, product?.variants]);

  const formatPrice = (price: number) =>
    new Intl.NumberFormat("en-NG", { style: "currency", currency: "NGN" }).format(price);

  const isDivinez = activeStore.slug === "divinez-signature";

  if (loading) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="flex items-center justify-center py-20">
          <div className={`animate-spin w-8 h-8 border-4 border-t-transparent rounded-full ${
            isDivinez ? "border-[#0B4A2B]" : "border-amber-600"
          }`} />
        </div>
      </div>
    );
  }

  if (!product) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="text-center py-20">
          <p className={`text-lg ${isDivinez ? "text-[#0B4A2B]/60" : "text-zinc-300"}`}>Product not found</p>
          <Link href="/products" className={`text-sm mt-2 inline-block ${
            isDivinez ? "text-[#0B4A2B] hover:text-[#0B4A2B]/80" : "text-amber-600 hover:text-amber-500"
          }`}>
            ← Back to shop
          </Link>
        </div>
      </div>
    );
  }

  const sizes = [...new Set(product.variants?.map((v: any) => v.size).filter(Boolean))] as string[];
  const colors = [...new Set(product.variants?.map((v: any) => v.color).filter(Boolean))] as string[];
  const currentStock = selectedVariant?.stock ?? product.variants?.reduce((sum: number, v: any) => sum + v.stock, 0) ?? 0;
  const currentPrice = selectedVariant?.price ?? product.price;

  if (isDivinez) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 text-[#0B4A2B]">
        {/* Breadcrumb */}
        <nav className="flex items-center gap-2 text-sm text-[#0B4A2B]/50 mb-8">
          <Link href="/" className="hover:text-[#0B4A2B]">Home</Link>
          <span>/</span>
          <Link href="/products" className="hover:text-[#0B4A2B]">Shop</Link>
          <span>/</span>
          <span className="text-[#0B4A2B]/80">{product.name}</span>
        </nav>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-12">
          {/* Product Image */}
          <div className="aspect-square bg-[#0B4A2B]/5 rounded-3xl flex items-center justify-center border border-[#0B4A2B]/10">
            <span className="text-xs font-semibold tracking-[0.3em] uppercase text-[#0B4A2B]/20">
              {product.category?.name ?? activeStore.shortName}
            </span>
          </div>

          {/* Product Info */}
          <div className="flex flex-col">
            <p className="text-sm font-medium text-[#0B4A2B]/60 mb-2">
              {product.category?.name ?? "General"}
            </p>
            <h1 className="text-3xl font-serif font-bold text-[#0B4A2B]">{product.name}</h1>

            <div className="flex items-center gap-2 mt-3">
              <div className="flex gap-0.5">
                {[1, 2, 3, 4, 5].map((i) => (
                  <Star key={i} className="w-4 h-4 text-[#0B4A2B]" />
                ))}
              </div>
              <span className="text-sm text-[#0B4A2B]/50">(4.8) · 12 reviews</span>
            </div>

            <p className="text-3xl font-bold text-[#0B4A2B] mt-6">
              {formatPrice(currentPrice)}
            </p>

            {product.description && (
              <p className="text-[#0B4A2B]/70 mt-6 leading-relaxed">{product.description}</p>
            )}

            {/* Size Selection */}
            {sizes.length > 0 && (
              <div className="mt-6">
                <label className="text-sm font-medium text-[#0B4A2B] mb-2 block">Size</label>
                <div className="flex gap-2">
                  {sizes.map((size) => (
                    <button
                      key={size}
                      onClick={() => setSelectedSize(size)}
                      className={`px-4 py-2 text-sm font-medium rounded-lg border transition-colors ${
                        selectedSize === size
                          ? "border-[#0B4A2B] bg-[#0B4A2B] text-[#FAFAF8]"
                          : "border-[#0B4A2B]/20 text-[#0B4A2B] hover:border-[#0B4A2B]/40"
                      }`}
                    >
                      {size}
                    </button>
                  ))}
                </div>
              </div>
            )}

            {/* Color Selection */}
            {colors.length > 0 && (
              <div className="mt-4">
                <label className="text-sm font-medium text-[#0B4A2B] mb-2 block">Color</label>
                <div className="flex gap-2">
                  {colors.map((color) => (
                    <button
                      key={color}
                      onClick={() => setSelectedColor(color)}
                      className={`px-4 py-2 text-sm font-medium rounded-lg border transition-colors ${
                        selectedColor === color
                          ? "border-[#0B4A2B] bg-[#0B4A2B] text-[#FAFAF8]"
                          : "border-[#0B4A2B]/20 text-[#0B4A2B] hover:border-[#0B4A2B]/40"
                      }`}
                    >
                      {color}
                    </button>
                  ))}
                </div>
              </div>
            )}

            {/* Stock Status */}
            <div className="mt-6">
              {currentStock > 0 ? (
                <p className="text-sm text-[#0B4A2B] font-medium">
                  ✓ In stock ({currentStock} available)
                </p>
              ) : (
                <p className="text-sm text-red-600 font-medium">Out of stock</p>
              )}
            </div>

            {/* Actions */}
            <div className="flex gap-3 mt-8">
              <button
                disabled={currentStock === 0}
                onClick={() => selectedVariant && addItem({ variantId: selectedVariant.id, productId: product.id, name: product.name, price: currentPrice, quantity: 1, sku: selectedVariant.sku })}
                className="flex-1 flex items-center justify-center gap-2 px-6 py-3 bg-[#0B4A2B] text-[#FAFAF8] font-semibold rounded-full hover:bg-[#07331D] transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <ShoppingBag className="w-5 h-5" />
                Add to Cart
              </button>
              <button className="p-3 border border-[#0B4A2B]/20 rounded-full hover:bg-[#0B4A2B]/5 transition-colors">
                <Heart className="w-5 h-5 text-[#0B4A2B]/60" />
              </button>
            </div>

            {/* Variant Details */}
            {selectedVariant && (
              <div className="mt-6 p-4 bg-[#0B4A2B]/5 border border-[#0B4A2B]/10 rounded-xl text-sm text-[#0B4A2B]/70 space-y-1">
                <p><span className="font-medium">SKU:</span> {selectedVariant.sku}</p>
                {selectedVariant.size && <p><span className="font-medium">Size:</span> {selectedVariant.size}</p>}
                {selectedVariant.color && <p><span className="font-medium">Color:</span> {selectedVariant.color}</p>}
              </div>
            )}
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 text-white">
      {/* Breadcrumb */}
      <nav className="flex items-center gap-2 text-sm text-zinc-400 mb-8">
        <Link href="/" className="hover:text-white">Home</Link>
        <span>/</span>
        <Link href="/products" className="hover:text-white">Shop</Link>
        <span>/</span>
        <span className="text-zinc-200">{product.name}</span>
      </nav>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-12">
        {/* Product Image */}
        <div className="aspect-square bg-gradient-to-br from-amber-100 to-orange-100 rounded-3xl flex items-center justify-center">
          <span className="text-8xl opacity-60">✦</span>
        </div>

        {/* Product Info */}
        <div className="flex flex-col">
          <p className="text-sm font-medium text-amber-600 mb-2">
            {product.category?.name ?? "General"}
          </p>
          <h1 className="text-3xl font-bold text-white">{product.name}</h1>

          <div className="flex items-center gap-2 mt-3">
            <div className="flex gap-0.5">
              {[1, 2, 3, 4, 5].map((i) => (
                <Star key={i} className="w-4 h-4 text-amber-400 fill-amber-400" />
              ))}
            </div>
            <span className="text-sm text-zinc-400">(4.8) · 12 reviews</span>
          </div>

          <p className="text-3xl font-bold text-white mt-6">
            {formatPrice(currentPrice)}
          </p>

          {product.description && (
            <p className="text-zinc-300 mt-6 leading-relaxed">{product.description}</p>
          )}

          {/* Size Selection */}
          {sizes.length > 0 && (
            <div className="mt-6">
              <label className="text-sm font-medium text-zinc-200 mb-2 block">Size</label>
              <div className="flex gap-2">
                {sizes.map((size) => (
                  <button
                    key={size}
                    onClick={() => setSelectedSize(size)}
                    className={`px-4 py-2 text-sm font-medium rounded-lg border transition-colors ${
                      selectedSize === size
                        ? "border-amber-600 bg-amber-50 text-amber-600"
                        : "border-zinc-600 text-zinc-200 hover:border-zinc-300"
                    }`}
                  >
                    {size}
                  </button>
                ))}
              </div>
            </div>
          )}

          {/* Color Selection */}
          {colors.length > 0 && (
            <div className="mt-4">
              <label className="text-sm font-medium text-zinc-200 mb-2 block">Color</label>
              <div className="flex gap-2">
                {colors.map((color) => (
                  <button
                    key={color}
                    onClick={() => setSelectedColor(color)}
                    className={`px-4 py-2 text-sm font-medium rounded-lg border transition-colors ${
                      selectedColor === color
                        ? "border-amber-600 bg-amber-50 text-amber-600"
                        : "border-zinc-600 text-zinc-200 hover:border-zinc-300"
                    }`}
                  >
                    {color}
                  </button>
                ))}
              </div>
            </div>
          )}

          {/* Stock Status */}
          <div className="mt-6">
            {currentStock > 0 ? (
              <p className="text-sm text-green-600 font-medium">
                ✓ In stock ({currentStock} available)
              </p>
            ) : (
              <p className="text-sm text-red-600 font-medium">Out of stock</p>
            )}
          </div>

          {/* Actions */}
          <div className="flex gap-3 mt-8">
            <button
              disabled={currentStock === 0}
              onClick={() => selectedVariant && addItem({ variantId: selectedVariant.id, productId: product.id, name: product.name, price: currentPrice, quantity: 1, sku: selectedVariant.sku })}
              className="flex-1 flex items-center justify-center gap-2 px-6 py-3 bg-amber-600 text-white font-semibold rounded-full hover:bg-amber-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <ShoppingBag className="w-5 h-5" />
              Add to Cart
            </button>
            <button className="p-3 border border-zinc-600 rounded-full hover:bg-white/10 transition-colors">
              <Heart className="w-5 h-5 text-zinc-200" />
            </button>
          </div>

          {/* Variant Details */}
          {selectedVariant && (
            <div className="mt-6 p-4 bg-white/5 border border-white/10 rounded-xl text-sm text-zinc-300 space-y-1">
              <p><span className="font-medium">SKU:</span> {selectedVariant.sku}</p>
              {selectedVariant.size && <p><span className="font-medium">Size:</span> {selectedVariant.size}</p>}
              {selectedVariant.color && <p><span className="font-medium">Color:</span> {selectedVariant.color}</p>}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
