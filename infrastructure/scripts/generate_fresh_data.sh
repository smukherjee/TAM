#!/bin/bash
# =============================================================================
# TAM Data Generator Script
# =============================================================================
# This script generates fresh simulation data for TAM airports.
# 
# Usage:
#   ./generate_fresh_data.sh                    # All airports (VIDP, LIRN, YBBN)
#   ./generate_fresh_data.sh VIDP               # Single airport
#   ./generate_fresh_data.sh VIDP LIRN          # Multiple airports
#   ./generate_fresh_data.sh --clear-only VIDP  # Clear data without generating
#   ./generate_fresh_data.sh --help             # Show help
#
# Options:
#   --clear-only    Clear data without generating new data
#   --skip-clear    Generate data without clearing first
#   --batch-size N  Set batch size (default: 100)
#   --days N        Days of historical data (default: 7)
#   --help          Show this help message
# =============================================================================

set -e

# Configuration
BACKEND_URL="${BACKEND_URL:-http://localhost:8080}"
DEFAULT_AIRPORTS=("VIDP" "LIRN" "YBBN")
BATCH_SIZE=100
HISTORICAL_DAYS=7
CLEAR_ONLY=false
SKIP_CLEAR=false

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Print functions
print_header() {
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}  TAM Data Generator${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
}

print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

print_info() {
    echo -e "${YELLOW}→${NC} $1"
}

show_help() {
    echo "TAM Data Generator - Generate fresh simulation data for TAM airports"
    echo ""
    echo "Usage:"
    echo "  $0 [OPTIONS] [AIRPORT_CODES...]"
    echo ""
    echo "Arguments:"
    echo "  AIRPORT_CODES   ICAO codes of airports to process (default: VIDP LIRN YBBN)"
    echo ""
    echo "Options:"
    echo "  --clear-only    Clear data without generating new data"
    echo "  --skip-clear    Generate data without clearing first"
    echo "  --batch-size N  Set batch size for generation (default: 100)"
    echo "  --days N        Days of historical data to generate (default: 7)"
    echo "  --help          Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0                          # Process all default airports"
    echo "  $0 VIDP                     # Process only VIDP"
    echo "  $0 --batch-size 200 LIRN    # Generate 200 records per batch for LIRN"
    echo "  $0 --clear-only YBBN        # Only clear data for YBBN"
    echo ""
    echo "Environment Variables:"
    echo "  BACKEND_URL     Backend API URL (default: http://localhost:8080)"
    echo ""
}

# Check if backend is running
check_backend() {
    print_info "Checking backend health..."
    if curl -s --fail "${BACKEND_URL}/api/admin/generators/status" > /dev/null 2>&1; then
        print_success "Backend is running at ${BACKEND_URL}"
        return 0
    else
        print_error "Backend is not responding at ${BACKEND_URL}"
        echo "  Make sure the backend is running with: docker-compose up -d backend"
        return 1
    fi
}

# Clear data for an airport
clear_airport_data() {
    local airport=$1
    print_info "Clearing data for ${airport}..."
    
    local response
    response=$(curl -s -X POST "${BACKEND_URL}/api/admin/generators/clear/${airport}")
    
    if echo "$response" | grep -q '"success":true'; then
        local rows_deleted=$(echo "$response" | grep -o '"rowsDeleted":[0-9]*' | cut -d: -f2)
        print_success "Cleared ${rows_deleted:-0} rows for ${airport}"
        return 0
    else
        print_error "Failed to clear data for ${airport}: $response"
        return 1
    fi
}

# Generate batch data for an airport
generate_batch_data() {
    local airport=$1
    local batch_size=$2
    print_info "Generating batch data for ${airport} (batch size: ${batch_size})..."
    
    local response
    response=$(curl -s -X POST "${BACKEND_URL}/api/admin/generators/batch/${airport}?batchSize=${batch_size}")
    
    if echo "$response" | grep -q '"success":true'; then
        print_success "Batch data generated for ${airport}"
        echo "  Results: $(echo "$response" | grep -o '"results":{[^}]*}' | head -1)"
        return 0
    else
        print_error "Failed to generate batch data for ${airport}: $response"
        return 1
    fi
}

# Generate historical data for an airport
generate_historical_data() {
    local airport=$1
    local days=$2
    print_info "Generating ${days} days of historical data for ${airport}..."
    
    local response
    response=$(curl -s -X POST "${BACKEND_URL}/api/admin/generators/historical/${airport}?days=${days}&samplesPerDay=24")
    
    if echo "$response" | grep -q '"success":true'; then
        local total_records=$(echo "$response" | grep -o '"totalRecords":[0-9]*' | cut -d: -f2)
        local duration=$(echo "$response" | grep -o '"durationMs":[0-9]*' | cut -d: -f2)
        print_success "Generated ${total_records:-0} historical records for ${airport} in ${duration:-0}ms"
        return 0
    else
        print_error "Failed to generate historical data for ${airport}: $response"
        return 1
    fi
}

# Sync vehicle->asset mappings and dependent tracking tables
sync_vehicle_assets() {
    local airport=$1
    print_info "Syncing vehicle-to-asset mappings for ${airport}..."

    local response
    response=$(curl -s -X POST "${BACKEND_URL}/api/admin/generators/sync-vehicle-assets/${airport}")

    if echo "$response" | grep -q '"success":true'; then
        local mappings_created
        mappings_created=$(echo "$response" | grep -o '"mappingsCreated":[0-9]*' | cut -d: -f2)
        print_success "Vehicle-asset sync completed for ${airport} (mappings: ${mappings_created:-0})"
        return 0
    else
        print_error "Failed to sync vehicle assets for ${airport}: $response"
        return 1
    fi
}

# Refresh materialized views
refresh_views() {
    print_info "Refreshing materialized views..."
    
    local response
    response=$(curl -s -X POST "${BACKEND_URL}/api/admin/generators/refresh-views")
    
    if echo "$response" | grep -q '"success":true'; then
        print_success "Materialized views refreshed"
        return 0
    else
        print_error "Failed to refresh views: $response"
        return 1
    fi
}

# Verify that movement trail data exists for every requested tenant.
# Fails hard if any tenant has zero rows.
verify_trail_counts() {
    local failed=0

    print_info "Verifying asset_movement_trail rows for requested tenants..."

    for airport in "$@"; do
        local count
        count=$(docker-compose -f docker-compose.dev.yml exec -T timescaledb \
            psql -U postgres -d utam -t -A \
            -c "SELECT COUNT(*) FROM asset_movement_trail WHERE tenant_code='${airport}';" \
            | tr -d '[:space:]')

        if [ -z "$count" ]; then
            print_error "Could not read trail count for ${airport}"
            failed=1
            continue
        fi

        if [ "$count" -gt 0 ]; then
            print_success "asset_movement_trail rows for ${airport}: ${count}"
        else
            print_error "asset_movement_trail rows for ${airport}: 0"
            failed=1
        fi
    done

    if [ "$failed" -ne 0 ]; then
        print_error "Post-check failed: one or more tenants have zero asset_movement_trail rows."
        return 1
    fi

    print_success "Post-check passed: all requested tenants have asset movement trail data."
    return 0
}

# Process a single airport
process_airport() {
    local airport=$1
    echo ""
    echo -e "${BLUE}Processing ${airport}...${NC}"
    echo "────────────────────────────────────────"
    
    # Clear data unless skipped
    if [ "$SKIP_CLEAR" = false ]; then
        clear_airport_data "$airport"
    fi
    
    # Generate data unless clear-only
    if [ "$CLEAR_ONLY" = false ]; then
        generate_batch_data "$airport" "$BATCH_SIZE"
        generate_historical_data "$airport" "$HISTORICAL_DAYS"
        sync_vehicle_assets "$airport"
    fi
}

# Main function
main() {
    local airports=()
    
    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --help)
                show_help
                exit 0
                ;;
            --clear-only)
                CLEAR_ONLY=true
                shift
                ;;
            --skip-clear)
                SKIP_CLEAR=true
                shift
                ;;
            --batch-size)
                BATCH_SIZE=$2
                shift 2
                ;;
            --days)
                HISTORICAL_DAYS=$2
                shift 2
                ;;
            *)
                airports+=("$1")
                shift
                ;;
        esac
    done
    
    # Use default airports if none specified
    if [ ${#airports[@]} -eq 0 ]; then
        airports=("${DEFAULT_AIRPORTS[@]}")
    fi
    
    print_header
    echo ""
    echo "Configuration:"
    echo "  Backend URL:     ${BACKEND_URL}"
    echo "  Airports:        ${airports[*]}"
    echo "  Batch Size:      ${BATCH_SIZE}"
    echo "  Historical Days: ${HISTORICAL_DAYS}"
    echo "  Clear Only:      ${CLEAR_ONLY}"
    echo "  Skip Clear:      ${SKIP_CLEAR}"
    echo ""
    
    # Check backend
    check_backend || exit 1
    
    # Process each airport
    for airport in "${airports[@]}"; do
        process_airport "$airport"
    done
    
    # Refresh materialized views after all data is generated
    if [ "$CLEAR_ONLY" = false ]; then
        echo ""
        verify_trail_counts "${airports[@]}"
        echo ""
        refresh_views
    fi
    
    echo ""
    echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${GREEN}  Data generation complete!${NC}"
    echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
}

# Run main function
main "$@"
