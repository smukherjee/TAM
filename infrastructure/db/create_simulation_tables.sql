-- Create missing simulation tables for data generators
-- This script creates tables required by the simulation package entities

-- ============================================
-- 1. Create depots table (GSE vehicle storage/dispatch locations)
-- ============================================
CREATE TABLE IF NOT EXISTS depots (
    id BIGSERIAL PRIMARY KEY,
    tenant_code VARCHAR(10) NOT NULL,
    depot_type VARCHAR(30) NOT NULL,
    name VARCHAR(100) NOT NULL,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    capacity INTEGER NOT NULL DEFAULT 10,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_depot_tenant_type ON depots(tenant_code, depot_type);

-- Insert depot data for VIDP (Delhi - Indira Gandhi International)
INSERT INTO depots (tenant_code, depot_type, name, latitude, longitude, capacity) VALUES
-- VIDP Depots
('VIDP', 'BAGGAGE', 'Baggage Hall Terminal 3', 28.5566, 77.0939, 20),
('VIDP', 'BAGGAGE', 'Baggage Hall Terminal 1D', 28.5598, 77.0838, 15),
('VIDP', 'FUEL', 'Fuel Farm East', 28.5510, 77.1050, 8),
('VIDP', 'FUEL', 'Fuel Farm West', 28.5620, 77.0780, 8),
('VIDP', 'CATERING', 'Air India Catering', 28.5480, 77.0900, 12),
('VIDP', 'CATERING', 'SATS Catering Facility', 28.5520, 77.0850, 10),
('VIDP', 'MAINTENANCE', 'MRO Hangar Complex', 28.5700, 77.0950, 15),
('VIDP', 'GPU', 'GPU Pool Terminal 3', 28.5565, 77.0935, 20),
('VIDP', 'GPU', 'GPU Pool Terminal 1D', 28.5590, 77.0830, 12),
('VIDP', 'PUSHBACK', 'Pushback Pool Cargo Area', 28.5450, 77.0920, 10),
('VIDP', 'WATER', 'Water Service Station', 28.5550, 77.0910, 8),
('VIDP', 'LAVATORY', 'Lavatory Service Base', 28.5545, 77.0905, 8),
('VIDP', 'STAIRS', 'Stairs Pool Terminal 3', 28.5570, 77.0945, 15),
('VIDP', 'BUS', 'Passenger Bus Depot', 28.5530, 77.0890, 25),
('VIDP', 'CARGO', 'Cargo Handling Area', 28.5460, 77.0930, 20),
-- LIRN Depots
('LIRN', 'BAGGAGE', 'Baggage Hall Terminal 1', 40.8848, 14.2898, 12),
('LIRN', 'FUEL', 'Fuel Farm', 40.8810, 14.2950, 6),
('LIRN', 'CATERING', 'LSG Catering Naples', 40.8800, 14.2870, 8),
('LIRN', 'MAINTENANCE', 'Maintenance Hangar', 40.8770, 14.2920, 10),
('LIRN', 'GPU', 'GPU Pool', 40.8840, 14.2890, 12),
('LIRN', 'PUSHBACK', 'Pushback Pool', 40.8835, 14.2885, 8),
('LIRN', 'WATER', 'Water Service', 40.8830, 14.2880, 5),
('LIRN', 'LAVATORY', 'Lavatory Service', 40.8828, 14.2878, 5),
('LIRN', 'STAIRS', 'Stairs Pool', 40.8845, 14.2895, 10),
('LIRN', 'BUS', 'Bus Depot', 40.8820, 14.2865, 15),
('LIRN', 'CARGO', 'Cargo Handling', 40.8780, 14.2910, 10),
-- YBBN Depots
('YBBN', 'BAGGAGE', 'Baggage Hall Domestic', -27.3845, 153.1178, 15),
('YBBN', 'BAGGAGE', 'Baggage Hall International', -27.3890, 153.1220, 15),
('YBBN', 'FUEL', 'Fuel Farm', -27.3950, 153.1280, 8),
('YBBN', 'CATERING', 'Gate Gourmet Brisbane', -27.3820, 153.1150, 10),
('YBBN', 'MAINTENANCE', 'Qantas Maintenance', -27.3980, 153.1100, 12),
('YBBN', 'GPU', 'GPU Pool Domestic', -27.3850, 153.1180, 15),
('YBBN', 'GPU', 'GPU Pool International', -27.3895, 153.1225, 12),
('YBBN', 'PUSHBACK', 'Pushback Pool', -27.3860, 153.1190, 10),
('YBBN', 'WATER', 'Water Service Station', -27.3855, 153.1185, 8),
('YBBN', 'LAVATORY', 'Lavatory Service Base', -27.3852, 153.1182, 8),
('YBBN', 'STAIRS', 'Stairs Pool', -27.3848, 153.1175, 12),
('YBBN', 'BUS', 'Passenger Bus Depot', -27.3830, 153.1160, 20),
('YBBN', 'CARGO', 'Cargo Terminal', -27.3920, 153.1250, 15)
ON CONFLICT DO NOTHING;

-- ============================================
-- 2. Create simulation_vehicles table
-- ============================================
CREATE TABLE IF NOT EXISTS simulation_vehicles (
    id UUID PRIMARY KEY,
    tenant_code VARCHAR(10) NOT NULL,
    vehicle_id VARCHAR(20) NOT NULL UNIQUE,
    vehicle_name VARCHAR(100) NOT NULL,
    vehicle_type_id UUID NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    speed DOUBLE PRECISION NOT NULL,
    heading DOUBLE PRECISION NOT NULL,
    status VARCHAR(20) NOT NULL,
    current_assignment_id UUID,
    current_stand_code VARCHAR(20),
    last_updated TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_vehicle_tenant ON simulation_vehicles(tenant_code);
CREATE INDEX IF NOT EXISTS idx_vehicle_status ON simulation_vehicles(status);
CREATE INDEX IF NOT EXISTS idx_vehicle_type ON simulation_vehicles(vehicle_type_id);
CREATE INDEX IF NOT EXISTS idx_vehicle_updated ON simulation_vehicles(last_updated);

-- ============================================
-- 3. Create simulation_assets table
-- ============================================
CREATE TABLE IF NOT EXISTS simulation_assets (
    id UUID PRIMARY KEY,
    tenant_code VARCHAR(10) NOT NULL,
    asset_id VARCHAR(30) NOT NULL UNIQUE,
    asset_name VARCHAR(100) NOT NULL,
    asset_type VARCHAR(20) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    status VARCHAR(20) NOT NULL,
    last_seen TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_asset_tenant ON simulation_assets(tenant_code);
CREATE INDEX IF NOT EXISTS idx_asset_type ON simulation_assets(asset_type);
CREATE INDEX IF NOT EXISTS idx_asset_status ON simulation_assets(status);
CREATE INDEX IF NOT EXISTS idx_asset_last_seen ON simulation_assets(last_seen);

-- ============================================
-- 4. Create simulation_vehicle_positions table (for tracking history)
-- ============================================
CREATE TABLE IF NOT EXISTS simulation_vehicle_positions (
    id UUID PRIMARY KEY,
    vehicle_id UUID NOT NULL,
    tenant_code VARCHAR(10) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    speed DOUBLE PRECISION NOT NULL,
    heading DOUBLE PRECISION NOT NULL,
    status VARCHAR(20) NOT NULL,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_veh_pos_vehicle ON simulation_vehicle_positions(vehicle_id);
CREATE INDEX IF NOT EXISTS idx_veh_pos_tenant ON simulation_vehicle_positions(tenant_code);
CREATE INDEX IF NOT EXISTS idx_veh_pos_recorded ON simulation_vehicle_positions(recorded_at);

-- Convert to hypertable for time-series
SELECT create_hypertable('simulation_vehicle_positions', by_range('recorded_at'), if_not_exists => true);

-- ============================================
-- 5. Create simulation_asset_positions table (for tracking history)
-- ============================================
CREATE TABLE IF NOT EXISTS simulation_asset_positions (
    id UUID PRIMARY KEY,
    asset_id UUID NOT NULL,
    tenant_code VARCHAR(10) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_asset_pos_asset ON simulation_asset_positions(asset_id);
CREATE INDEX IF NOT EXISTS idx_asset_pos_tenant ON simulation_asset_positions(tenant_code);
CREATE INDEX IF NOT EXISTS idx_asset_pos_recorded ON simulation_asset_positions(recorded_at);

-- Convert to hypertable for time-series
SELECT create_hypertable('simulation_asset_positions', by_range('recorded_at'), if_not_exists => true);

-- ============================================
-- 6. Create other simulation tables if needed
-- ============================================
CREATE TABLE IF NOT EXISTS simulation_discrepancies (
    id UUID PRIMARY KEY,
    tenant_code VARCHAR(10) NOT NULL,
    discrepancy_type VARCHAR(30) NOT NULL,
    asset_id VARCHAR(30),
    vehicle_id VARCHAR(20),
    description TEXT,
    severity VARCHAR(20),
    detected_at TIMESTAMP WITH TIME ZONE,
    resolved_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_discrepancy_tenant ON simulation_discrepancies(tenant_code);
CREATE INDEX IF NOT EXISTS idx_discrepancy_status ON simulation_discrepancies(status);

CREATE TABLE IF NOT EXISTS simulation_violations (
    id UUID PRIMARY KEY,
    tenant_code VARCHAR(10) NOT NULL,
    violation_type VARCHAR(30) NOT NULL,
    zone_id VARCHAR(30),
    asset_id VARCHAR(30),
    vehicle_id VARCHAR(20),
    description TEXT,
    severity VARCHAR(20),
    detected_at TIMESTAMP WITH TIME ZONE,
    resolved_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_violation_tenant ON simulation_violations(tenant_code);
CREATE INDEX IF NOT EXISTS idx_violation_status ON simulation_violations(status);

CREATE TABLE IF NOT EXISTS simulation_alerts (
    id UUID PRIMARY KEY,
    tenant_code VARCHAR(10) NOT NULL,
    alert_type VARCHAR(30) NOT NULL,
    entity_type VARCHAR(20),
    entity_id VARCHAR(30),
    message TEXT,
    severity VARCHAR(20),
    created_at TIMESTAMP WITH TIME ZONE,
    acknowledged_at TIMESTAMP WITH TIME ZONE,
    resolved_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_alert_tenant ON simulation_alerts(tenant_code);
CREATE INDEX IF NOT EXISTS idx_alert_status ON simulation_alerts(status);
CREATE INDEX IF NOT EXISTS idx_alert_type ON simulation_alerts(alert_type);

CREATE TABLE IF NOT EXISTS simulation_dispatches (
    id UUID PRIMARY KEY,
    tenant_code VARCHAR(10) NOT NULL,
    vehicle_id UUID,
    flight_id UUID,
    stand_code VARCHAR(20),
    dispatch_type VARCHAR(30) NOT NULL,
    priority INTEGER,
    created_at TIMESTAMP WITH TIME ZONE,
    dispatched_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_dispatch_tenant ON simulation_dispatches(tenant_code);
CREATE INDEX IF NOT EXISTS idx_dispatch_status ON simulation_dispatches(status);
CREATE INDEX IF NOT EXISTS idx_dispatch_vehicle ON simulation_dispatches(vehicle_id);

CREATE TABLE IF NOT EXISTS simulation_financial_metrics (
    id UUID PRIMARY KEY,
    tenant_code VARCHAR(10) NOT NULL,
    metric_type VARCHAR(30) NOT NULL,
    metric_name VARCHAR(100) NOT NULL,
    metric_value DOUBLE PRECISION NOT NULL,
    unit VARCHAR(20),
    period_start TIMESTAMP WITH TIME ZONE,
    period_end TIMESTAMP WITH TIME ZONE,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_financial_tenant ON simulation_financial_metrics(tenant_code);
CREATE INDEX IF NOT EXISTS idx_financial_type ON simulation_financial_metrics(metric_type);
CREATE INDEX IF NOT EXISTS idx_financial_recorded ON simulation_financial_metrics(recorded_at);

-- ============================================
-- 7. Ensure vehicle_types has seed data
-- ============================================
INSERT INTO vehicle_types (code, name, category, tenant_code, active, min_speed, max_speed, default_quantity, default_depot_type, icon_name, icon_color, is_motorized)
VALUES 
-- VIDP vehicle types
('BAG', 'Baggage Tractor', 'BAGGAGE', 'VIDP', true, 0, 25, 20, 'BAGGAGE', 'truck', '#4A90D9', true),
('BELT', 'Belt Loader', 'BAGGAGE', 'VIDP', true, 0, 10, 10, 'BAGGAGE', 'conveyor', '#5A9BD5', true),
('FUEL', 'Fuel Bowser', 'FUEL', 'VIDP', true, 0, 30, 8, 'FUEL', 'fuel', '#F5A623', true),
('HYDR', 'Hydrant Dispenser', 'FUEL', 'VIDP', true, 0, 20, 6, 'FUEL', 'hydrant', '#F5B041', true),
('CAT', 'Catering Truck', 'CATERING', 'VIDP', true, 0, 25, 12, 'CATERING', 'catering', '#7ED321', true),
('GPU', 'Ground Power Unit', 'POWER', 'VIDP', true, 0, 15, 15, 'GPU', 'power', '#9B59B6', true),
('ASU', 'Air Start Unit', 'POWER', 'VIDP', true, 0, 15, 8, 'GPU', 'air', '#8E44AD', true),
('PB', 'Pushback Tug', 'PUSHBACK', 'VIDP', true, 0, 20, 10, 'PUSHBACK', 'pushback', '#E74C3C', true),
('TWB', 'Towbar', 'PUSHBACK', 'VIDP', false, 0, 0, 15, 'PUSHBACK', 'towbar', '#C0392B', false),
('PAX', 'Passenger Bus', 'PASSENGER', 'VIDP', true, 0, 40, 20, 'BUS', 'bus', '#3498DB', true),
('STRS', 'Passenger Stairs', 'PASSENGER', 'VIDP', true, 0, 15, 12, 'STAIRS', 'stairs', '#2980B9', true),
('WATER', 'Water Truck', 'SERVICE', 'VIDP', true, 0, 25, 6, 'WATER', 'water', '#1ABC9C', true),
('LAV', 'Lavatory Service', 'SERVICE', 'VIDP', true, 0, 20, 6, 'LAVATORY', 'lavatory', '#16A085', true),
('CARGO', 'Cargo Loader', 'CARGO', 'VIDP', true, 0, 10, 10, 'CARGO', 'cargo', '#95A5A6', true),
('DEICE', 'De-icing Truck', 'DEICE', 'VIDP', true, 0, 25, 4, 'MAINTENANCE', 'snowflake', '#00BCD4', true),
-- LIRN vehicle types
('BAG', 'Baggage Tractor', 'BAGGAGE', 'LIRN', true, 0, 25, 15, 'BAGGAGE', 'truck', '#4A90D9', true),
('BELT', 'Belt Loader', 'BAGGAGE', 'LIRN', true, 0, 10, 8, 'BAGGAGE', 'conveyor', '#5A9BD5', true),
('FUEL', 'Fuel Bowser', 'FUEL', 'LIRN', true, 0, 30, 6, 'FUEL', 'fuel', '#F5A623', true),
('HYDR', 'Hydrant Dispenser', 'FUEL', 'LIRN', true, 0, 20, 4, 'FUEL', 'hydrant', '#F5B041', true),
('CAT', 'Catering Truck', 'CATERING', 'LIRN', true, 0, 25, 8, 'CATERING', 'catering', '#7ED321', true),
('GPU', 'Ground Power Unit', 'POWER', 'LIRN', true, 0, 15, 10, 'GPU', 'power', '#9B59B6', true),
('ASU', 'Air Start Unit', 'POWER', 'LIRN', true, 0, 15, 5, 'GPU', 'air', '#8E44AD', true),
('PB', 'Pushback Tug', 'PUSHBACK', 'LIRN', true, 0, 20, 8, 'PUSHBACK', 'pushback', '#E74C3C', true),
('TWB', 'Towbar', 'PUSHBACK', 'LIRN', false, 0, 0, 10, 'PUSHBACK', 'towbar', '#C0392B', false),
('PAX', 'Passenger Bus', 'PASSENGER', 'LIRN', true, 0, 40, 15, 'BUS', 'bus', '#3498DB', true),
('STRS', 'Passenger Stairs', 'PASSENGER', 'LIRN', true, 0, 15, 10, 'STAIRS', 'stairs', '#2980B9', true),
('WATER', 'Water Truck', 'SERVICE', 'LIRN', true, 0, 25, 4, 'SERVICE', 'water', '#1ABC9C', true),
('LAV', 'Lavatory Service', 'SERVICE', 'LIRN', true, 0, 20, 4, 'LAVATORY', 'lavatory', '#16A085', true),
('CARGO', 'Cargo Loader', 'CARGO', 'LIRN', true, 0, 10, 6, 'CARGO', 'cargo', '#95A5A6', true),
('DEICE', 'De-icing Truck', 'DEICE', 'LIRN', true, 0, 25, 2, 'MAINTENANCE', 'snowflake', '#00BCD4', true),
-- YBBN vehicle types
('BAG', 'Baggage Tractor', 'BAGGAGE', 'YBBN', true, 0, 25, 18, 'BAGGAGE', 'truck', '#4A90D9', true),
('BELT', 'Belt Loader', 'BAGGAGE', 'YBBN', true, 0, 10, 10, 'BAGGAGE', 'conveyor', '#5A9BD5', true),
('FUEL', 'Fuel Bowser', 'FUEL', 'YBBN', true, 0, 30, 7, 'FUEL', 'fuel', '#F5A623', true),
('HYDR', 'Hydrant Dispenser', 'FUEL', 'YBBN', true, 0, 20, 5, 'FUEL', 'hydrant', '#F5B041', true),
('CAT', 'Catering Truck', 'CATERING', 'YBBN', true, 0, 25, 10, 'CATERING', 'catering', '#7ED321', true),
('GPU', 'Ground Power Unit', 'POWER', 'YBBN', true, 0, 15, 12, 'GPU', 'power', '#9B59B6', true),
('ASU', 'Air Start Unit', 'POWER', 'YBBN', true, 0, 15, 6, 'GPU', 'air', '#8E44AD', true),
('PB', 'Pushback Tug', 'PUSHBACK', 'YBBN', true, 0, 20, 9, 'PUSHBACK', 'pushback', '#E74C3C', true),
('TWB', 'Towbar', 'PUSHBACK', 'YBBN', false, 0, 0, 12, 'PUSHBACK', 'towbar', '#C0392B', false),
('PAX', 'Passenger Bus', 'PASSENGER', 'YBBN', true, 0, 40, 18, 'BUS', 'bus', '#3498DB', true),
('STRS', 'Passenger Stairs', 'PASSENGER', 'YBBN', true, 0, 15, 11, 'STAIRS', 'stairs', '#2980B9', true),
('WATER', 'Water Truck', 'SERVICE', 'YBBN', true, 0, 25, 5, 'WATER', 'water', '#1ABC9C', true),
('LAV', 'Lavatory Service', 'SERVICE', 'YBBN', true, 0, 20, 5, 'LAVATORY', 'lavatory', '#16A085', true),
('CARGO', 'Cargo Loader', 'CARGO', 'YBBN', true, 0, 10, 8, 'CARGO', 'cargo', '#95A5A6', true),
('DEICE', 'De-icing Truck', 'DEICE', 'YBBN', true, 0, 25, 3, 'MAINTENANCE', 'snowflake', '#00BCD4', true)
ON CONFLICT DO NOTHING;

-- Verify table creation
SELECT 'Created tables:' as message;
SELECT tablename FROM pg_tables WHERE schemaname = 'public' AND tablename LIKE 'simulation_%';
SELECT 'Depots count:' as message, COUNT(*) FROM depots;
SELECT 'Vehicle types count:' as message, COUNT(*) FROM vehicle_types;
