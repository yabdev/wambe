import Link from "next/link";
import styles from "./page.module.css";

export default function EventNotFound() {
  return (
    <main className={styles.page} id="main">
      <article
        aria-labelledby="guest-unavailable-title"
        className={`${styles.invitation} ${styles.message}`}
      >
        <span className="eyebrow">Invitation unavailable</span>
        <h1 id="guest-unavailable-title">This event isn’t available.</h1>
        <div aria-hidden="true" className={styles.rule}>✦</div>
        <p className={styles.messageCopy}>
          Check the link with the person who shared it, or return to Wambe.
        </p>
        <div className={styles.messageActions}>
          <Link className="button secondary" href="/">
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
