#!/bin/bash
# NiFi Flow Connection and Start Script
# Connects processors and starts the flows

set -e

NIFI_URL="http://localhost:8091/nifi-api"

echo "🔗 Connecting NiFi processors and starting flows..."

# Get the ADSB and Vehicle process group IDs
ROOT_PG_ID=$(curl -s "$NIFI_URL/flow/process-groups/root" | jq -r '.processGroupFlow.id')
echo "📁 Root Process Group: $ROOT_PG_ID"

# Get child process groups
PG_LIST=$(curl -s "$NIFI_URL/process-groups/$ROOT_PG_ID/process-groups")

# Find ADSB and Vehicle PGs
ADSB_PG_ID=$(echo "$PG_LIST" | jq -r '.processGroups[] | select(.component.name == "ADSB Ingestion") | .id')
VEHICLE_PG_ID=$(echo "$PG_LIST" | jq -r '.processGroups[] | select(.component.name == "Vehicle Ingestion") | .id')
CV_PG_ID=$(echo "$PG_LIST" | jq -r '.processGroups[] | select(.component.name == "CV Event Ingestion") | .id')

echo "📦 ADSB Process Group: $ADSB_PG_ID"
echo "📦 Vehicle Process Group: $VEHICLE_PG_ID"
echo "📦 CV Event Process Group: $CV_PG_ID"

# Function to connect processors in a process group
connect_processors() {
    local PG_ID=$1
    local PG_NAME=$2
    
    echo "🔌 Connecting processors in $PG_NAME..."
    
    # Get processors in this PG
    PROCESSORS=$(curl -s "$NIFI_URL/process-groups/$PG_ID/processors")
    
    # Find source (ListenHTTP) and destination (PublishKafka)
    SOURCE_ID=$(echo "$PROCESSORS" | jq -r '.processors[] | select(.component.type | contains("ListenHTTP")) | .id')
    DEST_ID=$(echo "$PROCESSORS" | jq -r '.processors[] | select(.component.type | contains("PublishKafka")) | .id')
    
    if [ -z "$SOURCE_ID" ] || [ -z "$DEST_ID" ]; then
        echo "   ⚠️ Could not find processors to connect"
        return 1
    fi
    
    # Check if connection already exists
    CONNECTIONS=$(curl -s "$NIFI_URL/process-groups/$PG_ID/connections")
    EXISTING=$(echo "$CONNECTIONS" | jq -r '.connections[] | select(.sourceId == "'$SOURCE_ID'" and .destinationId == "'$DEST_ID'") | .id')
    
    if [ -n "$EXISTING" ]; then
         echo "   ✅ Connection already exists: $EXISTING"
         return 0
    fi
    
    echo "   Source (ListenHTTP): $SOURCE_ID"
    echo "   Destination (PublishKafka): $DEST_ID"
    
    # Create connection
    CONNECTION=$(curl -s -X POST "$NIFI_URL/process-groups/$PG_ID/connections" \
        -H "Content-Type: application/json" \
        -d "{
            \"revision\": {\"version\": 0},
            \"component\": {
                \"source\": {\"id\": \"$SOURCE_ID\", \"type\": \"PROCESSOR\", \"groupId\": \"$PG_ID\"},
                \"destination\": {\"id\": \"$DEST_ID\", \"type\": \"PROCESSOR\", \"groupId\": \"$PG_ID\"},
                \"selectedRelationships\": [\"success\"]
            }
        }")
    
    CONN_ID=$(echo "$CONNECTION" | jq -r '.id // empty')
    if [ -n "$CONN_ID" ]; then
        echo "   ✅ Created connection: $CONN_ID"
    else
        echo "   ⚠️ Failed to create connection"
    fi
}

# Function to start a process group
start_process_group() {
    local PG_ID=$1
    local PG_NAME=$2
    
    echo "▶️ Starting $PG_NAME..."
    
    curl -s -X PUT "$NIFI_URL/flow/process-groups/$PG_ID" \
        -H "Content-Type: application/json" \
        -d '{"id": "'$PG_ID'", "state": "RUNNING"}' > /dev/null
    
    echo "   ✅ Started"
}

# Connect and start ADSB PG
if [ -n "$ADSB_PG_ID" ]; then
    connect_processors "$ADSB_PG_ID" "ADSB Ingestion"
    start_process_group "$ADSB_PG_ID" "ADSB Ingestion"
fi

# Connect and start Vehicle PG
if [ -n "$VEHICLE_PG_ID" ]; then
    connect_processors "$VEHICLE_PG_ID" "Vehicle Ingestion"
    start_process_group "$VEHICLE_PG_ID" "Vehicle Ingestion"
fi

# Connect and start CV PG
if [ -n "$CV_PG_ID" ]; then
    connect_processors "$CV_PG_ID" "CV Event Ingestion"
    start_process_group "$CV_PG_ID" "CV Event Ingestion"
fi

echo ""
echo "✅ NiFi flows connected and started!"
echo ""
echo "🌐 NiFi UI: http://localhost:8091"
echo ""
echo "🧪 Test ADSB ingestion:"
echo "   curl -X POST http://localhost:8092/adsb-ingest -H 'Content-Type: application/json' -d '[{\"CallSign\":\"NIFI_TEST\"}]'"
echo ""
echo "🧪 Test Vehicle ingestion:"
echo "   curl -X POST http://localhost:8093/vehicle-ingest -H 'Content-Type: application/json' -d '[{\"vehicle_no\":\"NIFI_TEST\"}]'"
