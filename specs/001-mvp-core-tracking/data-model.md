# Data Model: Core MVP Tracking & Alerting

**Feature**: `001-mvp-core-tracking` | **Date**: 2025-12-12

## Message Broker (Kafka)

### Topic: `flight-raw-json`
- **Source**: Apache NiFi (Ingestion)
- **Consumer**: Spring Boot (Flight Service)
- **Format**: JSON
- **Schema**:
  ```json
  {
    "LivePlotId": "UUID",
    "Callsign": "String",
    "Latitude": "Double",
    "Longitude": "Double",
    "Speed": "Double",
    "Heading": "Double",
    "Altitude": "Double",
    "Status": "String",
    "TrackId": "String",
    "ModeSId": "String",
    "FlightLevel": "Double",
    "ROC": "Double",
    "SSR": "String",
    "SafetyAlert": "Boolean",
    "SystemStatus": "String",
    "Spi": "Boolean",
    "UpdateType": "String",
    "Time": "ISO8601 Timestamp"
  }
  ```

### Topic: `vehicle-raw-json`
- **Source**: Apache NiFi (Ingestion)
- **Consumer**: Spring Boot (Vehicle Service)
- **Format**: JSON
- **Schema**:
  ```json
  {
    "vehicle_no": "String",
    "vehicletype": "String",
    "latitude": "String (Double)",
    "longitude": "String (Double)",
    "speed": "String (Double)",
    "status": "String",
    "vehicle_name": "String",
    "company": "String",
    "location": "String",
    "gpsactualtime": "ISO8601 Timestamp",
    "ign": "String"
  }
  ```

### Topic: `turnaround-raw-json`
- **Source**: Apache NiFi (Ingestion)
- **Consumer**: Spring Boot (Turnaround Service)
- **Format**: JSON
- **Schema**:
  ```json
  {
    "eventUniqueId": "String",
    "cameraId": "String",
    "cameraName": "String",
    "activityType": "String",
    "eventType": "Integer (0=Start, 1=Stop)",
    "eventTimeStamp": "ISO8601 Timestamp",
    "stand": "String"
  }
  ```

### Topic: `alerts-json`
- **Source**: Spring Boot (Alert Service)
- **Consumer**: Spring Boot (Persistence), External Systems (Future)
- **Format**: JSON
- **Schema**:
  ```json
  {
    "alertId": "UUID",
    "type": "String (e.g., SPEED_VIOLATION)",
    "entityId": "String",
    "value": "Double",
    "timestamp": "ISO8601 Timestamp",
    "latitude": "Double",
    "longitude": "Double"
  }
  ```

## Database (PostgreSQL / TimescaleDB)

### Table: `flights` (Hypertable)
- **Partition Key**: `time`
- **Columns**:
  - `live_plot_id` (UUID)
  - `callsign` (VARCHAR)
  - `latitude` (DOUBLE PRECISION)
  - `longitude` (DOUBLE PRECISION)
  - `speed` (DOUBLE PRECISION)
  - `heading` (DOUBLE PRECISION)
  - `altitude` (DOUBLE PRECISION)
  - `status` (VARCHAR)
  - `track_id` (VARCHAR)
  - `mode_s_id` (VARCHAR)
  - `flight_level` (DOUBLE PRECISION)
  - `roc` (DOUBLE PRECISION)
  - `ssr` (VARCHAR)
  - `safety_alert` (BOOLEAN)
  - `system_status` (VARCHAR)
  - `spi` (BOOLEAN)
  - `update_type` (VARCHAR)
  - `time` (TIMESTAMPTZ)

### Table: `vehicles` (Hypertable)
- **Partition Key**: `gpsactualtime`
- **Columns**:
  - `vehicle_no` (VARCHAR)
  - `vehicletype` (VARCHAR)
  - `latitude` (DOUBLE PRECISION)
  - `longitude` (DOUBLE PRECISION)
  - `speed` (DOUBLE PRECISION)
  - `status` (VARCHAR)
  - `vehicle_name` (VARCHAR)
  - `company` (VARCHAR)
  - `location` (VARCHAR)
  - `gpsactualtime` (TIMESTAMPTZ)
  - `ign` (VARCHAR)

### Table: `alerts` (Hypertable)
- **Partition Key**: `timestamp`
- **Columns**:
  - `alert_id` (UUID)
  - `type` (VARCHAR)
  - `entity_id` (VARCHAR)
  - `value` (DOUBLE PRECISION)
  - `timestamp` (TIMESTAMPTZ)
  - `latitude` (DOUBLE PRECISION)
  - `longitude` (DOUBLE PRECISION)

### Table: `turnaround_events` (Hypertable)
- **Partition Key**: `event_time_stamp`
- **Columns**:
  - `event_unique_id` (VARCHAR)
  - `camera_id` (VARCHAR)
  - `camera_name` (VARCHAR)
  - `activity_type` (VARCHAR)
  - `event_type` (INTEGER)
  - `event_time_stamp` (TIMESTAMPTZ)
  - `stand` (VARCHAR)
