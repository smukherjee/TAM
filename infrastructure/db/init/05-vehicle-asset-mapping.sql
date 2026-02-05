-- ============================================================
-- Create Vehicle-Asset Mapping Table
-- All vehicles are assets, this creates the link between them
-- ============================================================
-- NOTE: This script only creates the schema. Data population happens
-- at runtime via the backend API (/api/admin/generators/sync-vehicle-assets)
-- after vehicles have been generated.
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
