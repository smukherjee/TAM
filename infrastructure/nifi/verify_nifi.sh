#!/bin/bash
echo "🧪 Sending test data to NiFi..."

# Send ADSB Data
curl -s -X POST http://localhost:8092/adsb-ingest \
  -H "Content-Type: application/json" \
  -d '[{
    "LivePlotId": "nifi-verify-1",
    "CallSign": "NIFI-VERIFY",
    "Lat": 28.5,
    "Lon": 77.1,
    "Speed": 400,
    "Altitude": 30000,
    "Status": "AIRBORNE",
    "Time": "2025-12-20T16:00:00Z"
  }]'
echo "   Sent ADSB data."

# Send Vehicle Data
curl -s -X POST http://localhost:8093/vehicle-ingest \
  -H "Content-Type: application/json" \
  -d '[{
    "vehicle_no": "NIFI-VEH-1",
    "vehicletype": "BUS",
    "latitude": "28.55",
    "longitude": "77.10",
    "speed": "50",
    "status": "RUNNING"
  }]'
echo "   Sent Vehicle data."

echo "⏳ Waiting for processing..."
sleep 2

echo "🔎 Checking Kafka Topics..."

# Check ADSB
echo "--- flight-raw-json ---"
docker exec tam-redpanda rpk topic consume flight-raw-json --num 20 --offset -20 2>/dev/null | grep "NIFI-VERIFY" || echo "❌ Not Found"

# Check Vehicle
echo "--- vehicle-raw-json ---"
docker exec tam-redpanda rpk topic consume vehicle-raw-json --num 20 --offset -20 2>/dev/null | grep "NIFI-VEH-1" || echo "❌ Not Found"

echo "🧪 Sending CV Event data..."
curl -s -X POST http://localhost:8094/cv-event-ingest \
  -H "Content-Type: application/json" \
  -d '[{
    "eventUniqueId": "NIFI-CV-1",
    "activityType": "NIFI_TEST_ACTIVITY",
    "eventType": 0,
    "eventTimeStamp": "2025-12-20T16:00:00",
    "stand": "A1"
  }]'

echo "--- turnaround-raw-json ---"
docker exec tam-redpanda rpk topic consume turnaround-raw-json --num 5 --offset -5 2>/dev/null | grep "NIFI_TEST_ACTIVITY" || echo "❌ Not Found"
