-- TAM Database: Admin Zones & Vehicle Paths Module (Layer 8)
-- Scope: Admin-defined zones for geofencing, vehicle path definitions
-- Feature: Admin zone editor, path editor

-- Prerequisites: Core platform tables must exist
-- Depends on: 01-core-platform.sql (tenants table)

-- 1. ZONES TABLE (Admin-defined zones for UI/geofencing)
-- Different from restricted_zones which is for security violations
CREATE TABLE IF NOT EXISTS zones (
    id SERIAL PRIMARY KEY,
    tenant_code VARCHAR(10) NOT NULL REFERENCES tenants(code),
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    type VARCHAR(30) NOT NULL, -- APRON, TERMINAL, GATE, CARGO, MAINTENANCE, RESTRICTED
    
    -- GeoJSON/Polygon data
    geojson TEXT,
    polygon TEXT,
    
    -- Styling
    fill_color VARCHAR(20),
    stroke_color VARCHAR(20),
    fill_opacity DOUBLE PRECISION,
    color VARCHAR(20),
    opacity DOUBLE PRECISION,
    
    -- Access control
    restricted BOOLEAN DEFAULT FALSE,
    active BOOLEAN DEFAULT TRUE,
    allowed_vehicle_types TEXT, -- JSON array as string
    allowed_roles TEXT, -- JSON array as string
    access_schedule VARCHAR(500), -- JSON schedule as string
    
    -- Alert configuration
    alert_on_entry BOOLEAN DEFAULT FALSE,
    alert_on_exit BOOLEAN DEFAULT FALSE,
    dwell_time_alert_minutes INTEGER,
    
    -- Audit
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    
    -- Unique constraint per tenant
    CONSTRAINT uk_zone_tenant_code UNIQUE (tenant_code, code)
);

CREATE INDEX IF NOT EXISTS idx_zones_tenant ON zones (tenant_code);
CREATE INDEX IF NOT EXISTS idx_zones_type ON zones (type);
CREATE INDEX IF NOT EXISTS idx_zones_active ON zones (active) WHERE active = TRUE;

COMMENT ON TABLE zones IS 'Admin-defined zones for UI display and geofencing rules';
COMMENT ON COLUMN zones.type IS 'APRON, TERMINAL, GATE, CARGO, MAINTENANCE, RESTRICTED';

-- 2. VEHICLE PATHS TABLE (Admin-defined paths for vehicle simulation)
CREATE TABLE IF NOT EXISTS vehicle_paths (
    id SERIAL PRIMARY KEY,
    tenant_code VARCHAR(10) NOT NULL REFERENCES tenants(code),
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    vehicle_type_code VARCHAR(20), -- Links to vehicle type
    
    -- Path data
    waypoints TEXT NOT NULL, -- JSON array of coordinates
    schedule TEXT, -- JSON: {"startTime": "06:00", "endTime": "23:00", "interval": 300}
    
    -- Path properties
    active BOOLEAN DEFAULT TRUE,
    loop BOOLEAN DEFAULT FALSE,
    total_distance_km DOUBLE PRECISION,
    estimated_duration_seconds INTEGER,
    
    -- Audit
    created_by VARCHAR(50),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_vehicle_paths_tenant ON vehicle_paths (tenant_code);
CREATE INDEX IF NOT EXISTS idx_vehicle_paths_vehicle_type ON vehicle_paths (vehicle_type_code);
CREATE INDEX IF NOT EXISTS idx_vehicle_paths_active ON vehicle_paths (tenant_code, active);

COMMENT ON TABLE vehicle_paths IS 'Admin-defined paths for vehicle movement simulation';
COMMENT ON COLUMN vehicle_paths.waypoints IS 'JSON array of {lat, lng, timestamp?, speed?, heading?} objects';

-- 3. VEHICLE TYPES REFERENCE TABLE
CREATE TABLE IF NOT EXISTS vehicle_types (
    id SERIAL PRIMARY KEY,
    tenant_code VARCHAR(10) NOT NULL REFERENCES tenants(code),
    code VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    category VARCHAR(50), -- Ground Support, Emergency, Cargo, Fueling, etc.
    icon VARCHAR(100),
    color VARCHAR(20),
    max_speed_kmh DOUBLE PRECISION,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW(),
    
    CONSTRAINT uk_vehicle_type_tenant_code UNIQUE (tenant_code, code)
);

CREATE INDEX IF NOT EXISTS idx_vehicle_types_tenant ON vehicle_types (tenant_code);

COMMENT ON TABLE vehicle_types IS 'Reference table for vehicle type definitions';

-- 4. SEED DEFAULT VEHICLE TYPES
INSERT INTO vehicle_types (tenant_code, code, name, category, max_speed) VALUES
('VIDP', 'PUSHBACK', 'Pushback Tractor', 'Ground Support', 25),
('VIDP', 'FUEL', 'Fuel Truck', 'Fueling', 30),
('VIDP', 'CARGO', 'Cargo Loader', 'Cargo', 20),
('VIDP', 'CATERING', 'Catering Truck', 'Services', 25),
('VIDP', 'BAGGAGE', 'Baggage Cart', 'Ground Support', 20),
('VIDP', 'EMERGENCY', 'Emergency Vehicle', 'Emergency', 50),
('VIDP', 'BUS', 'Passenger Bus', 'Transport', 30),
('VIDP', 'GPU', 'Ground Power Unit', 'Power', 15),
('VIDP', 'WATER', 'Water Truck', 'Services', 25),
('VIDP', 'LAVATORY', 'Lavatory Truck', 'Services', 25),
('LIRN', 'PUSHBACK', 'Pushback Tractor', 'Ground Support', 25),
('LIRN', 'FUEL', 'Fuel Truck', 'Fueling', 30),
('LIRN', 'CARGO', 'Cargo Loader', 'Cargo', 20),
('LIRN', 'EMERGENCY', 'Emergency Vehicle', 'Emergency', 50),
('YBBN', 'PUSHBACK', 'Pushback Tractor', 'Ground Support', 25),
('YBBN', 'FUEL', 'Fuel Truck', 'Fueling', 30),
('YBBN', 'CARGO', 'Cargo Loader', 'Cargo', 20),
('YBBN', 'EMERGENCY', 'Emergency Vehicle', 'Emergency', 50)
ON CONFLICT (tenant_code, code) DO NOTHING;

-- 5. SEED SAMPLE ZONES FOR VIDP
INSERT INTO zones (tenant_code, code, name, type, color, opacity, active) VALUES
('VIDP', 'APRON-1', 'Apron 1', 'APRON', '#3B82F6', 0.3, true),
('VIDP', 'APRON-2', 'Apron 2', 'APRON', '#3B82F6', 0.3, true),
('VIDP', 'T1', 'Terminal 1', 'TERMINAL', '#10B981', 0.3, true),
('VIDP', 'T2', 'Terminal 2', 'TERMINAL', '#10B981', 0.3, true),
('VIDP', 'T3', 'Terminal 3', 'TERMINAL', '#10B981', 0.3, true),
('VIDP', 'CARGO-1', 'Cargo Area 1', 'CARGO', '#F59E0B', 0.3, true),
('VIDP', 'MAINT-1', 'Maintenance Hangar', 'MAINTENANCE', '#8B5CF6', 0.3, true),
('VIDP', 'FUEL-1', 'Fuel Farm', 'RESTRICTED', '#EF4444', 0.4, true)
ON CONFLICT (tenant_code, code) DO NOTHING;

-- 6. GRANT PERMISSIONS
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'tam_app') THEN
        GRANT SELECT, INSERT, UPDATE, DELETE ON zones TO tam_app;
        GRANT SELECT, INSERT, UPDATE, DELETE ON vehicle_paths TO tam_app;
        GRANT SELECT, INSERT, UPDATE, DELETE ON vehicle_types TO tam_app;
        GRANT USAGE, SELECT ON SEQUENCE zones_id_seq TO tam_app;
        GRANT USAGE, SELECT ON SEQUENCE vehicle_paths_id_seq TO tam_app;
        GRANT USAGE, SELECT ON SEQUENCE vehicle_types_id_seq TO tam_app;
    END IF;
END $$;

COMMIT;
