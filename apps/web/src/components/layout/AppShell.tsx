import Link from "next/link";
import styles from "./AppShell.module.css";

export function WambeMark() {
  return (
    <Link className={styles.brand} href="/events" aria-label="Wambe home">
      <span className={styles.mark} aria-hidden="true">
        W
      </span>
      <span className="display">wambe</span>
    </Link>
  );
}

export function AppShell({ children }: { children: React.ReactNode }) {
  return (
    <div className={styles.shell}>
      <header className={styles.mobileHeader}>
        <WambeMark />
        <Link className="button" href="/events/new">
          Create
        </Link>
      </header>
      <aside className={styles.sidebar}>
        <WambeMark />
        <nav aria-label="Host navigation" className={styles.nav}>
          <Link href="/events">
            <span aria-hidden="true">⌂</span> My Wambes
          </Link>
          <Link href="/events/new">
            <span aria-hidden="true">＋</span> Create a Wambe
          </Link>
        </nav>
        <div className={styles.sidebarNote}>
          <span className="eyebrow">Made for gathering</span>
          <p>Every celebration deserves a beautiful beginning.</p>
        </div>
      </aside>
      <main id="main" className={styles.main}>
        {children}
      </main>
    </div>
  );
}
