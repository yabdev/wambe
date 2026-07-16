import type { NextConfig } from "next";
import path from "node:path";
import { getServerSiteOrigin } from "./src/lib/auth/site-origin";

const deployedVercelBuild =
  process.env.VERCEL_ENV === "preview" ||
  process.env.VERCEL_ENV === "production";

if (deployedVercelBuild) {
  if (process.env.NEXT_PUBLIC_DEMO_MODE === "true") {
    throw new Error("WAMBE_DEPLOYED_DEMO_MODE_FORBIDDEN");
  }
  getServerSiteOrigin();
}

const nextConfig: NextConfig = {
  allowedDevOrigins: ["127.0.0.1", "localhost"],
  reactCompiler: true,
  transpilePackages: ["@wambe/api-client"],
  turbopack: {
    root: path.resolve(__dirname, "../.."),
  },
};

export default nextConfig;
