import { expect, test } from '@playwright/test';

const fixtureUrl = 'http://127.0.0.1:10999';

interface FixtureState {
  ollamaCalls: { kind: string; request: string }[];
  tmdbRequests: { path: string; query: Record<string, string> }[];
}

test('uses the real concierge backend flow with protocol-mocked providers', async ({
  page,
  request,
}) => {
  await request.post(`${fixtureUrl}/__reset`);
  const downloadRequests: string[] = [];
  page.on('request', (browserRequest) => {
    const pathname = new URL(browserRequest.url()).pathname;
    if (pathname.startsWith('/api/bittorrent')) {
      downloadRequests.push(`${browserRequest.method()} ${pathname}`);
    }
  });
  await page.route('**/api/preference', async (route) => {
    if (route.request().method() === 'GET') {
      await route.fulfill({
        json: {
          downloadDirectoryPath: '/tmp',
          tmdbServiceKey: 'test-key',
          openAiApiKey: null,
        },
      });
    } else {
      await route.continue();
    }
  });

  await page.goto('/search');
  const prompt = page.getByLabel('What would you like to watch?');
  await prompt.fill(
    'Hopeful English science fiction from 2010 to 2020, rated at least 7 and under 130 minutes on Imaginary Network',
  );
  await prompt.press('Enter');

  const results = page.locator('.concierge-result-panel');
  await expect(results).toHaveCount(2, { timeout: 30000 });
  await expect(results.nth(0)).toContainText('#1');
  await expect(results.nth(0)).toContainText('Bright Horizons');
  await expect(results.nth(0)).toContainText(
    'Its optimistic space-community story best matches your hopeful mood.',
  );
  await expect(results.nth(1)).toContainText('#2');
  await expect(results.nth(1)).toContainText('Moonlit Journey');
  await expect(results.nth(1)).toContainText(
    'A highly rated science-fiction journey within your requested years.',
  );

  await results.nth(0).locator('mat-expansion-panel-header').click();
  await expect(results.nth(0).locator('.series-detail')).toContainText(
    'A hopeful crew builds a new life among the stars.',
  );
  expect(downloadRequests).toEqual([]);

  const fixtureState = await request.get(`${fixtureUrl}/__state`);
  const state = (await fixtureState.json()) as FixtureState;
  expect(state.ollamaCalls.map((call) => call.kind)).toEqual(['intent', 'ranking']);
  expect(state.tmdbRequests.some((call) => /\/discover\/movie$/.test(call.path))).toBe(true);
  expect(state.tmdbRequests.some((call) => /\/discover\/tv$/.test(call.path))).toBe(true);

  const discoverMovie = state.tmdbRequests.find((call) => /\/discover\/movie$/.test(call.path));
  expect(Number(discoverMovie?.query['vote_average.gte'])).toBe(7);
  expect(discoverMovie?.query['with_original_language']).toBe('en');
  expect(discoverMovie?.query['with_runtime.lte']).toBe('130');
  expect(discoverMovie?.query['year']).toBeUndefined();
  expect(discoverMovie?.query['with_networks']).toBeUndefined();
  const discoverSeries = state.tmdbRequests.find((call) => /\/discover\/tv$/.test(call.path));
  expect(discoverSeries?.query['with_networks']).toBeUndefined();
  expect(state.ollamaCalls[1]?.request).toContain('Imaginary Network');
});
