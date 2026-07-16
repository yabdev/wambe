import { describe, expect, it } from "vitest";
import {
  buildAuthCallbackUrl,
  DEFAULT_AUTH_REDIRECT,
  evaluateRedirectPath,
  safeRedirectPath,
} from "./safe-redirect-path";

const SITE_ORIGIN = "https://staging.wambe.test";

describe("safeRedirectPath", () => {
  it.each([
    ["/events", "/events"],
    ["/events/", "/events/"],
    ["/events/new", "/events/new"],
    ["/events/event-id/edit?step=venue", "/events/event-id/edit?step=venue"],
    ["/events?tab=drafts", "/events?tab=drafts"],
    ["/events#discarded", "/events"],
    ["/auth", "/auth"],
  ])("allows and normalizes %s", (input, expected) => {
    expect(safeRedirectPath(input, SITE_ORIGIN)).toBe(expected);
    expect(evaluateRedirectPath(input, SITE_ORIGIN).usedFallback).toBe(false);
  });

  it.each([
    [null, "absent"],
    [undefined, "absent"],
    ["", "blank"],
    ["   ", "blank"],
    ["events", "not_path"],
    [" /events", "not_path"],
    ["/events ", "not_path"],
    ["https://evil.example/events", "not_path"],
    ["https://staging.wambe.test/events", "not_path"],
    ["//evil.example", "protocol_relative"],
    [String.raw`/\evil.example`, "backslash"],
    [String.raw`/events\..\evil`, "backslash"],
    ["/%5Cevil.example", "encoded_separator"],
    ["/%2F%2Fevil.example", "encoded_separator"],
    ["/%255Cevil.example", "encoded_separator"],
    ["/%252F%252Fevil.example", "encoded_separator"],
    ["/%25255Cevil.example", "encoded_separator"],
    ["/events%00", "control_character"],
    ["/%09events", "control_character"],
    ["/%zz", "decode_error"],
    ["/events/../auth", "dot_segment"],
    ["/events/%2e%2e/auth", "dot_segment"],
    ["/events/%252e%252e/auth", "dot_segment"],
    ["/", "path_not_allowed"],
    ["/e/celebration", "path_not_allowed"],
    ["/auth/", "path_not_allowed"],
    ["/auth/callback", "path_not_allowed"],
    ["/Events", "path_not_allowed"],
  ])("falls back for hostile or disallowed input %#", (input, reason) => {
    expect(evaluateRedirectPath(input, SITE_ORIGIN)).toEqual({
      path: DEFAULT_AUTH_REDIRECT,
      usedFallback: true,
      reason,
    });
  });

  it("bounds work for oversized targets", () => {
    const decision = evaluateRedirectPath(
      `/events/${"a".repeat(2_048)}`,
      SITE_ORIGIN,
    );

    expect(decision).toMatchObject({
      path: DEFAULT_AUTH_REDIRECT,
      usedFallback: true,
      reason: "too_long",
    });
  });

  it("does not treat a query value as a second navigation target", () => {
    expect(
      safeRedirectPath("/events?next=//evil.example", SITE_ORIGIN),
    ).toBe("/events?next=//evil.example");
    expect(
      safeRedirectPath("/events?return=%2Fevents%2Fnew", SITE_ORIGIN),
    ).toBe("/events?return=%2Fevents%2Fnew");
  });

  it("fails closed when the configured site value is not an exact origin", () => {
    expect(
      evaluateRedirectPath("/events/new", "https://staging.wambe.test/base"),
    ).toMatchObject({
      path: DEFAULT_AUTH_REDIRECT,
      usedFallback: true,
      reason: "invalid_site_origin",
    });
  });

  it("builds callback URLs from the trusted origin and sanitized path", () => {
    const callback = new URL(
      buildAuthCallbackUrl(String.raw`/\evil.example`, SITE_ORIGIN),
    );

    expect(callback.origin).toBe(SITE_ORIGIN);
    expect(callback.pathname).toBe("/auth/callback");
    expect(callback.searchParams.get("next")).toBe(DEFAULT_AUTH_REDIRECT);
  });
});
