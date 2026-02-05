#!/bin/bash
# =====================================================================
# TAM Data Regeneration Script
# =====================================================================
# Purpose: Regenerate all data for a fresh deployment or data reset
#
# Usage: ./infrastructure/db/regenerate-all-data.sh
#
# This script runs all SQL scripts in order and refreshes materialized views
# =====================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DB_CONTAINER="tam-timescaledb-1"
DB_NAME="utam"
DB_USER="postgres"

echo ""
echo "=========================================="
echo "TAM Data Regeneration Starting..."
echo "=========================================="
echo ""

# Check if container is running
if ! docker ps | grep -q $DB_CONTAINER; then
    echo "Error: Container $DB_CONTAINER is not running"
    exit 1
fi

# Step 1: Schema fixes and stands
echo "Step 1/6: Checking schema and adding stands..."
docker exec -i $DB_CONTAINER psql -U $DB_USER -d $DB_NAME < "$SCRIPT_DIR/fix_schema_and_add_stands.sql"
echo "  ✓ Schema and stands updated"

# Step 2: Populate report data (violations, trails, discrepancies, predictions)
echo "Step 2/6: Populating report data..."
docker exec -i $DB_CONTAINER psql -U $DB_USER -d $DB_NAME < "$SCRIPT_DIR/populate_report_data.sql"
echo "  ✓ Report data populated (violations, trails, discrepancies, predictions)"

# Step 3: Update asset statuses
echo "Step 3/6: Updating asset statuses..."
docker exec -i $DB_CONTAINER psql -U $DB_USER -d $DB_NAME < "$SCRIPT_DIR/update_asset_statuses.sql"
echo "  ✓ Asset statuses updated"

# Step 4: Fix turnaround data
echo "Step 4/6: Fixing turnaround data..."
docker exec -i $DB_CONTAINER psql -U $DB_USER -d $DB_NAME < "$SCRIPT_DIR/fix_turnaround_data.sql"
echo "  ✓ Turnaround data fixed"

# Step 5: Refresh materialized views
echo "Step 5/6: Refreshing materialized views..."
docker exec -i $DB_CONTAINER psql -U $DB_USER -d $DB_NAME << 'EOF'
DO $$
BEGIN
    -- Refresh asset_activity_heatmap
    IF EXISTS (SELECT 1 FROM pg_matviews WHERE matviewname = 'asset_activity_heatmap') THEN
        REFRESH MATERIALIZED VIEW asset_activity_heatmap;
        RAISE NOTICE 'Refreshed asset_activity_heatmap';
    END IF;
    
    -- Refresh violation_heatmap
    IF EXISTS (SELECT 1 FROM pg_matviews WHERE matviewname = 'violation_heatmap') THEN
        REFRESH MATERIALIZED VIEW violation_heatmap;
        RAISE NOTICE 'Refreshed violation_heatmap';
    END IF;
    
    -- Refresh activity_heatmap (legacy)
    IF EXISTS (SELECT 1 FROM pg_matviews WHERE matviewname = 'activity_heatmap') THEN
        REFRESH MATERIALIZED VIEW activity_heatmap;
        RAISE NOTICE 'Refreshed activity_heatmap';
    END IF;
    
    RAISE NOTICE 'All materialized views refreshed';
EXCEPTION WHEN OTHERS THEN
    RAISE WARNING 'Error refreshing materialized views: %', SQLERRM;
END $$;
EOF
echo "  ✓ Materialized views refreshed"

# Step 6: Summary
echo "Step 6/6: Generating summary..."
docker exec -i $DB_CONTAINER psql -U $DB_USER -d $DB_NAME << 'EOF'
SELECT '========================================' as "===";
SELECT 'Data Regeneration Summary' as "Title";
SELECT '========================================' as "===";

SELECT 'zone_violations' as table_name, COUNT(*) as count FROM zone_violations
UNION ALL SELECT 'movement_discrepancies', COUNT(*) FROM movement_discrepancies
UNION ALL SELECT 'asset_movement_trail', COUNT(*) FROM asset_movement_trail
UNION ALL SELECT 'turnaround_sessions', COUNT(*) FROM turnaround_sessions
UNION ALL SELECT 'turnaround_tasks', COUNT(*) FROM turnaround_tasks
UNION ALL SELECT 'sensor_alerts', COUNT(*) FROM sensor_alerts
UNION ALL SELECT 'assets', COUNT(*) FROM assets
UNION ALL SELECT 'stands', COUNT(*) FROM stands
UNION ALL SELECT 'pred_turnaround_risk', COUNT(*) FROM pred_turnaround_risk
UNION ALL SELECT 'pred_congestion', COUNT(*) FROM pred_congestion
UNION ALL SELECT 'forecast_violations_hourly', COUNT(*) FROM forecast_violations_hourly
ORDER BY table_name;

SELECT '' as "";
SELECT 'Materialized Views:' as "Title";
SELECT matviewname, 
       pg_size_pretty(pg_relation_size(oid)) as size
FROM pg_matviews 
WHERE schemaname = 'public' 
AND matviewname LIKE '%heatmap%'
ORDER BY matviewname;
EOF

echo ""
echo "=========================================="
echo "TAM Data Regeneration Complete!"
echo "=========================================="
echo ""
echo "Data populated for all tables including:"
echo "  • Zone violations (for Safety & Security reports)"
echo "  • Movement discrepancies (for Discrepancy reports)"
echo "  • Asset movement trail (for Activity Heatmap)"
echo "  • Turnaround sessions & tasks (for SLA Compliance)"
echo "  • Sensor alerts (for Alerts reports)"
echo "  • Prediction tables (for ML/Forecasting)"
echo ""
echo "Materialized views refreshed:"
echo "  • asset_activity_heatmap"
echo "  • violation_heatmap"
echo ""
