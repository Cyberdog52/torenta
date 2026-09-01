import { defineConfig } from '@playwright/test';

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
      command: backendCommand,
      env: {
        ...process.env,
        CH_ANDRESKONRAD_TORENTA_TMDB_SERVICE_KEY: 'playwright-placeholder',
      },
      reuseExistingServer: !process.env.CI,
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
