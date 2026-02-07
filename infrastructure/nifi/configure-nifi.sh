#!/bin/bash
# TAM Unified NiFi Configuration Script
# Consolidates: setup-nifi.sh, setup-asset-tracking.sh, setup-datalake.sh, setup-monitoring.sh

set -e

# Configuration
NIFI_URL="http://localhost:8091/nifi-api"
REDPANDA_BROKERS="redpanda:29092"
KAFKA_CONTAINER="tam-redpanda-1"
MINIO_CONTAINER="tam-minio-1"

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

log() { echo -e "${BLUE}[INF]${NC} $1"; }
success() { echo -e "${GREEN}[OK]${NC} $1"; }
error() { echo -e "${RED}[ERR]${NC} $1"; }

# -----------------------------------------------------------------------------
# 1. Pre-flight Checks
# -----------------------------------------------------------------------------
check_services() {
    log "Checking if NiFi is ready..."
    MAX_RETRIES=60
    COUNT=0
    while ! curl -s "$NIFI_URL/flow/about" &> /dev/null; do
        COUNT=$((COUNT+1))
        if [ $COUNT -ge $MAX_RETRIES ]; then
            error "NiFi timed out."
            exit 1
        fi
        sleep 2
    done
    success "NiFi is up."
}

# -----------------------------------------------------------------------------
# 2. Helper Functions (NiFi REST API)
# -----------------------------------------------------------------------------
req() { curl -s "$@"; }

get_root_pg() {
    req "$NIFI_URL/flow/process-groups/root" | jq -r '.processGroupFlow.id'
}

get_pg_id_by_name() {
    local PARENT_ID=$1
    local NAME=$2
    req "$NIFI_URL/process-groups/$PARENT_ID/process-groups" | \
        jq -r ".processGroups[] | select(.component.name == \"$NAME\") | .id"
}

create_process_group() {
    local PARENT_ID=$1
    local NAME=$2
    local X=$3
    local Y=$4
    
    local EXISTING=$(get_pg_id_by_name "$PARENT_ID" "$NAME")
    if [ -n "$EXISTING" ]; then echo "$EXISTING"; return; fi

    req -X POST "$NIFI_URL/process-groups/$PARENT_ID/process-groups" \
        -H "Content-Type: application/json" \
        -d "{\"revision\":{\"version\":0},\"component\":{\"name\":\"$NAME\",\"position\":{\"x\":$X,\"y\":$Y}}}" | jq -r '.id'
}

# -----------------------------------------------------------------------------
# 3. Task Implementations
# -----------------------------------------------------------------------------

setup_kafka_topics() {
    log "Ensuring Kafka topics exist..."
    TOPICS=("flight-raw-json" "vehicle-raw-json" "turnaround-raw-json" "asset-positions-json")
    for t in "${TOPICS[@]}"; do
        if ! docker exec "$KAFKA_CONTAINER" rpk topic list | grep -q "^$t"; then
            log "Creating topic: $t"
            docker exec "$KAFKA_CONTAINER" rpk topic create "$t" --partitions 3
        fi
    done
    success "Kafka topics ready."
}

setup_minio_buckets() {
    log "Ensuring MinIO buckets exist..."
    # Using mc (minio client) inside container if available, or just use the API
    # Simpler: check if bucket exists, create if not
    # We'll assume the setup-datalake needs 'tam-raw-data'
    docker exec "$MINIO_CONTAINER" mkdir -p /data/tam-raw-data || true
    success "MinIO buckets ready."
}

configure_ingestion_flows() {
    log "Preparing Python environment..."
    VENV_DIR="$(dirname "$0")/.venv"
    
    if [ ! -d "$VENV_DIR" ]; then
        log "Creating virtual environment..."
        python3 -m venv "$VENV_DIR"
    fi
    
    source "$VENV_DIR/bin/activate"
    log "Installing dependencies..."
    pip install -q -r "$(dirname "$0")/requirements.txt"

    log "Running automated NiFi setup (setup_nifi_simple.py)..."
    # setup_nifi_simple.py now handles: ingestions, datalake/minio, and monitoring
    if python3 "$(dirname "$0")/setup_nifi_simple.py"; then
        success "NiFi foundations automated successfully."
    else
        error "Automated NiFi setup failed."
        deactivate
        exit 1
    fi
    deactivate
}

configure_asset_tracking() {
    log "Configuring Asset Tracking Flow..."
    # This one still has manual parts in the shell script, but we mark it clearly
    bash "$(dirname "$0")/scripts/setup-asset-tracking.sh"
}

# -----------------------------------------------------------------------------
# Main
# -----------------------------------------------------------------------------
main() {
    log "🚀 Starting Unified NiFi Configuration..."
    check_services
    setup_kafka_topics
    setup_minio_buckets
    configure_ingestion_flows
    configure_asset_tracking
    log "🎉 NiFi Configuration Sequence Complete!"
}

main "$@"
