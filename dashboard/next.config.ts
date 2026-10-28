import type { NextConfig } from "next";

// Deployed on Vercel as the `dashboard` service mounted at /dashboard (see
// ../vercel.json), so every page, asset and API proxy path lives under it.
const basePath = "/dashboard";

// NEXT_PUBLIC_* values are inlined at build time, so a build without the API
// URL would ship a dashboard whose browser calls http://localhost:8080. Fail
// the Vercel build instead of deploying that.
// VERCEL_ENV is "development" under `vercel dev`, so local runs are exempt.
const isDeployedBuild =
  process.env.VERCEL && process.env.VERCEL_ENV !== "development";

if (isDeployedBuild && !process.env.NEXT_PUBLIC_API_URL) {
  throw new Error(
    "NEXT_PUBLIC_API_URL is not set for this Vercel environment. Add it under " +
      "Project Settings -> Environment Variables (e.g. https://meethybridhub-api.onrender.com) " +
      "and redeploy.",
  );
}

const nextConfig: NextConfig = {
  basePath,
  env: {
    NEXT_PUBLIC_BASE_PATH: basePath,
  },
  async rewrites() {
    // Client code calls the API through NEXT_PUBLIC_API_URL (see src/lib/api.ts),
    // so this proxy is only a local-development convenience for same-origin
    // fetches. It follows the configured API origin instead of hardcoding
    // localhost, which would dead-end on a deployed build.
    const apiOrigin = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";
    return [
      {
        source: "/api/:path*",
        destination: `${apiOrigin}/api/:path*`,
      },
    ];
  },
};

export default nextConfig;
