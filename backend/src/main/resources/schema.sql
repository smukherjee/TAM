-- Flights Hypertable
CREATE TABLE IF NOT EXISTS flights (
    time        TIMESTAMPTZ NOT NULL,
    live_plot_id UUID NOT NULL,
    callsign    TEXT NOT NULL,
    latitude    DOUBLE PRECISION,
    longitude   DOUBLE PRECISION,
    speed       DOUBLE PRECISION,
    heading     DOUBLE PRECISION,
    altitude    DOUBLE PRECISION,
    status      TEXT,
    track_id    TEXT,
    mode_s_id   TEXT,
    flight_level DOUBLE PRECISION,
    roc         DOUBLE PRECISION,
    ssr         TEXT,
    safety_alert BOOLEAN,
    system_status TEXT,
    spi         BOOLEAN,
    update_type TEXT
);

SELECT create_hypertable('flights', 'time', if_not_exists => TRUE);

-- Add columns if they don't exist (for existing deployments)
ALTER TABLE flights ADD COLUMN IF NOT EXISTS track_id TEXT;
ALTER TABLE flights ADD COLUMN IF NOT EXISTS mode_s_id TEXT;
ALTER TABLE flights ADD COLUMN IF NOT EXISTS flight_level DOUBLE PRECISION;
ALTER TABLE flights ADD COLUMN IF NOT EXISTS roc DOUBLE PRECISION;
ALTER TABLE flights ADD COLUMN IF NOT EXISTS ssr TEXT;
ALTER TABLE flights ADD COLUMN IF NOT EXISTS safety_alert BOOLEAN;
ALTER TABLE flights ADD COLUMN IF NOT EXISTS system_status TEXT;
ALTER TABLE flights ADD COLUMN IF NOT EXISTS spi BOOLEAN;
ALTER TABLE flights ADD COLUMN IF NOT EXISTS update_type TEXT;


-- Vehicles Hypertable
CREATE TABLE IF NOT EXISTS vehicles (
    timestamp   TIMESTAMPTZ NOT NULL,
    vehicle_no  TEXT NOT NULL,
    type        TEXT,
    latitude    DOUBLE PRECISION,
    longitude   DOUBLE PRECISION,
    speed       DOUBLE PRECISION,
    altitude    DOUBLE PRECISION,
    status      TEXT,
    vehicle_name TEXT,
    company     TEXT,
    temperature TEXT,
    gps         TEXT,
    door1       TEXT,
    door2       TEXT,
    door3       TEXT,
    door4       TEXT,
    branch      TEXT,
    gps_actual_time TEXT,
    device_model TEXT,
    ac          TEXT,
    imei_no     TEXT,
    odometer    TEXT,
    poi         TEXT,
    driver_middle_name TEXT,
    driver_first_name TEXT,
    driver_last_name TEXT,
    immobilize_state TEXT,
    ign         TEXT,
    angle       DOUBLE PRECISION,
    sos         TEXT,
    battery_percentage TEXT,
    external_volt TEXT,
    power       TEXT,
    location    TEXT,
    fuel        TEXT
);

SELECT create_hypertable('vehicles', 'timestamp', if_not_exists => TRUE);

-- Add columns if they don't exist (for existing deployments)
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS vehicle_name TEXT;
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS company TEXT;
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS temperature TEXT;
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS gps TEXT;
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS door1 TEXT;
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS door2 TEXT;
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS door3 TEXT;
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS door4 TEXT;
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS branch TEXT;
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS gps_actual_time TEXT;
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS device_model TEXT;
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS ac TEXT;
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS imei_no TEXT;
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS odometer TEXT;
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS poi TEXT;
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS driver_middle_name TEXT;
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS driver_first_name TEXT;
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS driver_last_name TEXT;
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS immobilize_state TEXT;
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS ign TEXT;
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS angle DOUBLE PRECISION;
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS sos TEXT;
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS battery_percentage TEXT;
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS external_volt TEXT;
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS power TEXT;
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS location TEXT;
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS fuel TEXT;


-- Alerts Table (Standard)
CREATE TABLE IF NOT EXISTS alerts (
    alert_id    UUID PRIMARY KEY,
    timestamp   TIMESTAMPTZ NOT NULL,
    type        TEXT NOT NULL,
    entity_id   TEXT NOT NULL,
    value       DOUBLE PRECISION,
    latitude    DOUBLE PRECISION,
    longitude   DOUBLE PRECISION
);
