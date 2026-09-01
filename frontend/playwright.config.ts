import { defineConfig } from '@playwright/test';

const conciergeFixtureUrl = 'http://127.0.0.1:10999';
const backendCommand =
  process.platform === 'win32'
    ? '..\\gradlew.bat --project-dir .. bootRun'
    : '../gradlew --project-dir .. bootRun';

export default defineConfig({
  outputDir: './dist/playwright-test-results',
  testDir: './e2e',
  globalSetup: './e2e/global-setup.ts',
  expect: {
    timeout: 10000,
  },
  use: {
    baseURL: 'http://localhost:4200',
    trace: 'retain-on-failure',
  },
  webServer: [
    {
      command: 'node e2e/support/concierge-mock-server.mjs',
      env: {
        ...process.env,
        CONCIERGE_MOCK_PORT: '10999',
      },
      reuseExistingServer: false,
      timeout: 30000,
      url: `${conciergeFixtureUrl}/__state`,
    },
    {
      command: backendCommand,
      env: {
        ...process.env,
        APP_AI_OLLAMA_MODEL: 'playwright-model',
        APP_AI_PROVIDER: 'OLLAMA',
        APP_TMDB_BASE_URL: conciergeFixtureUrl,
        CH_ANDRESKONRAD_TORENTA_TMDB_SERVICE_KEY: 'playwright-placeholder',
        SPRING_AI_OLLAMA_BASE_URL: conciergeFixtureUrl,
      },
      reuseExistingServer: false,
      timeout: 120000,
      url: 'http://localhost:8080/actuator/health',
    },
    {
      command: 'npm start',
      reuseExistingServer: !process.env.CI,
      timeout: 120000,
      url: 'http://localhost:4200',
    },
  ],
});
