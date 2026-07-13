"use client";

import { createBrowserClient } from "@supabase/ssr";

export const isDemoMode =
  process.env.NEXT_PUBLIC_DEMO_MODE === "true";

let browserClient: ReturnType<typeof createBrowserClient> | undefined;

export function getBrowserSupabase() {
  if (isDemoMode) {
    return null;
  }
  if (
    !process.env.NEXT_PUBLIC_SUPABASE_URL ||
    !process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY
  ) {
    throw new Error(
      "Supabase browser configuration is required when demo mode is disabled.",
    );
  }
  browserClient ??= createBrowserClient(
    process.env.NEXT_PUBLIC_SUPABASE_URL!,
    process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY!,
  );
  return browserClient;
}
