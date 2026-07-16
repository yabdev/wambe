import { afterEach, describe, expect, it, vi } from "vitest";
import {
  getClientSiteOrigin,
  getServerSiteOrigin,
  SiteOriginConfigurationError,
} from "./site-origin";

afterEach(() => {
  vi.unstubAllEnvs();
});

describe("getServerSiteOrigin", () => {
  it("returns a configured exact HTTPS origin", () => {
    vi.stubEnv("NEXT_PUBLIC_SITE_URL", "https://staging.wambe.test");
    vi.stubEnv("NEXT_PUBLIC_DEMO_MODE", "false");

    expect(getServerSiteOrigin("https://attacker.example")).toBe(
      "https://staging.wambe.test",
    );
  });

  it.each([
    "",
    "http://staging.wambe.test",
    "https://user:password@staging.wambe.test",
    "https://staging.wambe.test:8443",
    "https://staging.wambe.test/path",
    "https://staging.wambe.test?query=value",
    "not-a-url",
  ])("rejects invalid deployed origin %s", (configured) => {
    vi.stubEnv("NEXT_PUBLIC_SITE_URL", configured);
    vi.stubEnv("NEXT_PUBLIC_DEMO_MODE", "false");

    expect(() => getServerSiteOrigin()).toThrow(
      SiteOriginConfigurationError,
    );
  });

  it("permits an observed local origin only in demo mode", () => {
    vi.stubEnv("NEXT_PUBLIC_SITE_URL", "");
    vi.stubEnv("NEXT_PUBLIC_DEMO_MODE", "true");

    expect(getServerSiteOrigin("http://127.0.0.1:3000")).toBe(
      "http://127.0.0.1:3000",
    );
  });

  it("rejects a remote origin as the demo fallback", () => {
    vi.stubEnv("NEXT_PUBLIC_SITE_URL", "");
    vi.stubEnv("NEXT_PUBLIC_DEMO_MODE", "true");

    expect(() =>
      getServerSiteOrigin("https://attacker.example"),
    ).toThrow(SiteOriginConfigurationError);
  });

  it("accepts an explicitly configured local demo port", () => {
    vi.stubEnv("NEXT_PUBLIC_SITE_URL", "http://localhost:3000");
    vi.stubEnv("NEXT_PUBLIC_DEMO_MODE", "true");

    expect(getServerSiteOrigin()).toBe("http://localhost:3000");
  });

  it("accepts a browser origin that matches local demo configuration", () => {
    vi.stubEnv("NEXT_PUBLIC_SITE_URL", window.location.origin);
    vi.stubEnv("NEXT_PUBLIC_DEMO_MODE", "true");

    expect(getClientSiteOrigin()).toBe(window.location.origin);
  });

  it("rejects a browser origin that differs from configuration", () => {
    vi.stubEnv("NEXT_PUBLIC_SITE_URL", "https://staging.wambe.test");
    vi.stubEnv("NEXT_PUBLIC_DEMO_MODE", "false");

    expect(() => getClientSiteOrigin()).toThrow(
      SiteOriginConfigurationError,
    );
  });
});
