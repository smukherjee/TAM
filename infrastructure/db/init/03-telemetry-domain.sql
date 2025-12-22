-- TAM Database Initialization: Layer 3 - Telemetry Domain
-- Scope: Sensor Alerts, Analytics

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. SENSOR ALERTS (Renamed from 'alerts' to avoid ambiguity)
CREATE TABLE IF NOT EXISTS sensor_alerts (
    alert_id UUID NOT NULL,
    tenant_code VARCHAR(4),
    type VARCHAR(50), -- e.g., 'SPEEDING', 'GEOFENCE'
    entity_id VARCHAR(100),
    value DOUBLE PRECISION,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    timestamp TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (alert_id, timestamp)
);

SELECT create_hypertable('sensor_alerts', 'timestamp', if_not_exists => TRUE);
CREATE INDEX IF NOT EXISTS idx_sensor_alerts_tenant ON sensor_alerts (tenant_code, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_sensor_alerts_type ON sensor_alerts (type);

-- 2. MATERIALIZED VIEWS (Analytics)

-- Flights Hourly Stats per Tenant
CREATE MATERIALIZED VIEW IF NOT EXISTS flights_hourly
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 hour', timestamp) AS bucket,
    tenant_code,
    COUNT(*) AS flight_count,
    AVG(altitude) AS avg_altitude,
    AVG(speed) AS avg_speed,
    COUNT(DISTINCT flight_number) AS unique_flights
FROM flights
GROUP BY bucket, tenant_code
WITH NO DATA;

SELECT add_continuous_aggregate_policy('flights_hourly',
    start_offset => INTERVAL '3 hours',
    end_offset => INTERVAL '1 hour',
    schedule_interval => INTERVAL '1 hour',
    if_not_exists => TRUE);

-- Vehicle Violations per Tenant
CREATE MATERIALIZED VIEW IF NOT EXISTS vehicle_speed_violations_hourly
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 hour', timestamp) AS bucket,
    tenant_code,
    vehicle_id,
    COUNT(*) AS violation_count,
    MAX(speed) AS max_speed,
    AVG(speed) AS avg_speed
FROM vehicles
WHERE speed > 25
GROUP BY bucket, tenant_code, vehicle_id
WITH NO DATA;

SELECT add_continuous_aggregate_policy('vehicle_speed_violations_hourly',
    start_offset => INTERVAL '3 hours',
    end_offset => INTERVAL '1 hour',
    schedule_interval => INTERVAL '1 hour',
    if_not_exists => TRUE);
