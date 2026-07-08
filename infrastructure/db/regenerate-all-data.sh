#!/bin/bash
# =====================================================================
# TAM Data Regeneration Script (UPDATED)
# =====================================================================
# Purpose: Re-run all init SQL scripts to refresh data.
# Usage:   ./infrastructure/db/regenerate-all-data.sh
#          ./infrastructure/db/regenerate-all-data.sh VIDP  (single tenant)
# =====================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INIT_DIR="$SCRIPT_DIR/init"
DB_CONTAINER="tam-timescaledb-1"
DB_NAME="utam"
DB_USER="postgres"

echo ""
echo "=========================================="
echo "TAM Data Regeneration Starting..."
echo "=========================================="
echo ""

# Check if container is running
if ! docker ps | grep -q "$DB_CONTAINER"; then
    echo "Error: Container $DB_CONTAINER is not running"
    exit 1
fi

run_sql() {
    local file="$1"
    local label="$2"
    echo "  → $label"
    docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" < "$file" 2>&1 | grep -E "^(NOTICE|ERROR|WARNING)" | head -10 || true
}

# Run all init scripts in sequence (only data-population ones, skip DDL already applied)
echo "Step 1/4: Schema & simulation tables..."
run_sql "$INIT_DIR/12-fix-schema-and-add-stands.sql"       "Schema + stands"
run_sql "$INIT_DIR/14-create-simulation-tables.sql"        "Simulation tables"

echo "Step 2/4: Seeding report & movement data..."
run_sql "$INIT_DIR/15-populate-report-data.sql"            "Zone violations / movement trail / discrepancies"
run_sql "$INIT_DIR/16-update-asset-statuses.sql"           "Asset statuses"
run_sql "$INIT_DIR/17-fix-turnaround-data.sql"             "Turnaround base data"

echo "Step 3/4: Asset & vehicle mapping..."
run_sql "$INIT_DIR/20-fix-asset-register-and-mapping.sql"  "Vehicle → asset sync"
run_sql "$INIT_DIR/21-fix-simulation-tables-and-trail-links.sql" "Airport boundaries + trail UUID links"
run_sql "$INIT_DIR/22-normalize-and-enrich-seed-data.sql"  "Category normalize / dwell heatmap / turnaround enrich"

echo "Step 4/5: Refreshing stale turnaround/camera-health timestamps..."
run_sql "$SCRIPT_DIR/refresh-demo-timestamps.sql"           "Shift turnaround/camera timestamps to now"

echo "Step 5/5: Refreshing materialized views..."
docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" << 'SQL'
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_matviews WHERE matviewname = 'asset_activity_heatmap') THEN
        REFRESH MATERIALIZED VIEW asset_activity_heatmap;
        RAISE NOTICE 'Refreshed asset_activity_heatmap';
    END IF;
    IF EXISTS (SELECT 1 FROM pg_matviews WHERE matviewname = 'violation_heatmap') THEN
        REFRESH MATERIALIZED VIEW violation_heatmap;
        RAISE NOTICE 'Refreshed violation_heatmap';
    END IF;
    IF EXISTS (SELECT 1 FROM pg_matviews WHERE matviewname = 'asset_dwell_heatmap') THEN
        REFRESH MATERIALIZED VIEW asset_dwell_heatmap;
        RAISE NOTICE 'Refreshed asset_dwell_heatmap';
    END IF;
    RAISE NOTICE 'All materialized views refreshed';
EXCEPTION WHEN OTHERS THEN
    RAISE WARNING 'Error refreshing views: %', SQLERRM;
END $$;
SQL

echo ""
echo "Data Summary:"
docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" << 'SQL'
SELECT table_name, count FROM (
  SELECT 'zone_violations'         AS table_name, COUNT(*) FROM zone_violations
  UNION ALL SELECT 'asset_movement_trail',   COUNT(*) FROM asset_movement_trail
  UNION ALL SELECT 'turnaround_sessions',    COUNT(*) FROM turnaround_sessions
  UNION ALL SELECT 'turnaround_tasks',       COUNT(*) FROM turnaround_tasks
  UNION ALL SELECT 'turnaround_alerts',      COUNT(*) FROM turnaround_alerts
  UNION ALL SELECT 'assets',                 COUNT(*) FROM assets
  UNION ALL SELECT 'vehicle_asset_map',      COUNT(*) FROM vehicle_asset_map
  UNION ALL SELECT 'airport_boundaries',     COUNT(*) FROM airport_boundaries
) t ORDER BY table_name;
SQL

echo ""
echo "=========================================="
echo "TAM Data Regeneration Complete!"
echo "=========================================="
