#!/bin/bash
# TAM Demo Data Seeder
# Triggers simulation data generation via backend REST APIs

set -e

# Configuration
BACKEND_URL="http://localhost:8080"
API_PATH="/api/admin/generators"

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

usage() {
    echo "Usage: $0 [options]"
    echo "Options:"
    echo "  --tenant CODE    Tenant code (e.g. VIDP, or 'all')"
    echo "  --days NUM       Historical days to generate (default: 7)"
    echo "  --batch NUM      Batch size for simulation (default: 100)"
    echo "  --clear          Clear existing simulation data first"
    echo "  --sync           Sync vehicles to assets after generation"
    exit 1
}

# Default values
TENANT="VIDP"
DAYS=7
BATCH=100
CLEAR=false
SYNC=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --tenant) TENANT="$2"; shift 2 ;;
        --days) DAYS="$2"; shift 2 ;;
        --batch) BATCH="$2"; shift 2 ;;
        --clear) CLEAR=true; shift 1 ;;
        --sync) SYNC=true; shift 1 ;;
        *) usage ;;
    esac
done

log() { echo -e "${BLUE}[INF]${NC} $1"; }
success() { echo -e "${GREEN}[OK]${NC} $1"; }
error() { echo -e "${RED}[ERR]${NC} $1"; }

# Check backend availability
log "Checking backend connectivity at $BACKEND_URL..."
if ! curl -s "$BACKEND_URL/health" &> /dev/null; then
    # Some setups return plain text, some JSON. If health exists, it's up.
    if ! curl -s "$BACKEND_URL/actuator/health" &> /dev/null; then
        error "Backend is not responding. Ensure it is running before seeding data."
        exit 1
    fi
fi

seed_tenant() {
    local code=$1
    log "Seeding data for tenant: $code"
    
    if [ "$CLEAR" = true ]; then
        log "Clearing existing simulation data..."
        curl -s -X POST "$BACKEND_URL$API_PATH/clear/$code" > /dev/null
    fi
    
    log "Generating batch data (size $BATCH)..."
    curl -s -X POST "$BACKEND_URL$API_PATH/batch/$code?batchSize=$BATCH" > /dev/null
    
    log "Generating historical data ($DAYS days)..."
    curl -s -X POST "$BACKEND_URL$API_PATH/historical/$code?days=$DAYS" > /dev/null
    
    if [ "$SYNC" = true ]; then
        log "Syncing vehicles to assets..."
        curl -s -X POST "$BACKEND_URL$API_PATH/sync-vehicle-assets/$code" > /dev/null
    fi
    
    success "Data seeded for $code"
}

if [ "$TENANT" = "all" ]; then
    # Get active tenants from API if possible, or use defaults
    TENANTS=("VIDP" "LIRN" "YBBN")
    for t in "${TENANTS[@]}"; do
        seed_tenant "$t"
    done
else
    seed_tenant "$TENANT"
fi

log "Refreshing materialized views..."
curl -s -X POST "$BACKEND_URL$API_PATH/refresh-views" > /dev/null

success "Demo data seeding completed! ✅"
