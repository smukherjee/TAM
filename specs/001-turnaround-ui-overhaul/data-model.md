# Data Model: Turnaround UI Overhaul

## Entities

### TurnaroundSession
Represents a single flight's turnaround process from landing to takeoff.

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Primary Key |
| `icao_code` | String | Tenant Identifier (e.g., "VIDP") |
| `flight_id` | String | Flight Number (e.g., "UA123") |
| `stand_id` | String | Stand Identifier (e.g., "A1") |
| `sirt` | Timestamp | Scheduled In-Block Time |
| `eibt` | Timestamp | Estimated In-Block Time |
| `aibt` | Timestamp | Actual In-Block Time |
| `tobt` | Timestamp | Target Off-Block Time |
| `tsat` | Timestamp | Target Startup Approval Time |
| `aobt` | Timestamp | Actual Off-Block Time |
| `status` | Enum | `SCHEDULED`, `ON_BLOCK`, `OFF_BLOCK`, `DEPARTED` |
| `created_at` | Timestamp | Record creation time |
| `updated_at` | Timestamp | Last update time |

### TurnaroundTask
Represents a specific service task within a turnaround.

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Primary Key |
| `icao_code` | String | Tenant Identifier (e.g., "VIDP") |
| `session_id` | UUID | Foreign Key to `TurnaroundSession` |
| `task_type` | Enum | `FUELING`, `CATERING`, `BAGGAGE_UNLOAD`, `BAGGAGE_LOAD`, `CLEANING`, `BOARDING` |
| `status` | Enum | `PENDING`, `IN_PROGRESS`, `COMPLETED`, `DELAYED` |
| `planned_start` | Timestamp | Scheduled start time |
| `planned_end` | Timestamp | Scheduled end time |
| `actual_start` | Timestamp | Actual start time (from CV event) |
| `actual_end` | Timestamp | Actual end time (from CV event) |

### Alert
Represents a deviation, delay, or safety issue.

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Primary Key |
| `icao_code` | String | Tenant Identifier (e.g., "VIDP") |
| `session_id` | UUID | Foreign Key to `TurnaroundSession` |
| `severity` | Enum | `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` |
| `type` | Enum | `SOP_VIOLATION`, `SAFETY_INCIDENT`, `PROCESS_DELAY` |
| `message` | String | Human-readable description |
| `timestamp` | Timestamp | When the alert was generated |
| `is_active` | Boolean | Whether the alert is currently active |

## Database Schema (PostgreSQL)

```sql
CREATE TABLE turnaround_sessions (
    id UUID PRIMARY KEY,
    icao_code VARCHAR(4) NOT NULL,
    flight_id VARCHAR(20) NOT NULL,
    stand_id VARCHAR(10) NOT NULL,
    sirt TIMESTAMPTZ,
    eibt TIMESTAMPTZ,
    aibt TIMESTAMPTZ,
    tobt TIMESTAMPTZ,
    tsat TIMESTAMPTZ,
    aobt TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE turnaround_tasks (
    id UUID PRIMARY KEY,
    icao_code VARCHAR(4) NOT NULL,
    session_id UUID REFERENCES turnaround_sessions(id),
    task_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    planned_start TIMESTAMPTZ,
    planned_end TIMESTAMPTZ,
    actual_start TIMESTAMPTZ,
    actual_end TIMESTAMPTZ
);

CREATE TABLE alerts (
    id UUID PRIMARY KEY,
    icao_code VARCHAR(4) NOT NULL,
    session_id UUID REFERENCES turnaround_sessions(id),
    severity VARCHAR(20) NOT NULL,
    type VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    timestamp TIMESTAMPTZ DEFAULT NOW(),
    is_active BOOLEAN DEFAULT TRUE
);
```
