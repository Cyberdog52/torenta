import { test, expect } from '@playwright/test';

test('loads preferences from the backend', async ({ page }) => {
  const preferencesResponse = page.waitForResponse(
    (response) =>
      response.url().endsWith('/api/preference') && response.request().method() === 'GET',
  );

  await page.goto('/preferences');

  expect((await preferencesResponse).ok()).toBeTruthy();
  await expect(page.getByLabel('Root folder for downloads')).toBeVisible();
});
