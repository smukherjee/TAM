#!/bin/bash

# Simulate Turnaround for EK500
FLIGHT="EK500"
ICAO="VIDP"
BASE_URL="http://localhost:8094/cv-event-ingest"

echo "Simulating Turnaround for $FLIGHT..."

# 1. Bridge Connect (Arrival)
echo "1. Bridge Connect..."
curl -X POST $BASE_URL -H 'Content-Type: application/json' -d "{
    \"flight_id\": \"$FLIGHT\",
    \"event_type\": \"bridge_connect\",
    \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\",
    \"icao_code\": \"$ICAO\"
}"
sleep 1

# 2. Baggage Unloading
echo "2. Baggage First..."
curl -X POST $BASE_URL -H 'Content-Type: application/json' -d "{
    \"flight_id\": \"$FLIGHT\",
    \"event_type\": \"baggage_first\",
    \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\",
    \"icao_code\": \"$ICAO\"
}"
sleep 1

# 3. Fueling
echo "3. Fuel Truck Connect..."
curl -X POST $BASE_URL -H 'Content-Type: application/json' -d "{
    \"flight_id\": \"$FLIGHT\",
    \"event_type\": \"fuel_truck_connect\",
    \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\",
    \"icao_code\": \"$ICAO\"
}"
sleep 1

# 4. Catering
echo "4. Catering Truck Connect..."
curl -X POST $BASE_URL -H 'Content-Type: application/json' -d "{
    \"flight_id\": \"$FLIGHT\",
    \"event_type\": \"catering_truck_connect\",
    \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\",
    \"icao_code\": \"$ICAO\"
}"
sleep 1

# 5. Cleaning
echo "5. Cleaner Enter..."
curl -X POST $BASE_URL -H 'Content-Type: application/json' -d "{
    \"flight_id\": \"$FLIGHT\",
    \"event_type\": \"cleaner_enter\",
    \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\",
    \"icao_code\": \"$ICAO\"
}"

echo "Done!"
