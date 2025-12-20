#!/bin/bash
set -e
NIFI="http://localhost:8091/nifi-api"

echo "🧨 Resetting CV Event Ingestion Flow..."

# 1. Find PG ID
ROOT_PG=$(curl -s "$NIFI/flow/process-groups/root" | jq -r '.processGroupFlow.id')
CV_PG=$(curl -s "$NIFI/process-groups/$ROOT_PG/process-groups" | jq -r '.processGroups[] | select(.component.name == "CV Event Ingestion") | .id')

if [ -n "$CV_PG" ]; then
    echo "   Found existing PG: $CV_PG. Stopping..."
    
    # Stop PG
    curl -s -X PUT "$NIFI/flow/process-groups/$CV_PG" \
        -H "Content-Type: application/json" \
        -d '{"id":"'$CV_PG'","state":"STOPPED"}' > /dev/null
    
    # Wait for stop
    sleep 3
    
    # Purge queues? (Hard to do via simple API, deleting PG purges it)
    
    # Disconnect input/output ports if any? (we only have processors)
    
    echo "   Deleting PG..."
    # Get version for delete
    VER=$(curl -s "$NIFI/process-groups/$CV_PG" | jq -r '.revision.version')
    curl -s -X DELETE "$NIFI/process-groups/$CV_PG?version=$VER" > /dev/null
    echo "   ✅ Deleted."
else
    echo "   No existing CV PG found."
fi

echo "🔄 Recreating..."
./infrastructure/nifi/setup-cv-flow.sh

echo "🔧 Fixing Transactions..."
./infrastructure/nifi/fix_transactions.sh

echo "🔗 Connecting and Starting..."
./infrastructure/nifi/start-flows.sh

echo "🎉 Done. CV Flow Reset."
