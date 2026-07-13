import Link from "next/link";
import { EventDashboard } from "@/components/events/EventDashboard";
import styles from "./page.module.css";

export default function EventsPage() {
  return (
    <div className={`container ${styles.page}`}>
      <header className={styles.heading}>
        <div>
          <p className="eyebrow">Your celebrations</p>
          <h1>My Wambes</h1>
          <p className="muted">Pick up a draft or manage a celebration already in motion.</p>
        </div>
        <Link className="button" href="/events/new">＋ Create a Wambe</Link>
      </header>
      <EventDashboard />
    </div>
  );
}
