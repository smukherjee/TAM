# Data Model: Platform Phase 1

**Feature**: `003-nifi-ksqldb-integration` | **Date**: 2025-12-21

## Redis Schema

### Active Flights
- **Key Pattern**: `flight:{icao_code}:{callsign}`
- **Type**: String (JSON)
- **TTL**: 300 seconds (5 minutes)
- **Value**:
  ```json
  {
    "callsign": "AIC101",
    "latitude": 28.556,
    "longitude": 77.100,
    "speed": 150.5,
    "heading": 90.0,
    "altitude": 3000.0,
    "status": "AIRBORNE",
    "icao_code": "VIDP",
    "last_updated": "2025-12-21T10:00:00Z"
  }
  ```

### Active Vehicles
- **Key Pattern**: `vehicle:{icao_code}:{vehicle_no}`
- **Type**: String (JSON)
- **TTL**: 300 seconds
- **Value**:
  ```json
  {
    "vehicle_no": "V001",
    "type": "FUEL_TRUCK",
    "latitude": 28.555,
    "longitude": 77.101,
    "speed": 25.0,
    "status": "ACTIVE",
    "icao_code": "VIDP",
    "last_updated": "2025-12-21T10:00:00Z"
  }
  ```

## WebSocket API (STOMP)

### Topic: `/topic/flights/{icao_code}`
- **Direction**: Server -> Client
- **Trigger**: Kafka Consumer receives `flight-raw-json`.
- **Payload**: Array of Flight Objects (Delta or Full Snapshot).

### Topic: `/topic/vehicles/{icao_code}`
- **Direction**: Server -> Client
- **Trigger**: Kafka Consumer receives `vehicle-raw-json`.
- **Payload**: Array of Vehicle Objects.

### Topic: `/topic/alerts/{icao_code}`
- **Direction**: Server -> Client
- **Trigger**: Alert Service detects violation.
- **Payload**: Alert Object.

## Database Schema Updates (PostgreSQL)

### Table: `turnaround_events` (Fixed)
- `event_unique_id` (PK)
- `event_time_stamp` (PK, Time)
- `activity_type`
- `event_type` (0/1)
- `stand`
- `icao_code` (Partition Key candidate)

### Table: `alerts` (Fixed)
- `alert_id` (PK)
- `timestamp` (PK, Time)
- `type`
- `value`
- `icao_code`
