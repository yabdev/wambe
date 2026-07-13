import Link from "next/link";
import styles from "./LandingPage.module.css";

const moments = [
  {
    number: "01",
    title: "Set the scene",
    copy: "Choose the details, place the venue and make the invitation feel unmistakably yours.",
  },
  {
    number: "02",
    title: "Share the joy",
    copy: "Send one beautiful link to family, friends and every group chat that matters.",
  },
  {
    number: "03",
    title: "Gather beautifully",
    copy: "Keep your celebration details together, from the first save-the-date to the final dance.",
  },
];

function Brand() {
  return (
    <Link className={styles.brand} href="/" aria-label="Wambe home">
      <span className={styles.brandMark} aria-hidden="true">W</span>
      <span className="display">wambe</span>
    </Link>
  );
}

export function LandingPage() {
  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <Brand />
        <nav aria-label="Main navigation" className={styles.nav}>
          <a href="#how-it-works">How it works</a>
          <Link href="/auth">Sign in</Link>
          <Link className="button" href="/events/new">Create your Wambe</Link>
        </nav>
      </header>

      <main id="main">
        <section className={styles.hero}>
          <div className={styles.heroCopy}>
            <p className="eyebrow">Made for the way we celebrate</p>
            <h1>Your celebration deserves a beautiful beginning.</h1>
            <p className={styles.lede}>
              Create a stunning digital home for the moments that bring everyone
              together — from weddings and birthdays to every unforgettable owambe.
            </p>
            <div className={styles.actions}>
              <Link className="button" href="/events/new">
                Create your Wambe <span aria-hidden="true">↗</span>
              </Link>
              <a className="button secondary" href="#celebration-preview">
                Watch it come alive <span aria-hidden="true">↓</span>
              </a>
            </div>
            <div className={styles.proof} aria-label="Wambe benefits">
              <span><b>3 min</b> to publish</span>
              <span><b>No app</b> for guests</span>
              <span><b>Made here</b> for our culture</span>
            </div>
          </div>

          <div
            aria-label="An animated preview of a Wambe wedding invitation"
            className={styles.celebrationStage}
            id="celebration-preview"
            role="img"
          >
            <span aria-hidden="true" className={styles.sun} />
            <span aria-hidden="true" className={styles.ringOne} />
            <span aria-hidden="true" className={styles.ringTwo} />
            <div aria-hidden="true" className={styles.guestCard}>
              <span>Guest list</span>
              <strong>Everyone&apos;s coming</strong>
              <div><i /><i /><i /><i /></div>
            </div>
            <article className={styles.invitation}>
              <span className={styles.inviteEyebrow}>Together with their families</span>
              <div className={styles.monogram} aria-hidden="true">A <i>&amp;</i> T</div>
              <h2>Ada &amp; Tunde</h2>
              <p>invite you to celebrate their wedding</p>
              <div className={styles.inviteRule} />
              <strong>21 · 08 · 2027</strong>
              <span className={styles.inviteVenue}>Tafawa Balewa Square · Lagos</span>
              <span className={styles.previewButton}>View celebration</span>
            </article>
            <div aria-hidden="true" className={styles.rsvpCard}>
              <span className={styles.liveDot} />
              <div><strong>RSVP received</strong><span>Chioma is celebrating with you</span></div>
            </div>
            <span aria-hidden="true" className={styles.sparkOne}>✦</span>
            <span aria-hidden="true" className={styles.sparkTwo}>✦</span>
          </div>
        </section>

        <div aria-hidden="true" className={styles.ribbon}>
          <div>
            <span>Weddings</span><i>✦</i><span>Birthdays</span><i>✦</i>
            <span>Introductions</span><i>✦</i><span>Naming ceremonies</span><i>✦</i>
            <span>Anniversaries</span><i>✦</i><span>Graduations</span><i>✦</i>
            <span>Weddings</span><i>✦</i><span>Birthdays</span><i>✦</i>
          </div>
        </div>

        <section className={styles.how} id="how-it-works">
          <div className={styles.sectionHeading}>
            <p className="eyebrow">From idea to invitation</p>
            <h2>Beautifully simple.<br />Joyfully yours.</h2>
          </div>
          <div className={styles.steps}>
            {moments.map((moment) => (
              <article key={moment.number}>
                <span>{moment.number}</span>
                <h3>{moment.title}</h3>
                <p>{moment.copy}</p>
              </article>
            ))}
          </div>
        </section>

        <section className={styles.finalCta}>
          <span aria-hidden="true" className={styles.finalStar}>✦</span>
          <p className="eyebrow">Your people are waiting</p>
          <h2>Let the celebration begin.</h2>
          <p>Give your next gathering the beautiful beginning it deserves.</p>
          <Link className="button" href="/events/new">Create your first Wambe</Link>
        </section>
      </main>

      <footer className={styles.footer}>
        <Brand />
        <p>Beautiful gatherings, made together.</p>
        <span>© {new Date().getFullYear()} Wambe</span>
      </footer>
    </div>
  );
}
