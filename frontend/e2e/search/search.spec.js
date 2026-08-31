const { test, expect } = require('@playwright/test');

test('shows search results and renders series detail without crashing', async ({ page }) => {
  const pageErrors = [];
  page.on('pageerror', (error) => pageErrors.push(error));

  await page.goto('/search');

  await page.getByLabel('TV show title').pressSequentially('The Office');

  const firstResult = page.locator('mat-expansion-panel-header').first();
  await expect(firstResult).toBeVisible({ timeout: 15000 });

  // Expanding a result triggers a library lookup
  // (`GET /api/directory/series/{name}`) that answers 404 for any series not
  // already on disk. `httpResource.value()` used to throw a
  // `ResourceValueError` on that response, crashing change detection for the
  // whole panel - see `safeValue()` in shared/resource.ts.
  await firstResult.click();
  await expect(page.locator('.series-detail')).toBeVisible();

  expect(pageErrors).toEqual([]);
});
