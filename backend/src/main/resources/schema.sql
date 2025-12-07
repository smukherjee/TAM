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
    status      TEXT
);

SELECT create_hypertable('flights', 'time', if_not_exists => TRUE);

-- Vehicles Hypertable
CREATE TABLE IF NOT EXISTS vehicles (
    timestamp   TIMESTAMPTZ NOT NULL,
    vehicle_no  TEXT NOT NULL,
    type        TEXT,
    latitude    DOUBLE PRECISION,
    longitude   DOUBLE PRECISION,
    speed       DOUBLE PRECISION,
    altitude    DOUBLE PRECISION,
    status      TEXT
);

SELECT create_hypertable('vehicles', 'timestamp', if_not_exists => TRUE);

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
