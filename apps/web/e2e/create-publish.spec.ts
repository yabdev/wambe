import { expect, test, type Page } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";

async function startCleanDraft(page: Page) {
  await page.goto("/events");
  await page.evaluate(() => localStorage.clear());
  await page.reload();
  await page.getByRole("link", { name: "Create a Wambe" }).first().click();
  await expect(page).toHaveURL(/\/events\/.+\/edit/, { timeout: 15_000 });
}

async function publishLeanWambe(page: Page, title = "Ada & Tunde's Wedding") {
  await startCleanDraft(page);
  await page.getByLabel("Wedding").check();
  await page.getByLabel("Event title").fill(title);
  await page.getByLabel("Date and time").fill("2027-08-21T14:00");
  await page.getByRole("button", { name: "Continue" }).click();
  await page.getByLabel("Venue address").fill("Tafawa Balewa Square, Lagos");
  await page.getByRole("button", { name: "Place sample pin" }).click();
  await page.getByRole("button", { name: "Confirm this pin" }).click();
  await page.getByRole("button", { name: "Continue" }).click();
  await page.getByRole("button", { name: "Continue" }).click();
  await page.getByLabel("Public").check();
  await page.getByRole("button", { name: "Publish Wambe" }).click();
  await expect(page).toHaveURL(/\/events\/.+\/published/);
}

test("creates and publishes a lean Wambe", async ({ page }) => {
  await publishLeanWambe(page);
  await expect(
    page.getByRole("heading", {
      name: "Your celebration has a digital home.",
    }),
  ).toBeVisible();
  await expect(
    page.getByRole("link", { name: "Share on WhatsApp" }),
  ).toHaveAttribute("href", /wa\.me\/\?text=.*%2Fe%2F/);
});

test("blocks publication and identifies every missing required field", async ({ page }) => {
  await startCleanDraft(page);
  await page.getByRole("button", { name: "Continue" }).click();
  await page.getByRole("button", { name: "Continue" }).click();
  await page.getByRole("button", { name: "Continue" }).click();
  await page.getByRole("button", { name: "Publish Wambe" }).click();

  const errors = page.locator("#publish-errors");
  await expect(errors).toContainText("Review these details");
  await expect(errors).toContainText("Choose an event type.");
  await expect(errors).toContainText("Add an event title.");
  await expect(errors).toContainText("Choose a future date and time.");
  await expect(errors).toContainText("Confirm the venue pin.");
  await expect(errors).toContainText("Choose who can see this event.");
  await expect(page).toHaveURL(/\/events\/.+\/edit/);
  expect((await new AxeBuilder({ page }).analyze()).violations).toEqual([]);
});

test("resumes an autosaved draft after leaving the editor", async ({ page }) => {
  await startCleanDraft(page);
  await page.getByLabel("Birthday").check();
  await page.getByLabel("Event title").fill("Amara's 40th");
  await page.getByLabel("Date and time").fill("2027-10-02T18:30");
  await expect(page.locator('[data-phase="saved"]')).toContainText("Saved");

  await page.getByRole("button", { name: "Exit" }).click();
  await expect(page.getByText("Amara's 40th", { exact: true })).toBeVisible();
  await page.getByRole("link", { name: "Continue" }).click();

  await expect(page.getByLabel("Birthday")).toBeChecked();
  await expect(page.getByLabel("Event title")).toHaveValue("Amara's 40th");
  await expect(page.getByLabel("Date and time")).toHaveValue("2027-10-02T18:30");
});

test("unpublishes and deletes an event through confirmed lifecycle actions", async ({ page }) => {
  await publishLeanWambe(page, "Lifecycle Celebration");
  const manageHref = await page.getByRole("link", { name: "Manage event" }).getAttribute("href");
  expect(manageHref).toMatch(/^\/events\/[^/]+$/);
  await page.goto(manageHref!);
  await expect(
    page.getByRole("heading", { level: 1, name: "Lifecycle Celebration" }),
  ).toBeVisible();
  expect((await new AxeBuilder({ page }).analyze()).violations).toEqual([]);

  await page.getByRole("button", { name: "Unpublish event" }).click();
  await expect(page.getByRole("heading", { name: "Take this Wambe offline?" })).toBeVisible();
  await page.getByRole("button", { name: "Unpublish", exact: true }).click();
  await expect(page.getByRole("status")).toContainText("no longer published");

  await page.getByRole("button", { name: "Delete event" }).click();
  await expect(page.getByRole("heading", { name: "Delete this Wambe?" })).toBeVisible();
  await page.getByRole("button", { name: "Delete permanently" }).click();
  await expect(page).toHaveURL(/\/events$/);
  await expect(page.getByText("Lifecycle Celebration", { exact: true })).toBeHidden();
});

test("landing page introduces the celebration experience", async ({ page }) => {
  await page.goto("/");

  await expect(
    page.getByRole("heading", {
      name: "Your celebration deserves a beautiful beginning.",
    }),
  ).toBeVisible();
  await expect(
    page.getByRole("img", {
      name: "An animated preview of a Wambe wedding invitation",
    }),
  ).toBeVisible();
  await expect(
    page.getByRole("link", { name: "Create your Wambe", exact: true }).first(),
  ).toBeVisible();

  const results = await new AxeBuilder({ page }).analyze();
  expect(results.violations).toEqual([]);
});

test("auth navigation rejects a hostile return target", async ({ page }) => {
  await page.goto(`/auth?next=${encodeURIComponent(String.raw`/\evil.example`)}`);
  await page.getByRole("button", { name: "Continue with Google" }).click();

  await expect(page).toHaveURL(/\/events$/);
});

test("dashboard has no automatically detectable accessibility violations", async ({
  page,
}) => {
  await page.goto("/events");
  await expect(
    page.getByRole("status", { name: "Loading your Wambes" }),
  ).toBeHidden();
  const results = await new AxeBuilder({ page }).analyze();
  expect(results.violations).toEqual([]);
});
