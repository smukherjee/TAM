-- Fix Schema Mismatches for Platform Phase 1

-- ============================================
-- 1. Fix Turnaround Events Table
-- ============================================
-- Drop the old table and recreate it to match the Java Entity
DROP TABLE IF EXISTS turnaround_events CASCADE;

CREATE TABLE turnaround_events (
    event_unique_id VARCHAR(255) NOT NULL,
    camera_id VARCHAR(255),
    camera_name VARCHAR(255),
    activity_type VARCHAR(255),
    event_type INTEGER, -- 0=Start, 1=Stop
    event_time_stamp TIMESTAMPTZ NOT NULL,
    stand VARCHAR(255),
    icao_code VARCHAR(4),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (event_unique_id, event_time_stamp)
);

-- Convert to hypertable
SELECT create_hypertable('turnaround_events', 'event_time_stamp', if_not_exists => TRUE);

CREATE INDEX idx_turnaround_stand ON turnaround_events (stand);
CREATE INDEX idx_turnaround_icao ON turnaround_events (icao_code);

-- ============================================
-- 2. Fix Alerts Table
-- ============================================
-- Drop the old table and recreate it to match the Java Entity
DROP TABLE IF EXISTS alerts CASCADE;

CREATE TABLE alerts (
    alert_id UUID NOT NULL,
    type VARCHAR(50),
    entity_id VARCHAR(100),
    value DOUBLE PRECISION,
    timestamp TIMESTAMPTZ NOT NULL,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    icao_code VARCHAR(4),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (alert_id, timestamp)
);

-- Convert to hypertable (as per spec, though code uses UUID PK, Timescale needs time column in PK)
SELECT create_hypertable('alerts', 'timestamp', if_not_exists => TRUE);

CREATE INDEX idx_alerts_icao ON alerts (icao_code);
CREATE INDEX idx_alerts_type ON alerts (type);

-- ============================================
-- 3. Fix Materialized Views (Multi-Tenancy)
-- ============================================

-- Drop existing views
DROP MATERIALIZED VIEW IF EXISTS flights_hourly CASCADE;
DROP MATERIALIZED VIEW IF EXISTS vehicle_speed_violations_hourly CASCADE;

-- Recreate Flights Hourly with ICAO Code
CREATE MATERIALIZED VIEW flights_hourly
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 hour', timestamp) AS bucket,
    icao_code,
    COUNT(*) AS flight_count,
    AVG(altitude) AS avg_altitude,
    AVG(speed) AS avg_speed,
    COUNT(DISTINCT flight_number) AS unique_flights
FROM flights
GROUP BY bucket, icao_code
WITH NO DATA;

SELECT add_continuous_aggregate_policy('flights_hourly',
    start_offset => INTERVAL '3 hours',
    end_offset => INTERVAL '1 hour',
    schedule_interval => INTERVAL '1 hour',
    if_not_exists => TRUE);

-- Recreate Vehicle Violations with ICAO Code
CREATE MATERIALIZED VIEW vehicle_speed_violations_hourly
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 hour', timestamp) AS bucket,
    icao_code,
    vehicle_id,
    COUNT(*) AS violation_count,
    MAX(speed) AS max_speed,
    AVG(speed) AS avg_speed
FROM vehicles
WHERE speed > 25
GROUP BY bucket, icao_code, vehicle_id
WITH NO DATA;

SELECT add_continuous_aggregate_policy('vehicle_speed_violations_hourly',
    start_offset => INTERVAL '3 hours',
    end_offset => INTERVAL '1 hour',
    schedule_interval => INTERVAL '1 hour',
    if_not_exists => TRUE);
