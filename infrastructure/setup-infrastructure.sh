#!/bin/bash
# TAM Platform: Unified Infrastructure Setup Orchestrator
# One-click setup for the complete TAM ecosystem

set -e

# Configuration
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
INFRA_DIR="$PROJECT_ROOT/infrastructure"
DOCKER_COMPOSE_FILE="$PROJECT_ROOT/docker-compose.yml"

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

log() { echo -e "${BLUE}[INF]${NC} $1"; }
success() { echo -e "${GREEN}[OK]${NC} $1"; }
warn() { echo -e "${YELLOW}[WRN]${NC} $1"; }
error() { echo -e "${RED}[ERR]${NC} $1"; }

header() {
    echo -e "${BLUE}================================================"
    echo -e "   TAM PLATFORM INFRASTRUCTURE SETUP   "
    echo -e "================================================${NC}"
}

# 1. Prerequisites
check_prereqs() {
    log "Checking prerequisites..."
    local tools=("docker" "docker-compose" "curl" "jq" "psql")
    for tool in "${tools[@]}"; do
        if ! command -v "$tool" &> /dev/null; then
            error "$tool is required. Please install it."
            exit 1
        fi
    done
    success "Prerequisites satisfied."
}

# 2. Launch Services
launch_services() {
    log "Starting Docker containers..."
    docker-compose -f "$DOCKER_COMPOSE_FILE" up -d
    success "Containers initiated."
}

# 3. Wait for Core Services
wait_for_health() {
    log "Waiting for core services (DB, Kafka, MinIO)..."
    # Database
    while ! docker exec tam-timescaledb-1 pg_isready -U postgres &> /dev/null; do sleep 2; done
    log "✅ Database is ready."
    
    # Redpanda (Kafka)
    while ! docker exec tam-redpanda-1 rpk cluster health | grep -q "Healthy"; do sleep 2; done
    log "✅ Redpanda is healthy."
    
    # MinIO
    while ! curl -s http://localhost:9000/minio/health/live &> /dev/null; do sleep 2; done
    log "✅ MinIO is ready."
}

# 4. Orchestrate Setup Scripts
run_setup() {
    log "------------------------------------------------"
    log "Step 1: Database Initialization"
    bash "$INFRA_DIR/init-database.sh"
    
    log "------------------------------------------------"
    log "Step 2: NiFi Configuration"
    bash "$INFRA_DIR/nifi/configure-nifi.sh"
    
    log "------------------------------------------------"
    log "Step 3: Superset Provisioning"
    bash "$INFRA_DIR/superset/configure-superset.sh"
    
    log "------------------------------------------------"
    log "Step 4: Populating Master Data & Predictive Seeding"
    # This ensures all 28+ charts have data immediately
    docker exec -i tam-timescaledb-1 psql -U postgres -d utam < "$INFRA_DIR/db/populate_report_data.sql"
    success "Master data populated."

    log "------------------------------------------------"
    log "Step 5: Demo Simulation Data (Optional)"
    # We'll seed it by default for the first setup
    bash "$INFRA_DIR/seed-demo-data.sh" --tenant all --days 1 --sync
}

# -----------------------------------------------------------------------------
# Main Execution Flow
# -----------------------------------------------------------------------------
header
check_prereqs
launch_services
wait_for_health
run_setup

echo ""
success "================================================"
success "   TAM PLATFORM IS READY! 🚀   "
success "================================================"
echo -e "${BLUE}Services:${NC}"
echo -e "  - Frontend:   http://localhost:3000"
echo -e "  - Backend:    http://localhost:8080"
echo -e "  - NiFi:       http://localhost:8091/nifi"
echo -e "  - Superset:   http://localhost:8089"
echo -e "  - MinIO UI:   http://localhost:9001"
echo ""
warn "Next step: Run verification suite if available."
