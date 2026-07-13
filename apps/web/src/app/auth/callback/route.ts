import { NextResponse, type NextRequest } from "next/server";
import { getServerSupabase } from "@/lib/auth/server";

export async function GET(request: NextRequest) {
  const code = request.nextUrl.searchParams.get("code");
  const next = request.nextUrl.searchParams.get("next") ?? "/events";
  const safeNext = next.startsWith("/") && !next.startsWith("//") ? next : "/events";
  const supabase = await getServerSupabase();

  if (code && supabase) {
    const { error } = await supabase.auth.exchangeCodeForSession(code);
    if (!error) {
      return NextResponse.redirect(new URL(safeNext, request.url));
    }
  }
  return NextResponse.redirect(new URL("/auth?error=callback", request.url));
}
