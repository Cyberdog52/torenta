import AxeBuilder from '@axe-core/playwright';
import { test, expect } from '@playwright/test';

/**
 * Automated accessibility regression coverage for every top-level route.
 *
 * This complements manual screen-reader testing (VoiceOver) rather than
 * replacing it: axe-core catches a subset of WCAG issues (missing labels,
 * invalid ARIA, contrast, landmark/heading structure, etc.) but cannot judge
 * things like whether `aria-live` announcements are well-timed or whether
 * focus management "feels" right - those still need manual verification.
 */
async function expectNoViolations(page: Parameters<typeof AxeBuilder>[0]['page']): Promise<void> {
  const results = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa']).analyze();
  expect(results.violations, JSON.stringify(results.violations, null, 2)).toEqual([]);
}

test('search page has no detectable accessibility violations', async ({ page }) => {
  await page.goto('/search');
  await expectNoViolations(page);
});

test('downloads page has no detectable accessibility violations', async ({ page }) => {
  await page.goto('/downloads');
  await expectNoViolations(page);
});

test('recommendations page has no detectable accessibility violations', async ({ page }) => {
  await page.goto('/recommendations');
  await expect(page.getByLabel(/Only rescan series touched in the last/)).toBeVisible();
  await expectNoViolations(page);
});

test('preferences page has no detectable accessibility violations', async ({ page }) => {
  await page.goto('/preferences');
  await expect(page.getByLabel('Root folder for downloads')).toBeVisible();
  await expectNoViolations(page);
});

test('the 404 page has no detectable accessibility violations', async ({ page }) => {
  await page.goto('/this-route-does-not-exist');
  await expectNoViolations(page);
});
