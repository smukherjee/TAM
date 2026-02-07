#!/bin/bash
# TAM Unified Superset Configuration Script
# Uses provision_superset.py for robust internal provisioning

set -e

# Configuration
SUPERSET_CONTAINER="tam-superset-1"
SUPERSET_URL="http://localhost:8089"
PROVISION_SCRIPT="$(dirname "$0")/provision_superset.py"

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

log() { echo -e "${BLUE}[INF]${NC} $1"; }
success() { echo -e "${GREEN}[OK]${NC} $1"; }
error() { echo -e "${RED}[ERR]${NC} $1"; }

# 1. Wait for healthy
log "Waiting for Superset to be healthy..."
MAX_RETRIES=90
COUNT=0
while ! curl -s "$SUPERSET_URL/health" | grep -q "OK" && ! curl -s "$SUPERSET_URL/health" | jq -r '.status' 2>/dev/null | grep -qi "healthy"; do
    COUNT=$((COUNT+1))
    if [ $COUNT -ge $MAX_RETRIES ]; then
        error "Superset health check timed out."
        exit 1
    fi
    sleep 3
done
success "Superset is healthy."

# 2. Run internal provisioner
log "Executing internal provisioner (provision_superset.py)..."

# Copy script to container
docker cp "$PROVISION_SCRIPT" "$SUPERSET_CONTAINER:/tmp/provision_superset.py"

# Execute inside container
if docker exec -u root "$SUPERSET_CONTAINER" python3 /tmp/provision_superset.py; then
    success "Superset provisioning successful! ✅"
else
    error "Superset provisioning failed."
    exit 1
fi

# Cleanup
docker exec -u root "$SUPERSET_CONTAINER" rm /tmp/provision_superset.py

log "🎉 Superset Configuration Complete!"
log "🌐 Visit: $SUPERSET_URL"
