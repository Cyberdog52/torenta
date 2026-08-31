import { test, expect } from '@playwright/test';

const SERIES_ID = 2316;

const SERIES_OVERVIEW = {
  id: SERIES_ID,
  name: 'The Office',
  original_name: 'The Office',
  overview: 'A mockumentary sitcom about the everyday lives of office employees.',
  genre_ids: [35],
  origin_country: ['US'],
  original_language: 'en',
  first_air_date: '2005-03-24',
  popularity: 100,
  vote_average: 8.5,
  vote_count: 1000,
  backdrop_path: null,
  poster_path: null,
};

const SERIES_DETAIL = {
  ...SERIES_OVERVIEW,
  episode_run_time: [22],
  genres: [{ id: 35, name: 'Comedy' }],
  seasons: [],
  created_by: [],
  homepage: '',
  in_production: false,
  languages: ['en'],
  last_air_date: '2013-05-16',
  last_episode_to_air: null,
  next_episode_to_air: null,
  networks: [],
  number_of_episodes: 201,
  number_of_seasons: 9,
  production_companies: [],
  status: 'Ended',
  type: 'Scripted',
};

test('shows search results and renders series detail without crashing', async ({ page }) => {
  const pageErrors: Error[] = [];
  page.on('pageerror', (error) => pageErrors.push(error));

  // The backend proxies TV search/detail to the real TMDB API, which needs a
  // valid API key. CI only ever configures a placeholder key (see
  // `playwright.config.ts`), so TMDB always rejects it and the backend
  // silently returns an empty result. Mocking these two calls keeps this
  // test deterministic and independent of a live third-party API/secret.
  await page.route('**/api/tmdb/tv**', async (route) => {
    const path = new URL(route.request().url()).pathname;
    if (path === '/api/tmdb/tv') {
      await route.fulfill({ json: { results: [SERIES_OVERVIEW] } });
    } else if (/^\/api\/tmdb\/tv\/\d+$/.test(path)) {
      await route.fulfill({ json: SERIES_DETAIL });
    } else {
      await route.continue();
    }
  });

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
