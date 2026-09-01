import { test, expect, type Page } from '@playwright/test';

// Without a stored TMDB key the app redirects to /preferences. Mock the
// preference endpoint so the key is always present, keeping these tests
// deterministic on CI where no OS-level preferences are pre-configured.
async function mockPreferenceWithTmdbKey(page: Page): Promise<void> {
  await page.route('**/api/preference', async (route) => {
    if (route.request().method() === 'GET') {
      await route.fulfill({ json: { downloadDirectoryPath: '/tmp', tmdbServiceKey: 'test-key' } });
    } else {
      await route.continue();
    }
  });
}

test('loads recommendations from the backend', async ({ page }) => {
  await mockPreferenceWithTmdbKey(page);

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
  await mockPreferenceWithTmdbKey(page);

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
