import {
  Configuration,
  DefaultApi,
  type PublicEventMetadata,
} from "@wambe/api-client";

export async function getPublicMetadata(
  slug: string,
): Promise<PublicEventMetadata | null> {
  if (process.env.NEXT_PUBLIC_DEMO_MODE === "true") {
    const title = slug
      .replace(/-[a-f0-9]{6}$/i, "")
      .split("-")
      .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
      .join(" ");
    return {
      slug,
      title: title || "A Wambe celebration",
      startsAt: new Date(Date.now() + 7 * 24 * 60 * 60_000),
      visibility: "private_link",
      indexable: false,
      canonicalUrl: `${process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000"}/e/${slug}`,
    };
  }
  if (!process.env.NEXT_PUBLIC_WAMBE_API_URL) {
    return null;
  }
  try {
    const api = new DefaultApi(
      new Configuration({ basePath: process.env.NEXT_PUBLIC_WAMBE_API_URL }),
    );
    return await api.getPublicEventMetadata({ slug });
  } catch {
    return null;
  }
}
