-- =============================================================================
-- 21-fix-simulation-tables-and-trail-links.sql
-- 1. Creates airport_boundaries table with all required JPA columns
-- 2. Backfills asset_id in asset_movement_trail from asset_location_register
-- =============================================================================

-- -----------------------------------------------------------------------
-- 1. airport_boundaries -- full schema required by AirportBoundary JPA entity
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS airport_boundaries (
    id BIGSERIAL PRIMARY KEY,
    tenant_code      VARCHAR(10)  NOT NULL UNIQUE,
    boundary_polygon GEOMETRY(POLYGON, 4326),
    boundary_geojson TEXT,
    name             VARCHAR(200),
    properties       JSONB,
    min_latitude     DOUBLE PRECISION,
    max_latitude     DOUBLE PRECISION,
    min_longitude    DOUBLE PRECISION,
    max_longitude    DOUBLE PRECISION,
    created_at       TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_airport_boundary_tenant ON airport_boundaries(tenant_code);
CREATE INDEX IF NOT EXISTS idx_airport_boundary_geom   ON airport_boundaries USING GIST(boundary_polygon);

-- Add any columns that may be missing if the table already existed
ALTER TABLE airport_boundaries
  ADD COLUMN IF NOT EXISTS boundary_geojson TEXT,
  ADD COLUMN IF NOT EXISTS name             VARCHAR(200),
  ADD COLUMN IF NOT EXISTS properties       JSONB,
  ADD COLUMN IF NOT EXISTS updated_at       TIMESTAMP WITH TIME ZONE DEFAULT NOW();

-- Seed boundary records for each tenant
INSERT INTO airport_boundaries (tenant_code, min_latitude, max_latitude, min_longitude, max_longitude)
VALUES
  ('VIDP', 28.5400, 28.5800, 77.0750, 77.1150),
  ('LIRN', 40.8750, 40.8950, 14.2800, 14.3100),
  ('YBBN', -27.4100, -27.3700, 153.1000, 153.1400)
ON CONFLICT (tenant_code) DO NOTHING;

-- -----------------------------------------------------------------------
-- 2. Backfill asset_id in asset_movement_trail from asset_location_register
--    so the trail API (which queries by asset_id UUID) can find trail points
-- -----------------------------------------------------------------------
UPDATE asset_movement_trail amt
SET asset_id = alr.asset_id
FROM asset_location_register alr
WHERE amt.asset_identifier = alr.asset_identifier
  AND amt.asset_id IS NULL;

DO $$
DECLARE linked INT;
BEGIN
  SELECT COUNT(*) INTO linked FROM asset_movement_trail WHERE asset_id IS NOT NULL;
  RAISE NOTICE 'asset_movement_trail rows with asset_id: %', linked;
END $$;
