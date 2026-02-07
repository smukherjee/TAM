-- TAM Database: Simulation Core Entities (Layer 14)
-- Scope: Vehicles, Assets, and real-time positions

-- ============================================
-- 1. Create simulation_vehicles table
-- ============================================
CREATE TABLE IF NOT EXISTS simulation_vehicles (
    id UUID PRIMARY KEY,
    tenant_code VARCHAR(10) NOT NULL REFERENCES tenants(code),
    vehicle_id VARCHAR(20) NOT NULL UNIQUE,
    vehicle_name VARCHAR(100) NOT NULL,
    vehicle_type_id UUID, -- Optional link to vehicle_types
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    speed DOUBLE PRECISION NOT NULL,
    heading DOUBLE PRECISION NOT NULL,
    status VARCHAR(20) NOT NULL,
    current_assignment_id UUID,
    current_stand_code VARCHAR(20),
    last_updated TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sim_veh_tenant ON simulation_vehicles(tenant_code);
CREATE INDEX IF NOT EXISTS idx_sim_veh_status ON simulation_vehicles(status);

-- ============================================
-- 2. Create simulation_assets table
-- ============================================
CREATE TABLE IF NOT EXISTS simulation_assets (
    id UUID PRIMARY KEY,
    tenant_code VARCHAR(10) NOT NULL REFERENCES tenants(code),
    asset_id VARCHAR(30) NOT NULL UNIQUE,
    asset_name VARCHAR(100) NOT NULL,
    asset_type VARCHAR(20) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    status VARCHAR(20) NOT NULL,
    last_seen TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sim_asset_tenant ON simulation_assets(tenant_code);

-- ============================================
-- 3. Create simulation_vehicle_positions table
-- ============================================
CREATE TABLE IF NOT EXISTS simulation_vehicle_positions (
    id UUID NOT NULL,
    vehicle_id UUID NOT NULL,
    tenant_code VARCHAR(10) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    speed DOUBLE PRECISION NOT NULL,
    heading DOUBLE PRECISION NOT NULL,
    status VARCHAR(20) NOT NULL,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Convert to hypertable
SELECT create_hypertable('simulation_vehicle_positions', 'recorded_at', if_not_exists => TRUE);

CREATE INDEX IF NOT EXISTS idx_vpos_vehicle ON simulation_vehicle_positions(vehicle_id);
CREATE INDEX IF NOT EXISTS idx_vpos_recorded ON simulation_vehicle_positions(recorded_at DESC);

-- ============================================
-- 4. Create simulation_asset_positions table
-- ============================================
CREATE TABLE IF NOT EXISTS simulation_asset_positions (
    id UUID NOT NULL,
    asset_id UUID NOT NULL,
    tenant_code VARCHAR(10) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    speed DOUBLE PRECISION NOT NULL,
    heading DOUBLE PRECISION NOT NULL,
    altitude DOUBLE PRECISION NOT NULL DEFAULT 0,
    accuracy DOUBLE PRECISION NOT NULL DEFAULT 1,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    source VARCHAR(20) NOT NULL DEFAULT 'SIM'
);

-- Convert to hypertable
SELECT create_hypertable('simulation_asset_positions', 'timestamp', if_not_exists => TRUE);

CREATE INDEX IF NOT EXISTS idx_apos_asset ON simulation_asset_positions(asset_id);
CREATE INDEX IF NOT EXISTS idx_apos_timestamp ON simulation_asset_positions(timestamp DESC);
