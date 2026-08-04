import { afterEach, describe, expect, it, vi } from "vitest";
import { ResponseError } from "@wambe/api-client";
import {
  classifyPublicMetadataError,
  resolvePublicMetadata,
} from "./public-api";

afterEach(() => {
  vi.unstubAllEnvs();
  vi.unstubAllGlobals();
});

describe("public metadata outcomes", () => {
  it("returns safe synthetic metadata in demo mode", async () => {
    vi.stubEnv("NEXT_PUBLIC_DEMO_MODE", "true");
    vi.stubEnv("NEXT_PUBLIC_SITE_URL", "https://wambe.test");

    const result = await resolvePublicMetadata("ada-and-tunde-abc123");

    expect(result.kind).toBe("available");
    if (result.kind !== "available") return;
    expect(result.metadata.title).toBe("Ada And Tunde");
    expect(result.metadata.canonicalUrl).toBe(
      "https://wambe.test/e/ada-and-tunde-abc123",
    );
    expect(result.metadata.startsAt).toBeInstanceOf(Date);
  });

  it("provides deterministic unavailable and retryable demo states", async () => {
    vi.stubEnv("NEXT_PUBLIC_DEMO_MODE", "true");

    await expect(
      resolvePublicMetadata("shared-event-unavailable"),
    ).resolves.toEqual({ kind: "unavailable" });
    await expect(
      resolvePublicMetadata("shared-event-retryable"),
    ).resolves.toEqual({ kind: "retryable" });
  });

  it("collapses not-found and deleted responses to the same safe outcome", () => {
    const missing = new ResponseError(
      new Response(null, { status: 404 }),
      "Not found",
    );
    const deleted = new ResponseError(
      new Response(null, { status: 410 }),
      "Gone",
    );

    expect(classifyPublicMetadataError(missing)).toEqual({
      kind: "unavailable",
    });
    expect(classifyPublicMetadataError(deleted)).toEqual({
      kind: "unavailable",
    });
  });

  it("classifies transport and server failures as retryable", () => {
    const serverError = new ResponseError(
      new Response(null, { status: 503 }),
      "Unavailable",
    );

    expect(classifyPublicMetadataError(serverError)).toEqual({
      kind: "retryable",
    });
    expect(classifyPublicMetadataError(new TypeError("network failed"))).toEqual({
      kind: "retryable",
    });
  });

  it("fails closed when no public API origin is configured", async () => {
    vi.stubEnv("NEXT_PUBLIC_DEMO_MODE", "false");
    vi.stubEnv("NEXT_PUBLIC_WAMBE_API_URL", "");

    await expect(resolvePublicMetadata("private-link")).resolves.toEqual({
      kind: "unavailable",
    });
  });

  it("treats malformed successful metadata as retryable", async () => {
    vi.stubEnv("NEXT_PUBLIC_DEMO_MODE", "false");
    vi.stubEnv("NEXT_PUBLIC_WAMBE_API_URL", "https://api.wambe.test/api/v1");
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            slug: "shared-event",
            title: "",
            startsAt: "2027-08-21T13:00:00.000Z",
            visibility: "private_link",
            indexable: false,
            canonicalUrl: "https://wambe.test/e/shared-event",
          }),
          {
            headers: { "content-type": "application/json" },
            status: 200,
          },
        ),
      ),
    );

    await expect(resolvePublicMetadata("shared-event")).resolves.toEqual({
      kind: "retryable",
    });
  });
});
