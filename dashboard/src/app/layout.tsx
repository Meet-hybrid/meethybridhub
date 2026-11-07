import type { Metadata } from "next";
import { headers } from "next/headers";
import { getStoreBrandName } from "@/lib/api";
import "./globals.css";

export async function generateMetadata(): Promise<Metadata> {
  const host = (await headers()).get("host") ?? "";
  const brand = getStoreBrandName(host);
  return {
    title: `${brand} Dashboard`,
    description: "Multi-tenant e-commerce dashboard",
  };
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" className="h-full antialiased">
      <body className="min-h-full flex flex-col font-sans">{children}</body>
    </html>
  );
}
