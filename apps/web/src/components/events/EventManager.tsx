"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import type { Event } from "@wambe/api-client";
import { getHostApi, WambeApiError } from "@/lib/api/host-api";
import styles from "./EventManager.module.css";

export function EventManager({ eventId }: { eventId: string }) {
  const router = useRouter();
  const [event, setEvent] = useState<Event>();
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState<string>();
  const [error, setError] = useState<string>();
  const unpublishKey = useRef<string | undefined>(undefined);
  const deleteKey = useRef<string | undefined>(undefined);
  const unpublishDialog = useRef<HTMLDialogElement>(null);
  const deleteDialog = useRef<HTMLDialogElement>(null);
  const unpublishTrigger = useRef<HTMLButtonElement>(null);
  const deleteTrigger = useRef<HTMLButtonElement>(null);

  const loadEvent = useCallback(() => {
    getHostApi()
      .getEvent(eventId)
      .then(setEvent)
      .catch(() => {
        setEvent(undefined);
        setError("We couldn’t load this event. Your saved details have not changed.");
      })
      .finally(() => setLoading(false));
  }, [eventId]);

  useEffect(() => {
    loadEvent();
  }, [loadEvent]);

  function retryLoad() {
    setLoading(true);
    setError(undefined);
    loadEvent();
  }

  async function unpublish() {
    if (!event) return;
    setBusy(true);
    setError(undefined);
    unpublishKey.current ??= crypto.randomUUID();
    try {
      const updated = await getHostApi().unpublishEvent(
        event.id,
        event.version,
        unpublishKey.current,
      );
      unpublishKey.current = undefined;
      setEvent(updated);
      setMessage("This event is no longer published. Its stable link is preserved.");
      unpublishDialog.current?.close();
    } catch (caught) {
      const apiError =
        caught instanceof WambeApiError
          ? caught
          : new WambeApiError("We couldn't unpublish this event.");
      setError(
        `${apiError.message}${apiError.requestId ? ` Reference: ${apiError.requestId}` : ""}`,
      );
      if (apiError.code === "EVENT_VERSION_CONFLICT") {
        setEvent(await getHostApi().getEvent(event.id));
      }
    } finally {
      setBusy(false);
    }
  }

  async function remove() {
    if (!event) return;
    setBusy(true);
    setError(undefined);
    deleteKey.current ??= crypto.randomUUID();
    try {
      await getHostApi().deleteEvent(event.id, deleteKey.current);
      deleteKey.current = undefined;
      deleteDialog.current?.close();
      router.push("/events");
    } catch (caught) {
      const apiError =
        caught instanceof WambeApiError
          ? caught
          : new WambeApiError("We couldn't delete this event.");
      setError(
        `${apiError.message}${apiError.requestId ? ` Reference: ${apiError.requestId}` : ""}`,
      );
    } finally {
      setBusy(false);
    }
  }

  if (loading) {
    return (
      <div aria-busy="true" className={styles.loading} role="status">
        <span aria-hidden="true" className="spinner" /> Loading event…
      </div>
    );
  }

  if (!event) {
    return (
      <section className={`container ${styles.loadError}`} role="alert">
        <p className="eyebrow">Event unavailable</p>
        <h1>We couldn’t load this event.</h1>
        <p className="muted">
          Your saved details have not changed. Check your connection and try again.
        </p>
        <button className="button secondary" onClick={retryLoad} type="button">
          Retry
        </button>
      </section>
    );
  }

  return (
    <div className={`container ${styles.page}`}>
      <header className={styles.heading}>
        <div>
          <span className={styles.badge}>
            {event.status === "draft"
              ? "In progress"
              : event.status === "published"
                ? "Published"
                : event.status === "unpublished"
                  ? "Unpublished"
                  : "Deleted"}
          </span>
          <h1>{event.title ?? "Untitled Wambe"}</h1>
          <p className="muted">
            Last saved {event.lastSavedAt?.toLocaleString("en-NG") ?? "recently"}
          </p>
        </div>
        <Link className="button" href={`/events/${event.id}/edit`}>Edit event</Link>
      </header>
      {message && <div className="alert success" role="status">{message}</div>}
      {error && <div className="alert error" role="alert">{error}</div>}
      <section className={styles.grid}>
        <article className={`card ${styles.hero}`}>
          <span className="eyebrow">{event.eventType?.replaceAll("_", " ")}</span>
          <h2>{event.title}</h2>
          <p>{event.startsAt?.toLocaleString("en-NG", { dateStyle: "long", timeStyle: "short" })}</p>
          <p>{event.venue?.displayAddress}</p>
        </article>
        <aside className={`card ${styles.controls}`}>
          <p className="eyebrow">Event controls</p>
          {event.canonicalUrl && (
            <div className={styles.link}>
              <span className="muted">Stable link</span>
              <strong>{event.canonicalUrl}</strong>
            </div>
          )}
          <Link className="button secondary" href={`/events/${event.id}/edit`}>Update details</Link>
          {event.status === "published" && (
            <button
              className="button secondary"
              onClick={() => unpublishDialog.current?.showModal()}
              ref={unpublishTrigger}
              type="button"
            >
              Unpublish event
            </button>
          )}
          <button
            className="button danger"
            onClick={() => deleteDialog.current?.showModal()}
            ref={deleteTrigger}
            type="button"
          >
            Delete event
          </button>
        </aside>
      </section>

      <dialog
        aria-describedby="unpublish-description"
        aria-labelledby="unpublish-title"
        className={styles.dialog}
        onClose={() => unpublishTrigger.current?.focus()}
        ref={unpublishDialog}
      >
        <form method="dialog">
          <p className="eyebrow">Unpublish event</p>
          <h2 id="unpublish-title">Take this Wambe offline?</h2>
          <p id="unpublish-description">The public page becomes unavailable, but your details and stable link are preserved for republishing.</p>
          <div>
            <button autoFocus className="button secondary" type="submit">Keep published</button>
            <button className="button danger" disabled={busy} onClick={(click) => { click.preventDefault(); void unpublish(); }} type="button">
              Unpublish
            </button>
          </div>
        </form>
      </dialog>

      <dialog
        aria-describedby="delete-description"
        aria-labelledby="delete-title"
        className={styles.dialog}
        onClose={() => deleteTrigger.current?.focus()}
        ref={deleteDialog}
      >
        <form method="dialog">
          <p className="eyebrow">Delete event</p>
          <h2 id="delete-title">Delete this Wambe?</h2>
          <p id="delete-description">It disappears from your workspace immediately. Associated uploaded media is scheduled for permanent purge after 30 days.</p>
          <div>
            <button autoFocus className="button secondary" type="submit">Cancel</button>
            <button className="button danger" disabled={busy} onClick={(click) => { click.preventDefault(); void remove(); }} type="button">
              Delete permanently
            </button>
          </div>
        </form>
      </dialog>
    </div>
  );
}
