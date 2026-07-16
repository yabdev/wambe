import type {
  RedirectFallbackReason,
  SafeRedirectDecision,
} from "./safe-redirect-path";

export type AuthCallbackOutcome =
  | "success"
  | "exchange_failed"
  | "missing_code"
  | "configuration_error";

type AuthCallbackEvent = {
  event: "wambe.auth.callback";
  outcome: AuthCallbackOutcome;
  redirectFallback: boolean;
  fallbackReason?: RedirectFallbackReason;
};

export function recordAuthCallbackOutcome(
  outcome: AuthCallbackOutcome,
  redirect: Pick<SafeRedirectDecision, "usedFallback" | "reason">,
): void {
  const event: AuthCallbackEvent = {
    event: "wambe.auth.callback",
    outcome,
    redirectFallback: redirect.usedFallback,
    ...(redirect.usedFallback && redirect.reason
      ? { fallbackReason: redirect.reason }
      : {}),
  };
  try {
    console.info(JSON.stringify(event));
  } catch {
    // Authentication and navigation must remain available if telemetry fails.
  }
}
