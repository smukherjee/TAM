-- ============================================================================
-- Update Asset Statuses Based on Vehicle Tracking Link
-- ============================================================================
-- Rules:
-- 1. Assets linked to vehicles (via vehicle_asset_map) → "In Use" (actively tracked)
-- 2. Standalone assets (not linked) → Random split between "Available" and "Maintenance"
-- 3. Location: "In Use" keeps current location, others get random location
-- ============================================================================

-- ============================================================================
-- Step 1: Set all vehicle-linked assets to "In Use"
-- ============================================================================
UPDATE assets a
SET 
    status = 'In Use',
    updated_at = NOW()
WHERE EXISTS (
    SELECT 1 FROM vehicle_asset_map vam WHERE vam.asset_id = a.id
);

-- ============================================================================
-- Step 2: Set standalone assets to random "Available" or "Maintenance"
-- Split roughly 70% Available, 30% Maintenance
-- Uses PL/pgSQL loop to ensure truly random values per row
-- ============================================================================
DO $$
DECLARE
    asset_rec RECORD;
    locations TEXT[] := ARRAY[
        'Terminal 1', 'Terminal 2', 'Terminal 3', 
        'Apron A', 'Apron B', 'Apron C',
        'Cargo Area', 'Fuel Station', 'Service Area',
        'Hangar 1', 'Hangar 2', 'Maintenance Bay',
        'Gate A1', 'Gate A2', 'Gate B1', 'Gate B2',
        'Remote Stand R1', 'Remote Stand R2', 'Remote Stand R3',
        'Equipment Yard', 'Ground Support Depot'
    ];
    maintenance_descriptions TEXT[] := ARRAY[
        'Scheduled maintenance - Expected return soon',
        'Oil change due',
        'Tire replacement in progress',
        'Annual safety inspection',
        'Brake service required',
        'Engine diagnostic check',
        'Mandatory safety inspection',
        'Battery replacement pending',
        'Filter and fluid change scheduled'
    ];
    rand_location_idx INT;
    rand_status FLOAT;
    new_status TEXT;
    new_desc TEXT;
BEGIN
    RAISE NOTICE 'Updating standalone asset statuses and locations...';
    
    FOR asset_rec IN 
        SELECT a.id, a.asset_id, a.name
        FROM assets a
        LEFT JOIN vehicle_asset_map vam ON a.id = vam.asset_id
        WHERE vam.id IS NULL  -- Not linked to any vehicle
    LOOP
        -- Random status: 70% Available, 30% Maintenance
        rand_status := random();
        IF rand_status < 0.7 THEN
            new_status := 'Available';
            new_desc := NULL;
        ELSE
            new_status := 'Maintenance';
            new_desc := maintenance_descriptions[1 + floor(random() * array_length(maintenance_descriptions, 1))::int];
            -- Append expected return date for some maintenance items
            IF random() > 0.5 THEN
                new_desc := new_desc || ' - Due: ' || (CURRENT_DATE + (floor(random() * 14) + 1)::int)::text;
            END IF;
        END IF;
        
        -- Random location from the list
        rand_location_idx := 1 + floor(random() * array_length(locations, 1))::int;
        
        UPDATE assets 
        SET 
            status = new_status,
            location = locations[rand_location_idx],
            description = COALESCE(new_desc, description),
            updated_at = NOW()
        WHERE id = asset_rec.id;
    END LOOP;
    
    RAISE NOTICE 'Asset status update complete.';
END $$;

-- ============================================================================
-- Verification: Show the distribution of asset statuses
-- ============================================================================
SELECT 
    status,
    COUNT(*) as count,
    ROUND(COUNT(*) * 100.0 / SUM(COUNT(*)) OVER(), 1) as percentage
FROM assets
GROUP BY status
ORDER BY count DESC;

-- Show sample of each status type
SELECT '=== Standalone Assets (Available/Maintenance) ===' as info;

SELECT 
    a.asset_id, 
    a.name, 
    a.status, 
    a.location,
    LEFT(a.description, 50) as description
FROM assets a
LEFT JOIN vehicle_asset_map vam ON a.id = vam.asset_id
WHERE vam.id IS NULL
ORDER BY a.status, a.asset_id;

SELECT '=== Sample In Use Assets (Vehicle-Tracked) ===' as info;

SELECT 
    a.asset_id, 
    a.name, 
    a.status,
    vam.vehicle_id as tracked_vehicle
FROM assets a
JOIN vehicle_asset_map vam ON a.id = vam.asset_id
LIMIT 10;
