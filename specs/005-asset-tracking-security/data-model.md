# Data Model: Asset Tracking & Security Module

**Feature**: 005  
**Created**: 2026-01-28  
**Status**: Complete ✅

---

## Entity Relationship Diagram

```
┌─────────────────────────┐
│      assets             │
│─────────────────────────│
│ PK id (uuid)            │
│    qr_id (varchar)      │◄────────┐
│    name                 │         │
│    category             │         │
│    status               │         │
│    tenant_code          │         │
└─────────────────────────┘         │
                                    │ FK asset_id (via qr_id match)
┌─────────────────────────┐         │
│      vehicles           │         │
│─────────────────────────│         │
│ PK vehicle_id (varchar) │─────────┤
│    latitude             │         │
│    longitude            │         │
│    speed                │         │
│    heading              │         │
│    status               │         │
│    updated_at           │         │
└─────────────────────────┘         │
         │                          │
         │ Polled by NiFi           │
         │ every 5 seconds          │
         ↓                          │
┌─────────────────────────┐         │
│  Kafka Topic:           │         │
│  asset-positions-json   │         │
└─────────────────────────┘         │
         │                          │
         │ Consumed by              │
         ↓                          │
┌──────────────────────────────────────┐
│  asset_movement_trail (Hypertable)   │
│──────────────────────────────────────│
│ PK id (bigserial)                    │
│ FK asset_id (uuid)                   │───┐
│    asset_identifier (varchar)        │   │
│    vehicle_id (varchar)              │   │
│    location (PostGIS POINT)          │   │
│    speed, heading, altitude          │   │
│    current_zone_name                 │   │
│ FK restricted_zone_id (uuid, null)   │───┼────┐
│    status                            │   │    │
│    timestamp (timestamptz)           │   │    │
│    metadata (jsonb)                  │   │    │
│    tenant_code                       │   │    │
└──────────────────────────────────────┘   │    │
                                           │    │
┌──────────────────────────────────────┐   │    │
│  asset_location_register             │   │    │
│  (Current state snapshot)            │   │    │
│──────────────────────────────────────│   │    │
│ PK asset_id (uuid)                   │◄──┘    │
│    asset_identifier                  │        │
│    current_location (PostGIS POINT)  │        │
│    current_zone_name                 │        │
│ FK restricted_zone_id (uuid, null)   │────┐   │
│    speed, heading, status            │    │   │
│    last_updated                      │    │   │
│    tenant_code                       │    │   │
└──────────────────────────────────────┘    │   │
                                            │   │
┌─────────────────────────────────────────────┐ │
│  zone_violations (Hypertable)               │ │
│─────────────────────────────────────────────│ │
│ PK id (uuid)                                │ │
│    violation_id (varchar, unique)           │ │
│ FK asset_id (uuid)                          │─┤
│    asset_identifier, asset_name             │ │
│    asset_category                           │ │
│ FK restricted_zone_id (uuid)                │◄┘
│    zone_name, zone_type                     │
│    violation_type (enum)                    │
│    entry_location (PostGIS POINT)           │
│    duration_seconds                         │
│    severity (enum)                          │
│    acknowledged (boolean)                   │
│    acknowledged_by, acknowledged_at         │
│    resolution_notes (text)                  │
│    timestamp (timestamptz)                  │
│    tenant_code                              │
└─────────────────────────────────────────────┘
         │
         │ Aggregated by
         ↓
┌─────────────────────────────────────────────┐
│  zone_violations_hourly                     │
│  (Continuous Aggregate)                     │
│─────────────────────────────────────────────│
│    hour (timestamptz)                       │
│    tenant_code                              │
│    zone_id, zone_name, zone_type            │
│    violation_count                          │
│    critical_count, high_count, etc.         │
│    unique_assets                            │
│    avg_duration                             │
└─────────────────────────────────────────────┘

┌─────────────────────────────────────────────┐
│  movement_discrepancies (Hypertable)        │
│─────────────────────────────────────────────│
│ PK id (uuid)                                │
│    discrepancy_id (varchar, unique)         │
│ FK asset_id (uuid)                          │
│    asset_identifier, asset_name             │
│    asset_category                           │
│    discrepancy_type (enum)                  │
│    expected_location (PostGIS POINT, null)  │
│    actual_location (PostGIS POINT)          │
│    deviation_meters (numeric)               │
│    expected_speed, actual_speed             │
│    description (text)                       │
│    severity (enum)                          │
│    acknowledged (boolean)                   │
│    acknowledged_by, acknowledged_at         │
│    investigation_notes (text)               │
│    timestamp (timestamptz)                  │
│    tenant_code                              │
└─────────────────────────────────────────────┘
         │
         │ Aggregated by
         ↓
┌─────────────────────────────────────────────┐
│  movement_discrepancies_daily               │
│  (Continuous Aggregate)                     │
│─────────────────────────────────────────────│
│    day (date)                               │
│    tenant_code                              │
│    discrepancy_type                         │
│    discrepancy_count                        │
│    critical_count, high_count, etc.         │
│    unique_assets                            │
│    avg_deviation_meters                     │
└─────────────────────────────────────────────┘

┌─────────────────────────────────────────────┐
│  restricted_zones                           │
│─────────────────────────────────────────────│
│ PK id (uuid)                                │
│    name (varchar)                           │
│    zone_type (enum)                         │
│    geometry (PostGIS POLYGON)               │
│    authorized_asset_categories (text[])     │
│    authorized_asset_ids (uuid[])            │
│    is_active (boolean)                      │
│    created_at, updated_at                   │
│    created_by, updated_by                   │
│    tenant_code                              │
└─────────────────────────────────────────────┘

┌─────────────────────────────────────────────┐
│  asset_activity_heatmap                     │
│  (Materialized View)                        │
│─────────────────────────────────────────────│
│    grid_location (PostGIS POINT)            │
│    tenant_code                              │
│    time_bucket (timestamptz)                │
│    activity_count                           │
│    unique_assets                            │
│    avg_speed                                │
│    restricted_zone_visits                   │
└─────────────────────────────────────────────┘

┌─────────────────────────────────────────────┐
│  violation_heatmap                          │
│  (Materialized View)                        │
│─────────────────────────────────────────────│
│    grid_location (PostGIS POINT)            │
│    tenant_code                              │
│    time_bucket (timestamptz)                │
│    violation_count                          │
│    unique_violators                         │
│    critical_count, high_count               │
└─────────────────────────────────────────────┘
```

---

## Table Schemas

### 1. restricted_zones

**Purpose**: Define restricted areas with authorization rules

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | uuid | PK, DEFAULT gen_random_uuid() | Unique zone identifier |
| name | varchar(255) | NOT NULL | Zone display name |
| zone_type | zone_type_enum | NOT NULL | PROHIBITED, RESTRICTED, CONTROLLED, MAINTENANCE |
| geometry | geometry(POLYGON, 4326) | NOT NULL | WGS84 polygon boundary |
| authorized_asset_categories | text[] | NULL | Allowed asset categories (NULL = no restrictions) |
| authorized_asset_ids | uuid[] | NULL | Specific asset exceptions |
| is_active | boolean | DEFAULT true | Enable/disable zone detection |
| created_at | timestamptz | DEFAULT now() | Creation timestamp |
| updated_at | timestamptz | DEFAULT now() | Last modification timestamp |
| created_by | varchar(255) | NULL | User who created zone |
| updated_by | varchar(255) | NULL | User who last modified zone |
| tenant_code | varchar(10) | NOT NULL | Tenant isolation |

**Indexes**:
- `idx_restricted_zones_geometry` (GIST on geometry)
- `idx_restricted_zones_tenant_active` (tenant_code, is_active)
- `idx_restricted_zones_type` (zone_type)

**Sample Data**:
```sql
-- VIDP Runway Safety Zone
('Runway 09/27 Safety Zone', 'PROHIBITED', ST_GeomFromText('POLYGON((...))', 4326), 
 NULL, NULL, true, 'VIDP')

-- LIRN Fuel Storage Area
('Fuel Storage Area', 'RESTRICTED', ST_GeomFromText('POLYGON((...))', 4326),
 '{Fueling}', NULL, true, 'LIRN')
```

---

### 2. asset_movement_trail (Hypertable)

**Purpose**: Time-series record of all asset positions

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | bigserial | PK | Auto-incrementing ID |
| asset_id | uuid | NULL, FK → assets.id | Linked asset (NULL if unmapped) |
| asset_identifier | varchar(50) | NOT NULL | Asset identifier (e.g., SAM-0005) |
| vehicle_id | varchar(50) | NOT NULL | Source vehicle ID |
| location | geometry(POINT, 4326) | NOT NULL | WGS84 lat/lng |
| speed | numeric(5,2) | NULL | km/h |
| heading | numeric(5,2) | NULL | Degrees (0-360) |
| altitude | numeric(6,2) | NULL | Meters above sea level |
| current_zone_name | varchar(255) | NULL | Zone name if in any zone |
| restricted_zone_id | uuid | NULL, FK → restricted_zones.id | NULL if not in restricted zone |
| status | varchar(50) | NULL | Asset status |
| timestamp | timestamptz | NOT NULL | Position timestamp |
| metadata | jsonb | NULL | Additional tracking data |
| tenant_code | varchar(10) | NOT NULL | Tenant isolation |

**Hypertable**: Partitioned by `timestamp` (7-day chunks)

**Indexes**:
- `idx_movement_trail_timestamp` (timestamp DESC)
- `idx_movement_trail_asset_time` (asset_id, timestamp DESC)
- `idx_movement_trail_location` (GIST on location)
- `idx_movement_trail_tenant_time` (tenant_code, timestamp DESC)

**Retention**: 90 days, then archive to S3

**Compression**: Enable after 7 days (expect 70-90% reduction)

---

### 3. asset_location_register

**Purpose**: Current state snapshot (last known position per asset)

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| asset_id | uuid | PK, FK → assets.id | Unique asset |
| asset_identifier | varchar(50) | NOT NULL | Asset identifier |
| current_location | geometry(POINT, 4326) | NOT NULL | Latest GPS position |
| current_zone_name | varchar(255) | NULL | Current zone |
| restricted_zone_id | uuid | NULL, FK → restricted_zones.id | NULL if not in restricted zone |
| speed | numeric(5,2) | NULL | Current speed km/h |
| heading | numeric(5,2) | NULL | Current heading degrees |
| status | varchar(50) | NULL | Current status |
| last_updated | timestamptz | NOT NULL | Last position update |
| tenant_code | varchar(10) | NOT NULL | Tenant isolation |

**Indexes**:
- `idx_location_register_tenant` (tenant_code)
- `idx_location_register_zone` (restricted_zone_id)
- `idx_location_register_location` (GIST on current_location)

**Update Strategy**: Upsert on each new position from Kafka consumer

---

### 4. zone_violations (Hypertable)

**Purpose**: Record of all zone incursions

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | uuid | PK, DEFAULT gen_random_uuid() | Unique violation ID |
| violation_id | varchar(50) | UNIQUE, NOT NULL | Human-readable ID (VIO-YYYYMMDD-NNN) |
| asset_id | uuid | NOT NULL, FK → assets.id | Violating asset |
| asset_identifier | varchar(50) | NOT NULL | Asset identifier |
| asset_name | varchar(255) | NOT NULL | Asset name |
| asset_category | varchar(50) | NOT NULL | Asset category |
| restricted_zone_id | uuid | NOT NULL, FK → restricted_zones.id | Zone entered |
| zone_name | varchar(255) | NOT NULL | Zone name |
| zone_type | zone_type_enum | NOT NULL | Zone type |
| violation_type | violation_type_enum | NOT NULL | UNAUTHORIZED_ENTRY, UNAUTHORIZED_CATEGORY, PROHIBITED_ZONE |
| entry_location | geometry(POINT, 4326) | NOT NULL | Where asset entered |
| duration_seconds | integer | NULL | Time spent in zone (NULL if ongoing) |
| severity | severity_enum | NOT NULL | CRITICAL, HIGH, MEDIUM, LOW |
| acknowledged | boolean | DEFAULT false | Reviewed by manager |
| acknowledged_by | varchar(255) | NULL | User who acknowledged |
| acknowledged_at | timestamptz | NULL | Acknowledgment timestamp |
| resolution_notes | text | NULL | Manager's notes |
| timestamp | timestamptz | NOT NULL | Entry timestamp |
| tenant_code | varchar(10) | NOT NULL | Tenant isolation |

**Hypertable**: Partitioned by `timestamp` (7-day chunks)

**Indexes**:
- `idx_violations_timestamp` (timestamp DESC)
- `idx_violations_asset_time` (asset_id, timestamp DESC)
- `idx_violations_zone` (restricted_zone_id)
- `idx_violations_tenant_time` (tenant_code, timestamp DESC)
- `idx_violations_severity` (severity)
- `idx_violations_acknowledged` (acknowledged, timestamp DESC)

**Retention**: Indefinite (compliance requirement)

---

### 5. movement_discrepancies (Hypertable)

**Purpose**: Record of movement anomalies

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | uuid | PK, DEFAULT gen_random_uuid() | Unique discrepancy ID |
| discrepancy_id | varchar(50) | UNIQUE, NOT NULL | Human-readable ID (DIS-YYYYMMDD-NNN) |
| asset_id | uuid | NOT NULL, FK → assets.id | Asset with discrepancy |
| asset_identifier | varchar(50) | NOT NULL | Asset identifier |
| asset_name | varchar(255) | NOT NULL | Asset name |
| asset_category | varchar(50) | NOT NULL | Asset category |
| discrepancy_type | discrepancy_type_enum | NOT NULL | UNEXPECTED_MOVEMENT, LOCATION_MISMATCH, SPEED_ANOMALY, MISSING_TRACKING, DUPLICATE_SIGNAL |
| expected_location | geometry(POINT, 4326) | NULL | Expected GPS position (NULL for MISSING_TRACKING) |
| actual_location | geometry(POINT, 4326) | NOT NULL | Actual GPS position |
| deviation_meters | numeric(8,2) | NULL | Distance between expected/actual |
| expected_speed | numeric(5,2) | NULL | Expected speed km/h |
| actual_speed | numeric(5,2) | NULL | Actual speed km/h |
| description | text | NOT NULL | Auto-generated description |
| severity | severity_enum | NOT NULL | CRITICAL, HIGH, MEDIUM, LOW |
| acknowledged | boolean | DEFAULT false | Reviewed by manager |
| acknowledged_by | varchar(255) | NULL | User who acknowledged |
| acknowledged_at | timestamptz | NULL | Acknowledgment timestamp |
| investigation_notes | text | NULL | Manager's investigation |
| timestamp | timestamptz | NOT NULL | Detection timestamp |
| tenant_code | varchar(10) | NOT NULL | Tenant isolation |

**Hypertable**: Partitioned by `timestamp` (7-day chunks)

**Indexes**: Similar to zone_violations

**Retention**: 1 year, then soft delete

---

## Enumerations

### zone_type_enum
```sql
CREATE TYPE zone_type_enum AS ENUM (
    'PROHIBITED',    -- No assets allowed (runways during operations)
    'RESTRICTED',    -- Only specific categories (fuel zones)
    'CONTROLLED',    -- Authorized categories with logging
    'MAINTENANCE'    -- Maintenance vehicles only
);
```

### violation_type_enum
```sql
CREATE TYPE violation_type_enum AS ENUM (
    'UNAUTHORIZED_ENTRY',      -- Asset entered without authorization
    'UNAUTHORIZED_CATEGORY',   -- Asset category not in allowed list
    'PROHIBITED_ZONE'          -- Entered PROHIBITED zone (always violation)
);
```

### discrepancy_type_enum
```sql
CREATE TYPE discrepancy_type_enum AS ENUM (
    'UNEXPECTED_MOVEMENT',  -- Asset moved while status = Maintenance/OOS
    'LOCATION_MISMATCH',    -- Register location != GPS location
    'SPEED_ANOMALY',        -- Speed exceeds category max
    'MISSING_TRACKING',     -- No GPS update for >10 min
    'DUPLICATE_SIGNAL'      -- Same asset ID at 2+ locations
);
```

### severity_enum
```sql
CREATE TYPE severity_enum AS ENUM (
    'CRITICAL',  -- Immediate safety/security risk
    'HIGH',      -- Significant compliance risk
    'MEDIUM',    -- Operational irregularity
    'LOW'        -- Minor deviation
);
```

---

## Continuous Aggregates

### zone_violations_hourly
```sql
CREATE MATERIALIZED VIEW zone_violations_hourly
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('1 hour', timestamp) AS hour,
    tenant_code,
    restricted_zone_id,
    zone_name,
    zone_type,
    COUNT(*) AS violation_count,
    COUNT(*) FILTER (WHERE severity = 'CRITICAL') AS critical_count,
    COUNT(*) FILTER (WHERE severity = 'HIGH') AS high_count,
    COUNT(*) FILTER (WHERE severity = 'MEDIUM') AS medium_count,
    COUNT(*) FILTER (WHERE severity = 'LOW') AS low_count,
    COUNT(DISTINCT asset_id) AS unique_assets,
    AVG(duration_seconds) AS avg_duration_seconds
FROM zone_violations
GROUP BY hour, tenant_code, restricted_zone_id, zone_name, zone_type;
```

### movement_discrepancies_daily
```sql
CREATE MATERIALIZED VIEW movement_discrepancies_daily
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('1 day', timestamp) AS day,
    tenant_code,
    discrepancy_type,
    COUNT(*) AS discrepancy_count,
    COUNT(*) FILTER (WHERE severity = 'CRITICAL') AS critical_count,
    COUNT(*) FILTER (WHERE severity = 'HIGH') AS high_count,
    COUNT(DISTINCT asset_id) AS unique_assets,
    AVG(deviation_meters) AS avg_deviation_meters
FROM movement_discrepancies
GROUP BY day, tenant_code, discrepancy_type;
```

---

## Materialized Views (Heatmaps)

### asset_activity_heatmap
```sql
CREATE MATERIALIZED VIEW asset_activity_heatmap AS
SELECT 
    ST_SnapToGrid(location, 0.0001) AS grid_location, -- 10m grid
    tenant_code,
    time_bucket('1 hour', timestamp) AS time_bucket,
    COUNT(*) AS activity_count,
    COUNT(DISTINCT asset_identifier) AS unique_assets,
    AVG(speed) AS avg_speed,
    COUNT(*) FILTER (WHERE restricted_zone_id IS NOT NULL) AS restricted_zone_visits
FROM asset_movement_trail
WHERE timestamp > NOW() - INTERVAL '30 days'
GROUP BY grid_location, tenant_code, time_bucket;

CREATE INDEX idx_activity_heatmap_grid ON asset_activity_heatmap USING GIST (grid_location);
CREATE INDEX idx_activity_heatmap_tenant_time ON asset_activity_heatmap (tenant_code, time_bucket DESC);
```

### violation_heatmap
```sql
CREATE MATERIALIZED VIEW violation_heatmap AS
SELECT 
    ST_SnapToGrid(entry_location, 0.0001) AS grid_location,
    tenant_code,
    time_bucket('1 hour', timestamp) AS time_bucket,
    COUNT(*) AS violation_count,
    COUNT(DISTINCT asset_id) AS unique_violators,
    COUNT(*) FILTER (WHERE severity = 'CRITICAL') AS critical_count,
    COUNT(*) FILTER (WHERE severity = 'HIGH') AS high_count
FROM zone_violations
WHERE timestamp > NOW() - INTERVAL '30 days'
GROUP BY grid_location, tenant_code, time_bucket;

CREATE INDEX idx_violation_heatmap_grid ON violation_heatmap USING GIST (grid_location);
CREATE INDEX idx_violation_heatmap_tenant_time ON violation_heatmap (tenant_code, time_bucket DESC);
```

**Refresh Policy**: Every 6 hours

---

## Database Functions

### get_asset_id_from_vehicle(p_vehicle_id VARCHAR)
```sql
CREATE OR REPLACE FUNCTION get_asset_id_from_vehicle(p_vehicle_id VARCHAR)
RETURNS UUID AS $$
BEGIN
    RETURN (
        SELECT id 
        FROM assets 
        WHERE qr_id = p_vehicle_id 
        LIMIT 1
    );
END;
$$ LANGUAGE plpgsql STABLE;
```

### check_zone_authorization(p_asset_id UUID, p_zone_id UUID)
```sql
CREATE OR REPLACE FUNCTION check_zone_authorization(
    p_asset_id UUID, 
    p_zone_id UUID
) RETURNS BOOLEAN AS $$
DECLARE
    v_asset_category VARCHAR;
    v_zone RECORD;
BEGIN
    -- Get asset category
    SELECT category INTO v_asset_category FROM assets WHERE id = p_asset_id;
    
    -- Get zone details
    SELECT * INTO v_zone FROM restricted_zones WHERE id = p_zone_id;
    
    -- PROHIBITED zones: always unauthorized
    IF v_zone.zone_type = 'PROHIBITED' THEN
        RETURN FALSE;
    END IF;
    
    -- Check specific asset ID exceptions
    IF p_asset_id = ANY(v_zone.authorized_asset_ids) THEN
        RETURN TRUE;
    END IF;
    
    -- Check category authorization
    IF v_zone.authorized_asset_categories IS NULL 
       OR array_length(v_zone.authorized_asset_categories, 1) = 0 THEN
        -- NULL or empty = no restrictions
        RETURN TRUE;
    END IF;
    
    IF v_asset_category = ANY(v_zone.authorized_asset_categories) THEN
        RETURN TRUE;
    END IF;
    
    -- Default: unauthorized
    RETURN FALSE;
END;
$$ LANGUAGE plpgsql STABLE;
```

---

## Data Flow Summary

1. **Ingestion**: NiFi polls `vehicles` table → Kafka `asset-positions-json` topic
2. **Processing**: Spring Boot Kafka consumer receives position events
3. **Mapping**: Match `vehicle_id` to `asset_id` via `qr_id` field
4. **Trail Recording**: Insert into `asset_movement_trail` hypertable
5. **State Update**: Upsert into `asset_location_register`
6. **Violation Detection**: Check `ST_DWithin(zone.geometry, location, 50)` for all active zones
7. **Discrepancy Detection**: Run 5 rule-based checks
8. **Event Broadcasting**: WebSocket publish to `/topic/violations/{tenantCode}` and `/topic/assets/live/{tenantCode}`
9. **Aggregation**: Continuous aggregates refresh hourly/daily
10. **Heatmap**: Materialized views refresh every 6 hours

---

**Status**: ✅ Data model complete  
**Next**: Generate API contracts (contracts/violations.openapi.yaml, etc.)
