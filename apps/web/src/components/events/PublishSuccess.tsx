"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import type { Event } from "@wambe/api-client";
import { getHostApi } from "@/lib/api/host-api";
import styles from "./PublishSuccess.module.css";

export function PublishSuccess({ eventId }: { eventId: string }) {
  const [event, setEvent] = useState<Event>();
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    getHostApi().getEvent(eventId).then(setEvent);
  }, [eventId]);

  if (!event) {
    return <div className={styles.loading}><span className="spinner" /> Preparing your link…</div>;
  }

  const url = event.canonicalUrl ?? "";
  async function copy() {
    await navigator.clipboard.writeText(url);
    setCopied(true);
    setTimeout(() => setCopied(false), 1800);
  }

  return (
    <main id="main" className={styles.page}>
      <div className={styles.burst} aria-hidden="true">✦</div>
      <p className="eyebrow">Published beautifully</p>
      <h1>Your celebration has a digital home.</h1>
      <p className={styles.intro}>
        {event.title} is live. Keep the link close or send it straight to your people.
      </p>
      <div className={`card ${styles.linkPanel}`}>
        <span className="muted">Your event link</span>
        <strong>{url || "Sharing is currently protected"}</strong>
        <div>
          <button className="button secondary" disabled={!url} onClick={copy} type="button">
            {copied ? "✓ Copied" : "Copy link"}
          </button>
          <a
            aria-disabled={!event.shareEligible}
            className="button whatsapp"
            href={
              event.shareEligible
                ? `https://wa.me/?text=${encodeURIComponent(`Join us for ${event.title}: ${url}`)}`
                : undefined
            }
            onClick={(click) => {
              if (!event.shareEligible) click.preventDefault();
            }}
            rel="noreferrer"
            target="_blank"
          >
            Share on WhatsApp
          </a>
        </div>
      </div>
      {!event.shareEligible && (
        <div className="alert">
          {event.shareBlockedReason ?? "Guest access setup is required before sharing."}
        </div>
      )}
      <div className={styles.actions}>
        <Link className="button secondary" href={`/events/${event.id}`}>Manage event</Link>
        <Link className="button ghost" href="/events">Back to My Wambes</Link>
      </div>
      <div aria-live="polite" className="sr-only">{copied ? "Link copied" : ""}</div>
    </main>
  );
}
