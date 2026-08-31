import { expect, Page, test } from '@playwright/test';

async function mockApi(page: Page): Promise<void> {
  await page.route('**/api/**', async route => {
    const request = route.request();
    const url = new URL(request.url());

    if (url.pathname === '/api/preference') {
      await route.fulfill({
        json: { downloadDirectoryPath: '/fixture/downloads' }
      });
      return;
    }

    if (url.pathname === '/api/bittorrent' && request.method() === 'GET') {
      await route.fulfill({ json: [] });
      return;
    }

    if (url.pathname === '/api/torrent/') {
      await route.fulfill({ json: [] });
      return;
    }

    await route.fulfill({ status: 404, json: {} });
  });
}

test.beforeEach(async ({ page }) => {
  await mockApi(page);
});

test('redirects to search and exposes primary navigation', async ({ page }, testInfo) => {
  await page.goto('/');

  await expect(page).toHaveURL(/\/search$/);
  await expect(page.getByText('Torenta', { exact: true })).toBeVisible();
  await expect(page.getByText('Search TV Show', { exact: true })).toBeVisible();
  await expect(page.getByText('Search Movie', { exact: true })).toBeVisible();
  await expect(page.getByText('Search Torrents', { exact: true })).toBeVisible();
  await page.screenshot({ path: testInfo.outputPath('search-page.png'), fullPage: true });

  await page.getByRole('button', { name: /Preferences$/ }).click();
  await expect(page).toHaveURL(/\/preferences$/);
  await expect(page.getByText('Preferences', { exact: true }).last()).toBeVisible();

  await page.getByRole('button', { name: /Downloads$/ }).click();
  await expect(page).toHaveURL(/\/downloads$/);
  await expect(page.getByText('Downloads', { exact: true }).last()).toBeVisible();

  await page.goto('/missing');
  await page.getByRole('button', { name: 'Return to base' }).click();
  await expect(page).toHaveURL(/\/search$/);
});

test('loads and saves preferences', async ({ page }) => {
  let savedPreferences: unknown;
  await page.route('**/api/preference', async route => {
    if (route.request().method() === 'POST') {
      savedPreferences = route.request().postDataJSON();
      await route.fulfill({ json: {} });
      return;
    }

    await route.fulfill({ json: { downloadDirectoryPath: '/fixture/downloads' } });
  });

  await page.goto('/preferences');
  const input = page.getByPlaceholder('Root folder for downloads');
  await expect(input).toHaveValue('/fixture/downloads');
  await input.fill('/fixture/new-downloads');
  await page.getByRole('button', { name: 'Save' }).click();

  await expect.poll(() => savedPreferences).toEqual({
    downloadDirectoryPath: '/fixture/new-downloads'
  });
});

test('searches torrents and starts a download', async ({ page }) => {
  let downloadRequest: unknown;
  const torrent = {
    name: 'Fixture Torrent',
    magnetLink: 'magnet:?xt=urn:btih:fixture',
    link: 'https://example.invalid/torrent',
    uploadedTime: 'Today',
    size: '1.00 GiB',
    uploader: 'fixture',
    numberOfSeeders: 42,
    numberOfLeechers: 2,
    category: 'Video',
    subCategory: 'HD',
    uploaderIsVIP: true,
    uploaderIsTrusted: true
  };

  await page.route('**/api/torrent/**', route => route.fulfill({ json: [torrent] }));
  await page.route('**/api/bittorrent', async route => {
    if (route.request().method() === 'POST') {
      downloadRequest = route.request().postDataJSON();
      await route.fulfill({ json: {} });
      return;
    }

    await route.fulfill({ json: [] });
  });

  await page.goto('/search');
  await page.getByPlaceholder('TorrentSearch').pressSequentially('fixture');

  const row = page.locator('tr.mat-row').filter({ hasText: 'Fixture Torrent' });
  await expect(row).toContainText('42');
  await expect(row).toContainText('1.00 GiB');
  await row.getByRole('button', { name: 'Download' }).click();

  await expect.poll(() => downloadRequest).toEqual({ torrentEntry: torrent });
  await expect(page.getByText('Started downloading Fixture Torrent')).toBeVisible();
});

test('expands movie and series search results', async ({ page }) => {
  const movieOverview = {
    id: 11,
    original_title: 'Fixture Movie',
    popularity: 99,
    poster_path: null
  };
  const seriesOverview = {
    id: 22,
    name: 'Fixture Series',
    popularity: 100,
    poster_path: null
  };

  await page.route('**/api/tmdb/movie?*', route => route.fulfill({
    json: { results: [movieOverview] }
  }));
  await page.route('**/api/tmdb/tv?*', route => route.fulfill({
    json: { results: [seriesOverview] }
  }));
  await page.route('**/api/tmdb/movie/11', route => route.fulfill({
    json: {
      ...movieOverview,
      title: 'Fixture Movie',
      backdrop_path: null,
      release_date: '2020-01-02',
      runtime: 123,
      vote_average: 8.5,
      genres: [{ id: 1, name: 'Drama' }],
      overview: 'Fixture movie overview'
    }
  }));
  await page.route('**/api/directory/movie/**', route => route.fulfill({
    json: { name: 'Fixture Movie', absolutePath: '/movies/Fixture Movie', directories: [], files: [] }
  }));
  await page.route('**/api/tmdb/tv/22', route => route.fulfill({
    json: {
      ...seriesOverview,
      backdrop_path: null,
      first_air_date: '2020-01-01',
      episode_run_time: [45],
      vote_average: 9,
      genres: [{ id: 2, name: 'Mystery' }],
      overview: 'Fixture series overview',
      seasons: [{ id: 1, name: 'Season 1', season_number: 1, air_date: '2020-01-01', overview: 'Season overview' }]
    }
  }));
  await page.route('**/api/directory/series/**', route => route.fulfill({
    json: { name: 'Fixture Series', absolutePath: '/series/Fixture Series', directories: [], files: [] }
  }));
  await page.route('**/api/tmdb/tv/22/season/1', route => route.fulfill({
    json: [{ id: 1, name: 'Pilot', season_number: 1, episode_number: 1, air_date: '2020-01-01', overview: 'Episode overview' }]
  }));
  await page.route('**/api/library/tv/**', route => route.fulfill({
    json: { name: 'Fixture Series', seasonList: [] }
  }));

  await page.goto('/search');
  await page.getByPlaceholder('Movie').pressSequentially('fixture');
  await page.locator('mat-expansion-panel').filter({ hasText: 'Fixture Movie' }).click();
  await expect(page.locator('app-movie-detail')).toContainText('Fixture movie overview');
  await expect(page.locator('app-movie-detail')).toContainText('/movies/Fixture Movie');

  await page.getByPlaceholder('TV Show').pressSequentially('fixture');
  await page.locator('mat-expansion-panel').filter({ hasText: 'Fixture Series' }).click();
  await expect(page.locator('app-series-detail')).toContainText('Fixture series overview');
  await page.getByRole('button', { name: 'S01' }).click();
  await page.getByRole('button', { name: 'E01' }).click();
  await expect(page.locator('app-episode')).toContainText('Episode overview');
});

test('renders downloads in descending start-time order', async ({ page }) => {
  const download = (id: number, title: string, startTimeInMs: number) => ({
    id,
    state: 'STARTED',
    progress: 0.5,
    downloadRequest: {
      torrentEntry: { name: title }
    },
    startTimeInMs,
    connectedPeers: 3,
    totalBytes: 200000000,
    downloadSpeedInBytesPerSecond: 1000000
  });

  await page.route('**/api/bittorrent', route => route.fulfill({
    json: [download(1, 'Older Fixture', 1000), download(2, 'Newer Fixture', 2000)]
  }));

  await page.goto('/downloads');
  const titles = page.locator('.download_title');
  await expect(titles).toHaveCount(2);
  await expect(titles.nth(0)).toContainText('Newer Fixture');
  await expect(titles.nth(1)).toContainText('Older Fixture');
  await expect(page.locator('app-download-detail').first()).toContainText('3 sources');
  await expect(page.locator('app-download-detail').first()).toContainText('50.0 %');
});

test('notifies when a polling download finishes', async ({ page }) => {
  let pollCount = 0;
  const fixture = {
    id: 1,
    state: 'STARTED',
    progress: 0.5,
    downloadRequest: { torrentEntry: { name: 'Completing Fixture' } },
    startTimeInMs: 1000,
    connectedPeers: 1,
    totalBytes: 1000,
    downloadSpeedInBytesPerSecond: 100
  };

  await page.route('**/api/bittorrent', route => {
    pollCount++;
    route.fulfill({
      json: [{
        ...fixture,
        state: pollCount > 1 ? 'FINISHED' : 'STARTED',
        progress: pollCount > 1 ? 1 : 0.5
      }]
    });
  });

  await page.goto('/downloads');
  await expect(page.locator('app-download-detail')).toContainText('50.0 %');
  await expect(page.locator('.mat-snack-bar-container')).toContainText(
    'Completing Fixture successfully downloaded.',
    { timeout: 5000 }
  );
  await expect(page.locator('app-download-detail')).toContainText('Finished');
});
