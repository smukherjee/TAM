-- Create materialized view for activity heatmap
-- Aggregates asset movement data into time-bucketed grid cells
CREATE MATERIALIZED VIEW IF NOT EXISTS asset_activity_heatmap AS
SELECT
    time_bucket('1 hour'::interval, timestamp) AS time_bucket,
    tenant_code,
    location AS grid_location,
    COUNT(*) AS activity_count,
    COUNT(DISTINCT asset_identifier) AS unique_assets,
    AVG(speed) AS avg_speed,
    MAX(speed) AS max_speed,
    MIN(timestamp) AS first_activity,
    MAX(timestamp) AS last_activity
FROM asset_movement_trail
WHERE location IS NOT NULL
GROUP BY time_bucket, tenant_code, location
ORDER BY time_bucket DESC, activity_count DESC;

-- Create index on time_bucket for faster time-range queries
CREATE INDEX IF NOT EXISTS idx_activity_heatmap_time_bucket 
ON asset_activity_heatmap (time_bucket DESC, tenant_code);

-- Create index on grid_location for spatial queries
CREATE INDEX IF NOT EXISTS idx_activity_heatmap_location 
ON asset_activity_heatmap USING GIST (grid_location);

-- Create materialized view for violation heatmap
-- Aggregates zone violations into time-bucketed grid cells
CREATE MATERIALIZED VIEW IF NOT EXISTS asset_violation_heatmap AS
SELECT
    time_bucket('1 hour'::interval, amt.timestamp) AS time_bucket,
    amt.tenant_code,
    amt.location AS grid_location,
    COUNT(*) AS violation_count,
    COUNT(DISTINCT amt.asset_identifier) AS unique_violators,
    AVG(amt.speed) AS avg_speed,
    MAX(amt.speed) AS max_speed,
    MIN(amt.timestamp) AS first_violation,
    MAX(amt.timestamp) AS last_violation,
    ARRAY_AGG(DISTINCT amt.zone) AS zones
FROM asset_movement_trail amt
WHERE amt.restricted_zone_id IS NOT NULL
AND amt.location IS NOT NULL
GROUP BY time_bucket, amt.tenant_code, amt.location
ORDER BY time_bucket DESC, violation_count DESC;

-- Create index on time_bucket for faster time-range queries
CREATE INDEX IF NOT EXISTS idx_violation_heatmap_time_bucket 
ON asset_violation_heatmap (time_bucket DESC, tenant_code);

-- Create index on grid_location for spatial queries
CREATE INDEX IF NOT EXISTS idx_violation_heatmap_location 
ON asset_violation_heatmap USING GIST (grid_location);

-- Create materialized view for dwell time heatmap
-- Identifies locations where assets spend significant time
CREATE MATERIALIZED VIEW IF NOT EXISTS asset_dwell_heatmap AS
WITH location_stays AS (
    SELECT
        asset_identifier,
        tenant_code,
        location,
        timestamp,
        LEAD(timestamp) OVER (PARTITION BY asset_identifier, location ORDER BY timestamp) AS next_timestamp,
        LAG(location) OVER (PARTITION BY asset_identifier ORDER BY timestamp) AS prev_location
    FROM asset_movement_trail
    WHERE location IS NOT NULL
)
SELECT
    time_bucket('1 hour'::interval, timestamp) AS time_bucket,
    tenant_code,
    location AS grid_location,
    COUNT(*) AS dwell_events,
    COUNT(DISTINCT asset_identifier) AS unique_assets,
    AVG(EXTRACT(EPOCH FROM (next_timestamp - timestamp))) AS avg_dwell_seconds,
    MAX(EXTRACT(EPOCH FROM (next_timestamp - timestamp))) AS max_dwell_seconds,
    MIN(timestamp) AS first_dwell,
    MAX(timestamp) AS last_dwell
FROM location_stays
WHERE next_timestamp IS NOT NULL
AND prev_location IS DISTINCT FROM location
GROUP BY time_bucket, tenant_code, location
HAVING AVG(EXTRACT(EPOCH FROM (next_timestamp - timestamp))) > 60  -- Dwell > 1 minute
ORDER BY time_bucket DESC, dwell_events DESC;

-- Create index on time_bucket for faster time-range queries
CREATE INDEX IF NOT EXISTS idx_dwell_heatmap_time_bucket 
ON asset_dwell_heatmap (time_bucket DESC, tenant_code);

-- Create index on grid_location for spatial queries
CREATE INDEX IF NOT EXISTS idx_dwell_heatmap_location 
ON asset_dwell_heatmap USING GIST (grid_location);

-- Refresh all materialized views
REFRESH MATERIALIZED VIEW asset_activity_heatmap;
REFRESH MATERIALIZED VIEW asset_violation_heatmap;
REFRESH MATERIALIZED VIEW asset_dwell_heatmap;
