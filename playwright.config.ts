import { defineConfig } from '@playwright/test';

/**
 * Playwright configuration for E2E tests
 * Task: T064
 */
export default defineConfig({
    testDir: './e2e',
    timeout: 30000,
    retries: 2,
    use: {
        baseURL: 'http://localhost:3000',
        screenshot: 'only-on-failure',
        video: 'retain-on-failure',
        trace: 'retain-on-failure',
    },
    projects: [
        {
            name: 'chromium',
            use: { browserName: 'chromium' },
        },
        {
            name: 'firefox',
            use: { browserName: 'firefox' },
        },
        {
            name: 'webkit',
            use: { browserName: 'webkit' },
        },
    ],
    webServer: {
        command: 'npm run dev',
        port: 3000,
        reuseExistingServer: true,
    },
});
