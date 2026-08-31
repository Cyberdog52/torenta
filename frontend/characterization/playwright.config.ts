import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  outputDir: '.artifacts/test-results',
  fullyParallel: true,
  forbidOnly: true,
  retries: 0,
  reporter: [['line'], ['html', { outputFolder: '.artifacts/report', open: 'never' }]],
  use: {
    baseURL: process.env.PLAYWRIGHT_BASE_URL || 'http://127.0.0.1:4300',
    channel: 'chrome',
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure'
  },
  webServer: {
    command: 'npm start -- --host 127.0.0.1 --port 4300',
    cwd: '..',
    env: {
      NODE_OPTIONS: '--openssl-legacy-provider'
    },
    url: 'http://127.0.0.1:4300',
    reuseExistingServer: false,
    timeout: 120000
  },
  projects: [
    {
      name: 'desktop-chromium',
      use: { ...devices['Desktop Chrome'] }
    },
    {
      name: 'mobile-chromium',
      use: { ...devices['Pixel 7'] }
    }
  ]
});
