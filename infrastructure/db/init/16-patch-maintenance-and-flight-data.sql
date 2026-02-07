-- =====================================================================
-- 12-fill-missing-data.sql
-- Purpose: Fill data gaps for specific charts that are showing empty results.
-- 1. v_maintenance_downtime_by_type needs records with status 'Maintenance' or 'Out of Service'.
-- 2. v_flight_movements_hourly needs more than 50 minutes of data for a "Trend" line.
-- =====================================================================

BEGIN;

-- 1. Fix Maintenance Downtime
-- Insert records into asset_movement_trail with 'Maintenance' status
INSERT INTO asset_movement_trail (
    asset_identifier, tenant_code, latitude, longitude,
    speed, heading, zone, status, timestamp
)
SELECT
    'MAINT-' || (gs)::TEXT,
    CASE (gs % 3)
        WHEN 0 THEN 'VIDP'
        WHEN 1 THEN 'LIRN'
        ELSE 'YBBN'
    END,
    CASE (gs % 3)
        WHEN 0 THEN 28.5562
        WHEN 1 THEN 40.8844
        ELSE -27.3842
    END,
    CASE (gs % 3)
        WHEN 0 THEN 77.1000
        WHEN 1 THEN 14.2908
        ELSE 153.1175
    END,
    0, -- Stationary
    0,
    'Maintenance Hangar',
    CASE (gs % 2)
        WHEN 0 THEN 'Maintenance'
        ELSE 'Out of Service'
    END,
    NOW() - (random() * 60 || ' minutes')::INTERVAL
FROM generate_series(1, 20) gs
WHERE NOT EXISTS (
    SELECT 1 FROM asset_movement_trail WHERE asset_identifier LIKE 'MAINT-%'
);

-- 2. Fix Flight Movement Trend
-- Insert flights spread over last 24 hours to populate v_flight_movements_hourly
INSERT INTO flights (
    tenant_code, flight_number, callsign, icao24, origin, destination,
    latitude, longitude, altitude, speed, heading, status, timestamp
)
SELECT
    CASE (gs % 3)
        WHEN 0 THEN 'VIDP'
        WHEN 1 THEN 'LIRN'
        ELSE 'YBBN'
    END,
    'FLT-TR-' || gs,
    'TR-' || gs,
    to_hex(20000 + gs),
    'ORIG', 'DEST',
    0.0, 0.0, 30000, 450, 90,
    'IN_AIR',
    NOW() - ((gs * 15) || ' minutes')::INTERVAL -- Spread over ~12 hours (50 * 15 = 750 mins)
FROM generate_series(1, 100) gs
ON CONFLICT DO NOTHING;

-- Verification
SELECT 'Added Maintenance Records' as check, count(*) FROM asset_movement_trail WHERE status IN ('Maintenance', 'Out of Service');
SELECT 'Added Flight Records' as check, count(*) FROM flights WHERE flight_number LIKE 'FLT-TR-%';

COMMIT;
