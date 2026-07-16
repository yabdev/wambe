export const DEFAULT_AUTH_REDIRECT = "/events";

const MAX_REDIRECT_LENGTH = 2_048;
const MAX_DECODE_PASSES = 2;
const CONTROL_CHARACTER = /[\u0000-\u001f\u007f]/;
const ENCODED_SEPARATOR = /%(?:2f|5c)/i;
const DOT_SEGMENT = /(?:^|\/)\.{1,2}(?:\/|$)/;

export type RedirectFallbackReason =
  | "absent"
  | "blank"
  | "too_long"
  | "not_path"
  | "protocol_relative"
  | "backslash"
  | "control_character"
  | "encoded_separator"
  | "decode_error"
  | "dot_segment"
  | "invalid_site_origin"
  | "external_origin"
  | "path_not_allowed";

export type SafeRedirectDecision = {
  path: string;
  usedFallback: boolean;
  reason?: RedirectFallbackReason;
};

export function safeRedirectPath(
  untrustedNext: string | null | undefined,
  siteOrigin: string,
): string {
  return evaluateRedirectPath(untrustedNext, siteOrigin).path;
}

export function evaluateRedirectPath(
  untrustedNext: string | null | undefined,
  siteOrigin: string,
): SafeRedirectDecision {
  if (untrustedNext == null) {
    return fallback("absent");
  }
  if (untrustedNext.trim() === "") {
    return fallback("blank");
  }
  if (
    untrustedNext.length > MAX_REDIRECT_LENGTH ||
    untrustedNext !== untrustedNext.trim()
  ) {
    return fallback(
      untrustedNext.length > MAX_REDIRECT_LENGTH ? "too_long" : "not_path",
    );
  }

  const fragmentIndex = untrustedNext.indexOf("#");
  const candidate =
    fragmentIndex >= 0 ? untrustedNext.slice(0, fragmentIndex) : untrustedNext;

  let inspected = candidate;
  for (let pass = 0; pass <= MAX_DECODE_PASSES; pass += 1) {
    const unsafeReason = structuralRejection(inspected);
    if (unsafeReason) {
      return fallback(unsafeReason);
    }
    if (pass === MAX_DECODE_PASSES) {
      break;
    }
    try {
      const decoded = decodeURIComponent(inspected);
      if (decoded === inspected) {
        break;
      }
      inspected = decoded;
    } catch {
      return fallback("decode_error");
    }
  }

  let trustedOrigin: string;
  let resolved: URL;
  try {
    const configured = new URL(siteOrigin);
    if (
      configured.username ||
      configured.password ||
      configured.pathname !== "/" ||
      configured.search ||
      configured.hash
    ) {
      return fallback("invalid_site_origin");
    }
    trustedOrigin = configured.origin;
    // Resolve the original value after bounded inspection so query encoding is preserved.
    resolved = new URL(candidate, trustedOrigin);
  } catch {
    return fallback("invalid_site_origin");
  }

  if (resolved.origin !== trustedOrigin) {
    return fallback("external_origin");
  }
  if (!isAllowedPath(resolved.pathname)) {
    return fallback("path_not_allowed");
  }

  return {
    path: `${resolved.pathname}${resolved.search}`,
    usedFallback: false,
  };
}

export function buildAuthCallbackUrl(
  untrustedNext: string | null | undefined,
  siteOrigin: string,
): string {
  const callback = new URL("/auth/callback", siteOrigin);
  callback.searchParams.set("next", safeRedirectPath(untrustedNext, siteOrigin));
  return callback.toString();
}

function structuralRejection(value: string): RedirectFallbackReason | undefined {
  if (value.length > MAX_REDIRECT_LENGTH) {
    return "too_long";
  }
  if (!value.startsWith("/")) {
    return "not_path";
  }
  if (value.startsWith("//")) {
    return "protocol_relative";
  }
  if (value.includes("\\")) {
    return "backslash";
  }
  if (CONTROL_CHARACTER.test(value)) {
    return "control_character";
  }
  const pathname = value.split("?", 1)[0] ?? "";
  if (ENCODED_SEPARATOR.test(pathname)) {
    return "encoded_separator";
  }
  if (DOT_SEGMENT.test(pathname)) {
    return "dot_segment";
  }
  return undefined;
}

function isAllowedPath(pathname: string): boolean {
  return (
    pathname === "/auth" ||
    pathname === "/events" ||
    pathname.startsWith("/events/")
  );
}

function fallback(reason: RedirectFallbackReason): SafeRedirectDecision {
  return {
    path: DEFAULT_AUTH_REDIRECT,
    usedFallback: true,
    reason,
  };
}
