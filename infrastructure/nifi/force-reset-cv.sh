#!/bin/bash
set -x
NIFI="http://localhost:8091/nifi-api"

echo "🧨 Force Resetting CV Event Ingestion Flow..."

ROOT_PG=$(curl -s "$NIFI/flow/process-groups/root" | jq -r '.processGroupFlow.id')
CV_PG=$(curl -s "$NIFI/process-groups/$ROOT_PG/process-groups" | jq -r '.processGroups[] | select(.component.name == "CV Event Ingestion") | .id')

if [ -n "$CV_PG" ]; then
    echo "Found PG: $CV_PG"
    
    # Stop PG
    curl -X PUT "$NIFI/flow/process-groups/$CV_PG" \
        -H "Content-Type: application/json" \
        -d '{"id":"'$CV_PG'","state":"STOPPED"}'
    sleep 3

    # Delete Connections inside PG
    CONN_IDS=$(curl -s "$NIFI/process-groups/$CV_PG/connections" | jq -r '.connections[].id')
    for CID in $CONN_IDS; do
        echo "   Deleting Connection $CID..."
        CVER=$(curl -s "$NIFI/connections/$CID" | jq -r '.revision.version')
        curl -X DELETE "$NIFI/connections/$CID?version=$CVER"
    done
    sleep 2

    # Delete Processors inside PG (Just in case)
    PROC_IDS=$(curl -s "$NIFI/process-groups/$CV_PG/processors" | jq -r '.processors[].id')
    for PID in $PROC_IDS; do
        echo "   Deleting Processor $PID..."
        PVER=$(curl -s "$NIFI/processors/$PID" | jq -r '.revision.version')
        curl -X DELETE "$NIFI/processors/$PID?version=$PVER"
    done
    sleep 2

    # Delete PG
    VER=$(curl -s "$NIFI/process-groups/$CV_PG" | jq -r '.revision.version')
    curl -X DELETE "$NIFI/process-groups/$CV_PG?version=$VER"
    
    echo "Deleted signal sent."
fi

sleep 5

# Check if gone
EXISTING=$(curl -s "$NIFI/process-groups/$ROOT_PG/process-groups" | jq -r '.processGroups[] | select(.component.name == "CV Event Ingestion") | .id')
if [ -n "$EXISTING" ]; then
    echo "❌ CRITICAL: Failed to delete PG: $EXISTING"
    # Try again with current version
    VER=$(curl -s "$NIFI/process-groups/$EXISTING" | jq -r '.revision.version')
    curl -X DELETE "$NIFI/process-groups/$EXISTING?version=$VER"
    sleep 2
    EXISTING2=$(curl -s "$NIFI/process-groups/$ROOT_PG/process-groups" | jq -r '.processGroups[] | select(.component.name == "CV Event Ingestion") | .id')
    if [ -n "$EXISTING2" ]; then
        echo "❌ Still exists. Giving up."
        exit 1
    fi
fi

echo "✅ PG Deleted. Creating Fresh..."
./infrastructure/nifi/setup-cv-flow.sh

echo "🔗 Starting..."
./infrastructure/nifi/start-flows.sh

echo "🎉 Done."
