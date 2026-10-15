"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { AuthProvider, useAuth } from "@/lib/auth-context";

function AuthInner({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!loading && user) {
      router.push("/");
    }
  }, [user, loading, router]);

  if (loading) {
    return (
      <div className="min-h-screen bg-[#100d0b] flex items-center justify-center">
        <div className="animate-spin w-8 h-8 border-2 border-[#9a6842] border-t-transparent rounded-full" />
      </div>
    );
  }

  if (user) return null;

  return <>{children}</>;
}

export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return (
    <AuthProvider>
      <AuthInner>{children}</AuthInner>
    </AuthProvider>
  );
}
