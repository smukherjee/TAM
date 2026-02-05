-- =============================================================================
-- Normalize Vehicle Type Categories
-- =============================================================================
-- This script normalizes inconsistent vehicle category names to use consistent
-- uppercase naming convention and removes duplicate entries.
-- 
-- Inconsistencies fixed:
-- - Fueling -> FUEL
-- - Power -> POWER  
-- - Services -> SERVICE
-- - Ground Support -> GROUND_SUPPORT -> moved to appropriate category
-- - Emergency -> EMERGENCY
-- - Cargo -> CARGO
-- - Transport -> PASSENGER (passenger buses)
-- =============================================================================

BEGIN;

-- Step 1: Normalize category names to uppercase with underscores
UPDATE vehicle_types SET category = 'FUEL' WHERE category = 'Fueling';
UPDATE vehicle_types SET category = 'POWER' WHERE category = 'Power';
UPDATE vehicle_types SET category = 'SERVICE' WHERE category = 'Services';
UPDATE vehicle_types SET category = 'EMERGENCY' WHERE category = 'Emergency';
UPDATE vehicle_types SET category = 'CARGO' WHERE category = 'Cargo';
UPDATE vehicle_types SET category = 'PASSENGER' WHERE category = 'Transport';

-- Step 2: Fix misclassified vehicle types
UPDATE vehicle_types SET category = 'PUSHBACK' WHERE code = 'PUSHBACK';
UPDATE vehicle_types SET category = 'BAGGAGE' WHERE code = 'BAGGAGE';
UPDATE vehicle_types SET category = 'CATERING' WHERE code = 'CATERING';
UPDATE vehicle_types SET category = 'SERVICE' WHERE code = 'LAVATORY';
UPDATE vehicle_types SET category = 'PASSENGER' WHERE code = 'BUS';

-- Step 3: Remove duplicate vehicle types (keep only one per code)
WITH duplicates AS (
    SELECT id, code, ROW_NUMBER() OVER (PARTITION BY code ORDER BY id) as rn
    FROM vehicle_types
)
DELETE FROM vehicle_types 
WHERE id IN (SELECT id FROM duplicates WHERE rn > 1);

-- Step 4: Remove redundant codes (keep better-named versions)
DELETE FROM vehicle_types WHERE code = 'CAT';  -- Keep CATERING
DELETE FROM vehicle_types WHERE code = 'LAV';  -- Keep LAVATORY  
DELETE FROM vehicle_types WHERE code = 'BUS';  -- Keep PAX

-- Step 5: Verify the results
SELECT category, COUNT(*) as count 
FROM vehicle_types 
GROUP BY category 
ORDER BY category;

COMMIT;

-- =============================================================================
-- Standard Vehicle Categories (18 types):
-- =============================================================================
-- BAGGAGE    (3): BAG (Tractor), BAGGAGE (Cart), BELT (Loader)
-- CARGO      (1): CARGO (Loader)
-- CATERING   (1): CATERING (Truck)
-- DEICE      (1): DEICE (Truck)
-- EMERGENCY  (1): EMERGENCY (Vehicle)
-- FUEL       (2): FUEL (Truck), HYDR (Hydrant Dispenser)
-- PASSENGER  (2): PAX (Bus), STRS (Stairs)
-- POWER      (2): ASU (Air Start Unit), GPU (Ground Power Unit)
-- PUSHBACK   (3): PB (Tug), PUSHBACK (Tractor), TWB (Towbar)
-- SERVICE    (2): LAVATORY (Truck), WATER (Truck)
-- =============================================================================
