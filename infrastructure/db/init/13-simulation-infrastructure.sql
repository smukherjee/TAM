-- TAM Database: Simulation Infrastructure (Layer 13)
-- Scope: Stands, Depots, Airport Boundaries

-- =============================================================================
-- 1. Create stands table
-- =============================================================================
CREATE TABLE IF NOT EXISTS stands (
    id SERIAL PRIMARY KEY,
    tenant_code VARCHAR(10) NOT NULL REFERENCES tenants(code),
    stand_id VARCHAR(10) NOT NULL,
    name VARCHAR(100) NOT NULL,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    terminal_id VARCHAR(10),
    stand_type VARCHAR(20),  -- CONTACT, REMOTE, PUSHBACK
    apron VARCHAR(50),
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT uk_stand_tenant_code UNIQUE (tenant_code, stand_id)
);

CREATE INDEX IF NOT EXISTS idx_stand_tenant_stand ON stands(tenant_code, stand_id);
CREATE INDEX IF NOT EXISTS idx_stand_tenant_terminal ON stands(tenant_code, terminal_id);
CREATE INDEX IF NOT EXISTS idx_stand_tenant_apron ON stands(tenant_code, apron);

-- =============================================================================
-- 2. Create depots table (GSE vehicle storage/dispatch locations)
-- =============================================================================
CREATE TABLE IF NOT EXISTS depots (
    id BIGSERIAL PRIMARY KEY,
    tenant_code VARCHAR(10) NOT NULL REFERENCES tenants(code),
    depot_type VARCHAR(30) NOT NULL,
    name VARCHAR(100) NOT NULL,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    capacity INTEGER NOT NULL DEFAULT 10,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_depot_tenant_type ON depots(tenant_code, depot_type);

-- =============================================================================
-- 3. Create airport_boundaries table
-- =============================================================================
CREATE TABLE IF NOT EXISTS airport_boundaries (
    id BIGSERIAL PRIMARY KEY,
    tenant_code VARCHAR(10) NOT NULL UNIQUE REFERENCES tenants(code),
    boundary_polygon GEOMETRY(POLYGON, 4326),
    min_latitude DOUBLE PRECISION,
    max_latitude DOUBLE PRECISION,
    min_longitude DOUBLE PRECISION,
    max_longitude DOUBLE PRECISION,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_airport_boundary_tenant ON airport_boundaries(tenant_code);
CREATE INDEX IF NOT EXISTS idx_airport_boundary_geom ON airport_boundaries USING GIST(boundary_polygon);

-- =============================================================================
-- 4. Seed Data
-- =============================================================================

-- Seed VIDP (Delhi) Stands
INSERT INTO stands (tenant_code, stand_id, name, latitude, longitude, terminal_id, stand_type, apron) VALUES
('VIDP', '1', 'Stand 1', 28.5575, 77.0980, 'T1', 'CONTACT', 'Apron 1'),
('VIDP', '2', 'Stand 2', 28.5575, 77.0985, 'T1', 'CONTACT', 'Apron 1'),
('VIDP', '3', 'Stand 3', 28.5575, 77.0990, 'T1', 'CONTACT', 'Apron 1'),
('VIDP', '4', 'Stand 4', 28.5575, 77.0995, 'T1', 'CONTACT', 'Apron 1'),
('VIDP', '5', 'Stand 5', 28.5575, 77.1000, 'T1', 'CONTACT', 'Apron 1'),
('VIDP', '101', 'Stand 101', 28.5535, 77.0965, 'T3', 'REMOTE', 'Apron 2')
ON CONFLICT DO NOTHING;

-- Seed VIDP Depots
INSERT INTO depots (tenant_code, depot_type, name, latitude, longitude, capacity) VALUES
('VIDP', 'BAGGAGE', 'Baggage Hall Terminal 3', 28.5566, 77.0939, 20),
('VIDP', 'FUEL', 'Fuel Farm East', 28.5510, 77.1050, 8),
('VIDP', 'CATERING', 'Air India Catering', 28.5480, 77.0900, 12),
('VIDP', 'MAINTENANCE', 'MRO Hangar Complex', 28.5700, 77.0950, 15)
ON CONFLICT DO NOTHING;

-- Seed Airport Boundaries
INSERT INTO airport_boundaries (tenant_code, min_latitude, max_latitude, min_longitude, max_longitude)
VALUES
('VIDP', 28.5400, 28.5800, 77.0750, 77.1150),
('LIRN', 40.8750, 40.8950, 14.2800, 14.3100),
('YBBN', -27.4100, -27.3700, 153.1000, 153.1400)
ON CONFLICT DO NOTHING;
