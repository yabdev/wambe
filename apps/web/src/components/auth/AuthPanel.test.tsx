import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
  getBrowserSupabase: vi.fn(),
  next: vi.fn(),
  push: vi.fn(),
  refresh: vi.fn(),
  signInWithOAuth: vi.fn(),
  signInWithPassword: vi.fn(),
  signUp: vi.fn(),
  resetPasswordForEmail: vi.fn(),
}));

vi.mock("next/navigation", () => ({
  useRouter: () => ({
    push: mocks.push,
    refresh: mocks.refresh,
  }),
  useSearchParams: () => ({
    get: mocks.next,
  }),
}));

vi.mock("@/lib/auth/client", () => ({
  getBrowserSupabase: mocks.getBrowserSupabase,
  isDemoMode: false,
}));

import { AuthPanel } from "./AuthPanel";

beforeEach(() => {
  vi.stubEnv("NEXT_PUBLIC_SITE_URL", window.location.origin);
  vi.stubEnv("NEXT_PUBLIC_DEMO_MODE", "true");
  mocks.next.mockReturnValue(String.raw`/\evil.example`);
  mocks.signInWithOAuth.mockResolvedValue({ error: null });
  mocks.signInWithPassword.mockResolvedValue({
    data: { session: { access_token: "not-logged" } },
    error: null,
  });
  mocks.signUp.mockResolvedValue({
    data: { session: null },
    error: null,
  });
  mocks.resetPasswordForEmail.mockResolvedValue({ error: null });
  mocks.getBrowserSupabase.mockReturnValue({
    auth: {
      signInWithOAuth: mocks.signInWithOAuth,
      signInWithPassword: mocks.signInWithPassword,
      signUp: mocks.signUp,
      resetPasswordForEmail: mocks.resetPasswordForEmail,
    },
  });
});

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
  vi.unstubAllEnvs();
});

describe("AuthPanel return targets", () => {
  it("sanitizes the Google OAuth callback target", async () => {
    const user = userEvent.setup();
    render(<AuthPanel />);

    await user.click(
      screen.getByRole("button", { name: "Continue with Google" }),
    );

    await waitFor(() => expect(mocks.signInWithOAuth).toHaveBeenCalledOnce());
    const options = mocks.signInWithOAuth.mock.calls[0]?.[0].options as {
      redirectTo: string;
    };
    const callback = new URL(options.redirectTo);
    expect(callback.origin).toBe(window.location.origin);
    expect(callback.pathname).toBe("/auth/callback");
    expect(callback.searchParams.get("next")).toBe("/events");
  });

  it("sanitizes local navigation after password sign-in", async () => {
    const user = userEvent.setup();
    render(<AuthPanel />);

    await user.type(screen.getByLabelText("Email address"), "host@example.com");
    await user.type(screen.getByLabelText("Password"), "password123");
    await user.click(
      screen.getByRole("button", { name: "Sign in with email" }),
    );

    await waitFor(() => expect(mocks.push).toHaveBeenCalledWith("/events"));
    expect(mocks.refresh).toHaveBeenCalledOnce();
  });

  it("sanitizes the email-verification callback target", async () => {
    const user = userEvent.setup();
    render(<AuthPanel />);

    await user.click(
      screen.getByRole("button", { name: "New to Wambe? Create an account" }),
    );
    await user.type(screen.getByLabelText("Email address"), "host@example.com");
    await user.type(screen.getByLabelText("Password"), "password123");
    await user.click(
      screen.getByRole("button", { name: "Create account" }),
    );

    await waitFor(() => expect(mocks.signUp).toHaveBeenCalledOnce());
    const options = mocks.signUp.mock.calls[0]?.[0].options as {
      emailRedirectTo: string;
    };
    const callback = new URL(options.emailRedirectTo);
    expect(callback.origin).toBe(window.location.origin);
    expect(callback.searchParams.get("next")).toBe("/events");
  });

  it("uses the approved auth path for password-reset callbacks", async () => {
    const user = userEvent.setup();
    render(<AuthPanel />);

    await user.click(
      screen.getByRole("button", { name: "Forgot your password?" }),
    );
    await user.type(screen.getByLabelText("Email address"), "host@example.com");
    await user.click(
      screen.getByRole("button", { name: "Send reset instructions" }),
    );

    await waitFor(() =>
      expect(mocks.resetPasswordForEmail).toHaveBeenCalledOnce(),
    );
    const options = mocks.resetPasswordForEmail.mock.calls[0]?.[1] as {
      redirectTo: string;
    };
    const callback = new URL(options.redirectTo);
    expect(callback.origin).toBe(window.location.origin);
    expect(callback.searchParams.get("next")).toBe("/auth");
  });
});
