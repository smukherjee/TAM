-- TAM Database: Asset Tracking Heatmap Views (Layer 6 Extension)
-- Scope: Heatmap materialized views for US5/US6 (Phase 2A)
-- Feature: 005-asset-tracking-security
-- Tasks: T024 (activity heatmap), T025 (violation heatmap), T026 (refresh policy)
-- Created: 2026-01-28

-- Prerequisites: 06-asset-tracking-security.sql must be applied
-- Requires: PostGIS, TimescaleDB extensions

BEGIN;

-- =============================================================================
-- T024: ASSET ACTIVITY HEATMAP MATERIALIZED VIEW
-- =============================================================================
-- Purpose: Grid-based aggregation of asset movements for hotspot analysis
-- Grid Resolution: 10m (0.0001° lat/lng) - finest resolution, can be resampled
-- Time Bucket: 1 hour - aggregated by hour for last 30 days
-- =============================================================================

CREATE MATERIALIZED VIEW IF NOT EXISTS asset_activity_heatmap AS
SELECT
    tenant_code,
    time_bucket('1 hour', timestamp) AS time_bucket,
    -- Snap to 10m grid (0.0001° resolution)
    ST_SnapToGrid(location, 0.0001) AS grid_location,
    ST_Y(ST_SnapToGrid(location, 0.0001)) AS grid_latitude,
    ST_X(ST_SnapToGrid(location, 0.0001)) AS grid_longitude,
    COUNT(*) AS activity_count,
    COUNT(DISTINCT asset_identifier) AS unique_assets,
    AVG(speed) AS avg_speed,
    MAX(speed) AS max_speed,
    PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY speed) AS median_speed,
    MIN(timestamp) AS first_activity,
    MAX(timestamp) AS last_activity
FROM asset_movement_trail
WHERE
    -- Last 30 days only (reduce view size)
    timestamp >= NOW() - INTERVAL '30 days'
    AND location IS NOT NULL
    AND speed IS NOT NULL
GROUP BY
    tenant_code,
    time_bucket,
    grid_location
WITH NO DATA; -- Don't populate initially, will refresh on-demand

-- Create indexes for efficient querying
CREATE INDEX IF NOT EXISTS idx_activity_heatmap_tenant_time
    ON asset_activity_heatmap (tenant_code, time_bucket DESC);

CREATE INDEX IF NOT EXISTS idx_activity_heatmap_location
    ON asset_activity_heatmap USING GIST (grid_location);

CREATE INDEX IF NOT EXISTS idx_activity_heatmap_activity_count
    ON asset_activity_heatmap (activity_count DESC)
    WHERE activity_count > 10; -- Index only significant activity

COMMENT ON MATERIALIZED VIEW asset_activity_heatmap IS
    'Aggregated asset movement activity by 10m grid cells and hourly time buckets. '
    'Used for US6 Activity Density heatmap visualization. '
    'Refresh policy: Every 6 hours.';

COMMENT ON COLUMN asset_activity_heatmap.grid_location IS
    '10m grid cell centroid (0.0001° resolution). Use ST_DWithin for spatial queries.';

COMMENT ON COLUMN asset_activity_heatmap.activity_count IS
    'Total movement records in this grid cell during the time bucket. High values indicate congestion.';

-- =============================================================================
-- T025: VIOLATION HEATMAP MATERIALIZED VIEW
-- =============================================================================
-- Purpose: Grid-based aggregation of zone violations for security hotspot analysis
-- Grid Resolution: 10m (0.0001° lat/lng)
-- Time Bucket: 1 hour
-- =============================================================================

CREATE MATERIALIZED VIEW IF NOT EXISTS violation_heatmap AS
SELECT
    tenant_code,
    time_bucket('1 hour', timestamp) AS time_bucket,
    -- Snap to 10m grid
    ST_SnapToGrid(entry_location, 0.0001) AS grid_location,
    ST_Y(ST_SnapToGrid(entry_location, 0.0001)) AS grid_latitude,
    ST_X(ST_SnapToGrid(entry_location, 0.0001)) AS grid_longitude,
    COUNT(*) AS violation_count,
    COUNT(*) FILTER (WHERE severity = 'CRITICAL') AS critical_count,
    COUNT(*) FILTER (WHERE severity = 'HIGH') AS high_count,
    COUNT(*) FILTER (WHERE severity = 'MEDIUM') AS medium_count,
    COUNT(*) FILTER (WHERE severity = 'LOW') AS low_count,
    COUNT(DISTINCT asset_identifier) AS unique_violating_assets,
    COUNT(DISTINCT restricted_zone_id) AS unique_zones_violated,
    MODE() WITHIN GROUP (ORDER BY zone_type) AS most_common_zone_type,
    MIN(timestamp) AS first_violation,
    MAX(timestamp) AS last_violation,
    -- List of assets that violated (for drill-down)
    ARRAY_AGG(DISTINCT asset_identifier ORDER BY asset_identifier) AS violating_assets
FROM zone_violations
WHERE
    -- Last 30 days only
    timestamp >= NOW() - INTERVAL '30 days'
    AND entry_location IS NOT NULL
GROUP BY
    tenant_code,
    time_bucket,
    grid_location
WITH NO DATA;

-- Create indexes for efficient querying
CREATE INDEX IF NOT EXISTS idx_violation_heatmap_tenant_time
    ON violation_heatmap (tenant_code, time_bucket DESC);

CREATE INDEX IF NOT EXISTS idx_violation_heatmap_location
    ON violation_heatmap USING GIST (grid_location);

CREATE INDEX IF NOT EXISTS idx_violation_heatmap_critical
    ON violation_heatmap (critical_count DESC)
    WHERE critical_count > 0;

CREATE INDEX IF NOT EXISTS idx_violation_heatmap_total
    ON violation_heatmap (violation_count DESC)
    WHERE violation_count > 0;

COMMENT ON MATERIALIZED VIEW violation_heatmap IS
    'Aggregated zone violations by 10m grid cells and hourly time buckets. '
    'Used for US6 Violation Density heatmap. Shows security hotspots. '
    'Refresh policy: Every 6 hours.';

COMMENT ON COLUMN violation_heatmap.violation_count IS
    'Total zone violations in this grid cell. High values indicate frequent security breaches.';

COMMENT ON COLUMN violation_heatmap.violating_assets IS
    'Array of asset identifiers that violated zones in this cell (for hotspot detail modal).';

-- =============================================================================
-- T026: REFRESH POLICIES FOR HEATMAP VIEWS
-- =============================================================================
-- Purpose: Automate materialized view refresh every 6 hours
-- Strategy: Trade data freshness for query performance
-- =============================================================================

-- Note: PostgreSQL materialized views don't have built-in refresh policies like TimescaleDB
-- We create a helper function and can schedule it via pg_cron if available

-- 1. Create manual refresh function
CREATE OR REPLACE FUNCTION refresh_heatmaps()
RETURNS TABLE(view_name TEXT, refresh_duration INTERVAL, rows_refreshed BIGINT) AS $$
DECLARE
    start_time TIMESTAMP;
    end_time TIMESTAMP;
    activity_rows BIGINT;
    violation_rows BIGINT;
BEGIN
    -- Refresh activity heatmap
    start_time := clock_timestamp();
    REFRESH MATERIALIZED VIEW CONCURRENTLY asset_activity_heatmap;
    end_time := clock_timestamp();
    SELECT count(*) INTO activity_rows FROM asset_activity_heatmap;
    
    view_name := 'asset_activity_heatmap';
    refresh_duration := end_time - start_time;
    rows_refreshed := activity_rows;
    RETURN NEXT;
    
    -- Refresh violation heatmap
    start_time := clock_timestamp();
    REFRESH MATERIALIZED VIEW CONCURRENTLY violation_heatmap;
    end_time := clock_timestamp();
    SELECT count(*) INTO violation_rows FROM violation_heatmap;
    
    view_name := 'violation_heatmap';
    refresh_duration := end_time - start_time;
    rows_refreshed := violation_rows;
    RETURN NEXT;
    
    RETURN;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION refresh_heatmaps() IS
    'Manually refresh both heatmap materialized views. '
    'Call this function every 6 hours via pg_cron or application scheduler. '
    'Example: SELECT * FROM refresh_heatmaps();';

-- 2. Initial population (commented out - uncomment after data is available)
-- REFRESH MATERIALIZED VIEW asset_activity_heatmap;
-- REFRESH MATERIALIZED VIEW violation_heatmap;

-- 3. Scheduling instructions (if pg_cron is available)
-- Uncomment below to enable automatic refresh every 6 hours

-- Install pg_cron extension (requires superuser)
-- CREATE EXTENSION IF NOT EXISTS pg_cron;

-- Schedule refresh every 6 hours (at 00:00, 06:00, 12:00, 18:00 UTC)
-- SELECT cron.schedule(
--     'refresh-heatmaps',
--     '0 */6 * * *',
--     'SELECT refresh_heatmaps();'
-- );

-- 4. Manual refresh via application (alternative to pg_cron)
-- Backend service can call this on-demand or via scheduled task
-- Spring Boot example: @Scheduled(cron = "0 0 */6 * * *")
-- public void refreshHeatmaps() { jdbcTemplate.query("SELECT * FROM refresh_heatmaps()", ...); }

-- =============================================================================
-- VERIFICATION QUERIES
-- =============================================================================
-- Uncomment to test after populating movement trail data

-- Test activity heatmap structure
-- SELECT 
--     tenant_code, 
--     time_bucket, 
--     grid_latitude, 
--     grid_longitude, 
--     activity_count, 
--     unique_assets 
-- FROM asset_activity_heatmap 
-- ORDER BY activity_count DESC 
-- LIMIT 10;

-- Test violation heatmap structure
-- SELECT 
--     tenant_code, 
--     time_bucket, 
--     grid_latitude, 
--     grid_longitude, 
--     violation_count, 
--     critical_count, 
--     violating_assets 
-- FROM violation_heatmap 
-- ORDER BY critical_count DESC, violation_count DESC 
-- LIMIT 10;

-- Test refresh function
-- SELECT * FROM refresh_heatmaps();

-- Check heatmap data coverage by tenant
-- SELECT tenant_code, COUNT(*) AS grid_cells, SUM(activity_count) AS total_movements
-- FROM asset_activity_heatmap
-- GROUP BY tenant_code;

COMMIT;

-- =============================================================================
-- POST-MIGRATION TASKS
-- =============================================================================
-- After applying this migration:
-- 1. Populate initial data:
--    REFRESH MATERIALIZED VIEW asset_activity_heatmap;
--    REFRESH MATERIALIZED VIEW violation_heatmap;
-- 2. Verify row counts: SELECT COUNT(*) FROM asset_activity_heatmap;
-- 3. Schedule refresh (pg_cron OR application scheduler)
-- 4. Test heatmap API endpoints (T033-T034 in tasks.md)
-- 5. Mark T024-T026 as complete in tasks.md
