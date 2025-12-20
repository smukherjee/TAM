#!/bin/bash
# CV Flow Setup Script

set -e
NIFI_URL="http://localhost:8091/nifi-api"

echo "🔧 Setting up CV NiFi flows..."

# Get root process group ID
ROOT_PG_ID=$(curl -s "$NIFI_URL/flow/process-groups/root" | jq -r '.processGroupFlow.id')

# Check if already exists
EXISTING_CV=$(curl -s "$NIFI_URL/process-groups/$ROOT_PG_ID/process-groups" | jq -r '.processGroups[] | select(.component.name == "CV Event Ingestion") | .id')

if [ -n "$EXISTING_CV" ]; then
    echo "✅ CV Event Ingestion already exists: $EXISTING_CV"
    exit 0
fi

# 3. CV Event Ingestion
echo "📦 Creating CV Event Ingestion Process Group..."
CV_PG=$(curl -s -X POST "$NIFI_URL/process-groups/$ROOT_PG_ID/process-groups" \
  -H "Content-Type: application/json" \
  -d '{"revision":{"version":0},"component":{"name":"CV Event Ingestion","position":{"x":100,"y":500}}}' | jq -r '.id')
echo "   CV Event PG: $CV_PG"

echo "   Creating ListenHTTP..."
CV_HTTP=$(curl -s -X POST "$NIFI_URL/process-groups/$CV_PG/processors" \
  -H "Content-Type: application/json" \
  -d '{"revision":{"version":0},"component":{"type":"org.apache.nifi.processors.standard.ListenHTTP","name":"Listen CV HTTP","position":{"x":100,"y":100},"config":{"properties":{"Listening Port":"8094","Base Path":"cv-event-ingest"}}}}' | jq -r '.id')

echo "   Creating PublishKafka..."
CV_KAFKA=$(curl -s -X POST "$NIFI_URL/process-groups/$CV_PG/processors" \
  -H "Content-Type: application/json" \
  -d '{"revision":{"version":0},"component":{"type":"org.apache.nifi.processors.kafka.pubsub.PublishKafka_2_6","name":"Publish to turnaround-raw-json","position":{"x":500,"y":100},"config":{"properties":{"bootstrap.servers":"redpanda:29092","topic":"turnaround-raw-json","acks":"1"}}}}' | jq -r '.id')

echo ""
echo "✅ CV NiFi flow created!"
