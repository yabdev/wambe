"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import type { Event } from "@wambe/api-client";
import { getHostApi } from "@/lib/api/host-api";
import styles from "./EventDashboard.module.css";

function statusLabel(status: Event["status"]) {
  switch (status) {
    case "draft":
      return "In progress";
    case "published":
      return "Published";
    case "unpublished":
      return "Unpublished";
    case "deleted":
      return "Deleted";
  }
}

function EventCard({ event }: { event: Event }) {
  const isPublished = event.status === "published";
  return (
    <article className={`card ${styles.eventCard}`}>
      <div className={styles.cardArt} data-published={isPublished}>
        <span>{event.eventType?.replaceAll("_", " ") ?? "Your celebration"}</span>
        <strong className="display">{event.title ?? "Untitled Wambe"}</strong>
      </div>
      <div className={styles.cardBody}>
        <div>
          <span className={styles.badge} data-status={event.status}>
            {statusLabel(event.status)}
          </span>
          <p className="muted">
            {event.startsAt
              ? new Intl.DateTimeFormat("en-NG", {
                  dateStyle: "medium",
                  timeStyle: "short",
                  timeZone: "Africa/Lagos",
                }).format(event.startsAt)
              : "Date not added yet"}
          </p>
        </div>
        <Link
          className="button secondary"
          href={
            isPublished
              ? `/events/${event.id}`
              : `/events/${event.id}/edit`
          }
        >
          {isPublished ? "Manage" : "Continue"}
        </Link>
      </div>
    </article>
  );
}

export function EventDashboard() {
  const [events, setEvents] = useState<Event[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();

  const loadEvents = useCallback(() => {
    getHostApi()
      .listEvents()
      .then(setEvents)
      .catch(() => setError("We couldn't load your Wambes. Please try again."))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    void loadEvents();
  }, [loadEvents]);

  function retryLoad() {
    setLoading(true);
    setError(undefined);
    loadEvents();
  }

  if (loading) {
    return (
      <div className={styles.grid} aria-label="Loading your Wambes" aria-busy="true" role="status">
        {[0, 1, 2].map((item) => (
          <div className={`card ${styles.skeleton}`} key={item} />
        ))}
      </div>
    );
  }

  if (error) {
    return (
      <div className={`alert error ${styles.loadError}`} role="alert">
        <p>{error}</p>
        <button className="button secondary" onClick={retryLoad} type="button">
          Retry
        </button>
      </div>
    );
  }

  if (events.length === 0) {
    return (
      <section className={`card ${styles.empty}`}>
        <span className={styles.confetti} aria-hidden="true">✦</span>
        <p className="eyebrow">Your first Wambe</p>
        <h2>Give your celebration a place to begin.</h2>
        <p className="muted">
          Add the essentials, confirm the venue and publish a beautiful link in minutes.
        </p>
        <Link className="button" href="/events/new">Create a Wambe</Link>
      </section>
    );
  }

  const drafts = events.filter((event) => event.status !== "published");
  const published = events.filter((event) => event.status === "published");
  return (
    <div className={styles.sections}>
      {drafts.length > 0 && (
        <section aria-labelledby="draft-heading">
          <div className={styles.sectionHeading}>
            <h2 id="draft-heading">In progress</h2>
            <span>{drafts.length}</span>
          </div>
          <div className={styles.grid}>{drafts.map((event) => <EventCard event={event} key={event.id} />)}</div>
        </section>
      )}
      {published.length > 0 && (
        <section aria-labelledby="published-heading">
          <div className={styles.sectionHeading}>
            <h2 id="published-heading">Published</h2>
            <span>{published.length}</span>
          </div>
          <div className={styles.grid}>{published.map((event) => <EventCard event={event} key={event.id} />)}</div>
        </section>
      )}
    </div>
  );
}
