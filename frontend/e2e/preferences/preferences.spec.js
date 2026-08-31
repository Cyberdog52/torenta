const { test, expect } = require('@playwright/test');

test('loads preferences from the backend', async ({ page }) => {
  const preferencesResponse = page.waitForResponse(response =>
    response.url().endsWith('/api/preference') && response.request().method() === 'GET'
  );

  await page.goto('/preferences');

  expect((await preferencesResponse).ok()).toBeTruthy();
  await expect(page.getByPlaceholder('Root folder for downloads')).toBeVisible();
});
