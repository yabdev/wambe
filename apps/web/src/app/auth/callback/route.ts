import { NextResponse, type NextRequest } from "next/server";
import { getServerSupabase } from "@/lib/auth/server";
import { recordAuthCallbackOutcome } from "@/lib/auth/auth-callback-outcome";
import { evaluateRedirectPath } from "@/lib/auth/safe-redirect-path";
import { getServerSiteOrigin } from "@/lib/auth/site-origin";

export async function GET(request: NextRequest) {
  let siteOrigin: string;
  try {
    siteOrigin = getServerSiteOrigin(request.nextUrl.origin);
  } catch {
    recordAuthCallbackOutcome("configuration_error", {
      usedFallback: false,
    });
    return NextResponse.json(
      { error: "Authentication configuration is unavailable." },
      { status: 500 },
    );
  }

  const code = request.nextUrl.searchParams.get("code");
  const redirect = evaluateRedirectPath(
    request.nextUrl.searchParams.get("next"),
    siteOrigin,
  );
  if (!code) {
    recordAuthCallbackOutcome("missing_code", redirect);
    return NextResponse.redirect(
      new URL("/auth?error=callback", siteOrigin),
    );
  }

  let supabase;
  try {
    supabase = await getServerSupabase();
  } catch {
    recordAuthCallbackOutcome("configuration_error", redirect);
    return NextResponse.redirect(
      new URL("/auth?error=configuration", siteOrigin),
    );
  }

  if (!supabase) {
    recordAuthCallbackOutcome("configuration_error", redirect);
    return NextResponse.redirect(
      new URL("/auth?error=configuration", siteOrigin),
    );
  }

  const { error } = await supabase.auth.exchangeCodeForSession(code);
  if (error) {
    recordAuthCallbackOutcome("exchange_failed", redirect);
    return NextResponse.redirect(
      new URL("/auth?error=callback", siteOrigin),
    );
  }

  recordAuthCallbackOutcome("success", redirect);
  return NextResponse.redirect(new URL(redirect.path, siteOrigin));
}
