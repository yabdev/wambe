"use client";

import { useState, type FormEvent } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { getBrowserSupabase, isDemoMode } from "@/lib/auth/client";
import styles from "./AuthPanel.module.css";

type Mode = "sign-in" | "register" | "reset";

export function AuthPanel() {
  const router = useRouter();
  const search = useSearchParams();
  const [mode, setMode] = useState<Mode>("sign-in");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [status, setStatus] = useState<string>();
  const [busy, setBusy] = useState(false);
  const next = search.get("next") ?? "/events";

  async function continueWithGoogle() {
    let supabase;
    try {
      supabase = getBrowserSupabase();
    } catch {
      setStatus("Sign-in is not configured for this environment.");
      return;
    }
    if (!supabase) {
      router.push(next);
      return;
    }
    setBusy(true);
    const redirectTo = `${window.location.origin}/auth/callback?next=${encodeURIComponent(next)}`;
    const { error } = await supabase.auth.signInWithOAuth({
      provider: "google",
      options: { redirectTo },
    });
    if (error) {
      setStatus("We couldn't start Google sign-in. Please try again.");
      setBusy(false);
    }
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    let supabase;
    try {
      supabase = getBrowserSupabase();
    } catch {
      setStatus("Sign-in is not configured for this environment.");
      return;
    }
    if (!supabase) {
      router.push(next);
      return;
    }
    setBusy(true);
    setStatus(undefined);
    if (mode === "reset") {
      await supabase.auth.resetPasswordForEmail(email, {
        redirectTo: `${window.location.origin}/auth/callback?next=/auth`,
      });
      setStatus("If that email is registered, reset instructions are on the way.");
      setBusy(false);
      return;
    }
    const result =
      mode === "register"
        ? await supabase.auth.signUp({
            email,
            password,
            options: {
              emailRedirectTo: `${window.location.origin}/auth/callback?next=${encodeURIComponent(next)}`,
            },
          })
        : await supabase.auth.signInWithPassword({ email, password });
    if (result.error) {
      setStatus("Those details didn't work. Check them or reset your password.");
      setBusy(false);
      return;
    }
    if (mode === "register" && !result.data.session) {
      setStatus("Check your inbox to verify your email, then return to sign in.");
      setBusy(false);
      return;
    }
    router.push(next);
    router.refresh();
  }

  return (
    <section className={styles.panel} aria-labelledby="auth-heading">
      <div className={styles.brand}>
        <span className={styles.mark} aria-hidden="true">W</span>
        <span className="display">wambe</span>
      </div>
      <p className="eyebrow">Your celebration starts here</p>
      <h1 id="auth-heading">
        {mode === "reset"
          ? "Find your way back."
          : "Create and share your celebration in minutes."}
      </h1>
      <p className={styles.intro}>
        A beautiful digital home for the moments your people will remember.
      </p>

      {mode !== "reset" && (
        <>
          <button
            className={`${styles.google} button secondary`}
            disabled={busy}
            onClick={continueWithGoogle}
            type="button"
          >
            <span aria-hidden="true">G</span> Continue with Google
          </button>
          <div className={styles.divider}><span>or</span></div>
        </>
      )}

      <form className={styles.form} onSubmit={submit}>
        <div className="field">
          <label htmlFor="email">Email address</label>
          <input
            autoComplete="email"
            id="email"
            onChange={(event) => setEmail(event.target.value)}
            required
            type="email"
            value={email}
          />
        </div>
        {mode !== "reset" && (
          <div className="field">
            <label htmlFor="password">Password</label>
            <input
              autoComplete={mode === "register" ? "new-password" : "current-password"}
              id="password"
              minLength={8}
              onChange={(event) => setPassword(event.target.value)}
              required
              type="password"
              value={password}
            />
          </div>
        )}
        {status && <div className="alert" role="status">{status}</div>}
        <button className="button" disabled={busy} type="submit">
          {busy && <span className="spinner" aria-hidden="true" />}
          {mode === "register"
            ? "Create account"
            : mode === "reset"
              ? "Send reset instructions"
              : "Sign in with email"}
        </button>
      </form>

      <div className={styles.switches}>
        {mode === "sign-in" ? (
          <>
            <button onClick={() => setMode("register")} type="button">
              New to Wambe? Create an account
            </button>
            <button onClick={() => setMode("reset")} type="button">
              Forgot your password?
            </button>
          </>
        ) : (
          <button onClick={() => setMode("sign-in")} type="button">
            Back to sign in
          </button>
        )}
      </div>
      {isDemoMode && (
        <p className={styles.demo}>
          Local demo mode is on. Authentication actions open the host workspace.
        </p>
      )}
    </section>
  );
}
