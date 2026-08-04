import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { getPublicMetadata } from "@/lib/api/public-api";
import { GuestRetry } from "./GuestRetry";
import styles from "./page.module.css";

const unavailableMetadata: Metadata = {
  title: "Event unavailable",
  robots: { index: false, follow: false },
};

export async function generateMetadata({
  params,
}: {
  params: Promise<{ slug: string }>;
}): Promise<Metadata> {
  const { slug } = await params;
  const result = await getPublicMetadata(slug);
  if (result.kind !== "available") return unavailableMetadata;
  const { metadata: event } = result;
  return {
    title: event.title,
    description: `Join us for ${event.title} on ${event.startsAt.toLocaleDateString("en-NG", { dateStyle: "long" })}.`,
    alternates: { canonical: event.canonicalUrl },
    robots: event.indexable ? { index: true, follow: true } : { index: false, follow: false },
    openGraph: {
      type: "website",
      title: event.title,
      url: event.canonicalUrl,
      images: event.invitationPreviewUrl ? [event.invitationPreviewUrl] : undefined,
    },
  };
}

export default async function PublicEventPage({
  params,
}: {
  params: Promise<{ slug: string }>;
}) {
  const { slug } = await params;
  const result = await getPublicMetadata(slug);
  if (result.kind === "unavailable") notFound();
  if (result.kind === "retryable") {
    return <GuestRetry />;
  }
  const { metadata: event } = result;
  return (
    <main id="main" className={styles.page}>
      <article className={styles.invitation}>
        <span className="eyebrow">You’re invited</span>
        <h1>{event.title}</h1>
        <div className={styles.rule} aria-hidden="true">✦</div>
        <time dateTime={event.startsAt.toISOString()}>
          {event.startsAt.toLocaleString("en-NG", {
            dateStyle: "full",
            timeStyle: "short",
            timeZone: "Africa/Lagos",
          })}
        </time>
        {(event.venueName || event.venueAddress) && (
          <address>
            <strong>{event.venueName}</strong>
            <span>{event.venueAddress}</span>
          </address>
        )}
        <footer>
          <span>Celebrated with</span>
          <strong className="display">wambe</strong>
        </footer>
      </article>
    </main>
  );
}
