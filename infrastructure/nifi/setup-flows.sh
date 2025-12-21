#!/bin/bash
# NiFi Flow Setup Script
# Creates HTTP ingestion flows for ADSB, Vehicle, and CV Event data

set -e

NIFI_URL="http://localhost:8091/nifi-api"

echo "🔧 Setting up NiFi flows..."

# Get root process group ID
ROOT_PG_ID=$(curl -s "$NIFI_URL/flow/process-groups/root" | jq -r '.processGroupFlow.id')
echo "📁 Root Process Group ID: $ROOT_PG_ID"

# 1. ADSB Ingestion
echo "📦 Creating ADSB Ingestion Process Group..."
ADSB_PG=$(curl -s -X POST "$NIFI_URL/process-groups/$ROOT_PG_ID/process-groups" \
  -H "Content-Type: application/json" \
  -d '{"revision":{"version":0},"component":{"name":"ADSB Ingestion","position":{"x":100,"y":100}}}' | jq -r '.id')
echo "   ADSB PG: $ADSB_PG"

echo "   Creating ListenHTTP..."
ADSB_HTTP=$(curl -s -X POST "$NIFI_URL/process-groups/$ADSB_PG/processors" \
  -H "Content-Type: application/json" \
  -d '{"revision":{"version":0},"component":{"type":"org.apache.nifi.processors.standard.ListenHTTP","name":"Listen ADSB HTTP","position":{"x":100,"y":100},"config":{"properties":{"Listening Port":"8092","Base Path":"adsb-ingest"}}}}' | jq -r '.id')

echo "   Creating PublishKafka..."
ADSB_KAFKA=$(curl -s -X POST "$NIFI_URL/process-groups/$ADSB_PG/processors" \
  -H "Content-Type: application/json" \
  -d '{"revision":{"version":0},"component":{"type":"org.apache.nifi.processors.kafka.pubsub.PublishKafka_2_6","name":"Publish to flight-raw-json","position":{"x":500,"y":100},"config":{"properties":{"bootstrap.servers":"redpanda:29092","topic":"flight-raw-json","acks":"1"},"autoTerminatedRelationships":["success","failure"]}}}' | jq -r '.id')

# 2. Vehicle Ingestion
echo "📦 Creating Vehicle Ingestion Process Group..."
VEHICLE_PG=$(curl -s -X POST "$NIFI_URL/process-groups/$ROOT_PG_ID/process-groups" \
  -H "Content-Type: application/json" \
  -d '{"revision":{"version":0},"component":{"name":"Vehicle Ingestion","position":{"x":100,"y":300}}}' | jq -r '.id')
echo "   Vehicle PG: $VEHICLE_PG"

echo "   Creating ListenHTTP..."
VEHICLE_HTTP=$(curl -s -X POST "$NIFI_URL/process-groups/$VEHICLE_PG/processors" \
  -H "Content-Type: application/json" \
  -d '{"revision":{"version":0},"component":{"type":"org.apache.nifi.processors.standard.ListenHTTP","name":"Listen Vehicle HTTP","position":{"x":100,"y":100},"config":{"properties":{"Listening Port":"8093","Base Path":"vehicle-ingest"}}}}' | jq -r '.id')

echo "   Creating PublishKafka..."
VEHICLE_KAFKA=$(curl -s -X POST "$NIFI_URL/process-groups/$VEHICLE_PG/processors" \
  -H "Content-Type: application/json" \
  -d '{"revision":{"version":0},"component":{"type":"org.apache.nifi.processors.kafka.pubsub.PublishKafka_2_6","name":"Publish to vehicle-raw-json","position":{"x":500,"y":100},"config":{"properties":{"bootstrap.servers":"redpanda:29092","topic":"vehicle-raw-json","acks":"1"},"autoTerminatedRelationships":["success","failure"]}}}' | jq -r '.id')

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
  -d '{"revision":{"version":0},"component":{"type":"org.apache.nifi.processors.kafka.pubsub.PublishKafka_2_6","name":"Publish to turnaround-raw-json","position":{"x":500,"y":100},"config":{"properties":{"bootstrap.servers":"kafka:29092","topic":"turnaround-raw-json","acks":"1"},"autoTerminatedRelationships":["success","failure"]}}}' | jq -r '.id')

echo ""
echo "✅ NiFi flows created successfully!"
echo "⚠️ Note: You may need to run 'fix_transactions.sh' to fix PublishKafka configuration."
