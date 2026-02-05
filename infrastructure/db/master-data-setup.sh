#!/bin/bash
# =====================================================================
# TAM MASTER DATA SETUP SCRIPT
# =====================================================================
# Purpose: Run all data generation scripts for a fresh deployment
# Usage: ./infrastructure/db/master-data-setup.sh
# =====================================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

CONTAINER="tam-timescaledb-1"
DB="utam"
USER="postgres"

echo ""
echo "=========================================="
echo "TAM Master Data Setup Starting..."
echo "=========================================="
echo ""

# Check if container is running
if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER}$"; then
    echo -e "${RED}Error: Container ${CONTAINER} is not running${NC}"
    echo "Start the services with: docker compose up -d"
    exit 1
fi

# Function to run a SQL file
run_sql() {
    local file=$1
    local description=$2
    
    if [ -f "$file" ]; then
        echo -e "${YELLOW}→ ${description}${NC}"
        docker exec -i ${CONTAINER} psql -U ${USER} -d ${DB} < "$file" 2>&1 | grep -v "^$" | head -20 || true
        echo -e "${GREEN}✓ Done${NC}"
        echo ""
    else
        echo -e "${RED}✗ File not found: ${file}${NC}"
    fi
}

# Step 1: Schema fixes and stands
echo "Step 1/6: Checking core schema and stands..."
run_sql "infrastructure/db/fix_schema_and_add_stands.sql" "Fixing schema and adding stands"

# Step 2: Initialize simulation infrastructure
echo "Step 2/6: Initializing simulation infrastructure..."
run_sql "infrastructure/db/fix_simulation_tables.sql" "Creating simulation tables"

# Step 3: Populate report data
echo "Step 3/6: Populating report data..."
run_sql "infrastructure/db/populate_report_data.sql" "Adding violations, movement trail, discrepancies"

# Step 4: Update asset statuses
echo "Step 4/6: Updating asset statuses..."
run_sql "infrastructure/db/update_asset_statuses.sql" "Setting asset statuses and locations"

# Step 5: Fix turnaround data
echo "Step 5/6: Fixing turnaround data..."
run_sql "infrastructure/db/fix_turnaround_data.sql" "Fixing turnaround sessions"

# Step 6: Refresh materialized views
echo "Step 6/6: Refreshing materialized views..."
docker exec -i ${CONTAINER} psql -U ${USER} -d ${DB} << 'EOF'
DO $$
BEGIN
    -- Refresh activity heatmap
    PERFORM 1 FROM pg_matviews WHERE matviewname = 'activity_heatmap';
    IF FOUND THEN
        REFRESH MATERIALIZED VIEW activity_heatmap;
        RAISE NOTICE 'Refreshed activity_heatmap';
    END IF;
    
    -- Refresh violation heatmap
    PERFORM 1 FROM pg_matviews WHERE matviewname = 'violation_heatmap';
    IF FOUND THEN
        REFRESH MATERIALIZED VIEW violation_heatmap;
        RAISE NOTICE 'Refreshed violation_heatmap';
    END IF;
END $$;
EOF
echo -e "${GREEN}✓ Materialized views refreshed${NC}"
echo ""

# Summary
echo "=========================================="
echo "Data Population Summary"
echo "=========================================="
docker exec -i ${CONTAINER} psql -U ${USER} -d ${DB} << 'EOF'
SELECT 'zone_violations' as table_name, COUNT(*) as count FROM zone_violations
UNION ALL
SELECT 'asset_movement_trail', COUNT(*) FROM asset_movement_trail
UNION ALL
SELECT 'movement_discrepancies', COUNT(*) FROM movement_discrepancies
UNION ALL
SELECT 'turnaround_sessions', COUNT(*) FROM turnaround_sessions
UNION ALL
SELECT 'assets', COUNT(*) FROM assets
ORDER BY table_name;
EOF

echo ""
echo "Heatmap Views Status:"
docker exec -i ${CONTAINER} psql -U ${USER} -d ${DB} <<'EOF'
SELECT matviewname, 
       pg_size_pretty(pg_total_relation_size(schemaname||'.'||matviewname)) as size
FROM pg_matviews 
WHERE schemaname = 'public' 
AND matviewname LIKE '%heatmap%'
ORDER BY matviewname;
EOF

echo ""
echo "=========================================="
echo -e "${GREEN}TAM Master Data Setup Complete!${NC}"
echo "=========================================="
