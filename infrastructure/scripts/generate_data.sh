#!/bin/bash
# =============================================================================
# TAM Data Generation Script
# =============================================================================
# Generic script to generate simulation data for any airport tenant.
# Usage: ./generate_data.sh [ICAO_CODE] [OPTIONS]
#
# Examples:
#   ./generate_data.sh VIDP              # Generate data for Delhi
#   ./generate_data.sh LIRN              # Generate data for Naples
#   ./generate_data.sh YBBN              # Generate data for Brisbane
#   ./generate_data.sh all               # Generate data for all airports
#   ./generate_data.sh VIDP --clear      # Clear and regenerate data
#   ./generate_data.sh VIDP --historical # Generate historical data for analytics
# =============================================================================

set -e

# Configuration
BACKEND_URL="${BACKEND_URL:-http://localhost:8080}"
BATCH_SIZE="${BATCH_SIZE:-100}"
HISTORICAL_DAYS="${HISTORICAL_DAYS:-7}"
SAMPLES_PER_DAY="${SAMPLES_PER_DAY:-24}"

# Supported airports
SUPPORTED_AIRPORTS=("VIDP" "LIRN" "YBBN")

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Functions
print_header() {
    echo -e "${BLUE}=============================================${NC}"
    echo -e "${BLUE}  TAM Data Generation Script${NC}"
    echo -e "${BLUE}=============================================${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}→ $1${NC}"
}

usage() {
    echo "Usage: $0 <ICAO_CODE|all> [OPTIONS]"
    echo ""
    echo "ICAO Codes:"
    echo "  VIDP    - Indira Gandhi International Airport, Delhi"
    echo "  LIRN    - Naples International Airport"
    echo "  YBBN    - Brisbane Airport"
    echo "  all     - Generate data for all airports"
    echo ""
    echo "Options:"
    echo "  --clear         Clear existing data before generating"
    echo "  --historical    Generate historical data for analytics"
    echo "  --batch-size N  Number of records per batch (default: 100)"
    echo "  --days N        Days of historical data (default: 7)"
    echo "  --help          Show this help message"
    echo ""
    echo "Environment Variables:"
    echo "  BACKEND_URL     Backend API URL (default: http://localhost:8080)"
    echo "  BATCH_SIZE      Records per batch (default: 100)"
    echo "  HISTORICAL_DAYS Days of historical data (default: 7)"
    exit 1
}

check_backend() {
    print_info "Checking backend connectivity..."
    if curl -s -o /dev/null -w "%{http_code}" "${BACKEND_URL}/actuator/health" | grep -q "200"; then
        print_success "Backend is reachable at ${BACKEND_URL}"
        return 0
    else
        print_error "Backend is not reachable at ${BACKEND_URL}"
        return 1
    fi
}

validate_icao() {
    local icao="$1"
    for valid in "${SUPPORTED_AIRPORTS[@]}"; do
        if [ "$icao" = "$valid" ]; then
            return 0
        fi
    done
    return 1
}

clear_data() {
    local icao="$1"
    print_info "Clearing simulation data for ${icao}..."
    
    response=$(curl -s -X POST "${BACKEND_URL}/api/admin/generators/clear/${icao}")
    
    if echo "$response" | grep -q '"success":true'; then
        print_success "Cleared data for ${icao}"
    else
        print_error "Failed to clear data for ${icao}: $response"
        return 1
    fi
}

generate_batch_data() {
    local icao="$1"
    local batch_size="$2"
    
    print_info "Generating batch data for ${icao} (batch size: ${batch_size})..."
    
    response=$(curl -s -X POST "${BACKEND_URL}/api/admin/generators/batch/${icao}?batchSize=${batch_size}")
    
    if echo "$response" | grep -q '"success":true'; then
        # Extract and display counts
        total=$(echo "$response" | grep -o '"results":{[^}]*}' | grep -o '[0-9]*' | awk '{sum+=$1}END{print sum}')
        print_success "Generated batch data for ${icao}: ${total:-0} records"
    else
        print_error "Failed to generate batch data for ${icao}: $response"
        return 1
    fi
}

generate_historical_data() {
    local icao="$1"
    local days="$2"
    local samples="$3"
    
    print_info "Generating ${days} days of historical data for ${icao}..."
    
    response=$(curl -s -X POST "${BACKEND_URL}/api/admin/generators/historical/${icao}?days=${days}&samplesPerDay=${samples}")
    
    if echo "$response" | grep -q '"success":true'; then
        total=$(echo "$response" | grep -o '"totalRecords":[0-9]*' | grep -o '[0-9]*')
        duration=$(echo "$response" | grep -o '"durationMs":[0-9]*' | grep -o '[0-9]*')
        print_success "Generated historical data for ${icao}: ${total:-0} records in ${duration:-0}ms"
    else
        print_error "Failed to generate historical data for ${icao}: $response"
        return 1
    fi
}

refresh_views() {
    print_info "Refreshing materialized views..."
    
    response=$(curl -s -X POST "${BACKEND_URL}/api/admin/generators/refresh-views")
    
    if echo "$response" | grep -q '"success":true'; then
        print_success "Materialized views refreshed"
    else
        print_error "Failed to refresh views: $response"
        return 1
    fi
}

start_continuous() {
    local icao="$1"
    print_info "Starting continuous simulation for ${icao}..."
    
    response=$(curl -s -X POST "${BACKEND_URL}/api/simulation/${icao}/start")
    
    if echo "$response" | grep -q '"status":"STARTED"'; then
        print_success "Continuous simulation started for ${icao}"
    else
        print_error "Failed to start simulation for ${icao}: $response"
    fi
}

generate_for_tenant() {
    local icao="$1"
    local clear="$2"
    local historical="$3"
    
    echo ""
    echo -e "${BLUE}--- Processing ${icao} ---${NC}"
    
    # Optionally clear existing data
    if [ "$clear" = "true" ]; then
        clear_data "$icao" || true
    fi
    
    # Generate batch data
    generate_batch_data "$icao" "$BATCH_SIZE"
    
    # Generate historical data if requested
    if [ "$historical" = "true" ]; then
        generate_historical_data "$icao" "$HISTORICAL_DAYS" "$SAMPLES_PER_DAY"
    fi
}

# =============================================================================
# Main Script
# =============================================================================

print_header

# Parse arguments
ICAO_CODE=""
CLEAR_DATA="false"
GENERATE_HISTORICAL="false"

while [[ $# -gt 0 ]]; do
    case $1 in
        --clear)
            CLEAR_DATA="true"
            shift
            ;;
        --historical)
            GENERATE_HISTORICAL="true"
            shift
            ;;
        --batch-size)
            BATCH_SIZE="$2"
            shift 2
            ;;
        --days)
            HISTORICAL_DAYS="$2"
            shift 2
            ;;
        --help|-h)
            usage
            ;;
        *)
            if [ -z "$ICAO_CODE" ]; then
                ICAO_CODE=$(echo "$1" | tr '[:lower:]' '[:upper:]')
            fi
            shift
            ;;
    esac
done

# Validate ICAO code
if [ -z "$ICAO_CODE" ]; then
    print_error "ICAO code is required"
    usage
fi

# Check backend connectivity
check_backend || exit 1

# Process based on ICAO code
if [ "$ICAO_CODE" = "ALL" ]; then
    print_info "Generating data for all airports: ${SUPPORTED_AIRPORTS[*]}"
    
    for airport in "${SUPPORTED_AIRPORTS[@]}"; do
        generate_for_tenant "$airport" "$CLEAR_DATA" "$GENERATE_HISTORICAL"
    done
else
    # Validate single ICAO
    if validate_icao "$ICAO_CODE"; then
        generate_for_tenant "$ICAO_CODE" "$CLEAR_DATA" "$GENERATE_HISTORICAL"
    else
        print_error "Unknown ICAO code: ${ICAO_CODE}"
        echo "Supported airports: ${SUPPORTED_AIRPORTS[*]}"
        exit 1
    fi
fi

# Refresh materialized views
refresh_views || true

echo ""
print_success "Data generation complete!"
echo ""
echo "Next steps:"
echo "  - View data at http://localhost:3000"
echo "  - Start continuous simulation: ./generate_data.sh ${ICAO_CODE:-VIDP} --continuous"
echo "  - Access admin panel: http://localhost:3000/admin/generators"
