#!/bin/bash
# TAM Platform: Unified Verification Suite
# Validates health of all infrastructure components

set -e

# Configuration
NIFI_URL="http://localhost:8091/nifi-api"
SUPERSET_URL="http://localhost:8089"
BACKEND_URL="http://localhost:8080"
DB_CONTAINER="tam-timescaledb-1"
KAFKA_CONTAINER="tam-redpanda-1"

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

log() { echo -e "${BLUE}[INF]${NC} $1"; }
ok() { echo -e "  [${GREEN}PASS${NC}] $1"; }
fail() { echo -e "  [${RED}FAIL${NC}] $1"; ERROR_COUNT=$((ERROR_COUNT+1)); }

ERROR_COUNT=0

echo "🔍 Starting TAM Verification Suite..."
echo "========================================"

# 1. Docker Containers
log "Checking Container Status..."
containers=("tam-timescaledb-1" "tam-redpanda-1" "tam-minio-1" "tam-nifi-1" "tam-superset-1" "tam-backend-1")
for c in "${containers[@]}"; do
    if docker ps --format '{{.Names}}' | grep -q "^$c$"; then
        ok "Container $c is running."
    else
        fail "Container $c is NOT running."
    fi
done

# 2. Database Validation
log "Validating Database..."
if docker exec "$DB_CONTAINER" pg_isready -U postgres &> /dev/null; then
    ok "Database connection established."
    
    # Check core tables
    TABLE_COUNT=$(docker exec -i "$DB_CONTAINER" psql -U postgres -d utam -t -c "SELECT count(*) FROM pg_tables WHERE schemaname = 'public';")
    if [ "$TABLE_COUNT" -gt 20 ]; then
        ok "Database schema initialized ($TABLE_COUNT tables)."
    else
        fail "Database schema might be incomplete (only $TABLE_COUNT tables)."
    fi
    
    # Check tenants
    TENANTS=$(docker exec -i "$DB_CONTAINER" psql -U postgres -d utam -t -c "SELECT string_agg(code, ', ') FROM tenants;")
    ok "Tenants found: $TENANTS"
else
    fail "Database is not reachable."
fi

# 3. Kafka Validation
log "Validating Kafka Topics..."
REQUIRED_TOPICS=("flight-raw-json" "vehicle-raw-json" "asset-positions-json")
for t in "${REQUIRED_TOPICS[@]}"; do
    if docker exec "$KAFKA_CONTAINER" rpk topic list | grep -q "^$t"; then
        ok "Topic $t exists."
    else
        fail "Topic $t is MISSING."
    fi
done

# 4. NiFi Validation
log "Validating NiFi Flows..."
if curl -s "$NIFI_URL/flow/about" &> /dev/null; then
    ok "NiFi API is reachable."
    
    # Check process groups
    ROOT_ID=$(curl -s "$NIFI_URL/flow/process-groups/root" | jq -r '.processGroupFlow.id')
    PG_COUNT=$(curl -s "$NIFI_URL/process-groups/$ROOT_ID/process-groups" | jq -r '.processGroups | length')
    if [ "$PG_COUNT" -ge 3 ]; then
        ok "NiFi has $PG_COUNT process groups configured."
    else
        fail "NiFi has only $PG_COUNT process groups (expected at least 3)."
    fi
else
    fail "NiFi API is not reachable."
fi

# 5. Superset Validation
log "Validating Superset..."
if curl -s "$SUPERSET_URL/health" | grep -q "OK"; then
    ok "Superset API is reachable."
    
    # Check dashboards (requires auth, so we just check health for now)
    # A more advanced test would check /api/v1/dashboard/
    ok "Superset health status is OK."
else
    fail "Superset health check failed."
fi

# 6. Backend Validation
log "Validating Backend API..."
if curl -s "$BACKEND_URL/actuator/health" | grep -q "UP" || curl -s "$BACKEND_URL/health" &> /dev/null; then
    ok "Backend is reachable."
else
    fail "Backend is NOT reachable."
fi

echo "========================================"
if [ $ERROR_COUNT -eq 0 ]; then
    echo -e "${GREEN}Verification SUCCESSFUL! All components healthy. ✅${NC}"
else
    echo -e "${RED}Verification FAILED with $ERROR_COUNT errors. ❌${NC}"
    exit 1
fi
