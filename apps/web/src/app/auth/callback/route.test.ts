// @vitest-environment node

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { NextRequest } from "next/server";

const mocks = vi.hoisted(() => ({
  exchangeCodeForSession: vi.fn(),
  getServerSupabase: vi.fn(),
  recordAuthCallbackOutcome: vi.fn(),
}));

vi.mock("@/lib/auth/server", () => ({
  getServerSupabase: mocks.getServerSupabase,
}));

vi.mock("@/lib/auth/auth-callback-outcome", () => ({
  recordAuthCallbackOutcome: mocks.recordAuthCallbackOutcome,
}));

import { GET } from "./route";

const SITE_ORIGIN = "https://staging.wambe.test";

beforeEach(() => {
  vi.stubEnv("NEXT_PUBLIC_SITE_URL", SITE_ORIGIN);
  vi.stubEnv("NEXT_PUBLIC_DEMO_MODE", "false");
  mocks.exchangeCodeForSession.mockResolvedValue({ error: null });
  mocks.getServerSupabase.mockResolvedValue({
    auth: {
      exchangeCodeForSession: mocks.exchangeCodeForSession,
    },
  });
});

afterEach(() => {
  vi.clearAllMocks();
  vi.unstubAllEnvs();
});

describe("GET /auth/callback", () => {
  it("exchanges the code and redirects to an allowed path on the configured origin", async () => {
    const response = await GET(
      callbackRequest("/events/event-id/edit?step=venue"),
    );

    expect(response.status).toBe(307);
    expect(response.headers.get("location")).toBe(
      `${SITE_ORIGIN}/events/event-id/edit?step=venue`,
    );
    expect(mocks.exchangeCodeForSession).toHaveBeenCalledWith("test-code");
    expect(mocks.recordAuthCallbackOutcome).toHaveBeenCalledWith(
      "success",
      expect.objectContaining({ usedFallback: false }),
    );
  });

  it("falls back for a backslash target and never trusts the request origin", async () => {
    const response = await GET(
      callbackRequest(String.raw`/\evil.example`, "https://attacker.example"),
    );

    expect(response.headers.get("location")).toBe(`${SITE_ORIGIN}/events`);
    expect(mocks.recordAuthCallbackOutcome).toHaveBeenCalledWith(
      "success",
      {
        path: "/events",
        usedFallback: true,
        reason: "backslash",
      },
    );
  });

  it("redirects exchange failures to the configured auth page", async () => {
    mocks.exchangeCodeForSession.mockResolvedValue({
      error: new Error("invalid code"),
    });

    const response = await GET(callbackRequest("/events"));

    expect(response.headers.get("location")).toBe(
      `${SITE_ORIGIN}/auth?error=callback`,
    );
    expect(mocks.recordAuthCallbackOutcome).toHaveBeenCalledWith(
      "exchange_failed",
      expect.objectContaining({ usedFallback: false }),
    );
  });

  it("records a missing code without attempting an exchange", async () => {
    const response = await GET(callbackRequest("/events", SITE_ORIGIN, false));

    expect(response.headers.get("location")).toBe(
      `${SITE_ORIGIN}/auth?error=callback`,
    );
    expect(mocks.getServerSupabase).not.toHaveBeenCalled();
    expect(mocks.recordAuthCallbackOutcome).toHaveBeenCalledWith(
      "missing_code",
      expect.objectContaining({ usedFallback: false }),
    );
  });

  it("records unavailable auth configuration without exposing details", async () => {
    mocks.getServerSupabase.mockResolvedValue(null);

    const response = await GET(callbackRequest("/events"));

    expect(response.headers.get("location")).toBe(
      `${SITE_ORIGIN}/auth?error=configuration`,
    );
    expect(mocks.recordAuthCallbackOutcome).toHaveBeenCalledWith(
      "configuration_error",
      expect.objectContaining({ usedFallback: false }),
    );
  });

  it("fails closed when the trusted site origin is missing", async () => {
    vi.stubEnv("NEXT_PUBLIC_SITE_URL", "");

    const response = await GET(callbackRequest("/events"));

    expect(response.status).toBe(500);
    await expect(response.json()).resolves.toEqual({
      error: "Authentication configuration is unavailable.",
    });
    expect(mocks.getServerSupabase).not.toHaveBeenCalled();
    expect(mocks.recordAuthCallbackOutcome).toHaveBeenCalledWith(
      "configuration_error",
      { usedFallback: false },
    );
  });

  it("records the bounded fallback reason when code is missing", async () => {
    const response = await GET(
      callbackRequest(String.raw`/\evil.example`, SITE_ORIGIN, false),
    );

    expect(response.headers.get("location")).toBe(
      `${SITE_ORIGIN}/auth?error=callback`,
    );
    expect(mocks.recordAuthCallbackOutcome).toHaveBeenCalledWith(
      "missing_code",
      {
        path: "/events",
        usedFallback: true,
        reason: "backslash",
      },
    );
  });
});

function callbackRequest(
  next: string,
  requestOrigin = SITE_ORIGIN,
  includeCode = true,
): NextRequest {
  const url = new URL("/auth/callback", requestOrigin);
  if (includeCode) {
    url.searchParams.set("code", "test-code");
  }
  url.searchParams.set("next", next);
  return new NextRequest(url);
}
