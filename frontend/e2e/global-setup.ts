import { request, type FullConfig } from '@playwright/test';

/**
 * The frontend webServer runs `ng serve`, which compiles each lazily-loaded
 * route on demand, the first time it's actually navigated to. On a cold CI
 * runner this first compile can take longer than an individual test's
 * `expect` timeout, causing flaky "element not found" failures that have
 * nothing to do with the page itself (see recommendations page flakiness).
 *
 * To avoid that race, warm up every top-level route once here, with a
 * generous timeout, before any test file runs.
 */
const ROUTES_TO_WARM_UP = [
  '/search',
  '/downloads',
  '/preferences',
  '/recommendations',
  '/this-route-does-not-exist',
];

const WARM_UP_TIMEOUT_MS = 60000;

export default async function globalSetup(config: FullConfig): Promise<void> {
  const baseURL = config.projects[0]?.use?.baseURL ?? 'http://localhost:4200';
  const context = await request.newContext({ baseURL });

  for (const route of ROUTES_TO_WARM_UP) {
    try {
      await context.get(route, { timeout: WARM_UP_TIMEOUT_MS });
    } catch (error) {
      // Best-effort warm-up: if a single route fails to compile in time, log
      // and move on rather than failing the whole run. The actual tests will
      // still catch a genuinely broken route.
      console.warn(`Warm-up request for ${route} failed`, error);
    }
  }

  await context.dispose();
}
