"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import type { Event } from "@wambe/api-client";
import { getHostApi } from "@/lib/api/host-api";
import styles from "./PublishSuccess.module.css";

export function PublishSuccess({ eventId }: { eventId: string }) {
  const [event, setEvent] = useState<Event>();
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(false);
  const [copied, setCopied] = useState(false);
  const [copyError, setCopyError] = useState<string>();

  const loadEvent = useCallback(() => {
    getHostApi()
      .getEvent(eventId)
      .then(setEvent)
      .catch(() => {
        setEvent(undefined);
        setLoadError(true);
      })
      .finally(() => setLoading(false));
  }, [eventId]);

  useEffect(() => {
    loadEvent();
  }, [loadEvent]);

  function retryLoad() {
    setLoading(true);
    setLoadError(false);
    loadEvent();
  }

  if (loading) {
    return (
      <div aria-busy="true" className={styles.loading} role="status">
        <span aria-hidden="true" className="spinner" /> Preparing your link…
      </div>
    );
  }

  if (loadError || !event) {
    return (
      <section className={styles.loadError} role="alert">
        <p className="eyebrow">Publication saved</p>
        <h1>We couldn’t prepare your sharing link.</h1>
        <p className="muted">Your event is still safe. Try loading the link again.</p>
        <button className="button secondary" onClick={retryLoad} type="button">
          Retry
        </button>
      </section>
    );
  }

  const url = event.canonicalUrl ?? "";
  async function copy() {
    try {
      await navigator.clipboard.writeText(url);
      setCopyError(undefined);
      setCopied(true);
    } catch {
      setCopied(false);
      setCopyError("We couldn’t copy the link. Select the address above and copy it manually.");
    }
  }

  return (
    <section aria-labelledby="publish-success-title" className={styles.page}>
      <div className={styles.burst} aria-hidden="true">✦</div>
      <p className="eyebrow">Published beautifully</p>
      <h1 id="publish-success-title">Your celebration has a digital home.</h1>
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
        {copyError && <p className="field-error" role="alert">{copyError}</p>}
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
    </section>
  );
}
