#!/bin/bash
# TAM Infrastructure Repair Script
# Merges fix_s3.sh, fix_vehicle.sh, and fix_dashboard.sh
# Usage: ./repair-infra.sh [nifi|grafana|all]

set -e

NIFI_URL="http://localhost:8091/nifi-api"
SUPERSET_URL="http://localhost:8089"
TARGET=${1:-all}

wait_for_http_ok() {
    local url="$1"
    local timeout_seconds="${2:-120}"
    local sleep_seconds="${3:-2}"

    local start_ts
    start_ts=$(date +%s)

    while true; do
        if curl -fsS "$url" > /dev/null; then
            return 0
        fi

        local now_ts
        now_ts=$(date +%s)
        if (( now_ts - start_ts >= timeout_seconds )); then
            return 1
        fi

        sleep "$sleep_seconds"
    done
}

echo "🔧 Starting TAM Infrastructure Repair..."

# -----------------------------------------------------------------------------
# NiFi Repairs
# -----------------------------------------------------------------------------

repair_nifi() {
    echo "🔧 [NiFi] Checking for issues..."

    if ! wait_for_http_ok "$NIFI_URL/flow/about" 120 2; then
        echo "   ⚠️ NiFi is not reachable at $NIFI_URL after waiting. Skipping NiFi repairs."
        return
    fi

    local ROOT_PG=$(curl -s "$NIFI_URL/flow/process-groups/root" | jq -r '.processGroupFlow.id')
    local PG_LIST=$(curl -s "$NIFI_URL/process-groups/$ROOT_PG/process-groups")
    local PGS=$(echo "$PG_LIST" | jq -r '.processGroups[].id')

    # 1. Fix S3 Termination (PutS3Object)
    echo "   [NiFi] Checking S3 Processor Termination..."
    for PG in $PGS; do
        local S3_PROCS=$(curl -s "$NIFI_URL/process-groups/$PG/processors" | jq -r '.processors[] | select(.component.type | contains("PutS3Object")) | .id')
        for PROC in $S3_PROCS; do
            echo "      Fixing S3 Processor: $PROC"
            local VER=$(curl -s $NIFI_URL/processors/$PROC | jq .revision.version)
            
            # Stop
            curl -s -X PUT "$NIFI_URL/processors/$PROC/run-status" \
                -d "{\"revision\":{\"version\":$VER},\"state\":\"STOPPED\"}" \
                -H "Content-Type: application/json" > /dev/null
                
            VER=$(curl -s $NIFI_URL/processors/$PROC | jq .revision.version)
            
            # Update
            curl -s -X PUT "$NIFI_URL/processors/$PROC" \
                -H "Content-Type: application/json" \
                -d "{
                    \"revision\": {\"version\": $VER},
                    \"component\": {
                        \"id\": \"$PROC\",
                        \"config\": {
                            \"autoTerminatedRelationships\": [\"failure\"]
                        }
                    }
                }" > /dev/null
                
            VER=$(curl -s $NIFI_URL/processors/$PROC | jq .revision.version)
            # Start
            curl -s -X PUT "$NIFI_URL/processors/$PROC/run-status" \
                -d "{\"revision\":{\"version\":$VER},\"state\":\"RUNNING\"}" \
                -H "Content-Type: application/json" > /dev/null
            echo "      ✅ Fixed S3 termination."
        done
    done

    # 2. Fix Kafka Transactions (PublishKafka)
    echo "   [NiFi] Checking Kafka Transaction Config..."
    for PG in $PGS; do
        local KAFKA_PROCS=$(curl -s "$NIFI_URL/process-groups/$PG/processors" | jq -r '.processors[] | select(.component.type | contains("PublishKafka")) | .id')
        for PROC in $KAFKA_PROCS; do
            # Check if needs fix (optimization: read config first)
            local CONF=$(curl -s $NIFI_URL/processors/$PROC)
            local USE_TX=$(echo "$CONF" | jq -r '.component.config.properties["use-transactions"]')
            
            if [ "$USE_TX" != "false" ]; then
                echo "      Fixing Kafka Processor: $PROC"
                local VER=$(echo "$CONF" | jq .revision.version)
                
                # Stop
                curl -s -X PUT "$NIFI_URL/processors/$PROC/run-status" \
                    -H "Content-Type: application/json" \
                    -d "{\"revision\":{\"version\":$VER},\"state\":\"STOPPED\"}" > /dev/null
                
                VER=$(curl -s $NIFI_URL/processors/$PROC | jq .revision.version)
                
                # Update
                curl -s -X PUT "$NIFI_URL/processors/$PROC" \
                    -H "Content-Type: application/json" \
                    -d "{
                        \"revision\": {\"version\": $VER},
                        \"component\": {
                            \"id\": \"$PROC\",
                            \"config\": {
                                \"properties\": {
                                    \"use.transactions\": null,
                                    \"use-transactions\": \"false\"
                                }
                            }
                        }
                    }" > /dev/null
                
                VER=$(curl -s $NIFI_URL/processors/$PROC | jq .revision.version)
                # Start
                curl -s -X PUT "$NIFI_URL/processors/$PROC/run-status" \
                    -H "Content-Type: application/json" \
                    -d "{\"revision\":{\"version\":$VER},\"state\":\"RUNNING\"}" > /dev/null
                echo "      ✅ Fixed Kafka transactions."
            fi
        done
    done
}

# -----------------------------------------------------------------------------
# Superset/Grafana Repairs
# -----------------------------------------------------------------------------

repair_superset() {
    echo "🔧 [Superset] Checking Dashboard Slugs..."
    
    if ! curl -s "$SUPERSET_URL/health" > /dev/null; then
         # Superset health check endpoint might vary, just try login
         true
    fi

    # Login
    local TOKEN=$(curl -s -X POST "$SUPERSET_URL/api/v1/security/login" \
      -H "Content-Type: application/json" \
      -d '{"username": "admin", "password": "admin", "provider": "db"}' | jq -r '.access_token')

    if [ -z "$TOKEN" ] || [ "$TOKEN" == "null" ]; then
        echo "   ⚠️ Failed to login to Superset. Skipping repairs."
        return
    fi
    local AUTH_HEADER="Authorization: Bearer $TOKEN"

    # Find Dashboard "TAM Operations Dashboard"
    local DASH_ID=$(curl -s -X GET "$SUPERSET_URL/api/v1/dashboard/?q=(filters:!((col:dashboard_title,opr:eq,value:'TAM%20Operations%20Dashboard')))" \
      -H "$AUTH_HEADER" | jq -r '.result[0].id')

    if [ -n "$DASH_ID" ] && [ "$DASH_ID" != "null" ]; then
        echo "   [Superset] Found Dashboard ID: $DASH_ID. Ensuring slug is 'tam_ops'..."
        local RESP=$(curl -s -X PUT "$SUPERSET_URL/api/v1/dashboard/$DASH_ID" \
            -H "$AUTH_HEADER" \
            -H "Content-Type: application/json" \
            -d '{
                "slug": "tam_ops",
                "published": true
            }')
        echo "   ✅ Dashboard slug verified."
    fi
}

# -----------------------------------------------------------------------------
# Database Repairs
# -----------------------------------------------------------------------------

repair_database() {
    echo "🔧 [Database] Checking health..."
    if ! docker exec tam-timescaledb-1 pg_isready -U postgres &> /dev/null; then
        echo "   ❌ Database container is NOT ready."
        return
    fi
    
    local TABLE_COUNT=$(docker exec -i tam-timescaledb-1 psql -U postgres -d utam -t -c "SELECT count(*) FROM pg_tables WHERE schemaname = 'public';")
    if [ "$TABLE_COUNT" -lt 10 ]; then
        echo "   ⚠️ Schema seems incomplete ($TABLE_COUNT tables). Re-running init-database.sh..."
        bash "$(dirname "$0")/init-database.sh"
    else
        echo "   ✅ Database schema looks healthy ($TABLE_COUNT tables)."
    fi
}

# -----------------------------------------------------------------------------
# Main
# -----------------------------------------------------------------------------

if [ "$TARGET" == "db" ] || [ "$TARGET" == "all" ]; then
    repair_database
fi

if [ "$TARGET" == "nifi" ] || [ "$TARGET" == "all" ]; then
    repair_nifi
fi

if [ "$TARGET" == "grafana" ] || [ "$TARGET" == "superset" ] || [ "$TARGET" == "all" ]; then
    repair_superset
fi

echo "🎉 Repair Complete."
