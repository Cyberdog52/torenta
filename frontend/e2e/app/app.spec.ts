import { test, expect } from '@playwright/test';

test('starts the application', async ({ page }) => {
  // Without a stored TMDB key the app redirects to /preferences. Mock the
  // preference endpoint so the key is always present, keeping the test
  // deterministic on CI where no OS-level preferences are pre-configured.
  await page.route('**/api/preference', async (route) => {
    if (route.request().method() === 'GET') {
      await route.fulfill({ json: { downloadDirectoryPath: '/tmp', tmdbServiceKey: 'test-key' } });
    } else {
      await route.continue();
    }
  });

  await page.goto('/');

  await expect(page).toHaveURL(/\/search$/);
  await expect(page.locator('mat-toolbar')).toContainText('Torenta');
});
