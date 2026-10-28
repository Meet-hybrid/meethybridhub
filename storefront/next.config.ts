import type { NextConfig } from "next";

// NEXT_PUBLIC_* values are inlined at build time — a build without the API URL
// would ship a storefront whose browser calls http://localhost:8080. Fail the
// Vercel build instead of deploying that.
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
  /* config options here */
};

export default nextConfig;
