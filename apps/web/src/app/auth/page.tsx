import { Suspense } from "react";
import { AuthPanel } from "@/components/auth/AuthPanel";
import styles from "./page.module.css";

export default function AuthPage() {
  return (
    <main id="main" className={styles.page}>
      <div className={styles.art} aria-hidden="true">
        <span className={styles.sun} />
        <span className={styles.arch} />
        <p className="display">Gather beautifully.</p>
      </div>
      <Suspense fallback={<div className="card">Preparing sign in…</div>}>
        <AuthPanel />
      </Suspense>
    </main>
  );
}
