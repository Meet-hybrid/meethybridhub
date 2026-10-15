"use client";

import { createContext, useContext, useEffect, useMemo, useState } from "react";

export type CartItem = {
  variantId: number;
  productId: number;
  name: string;
  price: number;
  quantity: number;
  sku?: string;
};

type CartContextValue = {
  items: CartItem[];
  addItem: (item: CartItem) => void;
  removeItem: (variantId: number) => void;
  clear: () => void;
  total: number;
};

const CartContext = createContext<CartContextValue | null>(null);

export function CartProvider({ children }: { children: React.ReactNode }) {
  const [items, setItems] = useState<CartItem[]>([]);
  useEffect(() => {
    try { setItems(JSON.parse(localStorage.getItem("meethybridhub-cart") ?? "[]")); } catch { setItems([]); }
  }, []);
  useEffect(() => { localStorage.setItem("meethybridhub-cart", JSON.stringify(items)); }, [items]);
  const value = useMemo(() => ({
    items,
    addItem: (item: CartItem) => setItems(current => {
      const found = current.find(existing => existing.variantId === item.variantId);
      return found ? current.map(existing => existing.variantId === item.variantId ? { ...existing, quantity: existing.quantity + item.quantity } : existing) : [...current, item];
    }),
    removeItem: (variantId: number) => setItems(current => current.filter(item => item.variantId !== variantId)),
    clear: () => setItems([]),
    total: items.reduce((sum, item) => sum + item.price * item.quantity, 0),
  }), [items]);
  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}

export function useCart() {
  const value = useContext(CartContext);
  if (!value) throw new Error("useCart must be used inside CartProvider");
  return value;
}
