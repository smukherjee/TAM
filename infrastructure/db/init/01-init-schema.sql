-- TAM Database Initialization Script
-- Creates TimescaleDB hypertables and initial schema

-- Enable TimescaleDB extension
CREATE EXTENSION IF NOT EXISTS timescaledb CASCADE;
CREATE EXTENSION IF NOT EXISTS postgis CASCADE;

-- ============================================
-- FLIGHTS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS flights (
    id BIGSERIAL,
    flight_number VARCHAR(20) NOT NULL,
    callsign VARCHAR(20),
    icao24 VARCHAR(10),
    origin VARCHAR(10),
    destination VARCHAR(10),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    altitude DOUBLE PRECISION,
    speed DOUBLE PRECISION,
    heading DOUBLE PRECISION,
    vertical_rate DOUBLE PRECISION,
    on_ground BOOLEAN DEFAULT FALSE,
    squawk VARCHAR(10),
    status VARCHAR(50),
    timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (id, timestamp)
);

-- Convert to hypertable for time-series optimization
SELECT create_hypertable('flights', 'timestamp', if_not_exists => TRUE);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_flights_flight_number ON flights (flight_number);
CREATE INDEX IF NOT EXISTS idx_flights_timestamp ON flights (timestamp DESC);

-- ============================================
-- VEHICLES TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS vehicles (
    id BIGSERIAL,
    vehicle_id VARCHAR(50) NOT NULL,
    vehicle_type VARCHAR(50),
    vehicle_name VARCHAR(100),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    speed DOUBLE PRECISION,
    heading DOUBLE PRECISION,
    zone VARCHAR(50),
    status VARCHAR(50),
    timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (id, timestamp)
);

-- Convert to hypertable
SELECT create_hypertable('vehicles', 'timestamp', if_not_exists => TRUE);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_vehicles_vehicle_id ON vehicles (vehicle_id);
CREATE INDEX IF NOT EXISTS idx_vehicles_timestamp ON vehicles (timestamp DESC);

-- ============================================
-- ALERTS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS alerts (
    id BIGSERIAL PRIMARY KEY,
    alert_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT,
    entity_type VARCHAR(50),
    entity_id VARCHAR(100),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    acknowledged BOOLEAN DEFAULT FALSE,
    acknowledged_by VARCHAR(100),
    acknowledged_at TIMESTAMPTZ,
    resolved BOOLEAN DEFAULT FALSE,
    resolved_at TIMESTAMPTZ,
    metadata JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_alerts_created_at ON alerts (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_alerts_severity ON alerts (severity);
CREATE INDEX IF NOT EXISTS idx_alerts_entity ON alerts (entity_type, entity_id);

-- ============================================
-- TURNAROUND EVENTS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS turnaround_events (
    id BIGSERIAL,
    flight_number VARCHAR(20) NOT NULL,
    stand_id VARCHAR(20),
    milestone VARCHAR(50) NOT NULL,
    scheduled_time TIMESTAMPTZ,
    actual_time TIMESTAMPTZ,
    status VARCHAR(20),
    delay_minutes INTEGER DEFAULT 0,
    source VARCHAR(50),
    metadata JSONB,
    timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (id, timestamp)
);

-- Convert to hypertable
SELECT create_hypertable('turnaround_events', 'timestamp', if_not_exists => TRUE);

CREATE INDEX IF NOT EXISTS idx_turnaround_flight ON turnaround_events (flight_number);
CREATE INDEX IF NOT EXISTS idx_turnaround_stand ON turnaround_events (stand_id);

-- ============================================
-- CONTINUOUS AGGREGATES (Materialized Views)
-- ============================================

-- Hourly flight statistics
CREATE MATERIALIZED VIEW IF NOT EXISTS flights_hourly
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 hour', timestamp) AS bucket,
    COUNT(*) AS flight_count,
    AVG(altitude) AS avg_altitude,
    AVG(speed) AS avg_speed,
    COUNT(DISTINCT flight_number) AS unique_flights
FROM flights
GROUP BY bucket
WITH NO DATA;

-- Refresh policy for continuous aggregate
SELECT add_continuous_aggregate_policy('flights_hourly',
    start_offset => INTERVAL '3 hours',
    end_offset => INTERVAL '1 hour',
    schedule_interval => INTERVAL '1 hour',
    if_not_exists => TRUE);

-- Vehicle speed violations per hour
CREATE MATERIALIZED VIEW IF NOT EXISTS vehicle_speed_violations_hourly
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 hour', timestamp) AS bucket,
    vehicle_id,
    COUNT(*) AS violation_count,
    MAX(speed) AS max_speed,
    AVG(speed) AS avg_speed
FROM vehicles
WHERE speed > 25
GROUP BY bucket, vehicle_id
WITH NO DATA;

SELECT add_continuous_aggregate_policy('vehicle_speed_violations_hourly',
    start_offset => INTERVAL '3 hours',
    end_offset => INTERVAL '1 hour',
    schedule_interval => INTERVAL '1 hour',
    if_not_exists => TRUE);

-- ============================================
-- RETENTION POLICIES
-- ============================================

-- Retain raw flight data for 7 days
SELECT add_retention_policy('flights', INTERVAL '7 days', if_not_exists => TRUE);

-- Retain raw vehicle data for 7 days
SELECT add_retention_policy('vehicles', INTERVAL '7 days', if_not_exists => TRUE);

-- Retain turnaround events for 30 days
SELECT add_retention_policy('turnaround_events', INTERVAL '30 days', if_not_exists => TRUE);

-- ============================================
-- GRANT PERMISSIONS
-- ============================================
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO postgres;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO postgres;

-- Log initialization complete
DO $$
BEGIN
    RAISE NOTICE 'TAM Database initialization completed successfully!';
END $$;
