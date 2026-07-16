import { afterEach, describe, expect, it, vi } from "vitest";
import { recordAuthCallbackOutcome } from "./auth-callback-outcome";

afterEach(() => {
  vi.restoreAllMocks();
});

describe("recordAuthCallbackOutcome", () => {
  it("records only bounded outcome and fallback labels", () => {
    const info = vi.spyOn(console, "info").mockImplementation(() => undefined);

    recordAuthCallbackOutcome("exchange_failed", {
      usedFallback: true,
      reason: "backslash",
    });

    expect(info).toHaveBeenCalledOnce();
    const event = JSON.parse(String(info.mock.calls[0]?.[0])) as Record<
      string,
      unknown
    >;
    expect(event).toEqual({
      event: "wambe.auth.callback",
      outcome: "exchange_failed",
      redirectFallback: true,
      fallbackReason: "backslash",
    });
    expect(Object.keys(event).sort()).toEqual([
      "event",
      "fallbackReason",
      "outcome",
      "redirectFallback",
    ]);
  });

  it("fails open when the telemetry sink is unavailable", () => {
    vi.spyOn(console, "info").mockImplementation(() => {
      throw new Error("sink unavailable");
    });

    expect(() =>
      recordAuthCallbackOutcome("success", { usedFallback: false }),
    ).not.toThrow();
  });
});
