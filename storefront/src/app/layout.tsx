import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";
import Header from "@/components/Header";
import Footer from "@/components/Footer";
import { StoreProvider } from "@/components/StoreProvider";
import { CartProvider } from "@/components/CartProvider";
import { getStoreConfigFromHost } from "@/lib/store-config";
import { headers } from "next/headers";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export async function generateMetadata(): Promise<Metadata> {
  const store = getStoreConfigFromHost((await headers()).get("host") ?? "");
  return { title: `${store.name} — ${store.tagline}`, description: store.description };
}

export default async function RootLayout({ children }: { children: React.ReactNode }) {
  const store = getStoreConfigFromHost((await headers()).get("host") ?? "");
  return (
    <html
      lang="en"
      className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}
    >
      <body data-store={store.slug} className="min-h-full flex flex-col bg-[#100d0b] text-[#f7f1e8]">
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
