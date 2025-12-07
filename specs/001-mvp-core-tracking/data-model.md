# Data Model: Core MVP Tracking

## Entities

### 1. Flight

Represents an aircraft tracked via ADS-B.

| Field | Type | Description | Constraints |
|-------|------|-------------|-------------|
| `livePlotId` | UUID | Unique identifier for the plot | PK |
| `callsign` | String | Flight callsign (e.g., "AI101") | Not Null |
| `latitude` | Double | Current latitude | -90 to 90 |
| `longitude` | Double | Current longitude | -180 to 180 |
| `speed` | Double | Ground speed in knots | >= 0 |
| `heading` | Double | Heading in degrees | 0-360 |
| `timestamp` | DateTime | Time of observation | UTC |
| `status` | String | Flight status (AIRBORNE, LANDED) | Enum |

### 2. Vehicle

Represents a ground vehicle tracked via TelIT.

| Field | Type | Description | Constraints |
|-------|------|-------------|-------------|
| `vehicleNo` | String | Unique vehicle number | PK |
| `type` | String | Vehicle type (BUS, TRUCK, CAR) | Not Null |
| `latitude` | Double | Current latitude | -90 to 90 |
| `longitude` | Double | Current longitude | -180 to 180 |
| `speed` | Double | Speed in km/h | >= 0 |
| `status` | String | Operational status (RUNNING, STOP) | Enum |
| `timestamp` | DateTime | Time of observation | UTC |

### 3. Alert

Represents a safety violation or event.

| Field | Type | Description | Constraints |
|-------|------|-------------|-------------|
| `alertId` | UUID | Unique alert identifier | PK |
| `type` | String | Alert type (SPEED_VIOLATION) | Enum |
| `entityId` | String | ID of the entity (VehicleNo) | Not Null |
| `value` | Double | Value that triggered alert (Speed) | Not Null |
| `timestamp` | DateTime | Time of alert generation | UTC |
| `location` | Point | Lat/Long of the event | |

## Database Schema (PostgreSQL/TimescaleDB)

```sql
-- Flights Hypertable
CREATE TABLE flights (
    time        TIMESTAMPTZ NOT NULL,
    live_plot_id UUID NOT NULL,
    callsign    TEXT NOT NULL,
    latitude    DOUBLE PRECISION,
    longitude   DOUBLE PRECISION,
    speed       DOUBLE PRECISION,
    heading     DOUBLE PRECISION,
    status      TEXT
);
SELECT create_hypertable('flights', 'time');

-- Vehicles Hypertable
CREATE TABLE vehicles (
    time        TIMESTAMPTZ NOT NULL,
    vehicle_no  TEXT NOT NULL,
    type        TEXT,
    latitude    DOUBLE PRECISION,
    longitude   DOUBLE PRECISION,
    speed       DOUBLE PRECISION,
    status      TEXT
);
SELECT create_hypertable('vehicles', 'time');

-- Alerts Table (Standard)
CREATE TABLE alerts (
    alert_id    UUID PRIMARY KEY,
    time        TIMESTAMPTZ NOT NULL,
    type        TEXT NOT NULL,
    entity_id   TEXT NOT NULL,
    value       DOUBLE PRECISION,
    latitude    DOUBLE PRECISION,
    longitude   DOUBLE PRECISION
);
```
