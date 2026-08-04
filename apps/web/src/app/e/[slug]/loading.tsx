import styles from "./page.module.css";

export default function LoadingPublicEvent() {
  return (
    <main className={styles.page} id="main">
      <article
        aria-busy="true"
        aria-labelledby="guest-loading-title"
        className={`${styles.invitation} ${styles.loadingInvitation}`}
        role="status"
      >
        <p className="eyebrow">You’re invited</p>
        <h1 className="sr-only" id="guest-loading-title">
          Opening your invitation
        </h1>
        <div aria-hidden="true" className={styles.skeletonTitle} />
        <div aria-hidden="true" className={styles.rule}>✦</div>
        <div aria-hidden="true" className={styles.skeletonDetail} />
        <div aria-hidden="true" className={styles.skeletonDetailShort} />
        <footer>
          <span>Celebrated with</span>
          <strong className="display">wambe</strong>
        </footer>
      </article>
    </main>
  );
}
