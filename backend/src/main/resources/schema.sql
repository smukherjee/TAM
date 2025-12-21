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


-- ==========================================
-- Turnaround Refactoring Tables (Phase 6)
-- ==========================================

-- 1. Turnarounds Table
CREATE TABLE IF NOT EXISTS turnarounds (
    id VARCHAR(50) PRIMARY KEY,
    stand_id VARCHAR(20) NOT NULL,
    terminal VARCHAR(10) NOT NULL,
    
    -- Aircraft Information
    aircraft_registration VARCHAR(20) NOT NULL,
    aircraft_type VARCHAR(50) NOT NULL,
    airline VARCHAR(100),
    
    -- Inbound Flight
    inbound_flight_number VARCHAR(20),
    inbound_origin VARCHAR(4),
    inbound_scheduled_arrival TIMESTAMPTZ,
    inbound_actual_arrival TIMESTAMPTZ,
    inbound_estimated_arrival TIMESTAMPTZ,
    inbound_status VARCHAR(20),
    inbound_passengers INTEGER,
    inbound_cargo_kg DECIMAL(10,2),
    
    -- Outbound Flight
    outbound_flight_number VARCHAR(20),
    outbound_destination VARCHAR(4),
    outbound_scheduled_departure TIMESTAMPTZ NOT NULL,
    outbound_estimated_departure TIMESTAMPTZ,
    outbound_actual_departure TIMESTAMPTZ,
    outbound_status VARCHAR(20),
    outbound_passengers INTEGER,
    outbound_cargo_kg DECIMAL(10,2),
    
    -- Timing
    blocks_on TIMESTAMPTZ,
    blocks_off TIMESTAMPTZ,
    planned_turnaround_time INTEGER, -- minutes
    actual_turnaround_time INTEGER, -- minutes
    buffer_time INTEGER, -- minutes
    
    -- Status & Metrics
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    on_time_status VARCHAR(20) DEFAULT 'ON_TIME',
    progress_percentage INTEGER DEFAULT 0,
    alert_count INTEGER DEFAULT 0,
    
    -- Audit
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50)
);

-- Turnarounds Hypertable
SELECT create_hypertable('turnarounds', 'outbound_scheduled_departure', if_not_exists => TRUE, migrate_data => TRUE);

-- 2. Turnaround Activities Table
CREATE TABLE IF NOT EXISTS turnaround_activities (
    id VARCHAR(50) PRIMARY KEY,
    turnaround_id VARCHAR(50) NOT NULL,
    
    -- Activity Details
    activity_type VARCHAR(50) NOT NULL,
    activity_name VARCHAR(100) NOT NULL,
    activity_category VARCHAR(20) NOT NULL,
    
    -- Timing
    planned_start_time TIMESTAMPTZ NOT NULL,
    planned_end_time TIMESTAMPTZ NOT NULL,
    actual_start_time TIMESTAMPTZ,
    actual_end_time TIMESTAMPTZ,
    
    -- Status & Metrics
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    is_critical_path BOOLEAN DEFAULT FALSE,
    duration_minutes INTEGER,
    variance_minutes INTEGER DEFAULT 0,
    
    -- Resources
    assigned_crew JSONB DEFAULT '[]'::jsonb,
    assigned_equipment JSONB DEFAULT '[]'::jsonb,
    dependencies JSONB DEFAULT '[]'::jsonb,
    
    -- UI/Display
    display_color VARCHAR(7) DEFAULT '#6B7280',
    display_order INTEGER DEFAULT 0,
    
    -- Audit
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_turnaround FOREIGN KEY (turnaround_id) REFERENCES turnarounds(id) ON DELETE CASCADE
);

-- 3. Stands Table
CREATE TABLE IF NOT EXISTS stands (
    id VARCHAR(20) PRIMARY KEY,
    terminal VARCHAR(10) NOT NULL,
    
    -- Classification
    stand_type VARCHAR(20) NOT NULL,
    classification VARCHAR(30) NOT NULL,
    
    -- Equipment
    has_jetbridge BOOLEAN DEFAULT FALSE,
    has_gpu BOOLEAN DEFAULT FALSE,
    has_asu BOOLEAN DEFAULT FALSE,
    has_pca BOOLEAN DEFAULT FALSE,
    has_water_service BOOLEAN DEFAULT FALSE,
    has_waste_service BOOLEAN DEFAULT FALSE,
    has_fuel_hydrant BOOLEAN DEFAULT FALSE,
    
    -- Dimensions
    length_meters DECIMAL(5,2),
    width_meters DECIMAL(5,2),
    max_wingspan_meters DECIMAL(5,2),
    max_aircraft_length_meters DECIMAL(5,2),
    
    -- Operational
    status VARCHAR(20) DEFAULT 'AVAILABLE',
    current_turnaround_id VARCHAR(50),
    next_available_time TIMESTAMPTZ,
    
    -- Restrictions
    aircraft_restrictions JSONB DEFAULT '[]'::jsonb,
    operational_restrictions TEXT,
    
    -- Audit
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_current_turnaround FOREIGN KEY (current_turnaround_id) REFERENCES turnarounds(id) ON DELETE SET NULL
);

-- 4. Turnaround Alerts Table
CREATE TABLE IF NOT EXISTS turnaround_alerts (
    id VARCHAR(50) PRIMARY KEY,
    turnaround_id VARCHAR(50) NOT NULL,
    activity_id VARCHAR(50),
    
    -- Alert Details
    severity VARCHAR(20) NOT NULL,
    alert_type VARCHAR(50) NOT NULL,
    category VARCHAR(30) NOT NULL,
    message TEXT NOT NULL,
    details JSONB,
    
    -- Lifecycle
    timestamp TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    acknowledged BOOLEAN DEFAULT FALSE,
    acknowledged_by VARCHAR(50),
    acknowledged_at TIMESTAMPTZ,
    resolved BOOLEAN DEFAULT FALSE,
    resolved_at TIMESTAMPTZ,
    resolution_note TEXT,
    
    -- Audit
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_alert_turnaround FOREIGN KEY (turnaround_id) REFERENCES turnarounds(id) ON DELETE CASCADE,
    CONSTRAINT fk_alert_activity FOREIGN KEY (activity_id) REFERENCES turnaround_activities(id) ON DELETE SET NULL
);

-- Alerts Hypertable
SELECT create_hypertable('turnaround_alerts', 'timestamp', if_not_exists => TRUE);

-- ==========================================
-- Seed Data
-- ==========================================

-- Seed Stands
INSERT INTO stands (id, terminal, stand_type, classification, has_jetbridge, has_gpu, has_asu, has_pca) VALUES
('A12', 'T1', 'CONTACT', 'NARROW_BODY', true, true, true, true),
('B05', 'T1', 'CONTACT', 'NARROW_BODY', true, true, true, true),
('C09', 'T2', 'CONTACT', 'WIDE_BODY', true, true, true, true),
('D14', 'T3', 'CONTACT', 'NARROW_BODY', true, true, true, true),
('E02', 'T3', 'CONTACT', 'WIDE_BODY', true, true, true, true),
('F11', 'T2', 'CONTACT', 'WIDE_BODY', true, true, true, true),
('G07', 'T3', 'CONTACT', 'WIDE_BODY', true, true, true, true),
('B08', 'T3', 'CONTACT', 'NARROW_BODY', true, true, true, true),
('B09', 'T2', 'REMOTE', 'NARROW_BODY', false, true, false, false)
ON CONFLICT (id) DO NOTHING;
