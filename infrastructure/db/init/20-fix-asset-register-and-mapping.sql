-- =============================================================================
-- 20-fix-asset-register-and-mapping.sql
-- 1. Adds missing column `current_zone_name` to asset_location_register
--    (required by the AssetLocationRegister JPA entity)
-- 2. Ensures every vehicle has a corresponding asset + vehicle_asset_map entry
--    so the asset screen shows all vehicles on the live map.
-- =============================================================================

-- -----------------------------------------------------------------------
-- 1. Add current_zone_name column if it doesn't exist
-- -----------------------------------------------------------------------
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_name = 'asset_location_register'
       AND column_name = 'current_zone_name'
  ) THEN
    ALTER TABLE asset_location_register
      ADD COLUMN current_zone_name VARCHAR(100);
    RAISE NOTICE 'Added current_zone_name column to asset_location_register';
  ELSE
    RAISE NOTICE 'current_zone_name already exists, skipping';
  END IF;

  -- Add columns that the JPA entity expects but may be missing from older schemas
  ALTER TABLE asset_location_register
    ADD COLUMN IF NOT EXISTS heading        DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS speed          DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS status         VARCHAR(50),
    ADD COLUMN IF NOT EXISTS is_authorized  BOOLEAN DEFAULT true,
    ADD COLUMN IF NOT EXISTS restricted_zone_id UUID;

END $$;

-- -----------------------------------------------------------------------
-- 2. Create an asset row + vehicle_asset_map for every vehicle that
--    does not yet have a mapping.
-- -----------------------------------------------------------------------
DO $$
DECLARE
  v RECORD;
  new_asset_id UUID;
  new_asset_code VARCHAR(20);
  inserted_reg  INT := 0;
  inserted_map  INT := 0;
BEGIN
  FOR v IN
    SELECT DISTINCT ON (vehicle_id) vehicle_id, tenant_code, vehicle_type,
           vehicle_name, latitude, longitude, zone, status
      FROM vehicles
     WHERE vehicle_id NOT IN (
             SELECT vehicle_id FROM vehicle_asset_map
           )
     ORDER BY vehicle_id, "timestamp" DESC
  LOOP
    -- Truncate vehicle_id to 20 chars for the asset_id column (VARCHAR(20))
    new_asset_code := LEFT(v.vehicle_id, 20);

    -- Insert into assets using the correct schema
    INSERT INTO assets (
        id, asset_id, name, category, status, tenant_code, created_at, updated_at
    ) VALUES (
        uuid_generate_v4(),
        new_asset_code,
        COALESCE(v.vehicle_name, v.vehicle_id),
        COALESCE(v.vehicle_type, 'Vehicle'),
        COALESCE(v.status, 'Available'),
        v.tenant_code,
        NOW(),
        NOW()
    ) ON CONFLICT (id) DO NOTHING;

    -- Now get the actual asset UUID (may have been inserted or not)
    SELECT id INTO new_asset_id FROM assets WHERE asset_id = new_asset_code AND tenant_code = v.tenant_code LIMIT 1;

    IF new_asset_id IS NOT NULL THEN
      -- Create the vehicle → asset mapping
      INSERT INTO vehicle_asset_map (id, vehicle_id, asset_id, tenant_code, created_at)
      VALUES (uuid_generate_v4(), v.vehicle_id, new_asset_id, v.tenant_code, NOW())
      ON CONFLICT (tenant_code, vehicle_id) DO NOTHING;
      inserted_map := inserted_map + 1;

      -- Upsert into asset_location_register
      INSERT INTO asset_location_register (
          asset_id, asset_identifier, tenant_code,
          current_latitude, current_longitude,
          current_location,
          current_zone, current_zone_name,
          last_movement_at, is_in_restricted_zone, is_authorized_for_zone, last_updated
      ) VALUES (
          new_asset_id,
          v.vehicle_id,
          v.tenant_code,
          v.latitude,
          v.longitude,
          CASE WHEN v.latitude IS NOT NULL AND v.longitude IS NOT NULL
               THEN ST_SetSRID(ST_MakePoint(v.longitude, v.latitude), 4326)
               ELSE NULL
          END,
          v.zone,
          v.zone,
          NOW(),
          false,
          true,
          NOW()
      ) ON CONFLICT (asset_identifier) DO UPDATE
          SET current_latitude   = EXCLUDED.current_latitude,
              current_longitude  = EXCLUDED.current_longitude,
              current_location   = EXCLUDED.current_location,
              current_zone       = EXCLUDED.current_zone,
              current_zone_name  = EXCLUDED.current_zone_name,
              last_updated       = NOW();

      inserted_reg := inserted_reg + 1;
    END IF;
  END LOOP;

  RAISE NOTICE 'Vehicle → Asset sync complete: % location register entries, % map entries created',
    inserted_reg, inserted_map;
END $$;
