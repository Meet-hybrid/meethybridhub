"use client";

import { createContext, useContext } from "react";
import type { StoreConfig } from "@/lib/store-config";

const StoreContext = createContext<StoreConfig | null>(null);

export function StoreProvider({ store, children }: { store: StoreConfig; children: React.ReactNode }) {
  return <StoreContext.Provider value={store}>{children}</StoreContext.Provider>;
}

export function useStore(): StoreConfig {
  const store = useContext(StoreContext);
  if (!store) throw new Error("useStore must be used inside StoreProvider");
  return store;
}
