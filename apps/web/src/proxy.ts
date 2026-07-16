import { createServerClient } from "@supabase/ssr";
import { NextResponse, type NextRequest } from "next/server";
import { safeRedirectPath } from "@/lib/auth/safe-redirect-path";
import { getServerSiteOrigin } from "@/lib/auth/site-origin";

export async function proxy(request: NextRequest) {
  if (
    process.env.NEXT_PUBLIC_DEMO_MODE === "true"
  ) {
    return NextResponse.next();
  }
  if (
    !process.env.NEXT_PUBLIC_SUPABASE_URL ||
    !process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY
  ) {
    if (request.nextUrl.pathname.startsWith("/events")) {
      const target = request.nextUrl.clone();
      target.pathname = "/auth";
      target.searchParams.set("error", "configuration");
      return NextResponse.redirect(target);
    }
    return NextResponse.next();
  }

  let response = NextResponse.next({ request });
  const supabase = createServerClient(
    process.env.NEXT_PUBLIC_SUPABASE_URL,
    process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY,
    {
      cookies: {
        getAll: () => request.cookies.getAll(),
        setAll: (values) => {
          values.forEach(({ name, value }) => request.cookies.set(name, value));
          response = NextResponse.next({ request });
          values.forEach(({ name, value, options }) =>
            response.cookies.set(name, value, options),
          );
        },
      },
    },
  );
  const {
    data: { user },
  } = await supabase.auth.getUser();

  if (!user && request.nextUrl.pathname.startsWith("/events")) {
    const target = request.nextUrl.clone();
    target.pathname = "/auth";
    target.searchParams.set(
      "next",
      safeRedirectPath(
        request.nextUrl.pathname,
        getServerSiteOrigin(request.nextUrl.origin),
      ),
    );
    return NextResponse.redirect(target);
  }
  return response;
}

export const config = {
  matcher: ["/events/:path*", "/auth/:path*"],
};
