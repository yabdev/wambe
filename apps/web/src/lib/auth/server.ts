import { createServerClient } from "@supabase/ssr";
import { cookies } from "next/headers";

export async function getServerSupabase() {
  const url = process.env.NEXT_PUBLIC_SUPABASE_URL;
  const key = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY;
  if (!url || !key || process.env.NEXT_PUBLIC_DEMO_MODE === "true") {
    return null;
  }
  const store = await cookies();
  return createServerClient(url, key, {
    cookies: {
      getAll: () => store.getAll(),
      setAll: (values) => {
        try {
          values.forEach(({ name, value, options }) =>
            store.set(name, value, options),
          );
        } catch {
          // Server Components cannot always mutate cookies; proxy refreshes them.
        }
      },
    },
  });
}
