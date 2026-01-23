#!/bin/bash
# TAM NiFi Setup Script
# Merges setup-flows.sh, setup-cv-flow.sh, start-flows.sh, and fix_transactions.sh
# Usage: ./setup-nifi.sh

set -e

NIFI_URL="http://localhost:8091/nifi-api"
REDPANDA_BROKERS="redpanda:29092"

echo "🔧 Starting TAM NiFi Setup..."

# -----------------------------------------------------------------------------
# Helper Functions
# -----------------------------------------------------------------------------

check_prereqs() {
    if ! command -v jq &> /dev/null; then
        echo "❌ Error: jq is required but not installed."
        exit 1
    fi
    if ! command -v curl &> /dev/null; then
        echo "❌ Error: curl is required but not installed."
        exit 1
    fi
}

get_root_pg() {
    curl -s "$NIFI_URL/flow/process-groups/root" | jq -r '.processGroupFlow.id'
}

get_pg_id_by_name() {
    local PARENT_ID=$1
    local NAME=$2
    curl -s "$NIFI_URL/process-groups/$PARENT_ID/process-groups" | \
        jq -r ".processGroups[] | select(.component.name == \"$NAME\") | .id"
}

create_http_context_map() {
    local PG_ID=$1
    
    # Check if exists
    local EXISTING=$(curl -s "$NIFI_URL/flow/process-groups/$PG_ID/controller-services" | \
        jq -r '.controllerServices[] | select(.component.type == "org.apache.nifi.http.StandardHttpContextMap") | .id')
    
    if [ -n "$EXISTING" ]; then
        echo "$EXISTING"
        return
    fi
    
    # Create
    local ID=$(curl -s -X POST "$NIFI_URL/process-groups/$PG_ID/controller-services" \
        -H "Content-Type: application/json" \
        -d '{
            "revision":{"version":0},
            "component":{
                "type":"org.apache.nifi.http.StandardHttpContextMap",
                "name":"HttpContextMap"
            }
        }' | jq -r '.id')
    
    # Enable it
    sleep 1
    local VER=$(curl -s "$NIFI_URL/controller-services/$ID" | jq -r '.revision.version')
    curl -s -X PUT "$NIFI_URL/controller-services/$ID/run-status" \
        -H "Content-Type: application/json" \
        -d "{\"revision\":{\"version\":$VER},\"state\":\"ENABLED\"}" > /dev/null
    
    echo "$ID"
}

create_process_group() {
    local PARENT_ID=$1
    local NAME=$2
    local X=$3
    local Y=$4
    
    # Check if exists
    local EXISTING_ID=$(get_pg_id_by_name "$PARENT_ID" "$NAME")
    if [ -n "$EXISTING_ID" ]; then
        echo "$EXISTING_ID"
        return
    fi

    # Create
    curl -s -X POST "$NIFI_URL/process-groups/$PARENT_ID/process-groups" \
        -H "Content-Type: application/json" \
        -d "{\"revision\":{\"version\":0},\"component\":{\"name\":\"$NAME\",\"position\":{\"x\":$X,\"y\":$Y}}}" | jq -r '.id'
}

create_listen_http() {
    local PG_ID=$1
    local NAME=$2
    local PORT=$3
    local BASE_PATH=$4
    local HTTP_CONTEXT_MAP_ID=$5
    
    # Check if exists
    local EXISTING=$(curl -s "$NIFI_URL/process-groups/$PG_ID/processors" | jq -r ".processors[] | select(.component.name == \"$NAME\") | .id")
    if [ -n "$EXISTING" ]; then
        # Update to use HttpContextMap
        local VER=$(curl -s "$NIFI_URL/processors/$EXISTING" | jq -r '.revision.version')
        curl -s -X PUT "$NIFI_URL/processors/$EXISTING" \
            -H "Content-Type: application/json" \
            -d "{
                \"revision\":{\"version\":$VER},
                \"component\":{
                    \"id\":\"$EXISTING\",
                    \"config\":{
                        \"properties\":{
                            \"Listening Port\":\"$PORT\",
                            \"Base Path\":\"$BASE_PATH\",
                            \"HTTP Context Map\":\"$HTTP_CONTEXT_MAP_ID\"
                        }
                    }
                }
            }" > /dev/null
        echo "$EXISTING"
        return
    fi

    curl -s -X POST "$NIFI_URL/process-groups/$PG_ID/processors" \
        -H "Content-Type: application/json" \
        -d "{
            \"revision\":{\"version\":0},
            \"component\":{
                \"type\":\"org.apache.nifi.processors.standard.ListenHTTP\",
                \"name\":\"$NAME\",
                \"position\":{\"x\":100,\"y\":100},
                \"config\":{
                    \"properties\":{
                        \"Listening Port\":\"$PORT\",
                        \"Base Path\":\"$BASE_PATH\",
                        \"HTTP Context Map\":\"$HTTP_CONTEXT_MAP_ID\"
                    }
                }
            }
        }" | jq -r '.id'
}

create_publish_kafka() {
    local PG_ID=$1
    local NAME=$2
    local TOPIC=$3
    
    # Check if exists
    local EXISTING=$(curl -s "$NIFI_URL/process-groups/$PG_ID/processors" | jq -r ".processors[] | select(.component.name == \"$NAME\") | .id")
    if [ -n "$EXISTING" ]; then
        # Reconcile config (topic / brokers / transactions) so reruns fix drift.
        local CURRENT=$(curl -s "$NIFI_URL/processors/$EXISTING")
        local VER=$(echo "$CURRENT" | jq -r '.revision.version')
        local CURRENT_TOPIC=$(echo "$CURRENT" | jq -r '.component.config.properties["topic"]')
        local CURRENT_BS=$(echo "$CURRENT" | jq -r '.component.config.properties["bootstrap.servers"]')
        local CURRENT_TX=$(echo "$CURRENT" | jq -r '.component.config.properties["use-transactions"]')

        if [ "$CURRENT_TOPIC" != "$TOPIC" ] || [ "$CURRENT_BS" != "$REDPANDA_BROKERS" ] || [ "$CURRENT_TX" != "false" ]; then
            echo "   🔁 Updating $NAME config (topic/brokers/transactions)"
            curl -s -X PUT "$NIFI_URL/processors/$EXISTING" \
                -H "Content-Type: application/json" \
                -d "{
                    \"revision\":{\"version\":$VER},
                    \"component\":{
                        \"id\":\"$EXISTING\",
                        \"config\":{
                            \"properties\":{
                                \"bootstrap.servers\":\"$REDPANDA_BROKERS\",
                                \"topic\":\"$TOPIC\",
                                \"use-transactions\":\"false\"
                            }
                        }
                    }
                }" > /dev/null
        fi

        echo "$EXISTING"
        return
    fi

    # Create with default config
    local ID=$(curl -s -X POST "$NIFI_URL/process-groups/$PG_ID/processors" \
        -H "Content-Type: application/json" \
        -d "{
            \"revision\":{\"version\":0},
            \"component\":{
                \"type\":\"org.apache.nifi.processors.kafka.pubsub.PublishKafka_2_6\",
                \"name\":\"$NAME\",
                \"position\":{\"x\":600,\"y\":100},
                \"config\":{
                    \"properties\":{
                        \"bootstrap.servers\":\"$REDPANDA_BROKERS\",
                        \"topic\":\"$TOPIC\",
                        \"acks\":\"1\",
                        \"use-transactions\":\"false\",
                        \"headers.X-Tenant-ID\":\"vidp\",
                        \"headers.X-Domain-ID\":\"tam\"
                    },
                    \"comments\":\"NOTE: Tenant should ideally come from event payload (tenant_code/icao_code), not hardcoded header. For multi-tenant support (VIDP/LIRN/YBBN), use separate processor groups or parameterize this value.\",
                    \"autoTerminatedRelationships\":[\"success\",\"failure\"]
                }
            }
        }" | jq -r '.id')
        
    echo "$ID"
}

connect_processors() {
    local PG_ID=$1
    local SOURCE_ID=$2
    local DEST_ID=$3
    
    # Check if connection exists
    local EXISTING=$(curl -s "$NIFI_URL/process-groups/$PG_ID/connections" | \
        jq -r ".connections[] | select(.sourceId == \"$SOURCE_ID\" and .destinationId == \"$DEST_ID\") | .id")
        
    if [ -n "$EXISTING" ]; then
        echo "   ✅ Connection already exists"
        return
    fi

    curl -s -X POST "$NIFI_URL/process-groups/$PG_ID/connections" \
        -H "Content-Type: application/json" \
        -d "{
            \"revision\":{\"version\":0},
            \"component\":{
                \"source\":{\"id\":\"$SOURCE_ID\",\"groupId\":\"$PG_ID\",\"type\":\"PROCESSOR\"},
                \"destination\":{\"id\":\"$DEST_ID\",\"groupId\":\"$PG_ID\",\"type\":\"PROCESSOR\"},
                \"selectedRelationships\":[\"success\"]
            }
        }" | jq -r '.id' > /dev/null
        
    echo "   ✅ Connected processors"
}

start_process_group() {
    local PG_ID=$1
    curl -s -X PUT "$NIFI_URL/flow/process-groups/$PG_ID" \
        -H "Content-Type: application/json" \
        -d "{\"id\":\"$PG_ID\",\"state\":\"RUNNING\"}" > /dev/null
    echo "   ▶️  Started Process Group"
}

# -----------------------------------------------------------------------------
# Main Execution
# -----------------------------------------------------------------------------

check_prereqs

ROOT_PG_ID=$(get_root_pg)
echo "📁 Root Process Group ID: $ROOT_PG_ID"

# Create HttpContextMap controller service (required for ListenHTTP)
echo "🔧 Creating HttpContextMap controller service..."
HTTP_CONTEXT_MAP_ID=$(create_http_context_map "$ROOT_PG_ID")
echo "   ✅ HttpContextMap ID: $HTTP_CONTEXT_MAP_ID"

# 1. ADSB Ingestion
echo "📦 Configuring ADSB Ingestion..."
ADSB_PG=$(create_process_group "$ROOT_PG_ID" "ADSB Ingestion" 100 100)
ADSB_HTTP=$(create_listen_http "$ADSB_PG" "Listen ADSB HTTP" "8092" "adsb-ingest" "$HTTP_CONTEXT_MAP_ID")
ADSB_KAFKA=$(create_publish_kafka "$ADSB_PG" "Publish to flight-raw-json" "flight-raw-json")
connect_processors "$ADSB_PG" "$ADSB_HTTP" "$ADSB_KAFKA"
start_process_group "$ADSB_PG"

# 2. Vehicle Ingestion
echo "📦 Configuring Vehicle Ingestion..."
VEHICLE_PG=$(create_process_group "$ROOT_PG_ID" "Vehicle Ingestion" 100 300)
VEHICLE_HTTP=$(create_listen_http "$VEHICLE_PG" "Listen Vehicle HTTP" "8093" "vehicle-ingest" "$HTTP_CONTEXT_MAP_ID")
VEHICLE_KAFKA=$(create_publish_kafka "$VEHICLE_PG" "Publish to vehicle-raw-json" "vehicle-raw-json")
connect_processors "$VEHICLE_PG" "$VEHICLE_HTTP" "$VEHICLE_KAFKA"
start_process_group "$VEHICLE_PG"

# 3. CV Event Ingestion
echo "📦 Configuring CV Event Ingestion..."
CV_PG=$(create_process_group "$ROOT_PG_ID" "CV Event Ingestion" 100 500)
CV_HTTP=$(create_listen_http "$CV_PG" "Listen CV HTTP" "8094" "cv-event-ingest" "$HTTP_CONTEXT_MAP_ID")
CV_KAFKA=$(create_publish_kafka "$CV_PG" "Publish to turnaround-raw-json" "turnaround-raw-json")
connect_processors "$CV_PG" "$CV_HTTP" "$CV_KAFKA"
start_process_group "$CV_PG"

echo ""
echo "🎉 NiFi Setup Complete!"
echo "🌐 UI: http://localhost:8091/nifi"
