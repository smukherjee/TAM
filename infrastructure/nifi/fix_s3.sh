#!/bin/bash
# Fix S3 Termination

set -e
NIFI_URL="http://localhost:8091/nifi-api"

echo "🔧 Fixing S3 Termination..."

ROOT_PG=$(curl -s "$NIFI_URL/flow/process-groups/root" | jq -r '.processGroupFlow.id')
# Search recursively? Or just known PGs.
PG_LIST=$(curl -s "$NIFI_URL/process-groups/$ROOT_PG/process-groups")
PGS=$(echo "$PG_LIST" | jq -r '.processGroups[].id')

for PG in $PGS; do
    S3_PROCS=$(curl -s "$NIFI_URL/process-groups/$PG/processors" | jq -r '.processors[] | select(.component.type | contains("PutS3Object")) | .id')
    for PROC in $S3_PROCS; do
        echo "   Fixing Processor: $PROC"
        VER=$(curl -s $NIFI_URL/processors/$PROC | jq .revision.version)
        
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
            
        echo "   ✅ Updated termination."
        
        VER=$(curl -s $NIFI_URL/processors/$PROC | jq .revision.version)
        # Start
        curl -s -X PUT "$NIFI_URL/processors/$PROC/run-status" \
            -d "{\"revision\":{\"version\":$VER},\"state\":\"RUNNING\"}" \
            -H "Content-Type: application/json" > /dev/null
        echo "   ✅ Restarted."
    done
done
