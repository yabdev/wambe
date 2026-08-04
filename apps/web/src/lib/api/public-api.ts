import { cache } from "react";
import {
  Configuration,
  DefaultApi,
  ResponseError,
  instanceOfPublicEventMetadata,
  type PublicEventMetadata,
} from "@wambe/api-client";

export type PublicMetadataResult =
  | { kind: "available"; metadata: PublicEventMetadata }
  | { kind: "unavailable" }
  | { kind: "retryable" };

const UNAVAILABLE_SUFFIX = "-unavailable";
const RETRYABLE_SUFFIX = "-retryable";

function validMetadata(value: unknown): value is PublicEventMetadata {
  return (
    typeof value === "object" &&
    value !== null &&
    instanceOfPublicEventMetadata(value) &&
    value.title.trim().length > 0 &&
    value.slug.trim().length > 0 &&
    value.canonicalUrl.trim().length > 0 &&
    value.startsAt instanceof Date &&
    Number.isFinite(value.startsAt.getTime()) &&
    (value.visibility === "public" || value.visibility === "private_link")
  );
}

export function classifyPublicMetadataError(
  error: unknown,
): Exclude<PublicMetadataResult, { kind: "available" }> {
  if (
    error instanceof ResponseError &&
    (error.response.status === 404 || error.response.status === 410)
  ) {
    return { kind: "unavailable" };
  }
  return { kind: "retryable" };
}

export async function resolvePublicMetadata(
  slug: string,
): Promise<PublicMetadataResult> {
  if (process.env.NEXT_PUBLIC_DEMO_MODE === "true") {
    if (slug.endsWith(UNAVAILABLE_SUFFIX)) {
      return { kind: "unavailable" };
    }
    if (slug.endsWith(RETRYABLE_SUFFIX)) {
      return { kind: "retryable" };
    }
    const title = slug
      .replace(/-[a-f0-9]{6}$/i, "")
      .split("-")
      .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
      .join(" ");
    return {
      kind: "available",
      metadata: {
        slug,
        title: title || "A Wambe celebration",
        startsAt: new Date(Date.now() + 7 * 24 * 60 * 60_000),
        visibility: "private_link",
        indexable: false,
        canonicalUrl: `${process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000"}/e/${slug}`,
      },
    };
  }
  if (!process.env.NEXT_PUBLIC_WAMBE_API_URL) {
    return { kind: "unavailable" };
  }
  try {
    const api = new DefaultApi(
      new Configuration({
        basePath: process.env.NEXT_PUBLIC_WAMBE_API_URL,
        fetchApi: (input, init) =>
          fetch(input, {
            ...init,
            cache: "no-store",
            signal: AbortSignal.timeout(10_000),
          }),
      }),
    );
    const metadata = await api.getPublicEventMetadata({ slug });
    return validMetadata(metadata)
      ? { kind: "available", metadata }
      : { kind: "retryable" };
  } catch (error) {
    return classifyPublicMetadataError(error);
  }
}

export const getPublicMetadata = cache(resolvePublicMetadata);
