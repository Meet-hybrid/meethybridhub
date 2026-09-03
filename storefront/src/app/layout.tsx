import type { Metadata } from "next";
import "./globals.css";
import Header from "@/components/Header";
import Footer from "@/components/Footer";
import { StoreProvider } from "@/components/StoreProvider";
import { CartProvider } from "@/components/CartProvider";
import { getStoreConfigFromHost } from "@/lib/store-config";
import { headers } from "next/headers";

export async function generateMetadata(): Promise<Metadata> {
  const store = getStoreConfigFromHost((await headers()).get("host") ?? "");
  return { title: `${store.name} — ${store.tagline}`, description: store.description };
}

export default async function RootLayout({ children }: { children: React.ReactNode }) {
  const store = getStoreConfigFromHost((await headers()).get("host") ?? "");
  const isDivinez = store.slug === "divinez-signature";
  return (
    <html lang="en" className="h-full antialiased">
      <body
        data-store={store.slug}
        className={`min-h-full flex flex-col ${
          isDivinez ? "bg-[#FAFAF8] text-[#0B4A2B]" : "bg-[#100d0b] text-[#f7f1e8]"
        }`}
      >
        <StoreProvider store={store}>
          <CartProvider>
            <Header />
            <main className="flex-1">{children}</main>
            <Footer />
          </CartProvider>
        </StoreProvider>
      </body>
    </html>
  );
}
