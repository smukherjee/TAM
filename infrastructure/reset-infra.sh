#!/bin/bash
# TAM Infrastructure Reset Script
# Merges cleanup_nifi.py, reset-cv-flow.sh, and force-reset-cv.sh
# Usage: ./reset-infra.sh [nifi|all] [--force]

set -e

NIFI_URL="http://localhost:8091/nifi-api"
TARGET=${1:-all}
FORCE=false

if [[ "$*" == *"--force"* ]]; then
    FORCE=true
fi

echo "🧨 Starting TAM Infrastructure Reset (Target: $TARGET, Force: $FORCE)..."

# -----------------------------------------------------------------------------
# NiFi Reset Logic
# -----------------------------------------------------------------------------

reset_nifi() {
    echo "🧨 [NiFi] Resetting Flows..."
    
    if ! curl -s "$NIFI_URL/flow/about" > /dev/null; then
        echo "   ⚠️ NiFi is not reachable. Cannot reset."
        return
    fi

    local ROOT_PG=$(curl -s "$NIFI_URL/flow/process-groups/root" | jq -r '.processGroupFlow.id')
    local PG_LIST=$(curl -s "$NIFI_URL/process-groups/$ROOT_PG/process-groups")
    local PGS=$(echo "$PG_LIST" | jq -r '.processGroups[].id')

    for PG in $PGS; do
        local NAME=$(curl -s "$NIFI_URL/process-groups/$PG" | jq -r '.component.name')
        echo "   Processing PG: $NAME ($PG)"
        
        # 1. Stop PG
        echo "      Stopping..."
        curl -s -X PUT "$NIFI_URL/flow/process-groups/$PG" \
            -H "Content-Type: application/json" \
            -d "{\"id\":\"$PG\",\"state\":\"STOPPED\"}" > /dev/null
        
        # Wait for stop
        sleep 2
        
        # 2. Delete Content (Force Mode)
        if [ "$FORCE" = true ]; then
            echo "      [Force] Deleting connections and processors individually..."
            
            # Delete Connections
            local CONNS=$(curl -s "$NIFI_URL/process-groups/$PG/connections" | jq -r '.connections[].id')
            for CONN in $CONNS; do
                local VER=$(curl -s "$NIFI_URL/connections/$CONN" | jq -r '.revision.version')
                curl -s -X DELETE "$NIFI_URL/connections/$CONN?version=$VER" > /dev/null
            done
            
            # Delete Processors
            local PROCS=$(curl -s "$NIFI_URL/process-groups/$PG/processors" | jq -r '.processors[].id')
            for PROC in $PROCS; do
                local VER=$(curl -s "$NIFI_URL/processors/$PROC" | jq -r '.revision.version')
                curl -s -X DELETE "$NIFI_URL/processors/$PROC?version=$VER" > /dev/null
            done
        fi

        # 3. Delete PG
        echo "      Deleting Process Group..."
        local VER=$(curl -s "$NIFI_URL/process-groups/$PG" | jq -r '.revision.version')
        curl -s -X DELETE "$NIFI_URL/process-groups/$PG?version=$VER" > /dev/null
        echo "      ✅ Deleted $NAME"
    done
    
    echo "   ✅ NiFi Reset Complete. Run ./infrastructure/nifi/setup-nifi.sh to restore."
}

# -----------------------------------------------------------------------------
# Main
# -----------------------------------------------------------------------------

if [ "$TARGET" == "nifi" ] || [ "$TARGET" == "all" ]; then
    reset_nifi
fi

echo "🎉 Reset Complete."
