-- Fix all simulation tables to match JPA entity definitions
-- This script drops and recreates simulation tables with the correct schema

-- ============================================
-- 1. Drop existing simulation tables
-- ============================================
DROP TABLE IF EXISTS simulation_vehicles CASCADE;
DROP TABLE IF EXISTS simulation_assets CASCADE;
DROP TABLE IF EXISTS simulation_vehicle_positions CASCADE;
DROP TABLE IF EXISTS simulation_asset_positions CASCADE;
DROP TABLE IF EXISTS simulation_dispatches CASCADE;
DROP TABLE IF EXISTS simulation_discrepancies CASCADE;
DROP TABLE IF EXISTS simulation_violations CASCADE;
DROP TABLE IF EXISTS simulation_alerts CASCADE;
DROP TABLE IF EXISTS simulation_financial_metrics CASCADE;
DROP TABLE IF EXISTS airport_boundaries CASCADE;

-- ============================================
-- 2. Create simulation_vehicles table (matches Vehicle.java)
-- ============================================
CREATE TABLE simulation_vehicles (
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

CREATE INDEX idx_sim_vehicle_tenant ON simulation_vehicles(tenant_code);
CREATE INDEX idx_sim_vehicle_status ON simulation_vehicles(status);
CREATE INDEX idx_sim_vehicle_type ON simulation_vehicles(vehicle_type_id);
CREATE INDEX idx_sim_vehicle_updated ON simulation_vehicles(last_updated);

-- ============================================
-- 3. Create simulation_assets table (matches Asset.java)
-- ============================================
CREATE TABLE simulation_assets (
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

CREATE INDEX idx_sim_asset_tenant ON simulation_assets(tenant_code);
CREATE INDEX idx_sim_asset_type ON simulation_assets(asset_type);
CREATE INDEX idx_sim_asset_status ON simulation_assets(status);
CREATE INDEX idx_sim_asset_last_seen ON simulation_assets(last_seen);

-- ============================================
-- 4. Create simulation_dispatches table (matches Dispatch.java)
-- ============================================
CREATE TABLE simulation_dispatches (
    id UUID PRIMARY KEY,
    tenant_code VARCHAR(10) NOT NULL,
    vehicle_id UUID NOT NULL,
    vehicle_type VARCHAR(20) NOT NULL,
    stand_code VARCHAR(10) NOT NULL,
    turnaround_id UUID,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    dispatched_at TIMESTAMP WITH TIME ZONE,
    arrived_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    estimated_duration_seconds INTEGER,
    actual_duration_seconds INTEGER,
    delay_seconds INTEGER,
    priority VARCHAR(20),
    notes VARCHAR(200)
);

CREATE INDEX idx_sim_dispatch_tenant ON simulation_dispatches(tenant_code);
CREATE INDEX idx_sim_dispatch_vehicle ON simulation_dispatches(vehicle_id);
CREATE INDEX idx_sim_dispatch_stand ON simulation_dispatches(stand_code);
CREATE INDEX idx_sim_dispatch_status ON simulation_dispatches(status);
CREATE INDEX idx_sim_dispatch_created ON simulation_dispatches(created_at);

-- ============================================
-- 5. Create simulation_alerts table (matches Alert.java)
-- ============================================
CREATE TABLE simulation_alerts (
    id UUID PRIMARY KEY,
    tenant_code VARCHAR(10) NOT NULL,
    alert_type VARCHAR(30) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    entity_type VARCHAR(20),
    entity_id VARCHAR(30),
    stand_code VARCHAR(10),
    message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    acknowledged_at TIMESTAMP WITH TIME ZONE,
    resolved_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) NOT NULL
);

CREATE INDEX idx_sim_alert_tenant ON simulation_alerts(tenant_code);
CREATE INDEX idx_sim_alert_status ON simulation_alerts(status);
CREATE INDEX idx_sim_alert_type ON simulation_alerts(alert_type);
CREATE INDEX idx_sim_alert_severity ON simulation_alerts(severity);
CREATE INDEX idx_sim_alert_created ON simulation_alerts(created_at);

-- ============================================
-- 6. Create simulation_financial_metrics table (matches FinancialMetric.java)
-- ============================================
CREATE TABLE simulation_financial_metrics (
    id UUID PRIMARY KEY,
    tenant_code VARCHAR(10) NOT NULL,
    metric_type VARCHAR(30) NOT NULL,
    report_date DATE NOT NULL,
    stand_code VARCHAR(10),
    vehicle_type VARCHAR(20),
    flight_number VARCHAR(10),
    turnaround_count INTEGER DEFAULT 0,
    turnarounds_completed INTEGER DEFAULT 0,
    turnarounds_delayed INTEGER DEFAULT 0,
    delay_minutes INTEGER DEFAULT 0,
    total_delay_minutes INTEGER DEFAULT 0,
    delay_cost_usd DOUBLE PRECISION DEFAULT 0,
    total_delay_cost DOUBLE PRECISION DEFAULT 0,
    prevented_delay_minutes INTEGER DEFAULT 0,
    prevented_delay_cost DOUBLE PRECISION DEFAULT 0,
    capacity_gain_value DOUBLE PRECISION DEFAULT 0,
    operational_cost_usd DOUBLE PRECISION DEFAULT 0,
    penalty_cost_usd DOUBLE PRECISION DEFAULT 0,
    total_cost_usd DOUBLE PRECISION DEFAULT 0,
    utilization_percent DOUBLE PRECISION DEFAULT 0,
    sla_compliance_percent DOUBLE PRECISION DEFAULT 0,
    dispatch_count INTEGER DEFAULT 0,
    alert_count INTEGER DEFAULT 0,
    alerts_generated INTEGER DEFAULT 0,
    alerts_resolved INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_sim_financial_tenant ON simulation_financial_metrics(tenant_code);
CREATE INDEX idx_sim_financial_type ON simulation_financial_metrics(metric_type);
CREATE INDEX idx_sim_financial_date ON simulation_financial_metrics(report_date);

-- ============================================
-- 7. Create simulation_discrepancies table (matches MovementDiscrepancy.java)
-- ============================================
CREATE TABLE simulation_discrepancies (
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

CREATE INDEX idx_sim_discrepancy_tenant ON simulation_discrepancies(tenant_code);
CREATE INDEX idx_sim_discrepancy_status ON simulation_discrepancies(status);

-- ============================================
-- 8. Create simulation_violations table (matches Violation.java)
-- ============================================
CREATE TABLE simulation_violations (
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

CREATE INDEX idx_sim_violation_tenant ON simulation_violations(tenant_code);
CREATE INDEX idx_sim_violation_status ON simulation_violations(status);

-- ============================================
-- 9. Create airport_boundaries table
-- ============================================
CREATE TABLE airport_boundaries (
    id BIGSERIAL PRIMARY KEY,
    tenant_code VARCHAR(10) NOT NULL UNIQUE,
    boundary_polygon GEOMETRY(POLYGON, 4326),
    min_latitude DOUBLE PRECISION,
    max_latitude DOUBLE PRECISION,
    min_longitude DOUBLE PRECISION,
    max_longitude DOUBLE PRECISION,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_airport_boundary_tenant ON airport_boundaries(tenant_code);
CREATE INDEX idx_airport_boundary_geom ON airport_boundaries USING GIST(boundary_polygon);

-- Insert airport boundary data for the three airports
INSERT INTO airport_boundaries (tenant_code, min_latitude, max_latitude, min_longitude, max_longitude)
VALUES
-- VIDP (Delhi) - approximate airport boundary
('VIDP', 28.5400, 28.5800, 77.0750, 77.1150),
-- LIRN (Naples) - approximate airport boundary
('LIRN', 40.8750, 40.8950, 14.2800, 14.3100),
-- YBBN (Brisbane) - approximate airport boundary
('YBBN', -27.4100, -27.3700, 153.1000, 153.1400);

-- ============================================
-- Verify table creation
-- ============================================
SELECT 'Simulation tables created:' as message;
SELECT tablename FROM pg_tables WHERE schemaname = 'public' AND tablename LIKE 'simulation_%' ORDER BY tablename;
SELECT 'Airport boundaries:' as message, COUNT(*) FROM airport_boundaries;
