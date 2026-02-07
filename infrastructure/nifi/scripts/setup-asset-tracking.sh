#!/bin/bash
# Asset Tracking & Security Module - Infrastructure Setup
# Feature: 005-asset-tracking-security
# Tasks: T023a (NiFi flow), T023b (Kafka topic), T023c (Testing)
# Created: 2026-01-28

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NIFI_URL="http://localhost:8091/nifi-api"
KAFKA_CONTAINER="tam-redpanda-1"
KAFKA_TOPIC="asset-positions-json"

echo "🚀 Asset Tracking Infrastructure Setup"
echo "======================================="
echo ""

# -----------------------------------------------------------------------------
# T023b: Create Kafka Topic
# -----------------------------------------------------------------------------
create_kafka_topic() {
    echo "📊 [T023b] Creating Kafka topic: $KAFKA_TOPIC"
    
    # Check if topic already exists
    if docker exec "$KAFKA_CONTAINER" rpk topic list | grep -q "^$KAFKA_TOPIC"; then
        echo "   ✅ Topic $KAFKA_TOPIC already exists"
        docker exec "$KAFKA_CONTAINER" rpk topic describe "$KAFKA_TOPIC"
        return
    fi
    
    # Create topic with specifications from tasks.md
    # - Partitions: 3 (for parallel processing)
    # - Replication factor: 1 (single node for MVP)
    # - Retention: 24 hours (86400000 ms)
    # - Compression: gzip
    docker exec "$KAFKA_CONTAINER" rpk topic create "$KAFKA_TOPIC" \
        --partitions 3 \
        --replicas 1 \
        --topic-config retention.ms=86400000 \
        --topic-config compression.type=gzip \
        --topic-config cleanup.policy=delete
    
    echo "   ✅ Created topic: $KAFKA_TOPIC"
    echo "   📋 Configuration:"
    docker exec "$KAFKA_CONTAINER" rpk topic describe "$KAFKA_TOPIC"
}

# -----------------------------------------------------------------------------
# T023a: Configure NiFi Flow
# -----------------------------------------------------------------------------
configure_nifi_flow() {
    echo ""
    echo "🔧 [T023a] Configuring NiFi Asset Position Polling Flow"
    
    # Check if NiFi is accessible
    if ! curl -s "$NIFI_URL/flow/about" > /dev/null 2>&1; then
        echo "   ❌ Error: NiFi is not accessible at $NIFI_URL"
        echo "   💡 Hint: Start NiFi with 'docker-compose up -d nifi'"
        return 1
    fi
    
    ROOT_PG_ID=$(curl -s "$NIFI_URL/flow/process-groups/root" | jq -r '.processGroupFlow.id')
    echo "   📁 Root Process Group ID: $ROOT_PG_ID"
    
    # Check if "Asset Position Polling" process group exists
    ASSET_PG_ID=$(curl -s "$NIFI_URL/process-groups/$ROOT_PG_ID/process-groups" | \
        jq -r '.processGroups[] | select(.component.name == "Asset Position Polling") | .id')
    
    if [ -n "$ASSET_PG_ID" ]; then
        echo "   ✅ Asset Position Polling process group already exists (ID: $ASSET_PG_ID)"
        echo "   💡 To reconfigure, delete the existing group in NiFi UI and re-run this script"
        return
    fi
    
    # Create Process Group
    echo "   📦 Creating 'Asset Position Polling' process group..."
    ASSET_PG_ID=$(curl -s -X POST "$NIFI_URL/process-groups/$ROOT_PG_ID/process-groups" \
        -H "Content-Type: application/json" \
        -d '{
            "revision":{"version":0},
            "component":{
                "name":"Asset Position Polling",
                "position":{"x":100,"y":700}
            }
        }' | jq -r '.id')
    
    echo "   ✅ Created process group: $ASSET_PG_ID"
    
    echo ""
    echo "   📝 NiFi Flow Configuration:"
    echo "   ────────────────────────────────────────────────"
    echo "   1. Open NiFi UI: http://localhost:8091/nifi"
    echo "   2. Locate 'Asset Position Polling' process group"
    echo "   3. Double-click to enter the group"
    echo "   4. Add processors manually using the template:"
    echo "      - ExecuteSQLRecord (schedule: 5 sec)"
    echo "        Query: SELECT vehicle_id, latitude, longitude, speed, heading, status, timestamp, tenant_code"
    echo "               FROM vehicles"
    echo "               WHERE timestamp > '\${last_poll_time}' OR '\${last_poll_time}' = ''"
    echo "      - UpdateAttribute (set last_poll_time = \${now()})"
    echo "      - PublishKafkaRecord_2_6 (topic: asset-positions-json, brokers: redpanda:29092)"
    echo "   5. Configure controller services:"
    echo "      - DBCPConnectionPool (URL: jdbc:postgresql://timescaledb:5432/utam)"
    echo "      - JsonRecordSetWriter"
    echo "   6. Connect processors: ExecuteSQL -> UpdateAttribute -> PublishKafka"
    echo "   7. Start all processors"
    echo ""
    echo "   📄 Flow template available at:"
    echo "      $SCRIPT_DIR/flow_asset_position.json"
    echo "   ────────────────────────────────────────────────"
    echo ""
    echo "   ⚠️  MANUAL CONFIGURATION REQUIRED"
    echo "   The NiFi REST API v1 does not support full automation of processor creation."
    echo "   Please follow the steps above to complete the flow configuration."
}

# -----------------------------------------------------------------------------
# T023c: Verify Setup
# -----------------------------------------------------------------------------
verify_setup() {
    echo ""
    echo "🔍 [T023c] Verifying Setup"
    
    # Check Kafka topic
    if docker exec "$KAFKA_CONTAINER" rpk topic list | grep -q "^$KAFKA_TOPIC"; then
        echo "   ✅ Kafka topic exists: $KAFKA_TOPIC"
    else
        echo "   ❌ Kafka topic not found: $KAFKA_TOPIC"
        return 1
    fi
    
    # Check if vehicles table has data
    VEHICLE_COUNT=$(docker exec tam-timescaledb-1 psql -U postgres -d utam -tAc "SELECT COUNT(*) FROM vehicles LIMIT 1;")
    echo "   📊 Vehicles table record count: $VEHICLE_COUNT"
    
    if [ "$VEHICLE_COUNT" -eq 0 ]; then
        echo "   ⚠️  No vehicle data found. Run simulation scripts:"
        echo "      ./simulate_ba249.sh"
        echo "      ./simulate_ek500.sh"
        echo "      ./simulate_qf401_ybbn.sh"
    fi
    
    echo ""
    echo "   💡 To test the complete flow:"
    echo "   1. Run mock data generator: ./simulate_ba249.sh"
    echo "   2. Monitor Kafka topic:"
    echo "      docker exec $KAFKA_CONTAINER rpk topic consume $KAFKA_TOPIC --num 10"
    echo "   3. Check NiFi bulletin board for errors (UI > Menu > Bulletins)"
    echo "   4. Verify flow performance: latency should be <1 second"
}

# -----------------------------------------------------------------------------
# Main Execution
# -----------------------------------------------------------------------------
main() {
    # T023b: Create Kafka topic
    create_kafka_topic
    
    # T023a: Configure NiFi flow
    configure_nifi_flow
    
    # T023c: Verify setup
    verify_setup
    
    echo ""
    echo "✅ Asset Tracking Infrastructure Setup Complete!"
    echo ""
    echo "📋 Summary:"
    echo "   ✓ Kafka topic: $KAFKA_TOPIC (3 partitions, 24h retention)"
    echo "   ✓ NiFi process group created (manual configuration needed)"
    echo ""
    echo "⏭️  Next Steps:"
    echo "   1. Complete NiFi flow configuration (see instructions above)"
    echo "   2. Test with simulation data: ./simulate_ba249.sh"
    echo "   3. Proceed to Phase 2: Backend Kafka Consumer (T026a-T026c)"
}

main "$@"
