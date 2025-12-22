-- TAM Database Initialization: Layer 2 - Aviation Domain
-- Scope: Flights, Vehicles (Raw Ingestion)

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. FLIGHTS
CREATE TABLE IF NOT EXISTS flights (
    id UUID DEFAULT uuid_generate_v4(),
    tenant_code VARCHAR(4) NOT NULL,
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

SELECT create_hypertable('flights', 'timestamp', if_not_exists => TRUE);
CREATE INDEX IF NOT EXISTS idx_flights_tenant ON flights (tenant_code, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_flights_flight_number ON flights (flight_number);

-- 2. VEHICLES
CREATE TABLE IF NOT EXISTS vehicles (
    id UUID DEFAULT uuid_generate_v4(),
    tenant_code VARCHAR(4) NOT NULL,
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

SELECT create_hypertable('vehicles', 'timestamp', if_not_exists => TRUE);
CREATE INDEX IF NOT EXISTS idx_vehicles_tenant ON vehicles (tenant_code, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_vehicles_vehicle_id ON vehicles (vehicle_id);
