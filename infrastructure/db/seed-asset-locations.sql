-- Seed asset location data for testing
-- Inserts sample location data for existing YBBN assets

-- First, let's get the existing YBBN assets and insert their locations
INSERT INTO asset_location_register (
    asset_id,
    asset_identifier,
    tenant_code,
    current_latitude,
    current_longitude,
    current_location,
    last_updated,
    is_in_restricted_zone,
    last_movement_at
)
SELECT 
    a.id,
    a.asset_id,
    a.tenant_code,
    -27.3840 + (RANDOM() - 0.5) * 0.01 AS current_latitude,   -- Latitude around Brisbane Airport
    153.1175 + (RANDOM() - 0.5) * 0.01 AS current_longitude,  -- Longitude around Brisbane Airport
    ST_SetSRID(ST_MakePoint(
        153.1175 + (RANDOM() - 0.5) * 0.01,
        -27.3840 + (RANDOM() - 0.5) * 0.01
    ), 4326) AS current_location,
    NOW() - (RANDOM() * INTERVAL '5 minutes') AS last_updated,
    RANDOM() > 0.8 AS is_in_restricted_zone,  -- 20% chance of being in restricted zone
    NOW() - (RANDOM() * INTERVAL '2 minutes') AS last_movement_at
FROM assets a
WHERE a.tenant_code = 'YBBN'
ON CONFLICT (asset_identifier) DO UPDATE SET
    current_latitude = EXCLUDED.current_latitude,
    current_longitude = EXCLUDED.current_longitude,
    current_location = EXCLUDED.current_location,
    last_updated = EXCLUDED.last_updated,
    is_in_restricted_zone = EXCLUDED.is_in_restricted_zone,
    last_movement_at = EXCLUDED.last_movement_at;

-- Insert some movement trail data
INSERT INTO asset_movement_trail (
    asset_identifier,
    location,
    speed,
    heading,
    timestamp
)
SELECT 
    a.asset_id,
    ST_SetSRID(ST_MakePoint(
        153.1175 + (RANDOM() - 0.5) * 0.01,
        -27.3840 + (RANDOM() - 0.5) * 0.01
    ), 4326) AS location,
    RANDOM() * 50 AS speed,  -- Random speed 0-50 km/h
    RANDOM() * 360 AS heading,  -- Random direction
    NOW() - (RANDOM() * INTERVAL '10 minutes') AS timestamp
FROM assets a
WHERE a.tenant_code = 'YBBN';

-- Verify the data
SELECT 
    COUNT(*) as total_locations,
    COUNT(DISTINCT asset_identifier) as unique_assets
FROM asset_location_register;

SELECT 
    a.asset_id,
    a.name,
    ST_Y(alr.current_location) as latitude,
    ST_X(alr.current_location) as longitude,
    alr.last_updated,
    alr.is_in_restricted_zone
FROM assets a
JOIN asset_location_register alr ON a.asset_id = alr.asset_identifier
WHERE a.tenant_code = 'YBBN'
ORDER BY alr.last_updated DESC
LIMIT 5;
