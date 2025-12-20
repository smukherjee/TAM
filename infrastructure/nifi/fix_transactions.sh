#!/bin/bash
# Fixes transaction configuration for all PublishKafka processors

set -e
NIFI="http://localhost:8091/nifi-api"

echo "🔧 Fixing transactions on all PublishKafka processors..."

# Get Root PG
ROOT_PG=$(curl -s "$NIFI/flow/process-groups/root" | jq -r '.processGroupFlow.id')

# Get All Process Groups
PGS=$(curl -s "$NIFI/process-groups/$ROOT_PG/process-groups" | jq -r '.processGroups[].id')

for PG in $PGS; do
    echo "Checking PG: $PG"
    
    # Get Processors
    PROCS=$(curl -s "$NIFI/process-groups/$PG/processors" | jq -r '.processors[] | select(.component.type | contains("PublishKafka")) | .id')
    
    for PROC in $PROCS; do
        echo "   Found PublishKafka: $PROC"
        
        # STOP if running
        VER=$(curl -s $NIFI/processors/$PROC | jq .revision.version)
        curl -s -X PUT "$NIFI/processors/$PROC/run-status" \
            -H "Content-Type: application/json" \
            -d "{\"revision\":{\"version\":$VER},\"state\":\"STOPPED\"}" > /dev/null
        
        # APPLY FIX
        VER=$(curl -s $NIFI/processors/$PROC | jq .revision.version)
        
        # Create payload file to avoid shell escaping issues
        cat <<EOF > fix_payload.json
{
  "revision": { "version": $VER },
  "component": {
    "id": "$PROC",
    "config": {
      "properties": {
        "use.transactions": null,
        "use-transactions": "false"
      }
    }
  }
}
EOF
        curl -s -X PUT "$NIFI/processors/$PROC" \
            -H "Content-Type: application/json" \
            -d @fix_payload.json > /dev/null
        
        echo "   ✅ Fixed configuration."
        
        # START
        VER=$(curl -s $NIFI/processors/$PROC | jq .revision.version)
        curl -s -X PUT "$NIFI/processors/$PROC/run-status" \
            -H "Content-Type: application/json" \
            -d "{\"revision\":{\"version\":$VER},\"state\":\"RUNNING\"}" > /dev/null
        
        echo "   ✅ Restarted."
    done
done

rm fix_payload.json 2>/dev/null
echo "🎉 All processors fixed!"
