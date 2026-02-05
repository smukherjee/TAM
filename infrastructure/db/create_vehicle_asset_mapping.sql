-- ============================================================
-- Create Vehicle-Asset Mapping Table and Populate Assets
-- All vehicles are assets, this creates the link between them
-- ============================================================

-- 1. Create the vehicle_asset_map table (referenced by VehicleAssetMapService.java)
CREATE TABLE IF NOT EXISTS vehicle_asset_map (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_code VARCHAR(4) NOT NULL,
    vehicle_id VARCHAR(50) NOT NULL,
    asset_id UUID NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now(),
    CONSTRAINT fk_vehicle_asset_map_tenant FOREIGN KEY (tenant_code) REFERENCES tenants(code),
    CONSTRAINT fk_vehicle_asset_map_asset FOREIGN KEY (asset_id) REFERENCES assets(id),
    CONSTRAINT uk_vehicle_asset_map UNIQUE (tenant_code, vehicle_id)
);

CREATE INDEX IF NOT EXISTS idx_vehicle_asset_map_vehicle ON vehicle_asset_map(vehicle_id);
CREATE INDEX IF NOT EXISTS idx_vehicle_asset_map_asset ON vehicle_asset_map(asset_id);
CREATE INDEX IF NOT EXISTS idx_vehicle_asset_map_tenant ON vehicle_asset_map(tenant_code);

-- 2. Map vehicle_type to asset category
-- This mapping ensures consistency between vehicle tracking and asset management
CREATE OR REPLACE FUNCTION get_asset_category(v_type VARCHAR) RETURNS VARCHAR AS $$
BEGIN
    RETURN CASE v_type
        WHEN 'ASU' THEN 'Power'
        WHEN 'Ambulift' THEN 'Transport'
        WHEN 'Baggage Cart' THEN 'Baggage'
        WHEN 'Baggage Loader' THEN 'Baggage'
        WHEN 'Baggage Tug' THEN 'Baggage'
        WHEN 'Belt Loader' THEN 'Cargo'
        WHEN 'Bus' THEN 'Transport'
        WHEN 'Cargo Loader' THEN 'Cargo'
        WHEN 'Catering Truck' THEN 'Catering'
        WHEN 'De-icing Truck' THEN 'De-icing'
        WHEN 'Fuel Truck' THEN 'Fueling'
        WHEN 'GPU' THEN 'Power'
        WHEN 'Lavatory Truck' THEN 'Services'
        WHEN 'Pushback' THEN 'Ground Support'
        WHEN 'Stairs' THEN 'Passenger'
        WHEN 'Tug' THEN 'Ground Support'
        WHEN 'Water Truck' THEN 'Services'
        ELSE 'Other'
    END;
END;
$$ LANGUAGE plpgsql;

-- 3. Insert assets for all unique vehicles that don't already exist
-- Each vehicle becomes an asset with proper categorization
INSERT INTO assets (asset_id, name, category, status, tenant_code, description, created_at)
SELECT DISTINCT
    v.vehicle_id AS asset_id,
    COALESCE(
        NULLIF(v.vehicle_name, ''),
        v.vehicle_type || ' - ' || v.vehicle_id
    ) AS name,
    get_asset_category(v.vehicle_type) AS category,
    'Available' AS status,
    v.tenant_code,
    'Auto-generated from vehicle tracking data. Type: ' || v.vehicle_type AS description,
    now()
FROM vehicles v
WHERE NOT EXISTS (
    SELECT 1 FROM assets a 
    WHERE a.asset_id = v.vehicle_id 
      AND a.tenant_code = v.tenant_code
)
ON CONFLICT DO NOTHING;

-- 4. Populate the vehicle_asset_map for all vehicles
INSERT INTO vehicle_asset_map (tenant_code, vehicle_id, asset_id)
SELECT DISTINCT
    v.tenant_code,
    v.vehicle_id,
    a.id
FROM vehicles v
JOIN assets a ON a.asset_id = v.vehicle_id AND a.tenant_code = v.tenant_code
WHERE NOT EXISTS (
    SELECT 1 FROM vehicle_asset_map vam 
    WHERE vam.vehicle_id = v.vehicle_id 
      AND vam.tenant_code = v.tenant_code
)
ON CONFLICT DO NOTHING;

-- 5. Report the results
DO $$
DECLARE
    asset_count INTEGER;
    map_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO asset_count FROM assets;
    SELECT COUNT(*) INTO map_count FROM vehicle_asset_map;
    
    RAISE NOTICE '===========================================';
    RAISE NOTICE 'Vehicle-Asset Mapping Complete';
    RAISE NOTICE '===========================================';
    RAISE NOTICE 'Total Assets: %', asset_count;
    RAISE NOTICE 'Vehicle-Asset Mappings: %', map_count;
    RAISE NOTICE '===========================================';
END $$;

-- Show category distribution
SELECT category, COUNT(*) as count 
FROM assets 
GROUP BY category 
ORDER BY count DESC;
