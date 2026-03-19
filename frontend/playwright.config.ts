import { defineConfig } from '@playwright/test'

const e2eMode = process.env.E2E_MODE ?? 'pr'
const isFullstack = e2eMode === 'fullstack'

export default defineConfig({
  testDir: './e2e',
  fullyParallel: !isFullstack,
  retries: isFullstack ? 1 : 0,
  workers: process.env.CI ? 2 : undefined,
  reporter: [['list']],
  use: {
    baseURL: 'http://127.0.0.1:4173',
    headless: true,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  webServer: {
    command: isFullstack ? 'npm run preview:smoke' : 'npm run preview:mock',
    url: 'http://127.0.0.1:4173',
    reuseExistingServer: !process.env.CI,
    timeout: 120000,
  },
})
