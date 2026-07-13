import { expect, test } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";

test("creates and publishes a lean Wambe", async ({ page }) => {
  await page.goto("/events");
  await page.evaluate(() => localStorage.clear());
  await page.reload();

  await page.getByRole("link", { name: "Create a Wambe" }).first().click();
  await expect(page).toHaveURL(/\/events\/.+\/edit/, { timeout: 15_000 });

  await page.getByLabel("Wedding").check();
  await page.getByLabel("Event title").fill("Ada & Tunde's Wedding");
  await page.getByLabel("Date and time").fill("2027-08-21T14:00");
  await page.getByRole("button", { name: "Continue" }).click();

  await page.getByLabel("Venue address").fill("Tafawa Balewa Square, Lagos");
  await page.getByRole("button", { name: "Place sample pin" }).click();
  await page.getByRole("button", { name: "Confirm this pin" }).click();
  await page.getByRole("button", { name: "Continue" }).click();
  await page.getByRole("button", { name: "Continue" }).click();

  await page.getByLabel("Public").check();
  await page.getByRole("button", { name: "Publish Wambe" }).click();

  await expect(
    page.getByRole("heading", {
      name: "Your celebration has a digital home.",
    }),
  ).toBeVisible();
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
