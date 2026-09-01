import { test, expect } from '@playwright/test';

test('loads recommendations from the backend', async ({ page }) => {
  const recommendationResponse = page.waitForResponse(
    (response) =>
      response.url().includes('/api/recommendation') && response.request().method() === 'GET',
  );

  await page.goto('/recommendations');

  expect((await recommendationResponse).ok()).toBeTruthy();
  await expect(page.getByRole('heading', { name: 'Recommendations' })).toBeVisible();
  await expect(page.getByLabel(/Only rescan series touched in the last/)).toBeVisible();

  // Whether the scan finds missing episodes or not depends on whatever library is configured in
  // this environment (empty in CI, but this may run against a real, pre-existing library
  // locally), so only assert that one of the two expected end states rendered - not which one.
  await expect(
    page.getByText("You're all caught up").or(page.locator('.series-card').first()),
  ).toBeVisible();
});

test('scans the entire library when days is set to 0', async ({ page }) => {
  await page.goto('/recommendations');

  const daysInput = page.getByLabel(/Only rescan series touched in the last/);
  await expect(daysInput).toBeVisible();

  const recommendationResponse = page.waitForResponse(
    (response) =>
      response.url().includes('/api/recommendation?days=0') &&
      response.request().method() === 'GET',
  );

  await daysInput.fill('0');
  await daysInput.dispatchEvent('change');

  expect((await recommendationResponse).ok()).toBeTruthy();
});
