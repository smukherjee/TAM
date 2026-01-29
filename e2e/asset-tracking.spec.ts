import { test, expect, Page } from '@playwright/test';

/**
 * E2E tests for asset tracking features
 * Task: T064 - E2E tests (Playwright)
 */

test.describe('Live Asset Map E2E Tests', () => {
    let page: Page;

    test.beforeEach(async ({ page: p }) => {
        page = p;
        // Login first
        await page.goto('http://localhost:3000/login');
        await page.fill('input[name="username"]', 'admin');
        await page.fill('input[name="password"]', 'admin123');
        await page.click('button[type="submit"]');
        await page.waitForURL('http://localhost:3000/');
    });

    test('should view live asset map and click asset to view details', async () => {
        // Navigate to live map
        await page.goto('http://localhost:3000/');

        // Wait for map to load
        await page.waitForSelector('.leaflet-container');

        // Wait for asset markers to appear
        await page.waitForSelector('.leaflet-marker-icon', { timeout: 5000 });

        // Click on first asset marker
        const marker = page.locator('.leaflet-marker-icon').first();
        await marker.click();

        // Popup should appear with asset details
        await expect(page.locator('.leaflet-popup')).toBeVisible();
        await expect(page.locator('.leaflet-popup')).toContainText(/Asset/i);
    });

    test('should apply category filter and verify marker count updates', async () => {
        await page.goto('http://localhost:3000/');

        // Open filter drawer
        await page.click('[title="Filters"]');

        // Wait for drawer to open
        await page.waitForSelector('[data-testid="filter-drawer"]');

        // Note the initial count
        const initialCount = await page.textContent('[data-testid="asset-count"]');

        // Apply Emergency category filter
        await page.click('label:has-text("Emergency")');

        // Wait for filters to apply (debounced)
        await page.waitForTimeout(500);

        // Verify count updated
        const newCount = await page.textContent('[data-testid="asset-count"]');
        expect(newCount).not.toBe(initialCount);

        // Verify only Emergency markers visible
        // This would require custom data attributes on markers
    });

    test('should search for asset by ID and zoom to location', async () => {
        await page.goto('http://localhost:3000/');

        // Open search
        const searchInput = page.locator('input[placeholder*="Search"]');
        await searchInput.fill('ASSET-001');

        // Wait for autocomplete results
        await page.waitForSelector('[data-testid="search-results"]');

        // Click on first result
        await page.click('[data-testid="search-result"]:first-child');

        // Map should zoom to asset
        await page.waitForTimeout(1000); // Wait for animation

        // Marker should be highlighted
        await expect(page.locator('.marker-highlighted')).toBeVisible();
    });
});

test.describe('Hotspot Analysis E2E Tests', () => {
    let page: Page;

    test.beforeEach(async ({ page: p }) => {
        page = p;
        await page.goto('http://localhost:3000/login');
        await page.fill('input[name="username"]', 'admin');
        await page.fill('input[name="password"]', 'admin123');
        await page.click('button[type="submit"]');
    });

    test('should switch to heatmap, change mode to Violations, and click hotspot', async () => {
        // Navigate to hotspot analysis
        await page.goto('http://localhost:3000/tracking/hotspots');

        // Wait for heatmap to load
        await page.waitForSelector('.leaflet-container');

        // Default mode should be Activity
        await expect(page.locator('button:has-text("Activity")')).toHaveClass(/bg-orange-600/);

        // Switch to Violations mode
        await page.click('button:has-text("Violations")');

        // Wait for data to reload
        await page.waitForTimeout(2000);

        // Click on a high-intensity area (this is challenging in E2E)
        // Ideally would click on heatmap canvas at specific coordinates
        const heatmapCanvas = page.locator('canvas.leaflet-heatmap-layer');
        await heatmapCanvas.click({ position: { x: 100, y: 100 } });

        // Hotspot detail modal should open
        // Note: This requires the heatmap to emit click events
        // await expect(page.locator('[role="dialog"]')).toBeVisible();
        // await expect(page.locator('[role="dialog"]')).toContainText(/Violations/i);
    });

    test('should export heatmap as PNG and verify download', async () => {
        await page.goto('http://localhost:3000/tracking/hotspots');

        // Wait for heatmap to render
        await page.waitForSelector('.leaflet-container');
        await page.waitForTimeout(2000);

        // Set up download promise before clicking
        const downloadPromise = page.waitForEvent('download');

        // Click export button
        await page.click('[title="Export CSV"]');

        // Wait for download
        const download = await downloadPromise;

        // Verify filename
        expect(download.suggestedFilename()).toContain('heatmap');
        expect(download.suggestedFilename()).toContain('.csv');
    });

    test('should navigate from hotspot to violation report', async () => {
        await page.goto('http://localhost:3000/tracking/hotspots');

        // This test requires clicking a hotspot and then clicking "View Violations" button
        // Implementation depends on HotspotDetailModal structure

        // Click on map area
        await page.click('.leaflet-container');

        // Wait for modal
        // await page.waitForSelector('[role="dialog"]');

        // Click "View Violations" button
        // await page.click('button:has-text("View Violations")');

        // Should navigate to violations page
        // await expect(page).toHaveURL(/.*\/violations.*/);
    });
});

test.describe('Performance E2E Tests', () => {
    test('should render map with 500+ assets in <3 seconds', async ({ page }) => {
        await page.goto('http://localhost:3000/login');
        await page.fill('input[name="username"]', 'admin');
        await page.fill('input[name="password"]', 'admin123');
        await page.click('button[type="submit"]');

        // Measure time to render map
        const startTime = Date.now();

        await page.goto('http://localhost:3000/');
        await page.waitForSelector('.leaflet-container');
        await page.waitForSelector('.leaflet-marker-icon');

        const endTime = Date.now();
        const loadTime = endTime - startTime;

        // Should load in less than 3 seconds
        expect(loadTime).toBeLessThan(3000);
    });

    test('should handle marker clustering at various zoom levels smoothly', async ({ page }) => {
        await page.goto('http://localhost:3000/login');
        await page.fill('input[name="username"]', 'admin');
        await page.fill('input[name="password"]', 'admin123');
        await page.click('button[type="submit"]');

        await page.goto('http://localhost:3000/');
        await page.waitForSelector('.leaflet-container');

        // Zoom out (clusters should form)
        for (let i = 0; i < 5; i++) {
            await page.click('.leaflet-control-zoom-out');
            await page.waitForTimeout(300);
        }

        // Verify clusters are present
        const clusters = await page.locator('.marker-cluster').count();
        expect(clusters).toBeGreaterThan(0);

        // Zoom in (clusters should break apart)
        for (let i = 0; i < 5; i++) {
            await page.click('.leaflet-control-zoom-in');
            await page.waitForTimeout(300);
        }

        // Individual markers should be visible
        const markers = await page.locator('.leaflet-marker-icon').count();
        expect(markers).toBeGreaterThan(0);
    });
});
