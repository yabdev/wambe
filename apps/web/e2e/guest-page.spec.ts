import { expect, test } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";

test("renders the existing metadata-only guest invitation", async ({ page }) => {
  await page.setViewportSize({ width: 320, height: 568 });
  await page.goto("/e/guest-celebration-abc123");

  await expect(
    page.getByRole("heading", { name: "Guest Celebration" }),
  ).toBeVisible();
  await expect(page.getByText("Celebrated with")).toBeVisible();
  await expect(
    page.getByRole("button", { name: /copy|share/i }),
  ).toHaveCount(0);
  await expect(
    page.getByRole("link", { name: /copy|share/i }),
  ).toHaveCount(0);
  expect(
    await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth),
  ).toBe(true);
  expect((await new AxeBuilder({ page }).analyze()).violations).toEqual([]);
});

test("collapses unavailable guest outcomes to neutral copy", async ({ page }) => {
  await page.goto("/e/guest-link-unavailable");

  await expect(
    page.getByRole("heading", { name: "This event isn’t available." }),
  ).toBeVisible();
  await expect(page.locator('meta[name="robots"]').first()).toHaveAttribute(
    "content",
    /noindex/,
  );
  const mainCopy = (await page.getByRole("main").innerText()).toLowerCase();
  expect(mainCopy).not.toMatch(/\b(private|deleted|owner)\b/);
  await expect(
    page.getByRole("link", { name: "Return to Wambe" }),
  ).toBeVisible();
  expect((await new AxeBuilder({ page }).analyze()).violations).toEqual([]);
});

test("offers a non-disclosing retry state without motion dependency", async ({
  page,
}) => {
  await page.emulateMedia({ reducedMotion: "reduce" });
  await page.goto("/e/guest-link-retryable");

  await expect(
    page.getByRole("heading", {
      name: "We couldn’t open this event right now.",
    }),
  ).toBeVisible();
  await expect(page.getByRole("button", { name: "Try again" })).toBeVisible();
  await page.getByRole("button", { name: "Try again" }).click();
  await expect(page.getByRole("button", { name: "Try again" })).toBeEnabled();
  await expect(
    page.getByRole("button", { name: /copy|share/i }),
  ).toHaveCount(0);
  expect((await new AxeBuilder({ page }).analyze()).violations).toEqual([]);
});
