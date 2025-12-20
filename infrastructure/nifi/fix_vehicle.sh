#!/bin/bash
set -e
NIFI="http://localhost:8091/nifi-api"
PROC_ID="3c7c99b7-019b-1000-27ff-442e4557878a"

echo "🔧 Fixing Vehicle Processor $PROC_ID..."

# 1. Get current version
VER=$(curl -s $NIFI/processors/$PROC_ID | jq .revision.version)
echo "Current Version: $VER"

# 2. Stop Processor
echo "Stopping processor..."
curl -s -X PUT "$NIFI/processors/$PROC_ID/run-status" \
  -H "Content-Type: application/json" \
  -d "{\"revision\":{\"version\":$VER},\"state\":\"STOPPED\"}" > /dev/null

# 3. Get new version
VER=$(curl -s $NIFI/processors/$PROC_ID | jq .revision.version)
echo "New Version: $VER"

# 4. Apply Configuration Fix
echo "Applying config fix..."
cat <<EOF > vehicle_payload.json
{
  "revision": {
    "version": $VER
  },
  "component": {
    "id": "$PROC_ID",
    "config": {
      "properties": {
        "use.transactions": null,
        "use-transactions": "false"
      }
    }
  }
}
EOF

curl -s -X PUT "$NIFI/processors/$PROC_ID" \
  -H "Content-Type: application/json" \
  -d @vehicle_payload.json > /dev/null

# 5. Get new version
VER=$(curl -s $NIFI/processors/$PROC_ID | jq .revision.version)
echo "New Version: $VER"

# 6. Start Processor
echo "Starting processor..."
curl -s -X PUT "$NIFI/processors/$PROC_ID/run-status" \
  -H "Content-Type: application/json" \
  -d "{\"revision\":{\"version\":$VER},\"state\":\"RUNNING\"}" > /dev/null

echo "✅ Vehicle Processor Fixed & Restarted!"
