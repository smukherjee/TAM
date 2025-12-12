-- Enable TimescaleDB extension
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- Flights Table
CREATE TABLE IF NOT EXISTS flights (
    time TIMESTAMPTZ NOT NULL,
    live_plot_id UUID,
    callsign VARCHAR(255),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    speed DOUBLE PRECISION,
    heading DOUBLE PRECISION,
    altitude DOUBLE PRECISION,
    status VARCHAR(50),
    track_id VARCHAR(255),
    mode_s_id VARCHAR(255),
    flight_level DOUBLE PRECISION,
    roc DOUBLE PRECISION,
    ssr VARCHAR(50),
    safety_alert BOOLEAN,
    system_status VARCHAR(50),
    spi BOOLEAN,
    update_type VARCHAR(50)
);

-- Convert to Hypertable
SELECT create_hypertable('flights', 'time', if_not_exists => TRUE);

-- Vehicles Table
CREATE TABLE IF NOT EXISTS vehicles (
    gpsactualtime TIMESTAMPTZ NOT NULL,
    vehicle_no VARCHAR(255),
    vehicletype VARCHAR(255),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    speed DOUBLE PRECISION,
    status VARCHAR(50),
    vehicle_name VARCHAR(255),
    company VARCHAR(255),
    location VARCHAR(255),
    ign VARCHAR(50)
);

-- Convert to Hypertable
SELECT create_hypertable('vehicles', 'gpsactualtime', if_not_exists => TRUE);

-- Alerts Table
CREATE TABLE IF NOT EXISTS alerts (
    timestamp TIMESTAMPTZ NOT NULL,
    alert_id UUID,
    type VARCHAR(50),
    entity_id VARCHAR(255),
    value DOUBLE PRECISION,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION
);

-- Convert to Hypertable
SELECT create_hypertable('alerts', 'timestamp', if_not_exists => TRUE);

-- Turnaround Events Table
CREATE TABLE IF NOT EXISTS turnaround_events (
    event_time_stamp TIMESTAMPTZ NOT NULL,
    event_unique_id VARCHAR(255),
    camera_id VARCHAR(255),
    camera_name VARCHAR(255),
    activity_type VARCHAR(255),
    event_type INTEGER,
    stand VARCHAR(255)
);

-- Convert to Hypertable
SELECT create_hypertable('turnaround_events', 'event_time_stamp', if_not_exists => TRUE);
