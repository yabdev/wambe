"use client";

import { useTransition } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import styles from "./page.module.css";

export function GuestRetry() {
  const router = useRouter();
  const [retrying, startRetry] = useTransition();

  return (
    <main className={styles.page} id="main">
      <article
        aria-labelledby="guest-retry-title"
        className={`${styles.invitation} ${styles.message}`}
      >
        <span className="eyebrow">A brief pause</span>
        <h1 id="guest-retry-title">We couldn’t open this event right now.</h1>
        <div aria-hidden="true" className={styles.rule}>✦</div>
        <p className={styles.messageCopy}>
          The link may still be available. Try once more, or return to Wambe.
        </p>
        <div className={styles.messageActions}>
          <button
            className="button"
            disabled={retrying}
            onClick={() => startRetry(() => router.refresh())}
            type="button"
          >
            {retrying ? "Trying again…" : "Try again"}
          </button>
          <Link className="button ghost" href="/">
            Return to Wambe
          </Link>
        </div>
        <footer>
          <span>Celebrated with</span>
          <strong className="display">wambe</strong>
        </footer>
      </article>
    </main>
  );
}
