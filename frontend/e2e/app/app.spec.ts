import { test, expect } from '@playwright/test';

test('starts the application', async ({ page }) => {
  await page.goto('/');

  await expect(page).toHaveURL(/\/search$/);
  await expect(page.locator('mat-toolbar')).toContainText('Torenta');
});
