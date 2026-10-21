import type { NextConfig } from "next";

// Deployed on Vercel as the `dashboard` service mounted at /dashboard (see
// ../vercel.json), so every page, asset and API proxy path lives under it.
const basePath = "/dashboard";

const nextConfig: NextConfig = {
  basePath,
  env: {
    NEXT_PUBLIC_BASE_PATH: basePath,
  },
  async rewrites() {
    return [
      {
        source: "/api/:path*",
        destination: "http://localhost:8080/api/:path*",
      },
    ];
  },
};

export default nextConfig;
