-- TAM Database: Asset Tracking & Security Module (Layer 6)
-- Scope: Restricted Zones, Movement Tracking, Discrepancy Reporting
-- Feature: 005-asset-tracking-security
-- Created: 2026-01-28

-- Prerequisites: PostGIS extension must be enabled
CREATE EXTENSION IF NOT EXISTS postgis CASCADE;

-- 1. RESTRICTED ZONES TABLE
-- Defines secure/prohibited areas with authorization rules
CREATE TABLE IF NOT EXISTS restricted_zones (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    zone_id VARCHAR(50) NOT NULL UNIQUE,
    zone_name VARCHAR(100) NOT NULL,
    zone_type VARCHAR(50) NOT NULL CHECK (zone_type IN ('RESTRICTED', 'PROHIBITED', 'CONTROLLED', 'MAINTENANCE')),
    description TEXT,
    tenant_code VARCHAR(4) REFERENCES tenants(code) NOT NULL,
    geometry GEOMETRY(POLYGON, 4326), -- WGS84 coordinate system
    authorized_asset_categories TEXT[], -- Array of allowed categories
    authorized_asset_ids TEXT[], -- Array of specific allowed asset IDs
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_restricted_zones_tenant ON restricted_zones (tenant_code);
CREATE INDEX idx_restricted_zones_type ON restricted_zones (zone_type);
CREATE INDEX idx_restricted_zones_active ON restricted_zones (is_active) WHERE is_active = TRUE;
CREATE INDEX idx_restricted_zones_geom ON restricted_zones USING GIST (geometry);

COMMENT ON TABLE restricted_zones IS 'Defines restricted/prohibited zones with authorization rules for asset movement';
COMMENT ON COLUMN restricted_zones.zone_type IS 'PROHIBITED=no access, RESTRICTED=authorized only, CONTROLLED=logged access, MAINTENANCE=maintenance vehicles';
COMMENT ON COLUMN restricted_zones.geometry IS 'PostGIS polygon boundary in WGS84 (SRID 4326)';

-- 2. ASSET MOVEMENT TRAIL TABLE (TimescaleDB Hypertable)
-- Captures complete history of asset positions
CREATE TABLE IF NOT EXISTS asset_movement_trail (
    id UUID DEFAULT uuid_generate_v4(),
    asset_id UUID, -- Nullable for unmatched vehicles
    asset_identifier VARCHAR(50) NOT NULL, -- vehicle_id or asset qr_id
    tenant_code VARCHAR(4) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    location GEOMETRY(POINT, 4326), -- PostGIS point
    speed DOUBLE PRECISION, -- km/h
    heading DOUBLE PRECISION, -- degrees 0-360
    zone VARCHAR(100), -- Current zone name
    restricted_zone_id UUID REFERENCES restricted_zones(id),
    altitude DOUBLE PRECISION, -- meters
    status VARCHAR(50),
    metadata JSONB, -- Additional custom data
    timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (id, timestamp)
);

-- Convert to TimescaleDB hypertable
SELECT create_hypertable('asset_movement_trail', 'timestamp', if_not_exists => TRUE);

CREATE INDEX idx_movement_trail_asset ON asset_movement_trail (asset_identifier, timestamp DESC);
CREATE INDEX idx_movement_trail_tenant ON asset_movement_trail (tenant_code, timestamp DESC);
CREATE INDEX idx_movement_trail_zone ON asset_movement_trail (restricted_zone_id, timestamp DESC) WHERE restricted_zone_id IS NOT NULL;
CREATE INDEX idx_movement_trail_location ON asset_movement_trail USING GIST (location);

COMMENT ON TABLE asset_movement_trail IS 'Time-series record of all asset positions and movements';
COMMENT ON COLUMN asset_movement_trail.asset_identifier IS 'vehicle_id from vehicles table or qr_id from assets table';

-- 3. ZONE VIOLATIONS TABLE (TimescaleDB Hypertable)
-- Records unauthorized zone entries
CREATE TABLE IF NOT EXISTS zone_violations (
    id UUID DEFAULT uuid_generate_v4(),
    violation_id VARCHAR(50) NOT NULL,
    asset_id UUID,
    asset_identifier VARCHAR(50) NOT NULL,
    asset_name VARCHAR(100),
    asset_category VARCHAR(50),
    restricted_zone_id UUID REFERENCES restricted_zones(id),
    zone_name VARCHAR(100),
    zone_type VARCHAR(50),
    tenant_code VARCHAR(4) NOT NULL,
    violation_type VARCHAR(50) NOT NULL CHECK (violation_type IN ('UNAUTHORIZED_ENTRY', 'UNAUTHORIZED_CATEGORY', 'PROHIBITED_ZONE')),
    entry_latitude DOUBLE PRECISION,
    entry_longitude DOUBLE PRECISION,
    entry_location GEOMETRY(POINT, 4326),
    duration_seconds INTEGER, -- Time spent in zone
    severity VARCHAR(20) DEFAULT 'MEDIUM' CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    acknowledged BOOLEAN DEFAULT FALSE,
    acknowledged_by UUID REFERENCES users(id),
    acknowledged_at TIMESTAMPTZ,
    resolution_notes TEXT,
    timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (id, timestamp)
);

-- Convert to TimescaleDB hypertable
SELECT create_hypertable('zone_violations', 'timestamp', if_not_exists => TRUE);

CREATE INDEX idx_zone_violations_asset ON zone_violations (asset_identifier, timestamp DESC);
CREATE INDEX idx_zone_violations_tenant ON zone_violations (tenant_code, timestamp DESC);
CREATE INDEX idx_zone_violations_zone ON zone_violations (restricted_zone_id, timestamp DESC);
CREATE INDEX idx_zone_violations_severity ON zone_violations (severity, timestamp DESC);
CREATE INDEX idx_zone_violations_ack ON zone_violations (acknowledged, timestamp DESC) WHERE acknowledged = FALSE;
CREATE INDEX idx_zone_violations_type ON zone_violations (violation_type, timestamp DESC);

COMMENT ON TABLE zone_violations IS 'Records of unauthorized asset entries into restricted zones';
COMMENT ON COLUMN zone_violations.severity IS 'AUTO-ASSIGNED: CRITICAL=prohibited zone, HIGH=restricted unauthorized, MEDIUM=controlled unauthorized, LOW=maintenance after hours';

-- 4. MOVEMENT DISCREPANCIES TABLE (TimescaleDB Hypertable)
-- Detects anomalies in asset movement and location
CREATE TABLE IF NOT EXISTS movement_discrepancies (
    id UUID DEFAULT uuid_generate_v4(),
    discrepancy_id VARCHAR(50) NOT NULL,
    asset_id UUID,
    asset_identifier VARCHAR(50) NOT NULL,
    asset_name VARCHAR(100),
    asset_category VARCHAR(50),
    tenant_code VARCHAR(4) NOT NULL,
    discrepancy_type VARCHAR(50) NOT NULL CHECK (discrepancy_type IN ('UNEXPECTED_MOVEMENT', 'LOCATION_MISMATCH', 'SPEED_ANOMALY', 'MISSING_TRACKING', 'DUPLICATE_SIGNAL')),
    expected_location VARCHAR(100),
    actual_location VARCHAR(100),
    expected_latitude DOUBLE PRECISION,
    expected_longitude DOUBLE PRECISION,
    actual_latitude DOUBLE PRECISION,
    actual_longitude DOUBLE PRECISION,
    distance_deviation_meters DOUBLE PRECISION,
    expected_status VARCHAR(50),
    actual_status VARCHAR(50),
    description TEXT,
    severity VARCHAR(20) DEFAULT 'MEDIUM' CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    acknowledged BOOLEAN DEFAULT FALSE,
    acknowledged_by UUID REFERENCES users(id),
    acknowledged_at TIMESTAMPTZ,
    resolution_notes TEXT,
    timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (id, timestamp)
);

-- Convert to TimescaleDB hypertable
SELECT create_hypertable('movement_discrepancies', 'timestamp', if_not_exists => TRUE);

CREATE INDEX idx_movement_discrepancies_asset ON movement_discrepancies (asset_identifier, timestamp DESC);
CREATE INDEX idx_movement_discrepancies_tenant ON movement_discrepancies (tenant_code, timestamp DESC);
CREATE INDEX idx_movement_discrepancies_type ON movement_discrepancies (discrepancy_type, timestamp DESC);
CREATE INDEX idx_movement_discrepancies_severity ON movement_discrepancies (severity, timestamp DESC);
CREATE INDEX idx_movement_discrepancies_ack ON movement_discrepancies (acknowledged, timestamp DESC) WHERE acknowledged = FALSE;

COMMENT ON TABLE movement_discrepancies IS 'Detects and records anomalies in asset movement, location, and status';
COMMENT ON COLUMN movement_discrepancies.discrepancy_type IS 'UNEXPECTED_MOVEMENT=asset moved while out of service, LOCATION_MISMATCH=register vs GPS, SPEED_ANOMALY=exceeded limits, MISSING_TRACKING=no signal, DUPLICATE_SIGNAL=same asset multiple locations';

-- 5. ASSET LOCATION REGISTER (Current State Snapshot)
-- Maintains current real-time location of each asset
CREATE TABLE IF NOT EXISTS asset_location_register (
    asset_id UUID PRIMARY KEY,
    asset_identifier VARCHAR(50) NOT NULL UNIQUE,
    tenant_code VARCHAR(4) NOT NULL,
    current_latitude DOUBLE PRECISION,
    current_longitude DOUBLE PRECISION,
    current_location GEOMETRY(POINT, 4326),
    current_zone VARCHAR(100),
    current_restricted_zone_id UUID REFERENCES restricted_zones(id),
    last_movement_at TIMESTAMPTZ,
    is_in_restricted_zone BOOLEAN DEFAULT FALSE,
    is_authorized_for_zone BOOLEAN DEFAULT TRUE,
    last_updated TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_asset_location_tenant ON asset_location_register (tenant_code);
CREATE INDEX idx_asset_location_zone ON asset_location_register (current_restricted_zone_id) WHERE current_restricted_zone_id IS NOT NULL;
CREATE INDEX idx_asset_location_in_restricted ON asset_location_register (is_in_restricted_zone) WHERE is_in_restricted_zone = TRUE;
CREATE INDEX idx_asset_location_geom ON asset_location_register USING GIST (current_location);

COMMENT ON TABLE asset_location_register IS 'Real-time snapshot of current asset locations and zone status';

-- 6. HELPER FUNCTION: Match Vehicle ID to Asset ID
CREATE OR REPLACE FUNCTION get_asset_id_from_vehicle(vehicle_identifier VARCHAR)
RETURNS UUID AS $$
DECLARE
    asset_uuid UUID;
BEGIN
    -- Try matching on qr_id (primary identifier)
    SELECT id INTO asset_uuid
    FROM assets
    WHERE qr_id = vehicle_identifier;
    
    -- If not found, try asset_id
    IF asset_uuid IS NULL THEN
        SELECT id INTO asset_uuid
        FROM assets
        WHERE asset_id = vehicle_identifier;
    END IF;
    
    RETURN asset_uuid;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION get_asset_id_from_vehicle IS 'Maps vehicle_id to asset UUID using qr_id or asset_id';

-- 7. HELPER FUNCTION: Check if point is in restricted zone
CREATE OR REPLACE FUNCTION check_zone_authorization(
    p_latitude DOUBLE PRECISION,
    p_longitude DOUBLE PRECISION,
    p_tenant_code VARCHAR,
    p_asset_category VARCHAR,
    p_asset_id VARCHAR
)
RETURNS TABLE (
    zone_id UUID,
    zone_name VARCHAR,
    zone_type VARCHAR,
    is_authorized BOOLEAN,
    violation_type VARCHAR
) AS $$
DECLARE
    point_geom GEOMETRY;
BEGIN
    -- Create PostGIS point
    point_geom := ST_SetSRID(ST_MakePoint(p_longitude, p_latitude), 4326);
    
    RETURN QUERY
    SELECT 
        z.id,
        z.zone_name,
        z.zone_type,
        CASE
            -- PROHIBITED zones: always unauthorized
            WHEN z.zone_type = 'PROHIBITED' THEN FALSE
            -- Check if asset ID is in exceptions list
            WHEN p_asset_id = ANY(z.authorized_asset_ids) THEN TRUE
            -- Check if asset category is authorized
            WHEN p_asset_category = ANY(z.authorized_asset_categories) THEN TRUE
            -- Default: unauthorized
            ELSE FALSE
        END as is_authorized,
        CASE
            WHEN z.zone_type = 'PROHIBITED' THEN 'PROHIBITED_ZONE'
            WHEN NOT (p_asset_category = ANY(z.authorized_asset_categories)) THEN 'UNAUTHORIZED_CATEGORY'
            ELSE 'UNAUTHORIZED_ENTRY'
        END as violation_type
    FROM restricted_zones z
    WHERE z.tenant_code = p_tenant_code
      AND z.is_active = TRUE
      AND ST_Contains(z.geometry, point_geom);
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION check_zone_authorization IS 'Returns zones containing the point and authorization status';

-- 8. CONTINUOUS AGGREGATE: Zone Violations Hourly Summary
CREATE MATERIALIZED VIEW IF NOT EXISTS zone_violations_hourly
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 hour', timestamp) AS bucket,
    tenant_code,
    zone_name,
    zone_type,
    violation_type,
    COUNT(*) AS violation_count,
    COUNT(DISTINCT asset_identifier) AS unique_assets,
    AVG(duration_seconds) AS avg_duration_seconds,
    MAX(duration_seconds) AS max_duration_seconds
FROM zone_violations
GROUP BY bucket, tenant_code, zone_name, zone_type, violation_type
WITH NO DATA;

-- Refresh policy: aggregate data from 3 hours ago to 1 hour ago, run every hour
SELECT add_continuous_aggregate_policy('zone_violations_hourly',
    start_offset => INTERVAL '3 hours',
    end_offset => INTERVAL '1 hour',
    schedule_interval => INTERVAL '1 hour',
    if_not_exists => TRUE);

-- Note: zone_violations_hourly is a TimescaleDB continuous aggregate (hourly aggregation of zone violations for analytics)

-- 9. CONTINUOUS AGGREGATE: Movement Discrepancies Daily Summary
CREATE MATERIALIZED VIEW IF NOT EXISTS movement_discrepancies_daily
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 day', timestamp) AS bucket,
    tenant_code,
    discrepancy_type,
    asset_category,
    COUNT(*) AS discrepancy_count,
    COUNT(DISTINCT asset_identifier) AS unique_assets,
    AVG(distance_deviation_meters) AS avg_deviation_meters,
    MAX(distance_deviation_meters) AS max_deviation_meters,
    COUNT(*) FILTER (WHERE acknowledged = TRUE) AS acknowledged_count,
    COUNT(*) FILTER (WHERE severity = 'CRITICAL') AS critical_count
FROM movement_discrepancies
GROUP BY bucket, tenant_code, discrepancy_type, asset_category
WITH NO DATA;

-- Refresh policy: aggregate data from 7 days ago to 1 day ago, run daily
SELECT add_continuous_aggregate_policy('movement_discrepancies_daily',
    start_offset => INTERVAL '7 days',
    end_offset => INTERVAL '1 day',
    schedule_interval => INTERVAL '1 day',
    if_not_exists => TRUE);

-- Note: movement_discrepancies_daily is a TimescaleDB continuous aggregate (daily aggregation of movement discrepancies for trend analysis)

-- 10. SEED DATA: Restricted Zones for VIDP (Delhi)
INSERT INTO restricted_zones (zone_id, zone_name, zone_type, description, tenant_code, geometry, authorized_asset_categories) VALUES
(
    'VIDP-RZ-001',
    'Runway 09/27 Safety Zone',
    'PROHIBITED',
    'Active runway area - no ground vehicles allowed during operations',
    'VIDP',
    ST_GeomFromText('POLYGON((77.0850 28.5560, 77.1050 28.5560, 77.1050 28.5510, 77.0850 28.5510, 77.0850 28.5560))', 4326),
    ARRAY[]::TEXT[] -- No vehicles allowed
),
(
    'VIDP-RZ-002',
    'Fuel Storage Area',
    'RESTRICTED',
    'Fuel storage and distribution - authorized vehicles only',
    'VIDP',
    ST_GeomFromText('POLYGON((77.0900 28.5580, 77.0950 28.5580, 77.0950 28.5600, 77.0900 28.5600, 77.0900 28.5580))', 4326),
    ARRAY['Fueling', 'Emergency']
),
(
    'VIDP-RZ-003',
    'Cargo Security Zone',
    'CONTROLLED',
    'High-value cargo area - authorized cargo vehicles only',
    'VIDP',
    ST_GeomFromText('POLYGON((77.0820 28.5520, 77.0870 28.5520, 77.0870 28.5540, 77.0820 28.5540, 77.0820 28.5520))', 4326),
    ARRAY['Cargo', 'Emergency']
),
(
    'VIDP-RZ-004',
    'Maintenance Hangar Area',
    'RESTRICTED',
    'Aircraft maintenance area - maintenance vehicles only',
    'VIDP',
    ST_GeomFromText('POLYGON((77.0950 28.5620, 77.1000 28.5620, 77.1000 28.5640, 77.0950 28.5640, 77.0950 28.5620))', 4326),
    ARRAY['Ground Support', 'Power', 'Emergency']
)
ON CONFLICT (zone_id) DO NOTHING;

-- 11. SEED DATA: Restricted Zones for LIRN (Naples)
INSERT INTO restricted_zones (zone_id, zone_name, zone_type, description, tenant_code, geometry, authorized_asset_categories) VALUES
(
    'LIRN-RZ-001',
    'Runway 06/24 Safety Zone',
    'PROHIBITED',
    'Active runway - no ground vehicles during operations',
    'LIRN',
    ST_GeomFromText('POLYGON((14.2800 40.8850, 14.3000 40.8850, 14.3000 40.8800, 14.2800 40.8800, 14.2800 40.8850))', 4326),
    ARRAY[]::TEXT[]
),
(
    'LIRN-RZ-002',
    'VIP Terminal Area',
    'RESTRICTED',
    'VIP and government aircraft area',
    'LIRN',
    ST_GeomFromText('POLYGON((14.2850 40.8900, 14.2900 40.8900, 14.2900 40.8920, 14.2850 40.8920, 14.2850 40.8900))', 4326),
    ARRAY['Transport', 'Emergency']
)
ON CONFLICT (zone_id) DO NOTHING;

-- 12. SEED DATA: Restricted Zones for YBBN (Brisbane)
INSERT INTO restricted_zones (zone_id, zone_name, zone_type, description, tenant_code, geometry, authorized_asset_categories) VALUES
(
    'YBBN-RZ-001',
    'Runway 01/19 Safety Zone',
    'PROHIBITED',
    'Primary runway safety area',
    'YBBN',
    ST_GeomFromText('POLYGON((153.1100 -27.3840, 153.1200 -27.3840, 153.1200 -27.3900, 153.1100 -27.3900, 153.1100 -27.3840))', 4326),
    ARRAY[]::TEXT[]
),
(
    'YBBN-RZ-002',
    'International Terminal Secure Zone',
    'CONTROLLED',
    'International flights security area',
    'YBBN',
    ST_GeomFromText('POLYGON((153.1150 -27.3950, 153.1180 -27.3950, 153.1180 -27.3970, 153.1150 -27.3970, 153.1150 -27.3950))', 4326),
    ARRAY['Transport', 'Cargo', 'Emergency']
)
ON CONFLICT (zone_id) DO NOTHING;

-- 13. DATA RETENTION POLICY: Archive old movement trail data
-- Keep raw data for 90 days, then compress
SELECT add_retention_policy('asset_movement_trail', INTERVAL '90 days', if_not_exists => TRUE);

-- Enable compression on movement trail (compress data older than 7 days)
ALTER TABLE asset_movement_trail SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'tenant_code, asset_identifier',
    timescaledb.compress_orderby = 'timestamp DESC'
);

SELECT add_compression_policy('asset_movement_trail', INTERVAL '7 days', if_not_exists => TRUE);

-- 14. GRANT PERMISSIONS
-- Ensure application user has necessary permissions
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'tam_app') THEN
        GRANT SELECT, INSERT, UPDATE ON restricted_zones TO tam_app;
        GRANT SELECT, INSERT ON asset_movement_trail TO tam_app;
        GRANT SELECT, INSERT, UPDATE ON zone_violations TO tam_app;
        GRANT SELECT, INSERT, UPDATE ON movement_discrepancies TO tam_app;
        GRANT SELECT, INSERT, UPDATE, DELETE ON asset_location_register TO tam_app;
        GRANT SELECT ON zone_violations_hourly TO tam_app;
        GRANT SELECT ON movement_discrepancies_daily TO tam_app;
    END IF;
END $$;

-- 15. VERIFICATION QUERIES
-- Uncomment to test after migration

-- SELECT 'Restricted zones created:', COUNT(*) FROM restricted_zones;
-- SELECT 'Testing spatial query...', zone_name FROM restricted_zones WHERE ST_Contains(geometry, ST_SetSRID(ST_MakePoint(77.0925, 28.5590), 4326)) LIMIT 1;
-- SELECT 'Testing authorization function...', * FROM check_zone_authorization(28.5590, 77.0925, 'VIDP', 'Fueling', 'SAM-0005');

COMMIT;
