-- Fix schema mismatch and add stands table with data
-- Run this script to sync JPA entities with database schema

-- =============================================================================
-- 1. Fix vehicle_types table - add missing columns
-- =============================================================================

-- Add missing columns that JPA entity expects
ALTER TABLE vehicle_types ADD COLUMN IF NOT EXISTS min_speed INTEGER DEFAULT 5;
ALTER TABLE vehicle_types ADD COLUMN IF NOT EXISTS default_quantity INTEGER DEFAULT 10;
ALTER TABLE vehicle_types ADD COLUMN IF NOT EXISTS default_depot_type VARCHAR(30);
ALTER TABLE vehicle_types ADD COLUMN IF NOT EXISTS is_motorized BOOLEAN DEFAULT true;

-- Rename columns if they exist with different names
DO $$ 
BEGIN
    -- Rename icon to icon_name if icon exists
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='vehicle_types' AND column_name='icon') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='vehicle_types' AND column_name='icon_name') THEN
            ALTER TABLE vehicle_types RENAME COLUMN icon TO icon_name;
        END IF;
    ELSE
        -- Add icon_name if neither exists
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='vehicle_types' AND column_name='icon_name') THEN
            ALTER TABLE vehicle_types ADD COLUMN icon_name VARCHAR(50);
        END IF;
    END IF;

    -- Rename color to icon_color if color exists
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='vehicle_types' AND column_name='color') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='vehicle_types' AND column_name='icon_color') THEN
            ALTER TABLE vehicle_types RENAME COLUMN color TO icon_color;
        END IF;
    ELSE
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='vehicle_types' AND column_name='icon_color') THEN
            ALTER TABLE vehicle_types ADD COLUMN icon_color VARCHAR(7);
        END IF;
    END IF;

    -- Rename max_speed_kmh to max_speed if max_speed_kmh exists
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='vehicle_types' AND column_name='max_speed_kmh') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='vehicle_types' AND column_name='max_speed') THEN
            ALTER TABLE vehicle_types RENAME COLUMN max_speed_kmh TO max_speed;
        END IF;
    ELSE
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='vehicle_types' AND column_name='max_speed') THEN
            ALTER TABLE vehicle_types ADD COLUMN max_speed INTEGER DEFAULT 40;
        END IF;
    END IF;
END $$;

-- =============================================================================
-- 2. Create stands table
-- =============================================================================

CREATE TABLE IF NOT EXISTS stands (
    id SERIAL PRIMARY KEY,
    tenant_code VARCHAR(10) NOT NULL,
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
    CONSTRAINT uk_stand_tenant_code UNIQUE (tenant_code, stand_id),
    CONSTRAINT fk_stands_tenant FOREIGN KEY (tenant_code) REFERENCES tenants(code)
);

CREATE INDEX IF NOT EXISTS idx_stand_tenant_stand ON stands(tenant_code, stand_id);
CREATE INDEX IF NOT EXISTS idx_stand_tenant_terminal ON stands(tenant_code, terminal_id);
CREATE INDEX IF NOT EXISTS idx_stand_tenant_apron ON stands(tenant_code, apron);

-- =============================================================================
-- 3. Insert VIDP (Delhi) Stands Data
-- =============================================================================
-- Airport coordinates: 28.5562, 77.1000
-- Spread stands around apron areas

-- Delete existing VIDP stands to avoid duplicates
DELETE FROM stands WHERE tenant_code = 'VIDP';

-- Apron 1 (Domestic) - Stands 1-30, 121-152
INSERT INTO stands (tenant_code, stand_id, name, latitude, longitude, terminal_id, stand_type, apron) VALUES
-- Apron 1 Row 1: Stands 1-13
('VIDP', '1', 'Stand 1', 28.5575, 77.0980, 'T1', 'CONTACT', 'Apron 1'),
('VIDP', '2', 'Stand 2', 28.5575, 77.0985, 'T1', 'CONTACT', 'Apron 1'),
('VIDP', '3', 'Stand 3', 28.5575, 77.0990, 'T1', 'CONTACT', 'Apron 1'),
('VIDP', '4', 'Stand 4', 28.5575, 77.0995, 'T1', 'CONTACT', 'Apron 1'),
('VIDP', '5', 'Stand 5', 28.5575, 77.1000, 'T1', 'CONTACT', 'Apron 1'),
('VIDP', '6', 'Stand 6', 28.5575, 77.1005, 'T1', 'CONTACT', 'Apron 1'),
('VIDP', '7', 'Stand 7', 28.5575, 77.1010, 'T1', 'CONTACT', 'Apron 1'),
('VIDP', '8', 'Stand 8', 28.5575, 77.1015, 'T1', 'CONTACT', 'Apron 1'),
('VIDP', '9', 'Stand 9', 28.5575, 77.1020, 'T1', 'CONTACT', 'Apron 1'),
('VIDP', '10', 'Stand 10', 28.5575, 77.1025, 'T1', 'CONTACT', 'Apron 1'),
('VIDP', '11', 'Stand 11', 28.5575, 77.1030, 'T1', 'CONTACT', 'Apron 1'),
('VIDP', '12', 'Stand 12', 28.5575, 77.1035, 'T1', 'CONTACT', 'Apron 1'),
('VIDP', '13', 'Stand 13', 28.5575, 77.1040, 'T1', 'CONTACT', 'Apron 1'),
-- Apron 1 Row 2: Stands 15-17, 19-30
('VIDP', '15', 'Stand 15', 28.5570, 77.0980, 'T1', 'REMOTE', 'Apron 1'),
('VIDP', '16', 'Stand 16', 28.5570, 77.0985, 'T1', 'REMOTE', 'Apron 1'),
('VIDP', '17', 'Stand 17', 28.5570, 77.0990, 'T1', 'REMOTE', 'Apron 1'),
('VIDP', '19', 'Stand 19', 28.5570, 77.0995, 'T1', 'REMOTE', 'Apron 1'),
('VIDP', '20', 'Stand 20', 28.5570, 77.1000, 'T1', 'REMOTE', 'Apron 1'),
('VIDP', '21', 'Stand 21', 28.5570, 77.1005, 'T1', 'REMOTE', 'Apron 1'),
('VIDP', '22', 'Stand 22', 28.5570, 77.1010, 'T1', 'REMOTE', 'Apron 1'),
('VIDP', '23', 'Stand 23', 28.5570, 77.1015, 'T1', 'REMOTE', 'Apron 1'),
('VIDP', '24', 'Stand 24', 28.5570, 77.1020, 'T1', 'REMOTE', 'Apron 1'),
('VIDP', '25', 'Stand 25', 28.5570, 77.1025, 'T1', 'REMOTE', 'Apron 1'),
('VIDP', '26', 'Stand 26', 28.5570, 77.1030, 'T1', 'REMOTE', 'Apron 1'),
('VIDP', '27', 'Stand 27', 28.5570, 77.1035, 'T1', 'REMOTE', 'Apron 1'),
('VIDP', '28', 'Stand 28', 28.5570, 77.1040, 'T1', 'REMOTE', 'Apron 1'),
('VIDP', '29', 'Stand 29', 28.5570, 77.1045, 'T1', 'REMOTE', 'Apron 1'),
('VIDP', '30', 'Stand 30', 28.5570, 77.1050, 'T1', 'REMOTE', 'Apron 1'),
('VIDP', '40C', 'Stand 40C', 28.5565, 77.0995, 'T1', 'REMOTE', 'Apron 1'),
-- Apron 1: Stands 121-132
('VIDP', '121', 'Stand 121', 28.5565, 77.1000, 'T1', 'REMOTE', 'Apron 1'),
('VIDP', '122', 'Stand 122', 28.5565, 77.1005, 'T1', 'REMOTE', 'Apron 1'),
('VIDP', '123', 'Stand 123', 28.5565, 77.1010, 'T1', 'REMOTE', 'Apron 1'),
('VIDP', '124', 'Stand 124', 28.5565, 77.1015, 'T1', 'REMOTE', 'Apron 1'),
('VIDP', '125', 'Stand 125', 28.5565, 77.1020, 'T1', 'REMOTE', 'Apron 1'),
('VIDP', '126', 'Stand 126', 28.5565, 77.1025, 'T1', 'REMOTE', 'Apron 1'),
('VIDP', '127', 'Stand 127', 28.5565, 77.1030, 'T1', 'REMOTE', 'Apron 1'),
('VIDP', '128', 'Stand 128', 28.5565, 77.1035, 'T1', 'REMOTE', 'Apron 1'),
('VIDP', '129', 'Stand 129', 28.5565, 77.1040, 'T1', 'REMOTE', 'Apron 1'),
('VIDP', '130', 'Stand 130', 28.5565, 77.1045, 'T1', 'REMOTE', 'Apron 1'),
('VIDP', '131', 'Stand 131', 28.5565, 77.1050, 'T1', 'REMOTE', 'Apron 1'),
('VIDP', '132', 'Stand 132', 28.5565, 77.1055, 'T1', 'REMOTE', 'Apron 1'),
-- Apron 1: Stands 135-142
('VIDP', '135', 'Stand 135', 28.5560, 77.1000, 'T1', 'REMOTE', 'Apron 1'),
('VIDP', '136', 'Stand 136', 28.5560, 77.1005, 'T1', 'REMOTE', 'Apron 1'),
('VIDP', '137', 'Stand 137', 28.5560, 77.1010, 'T1', 'REMOTE', 'Apron 1'),
('VIDP', '138', 'Stand 138', 28.5560, 77.1015, 'T1', 'REMOTE', 'Apron 1'),
('VIDP', '139', 'Stand 139', 28.5560, 77.1020, 'T1', 'REMOTE', 'Apron 1'),
('VIDP', '140', 'Stand 140', 28.5560, 77.1025, 'T1', 'REMOTE', 'Apron 1'),
('VIDP', '141', 'Stand 141', 28.5560, 77.1030, 'T1', 'REMOTE', 'Apron 1'),
('VIDP', '142', 'Stand 142', 28.5560, 77.1035, 'T1', 'REMOTE', 'Apron 1'),
-- Apron 1: Stands 143-152
('VIDP', '143', 'Stand 143', 28.5555, 77.1000, 'T1', 'REMOTE', 'Apron 1'),
('VIDP', '144', 'Stand 144', 28.5555, 77.1005, 'T1', 'REMOTE', 'Apron 1'),
('VIDP', '145', 'Stand 145', 28.5555, 77.1010, 'T1', 'REMOTE', 'Apron 1'),
('VIDP', '146', 'Stand 146', 28.5555, 77.1015, 'T1', 'REMOTE', 'Apron 1'),
('VIDP', '147', 'Stand 147', 28.5555, 77.1020, 'T1', 'REMOTE', 'Apron 1'),
('VIDP', '148', 'Stand 148', 28.5555, 77.1025, 'T1', 'REMOTE', 'Apron 1'),
('VIDP', '149', 'Stand 149', 28.5555, 77.1030, 'T1', 'REMOTE', 'Apron 1'),
('VIDP', '150', 'Stand 150', 28.5555, 77.1035, 'T1', 'REMOTE', 'Apron 1'),
('VIDP', '151', 'Stand 151', 28.5555, 77.1040, 'T1', 'REMOTE', 'Apron 1'),
('VIDP', '152', 'Stand 152', 28.5555, 77.1045, 'T1', 'REMOTE', 'Apron 1'),
-- Apron 2/T3 (International) - Stands 41-49, 81-92, 98-103, 301-305
('VIDP', '41', 'Stand 41', 28.5545, 77.0950, 'T3', 'CONTACT', 'Apron 2'),
('VIDP', '42', 'Stand 42', 28.5545, 77.0955, 'T3', 'CONTACT', 'Apron 2'),
('VIDP', '43', 'Stand 43', 28.5545, 77.0960, 'T3', 'CONTACT', 'Apron 2'),
('VIDP', '44', 'Stand 44', 28.5545, 77.0965, 'T3', 'CONTACT', 'Apron 2'),
('VIDP', '45', 'Stand 45', 28.5545, 77.0970, 'T3', 'CONTACT', 'Apron 2'),
('VIDP', '46', 'Stand 46', 28.5545, 77.0975, 'T3', 'CONTACT', 'Apron 2'),
('VIDP', '47', 'Stand 47', 28.5545, 77.0980, 'T3', 'CONTACT', 'Apron 2'),
('VIDP', '48', 'Stand 48', 28.5545, 77.0985, 'T3', 'CONTACT', 'Apron 2'),
('VIDP', '49', 'Stand 49', 28.5545, 77.0990, 'T3', 'CONTACT', 'Apron 2'),
('VIDP', '81', 'Stand 81', 28.5540, 77.0950, 'T3', 'PUSHBACK', 'Apron 2'),
('VIDP', '82', 'Stand 82', 28.5540, 77.0955, 'T3', 'PUSHBACK', 'Apron 2'),
('VIDP', '83', 'Stand 83', 28.5540, 77.0960, 'T3', 'PUSHBACK', 'Apron 2'),
('VIDP', '84', 'Stand 84', 28.5540, 77.0965, 'T3', 'PUSHBACK', 'Apron 2'),
('VIDP', '85', 'Stand 85', 28.5540, 77.0970, 'T3', 'PUSHBACK', 'Apron 2'),
('VIDP', '86', 'Stand 86', 28.5540, 77.0975, 'T3', 'PUSHBACK', 'Apron 2'),
('VIDP', '87', 'Stand 87', 28.5540, 77.0980, 'T3', 'PUSHBACK', 'Apron 2'),
('VIDP', '88', 'Stand 88', 28.5540, 77.0985, 'T3', 'PUSHBACK', 'Apron 2'),
('VIDP', '89', 'Stand 89', 28.5540, 77.0990, 'T3', 'PUSHBACK', 'Apron 2'),
('VIDP', '90', 'Stand 90', 28.5540, 77.0995, 'T3', 'PUSHBACK', 'Apron 2'),
('VIDP', '91', 'Stand 91', 28.5540, 77.1000, 'T3', 'PUSHBACK', 'Apron 2'),
('VIDP', '92', 'Stand 92', 28.5540, 77.1005, 'T3', 'PUSHBACK', 'Apron 2'),
('VIDP', '98', 'Stand 98', 28.5535, 77.0950, 'T3', 'REMOTE', 'Apron 2'),
('VIDP', '99', 'Stand 99', 28.5535, 77.0955, 'T3', 'REMOTE', 'Apron 2'),
('VIDP', '100', 'Stand 100', 28.5535, 77.0960, 'T3', 'REMOTE', 'Apron 2'),
('VIDP', '101', 'Stand 101', 28.5535, 77.0965, 'T3', 'REMOTE', 'Apron 2'),
('VIDP', '102', 'Stand 102', 28.5535, 77.0970, 'T3', 'REMOTE', 'Apron 2'),
('VIDP', '103', 'Stand 103', 28.5535, 77.0975, 'T3', 'REMOTE', 'Apron 2'),
('VIDP', '301', 'Stand 301', 28.5530, 77.0950, 'T3', 'CONTACT', 'Apron 2'),
('VIDP', '302', 'Stand 302', 28.5530, 77.0955, 'T3', 'CONTACT', 'Apron 2'),
('VIDP', '303', 'Stand 303', 28.5530, 77.0960, 'T3', 'CONTACT', 'Apron 2'),
('VIDP', '304', 'Stand 304', 28.5530, 77.0965, 'T3', 'CONTACT', 'Apron 2'),
('VIDP', '305', 'Stand 305', 28.5530, 77.0970, 'T3', 'CONTACT', 'Apron 2'),
-- GA Apron: Stands 31-35
('VIDP', '31', 'Stand 31', 28.5520, 77.1050, 'GA', 'REMOTE', 'GA Apron'),
('VIDP', '32', 'Stand 32', 28.5520, 77.1055, 'GA', 'REMOTE', 'GA Apron'),
('VIDP', '33', 'Stand 33', 28.5520, 77.1060, 'GA', 'REMOTE', 'GA Apron'),
('VIDP', '34', 'Stand 34', 28.5520, 77.1065, 'GA', 'REMOTE', 'GA Apron'),
('VIDP', '35', 'Stand 35', 28.5520, 77.1070, 'GA', 'REMOTE', 'GA Apron');

-- =============================================================================
-- 4. Insert LIRN (Naples) Stands Data
-- =============================================================================
-- Airport coordinates: 40.8860, 14.2908

DELETE FROM stands WHERE tenant_code = 'LIRN';

INSERT INTO stands (tenant_code, stand_id, name, latitude, longitude, terminal_id, stand_type, apron) VALUES
-- Apron 1: Stands 11-23 (Pushback required)
('LIRN', '11', 'Stand 11', 40.8865, 14.2890, 'T1', 'PUSHBACK', 'Apron 1'),
('LIRN', '12', 'Stand 12', 40.8865, 14.2895, 'T1', 'PUSHBACK', 'Apron 1'),
('LIRN', '13', 'Stand 13', 40.8865, 14.2900, 'T1', 'PUSHBACK', 'Apron 1'),
('LIRN', '14', 'Stand 14', 40.8865, 14.2905, 'T1', 'PUSHBACK', 'Apron 1'),
('LIRN', '15', 'Stand 15', 40.8865, 14.2910, 'T1', 'PUSHBACK', 'Apron 1'),
('LIRN', '16', 'Stand 16', 40.8865, 14.2915, 'T1', 'PUSHBACK', 'Apron 1'),
('LIRN', '17', 'Stand 17', 40.8865, 14.2920, 'T1', 'PUSHBACK', 'Apron 1'),
('LIRN', '18', 'Stand 18', 40.8865, 14.2925, 'T1', 'PUSHBACK', 'Apron 1'),
('LIRN', '19', 'Stand 19', 40.8865, 14.2930, 'T1', 'PUSHBACK', 'Apron 1'),
('LIRN', '20', 'Stand 20', 40.8865, 14.2935, 'T1', 'PUSHBACK', 'Apron 1'),
('LIRN', '21', 'Stand 21', 40.8865, 14.2940, 'T1', 'PUSHBACK', 'Apron 1'),
('LIRN', '22', 'Stand 22', 40.8865, 14.2945, 'T1', 'PUSHBACK', 'Apron 1'),
('LIRN', '23', 'Stand 23', 40.8865, 14.2950, 'T1', 'PUSHBACK', 'Apron 1'),
-- Apron 1: Stands 41-46
('LIRN', '41', 'Stand 41', 40.8860, 14.2890, 'T1', 'REMOTE', 'Apron 1'),
('LIRN', '42', 'Stand 42', 40.8860, 14.2895, 'T1', 'REMOTE', 'Apron 1'),
('LIRN', '43', 'Stand 43', 40.8860, 14.2900, 'T1', 'REMOTE', 'Apron 1'),
('LIRN', '44', 'Stand 44', 40.8860, 14.2905, 'T1', 'REMOTE', 'Apron 1'),
('LIRN', '45', 'Stand 45', 40.8860, 14.2910, 'T1', 'REMOTE', 'Apron 1'),
('LIRN', '46', 'Stand 46', 40.8860, 14.2915, 'T1', 'REMOTE', 'Apron 1'),
-- Apron 1: Stands 51-57 (Pushback required)
('LIRN', '51', 'Stand 51', 40.8855, 14.2890, 'T1', 'PUSHBACK', 'Apron 1'),
('LIRN', '52', 'Stand 52', 40.8855, 14.2895, 'T1', 'PUSHBACK', 'Apron 1'),
('LIRN', '53', 'Stand 53', 40.8855, 14.2900, 'T1', 'PUSHBACK', 'Apron 1'),
('LIRN', '54', 'Stand 54', 40.8855, 14.2905, 'T1', 'PUSHBACK', 'Apron 1'),
('LIRN', '55', 'Stand 55', 40.8855, 14.2910, 'T1', 'PUSHBACK', 'Apron 1'),
('LIRN', '56', 'Stand 56', 40.8855, 14.2915, 'T1', 'PUSHBACK', 'Apron 1'),
('LIRN', '57', 'Stand 57', 40.8855, 14.2920, 'T1', 'PUSHBACK', 'Apron 1'),
-- Apron 2: Stands 71-74
('LIRN', '71', 'Stand 71', 40.8850, 14.2920, 'T1', 'REMOTE', 'Apron 2'),
('LIRN', '72', 'Stand 72', 40.8850, 14.2925, 'T1', 'REMOTE', 'Apron 2'),
('LIRN', '73', 'Stand 73', 40.8850, 14.2930, 'T1', 'REMOTE', 'Apron 2'),
('LIRN', '74', 'Stand 74', 40.8850, 14.2935, 'T1', 'REMOTE', 'Apron 2'),
-- Apron 3: Stands 61-66 (Pushback required, code C/D aircraft)
('LIRN', '61', 'Stand 61', 40.8870, 14.2920, 'T1', 'PUSHBACK', 'Apron 3'),
('LIRN', '62', 'Stand 62', 40.8870, 14.2925, 'T1', 'PUSHBACK', 'Apron 3'),
('LIRN', '63', 'Stand 63', 40.8870, 14.2930, 'T1', 'PUSHBACK', 'Apron 3'),
('LIRN', '64', 'Stand 64', 40.8870, 14.2935, 'T1', 'PUSHBACK', 'Apron 3'),
('LIRN', '65', 'Stand 65', 40.8870, 14.2940, 'T1', 'PUSHBACK', 'Apron 3'),
('LIRN', '66', 'Stand 66', 40.8870, 14.2945, 'T1', 'PUSHBACK', 'Apron 3'),
-- Apron 3: Special stands M, N, NA, P1, TN, TN3
('LIRN', 'M', 'Stand M', 40.8875, 14.2920, 'T1', 'REMOTE', 'Apron 3'),
('LIRN', 'N', 'Stand N', 40.8875, 14.2925, 'T1', 'REMOTE', 'Apron 3'),
('LIRN', 'NA', 'Stand NA', 40.8875, 14.2930, 'T1', 'REMOTE', 'Apron 3'),
('LIRN', 'P1', 'Stand P1', 40.8875, 14.2935, 'T1', 'REMOTE', 'Apron 3'),
('LIRN', 'TN', 'Stand TN', 40.8875, 14.2940, 'T1', 'REMOTE', 'Apron 3'),
('LIRN', 'TN3', 'Stand TN3', 40.8875, 14.2945, 'T1', 'REMOTE', 'Apron 3');

-- =============================================================================
-- 5. Insert YBBN (Brisbane) Stands Data
-- =============================================================================
-- Airport coordinates: -27.3842, 153.1175

DELETE FROM stands WHERE tenant_code = 'YBBN';

INSERT INTO stands (tenant_code, stand_id, name, latitude, longitude, terminal_id, stand_type, apron) VALUES
-- North Remote Apron: E1-D3
('YBBN', 'E1', 'Stand E1', -27.3830, 153.1160, 'INT', 'REMOTE', 'North Remote'),
('YBBN', 'E2', 'Stand E2', -27.3830, 153.1165, 'INT', 'REMOTE', 'North Remote'),
('YBBN', 'E3', 'Stand E3', -27.3830, 153.1170, 'INT', 'REMOTE', 'North Remote'),
('YBBN', 'D1', 'Stand D1', -27.3835, 153.1160, 'INT', 'REMOTE', 'North Remote'),
('YBBN', 'D2', 'Stand D2', -27.3835, 153.1165, 'INT', 'REMOTE', 'North Remote'),
('YBBN', 'D3', 'Stand D3', -27.3835, 153.1170, 'INT', 'REMOTE', 'North Remote'),
-- South Parking: C1-C11 (Non-manoeuvring area)
('YBBN', 'C1', 'Stand C1', -27.3850, 153.1180, 'DOM', 'REMOTE', 'South Parking'),
('YBBN', 'C2', 'Stand C2', -27.3850, 153.1185, 'DOM', 'REMOTE', 'South Parking'),
('YBBN', 'C3', 'Stand C3', -27.3850, 153.1190, 'DOM', 'REMOTE', 'South Parking'),
('YBBN', 'C4', 'Stand C4', -27.3850, 153.1195, 'DOM', 'REMOTE', 'South Parking'),
('YBBN', 'C5', 'Stand C5', -27.3850, 153.1200, 'DOM', 'REMOTE', 'South Parking'),
('YBBN', 'C6', 'Stand C6', -27.3855, 153.1180, 'DOM', 'REMOTE', 'South Parking'),
('YBBN', 'C7', 'Stand C7', -27.3855, 153.1185, 'DOM', 'REMOTE', 'South Parking'),
('YBBN', 'C8', 'Stand C8', -27.3855, 153.1190, 'DOM', 'REMOTE', 'South Parking'),
('YBBN', 'C9', 'Stand C9', -27.3855, 153.1195, 'DOM', 'REMOTE', 'South Parking'),
('YBBN', 'C10', 'Stand C10', -27.3855, 153.1200, 'DOM', 'REMOTE', 'South Parking'),
('YBBN', 'C11', 'Stand C11', -27.3855, 153.1205, 'DOM', 'REMOTE', 'South Parking'),
-- Domestic Terminal stands
('YBBN', '1', 'Stand 1', -27.3845, 153.1175, 'DOM', 'CONTACT', 'Domestic Apron'),
('YBBN', '2', 'Stand 2', -27.3845, 153.1180, 'DOM', 'CONTACT', 'Domestic Apron'),
('YBBN', '3', 'Stand 3', -27.3845, 153.1185, 'DOM', 'CONTACT', 'Domestic Apron'),
('YBBN', '4', 'Stand 4', -27.3845, 153.1190, 'DOM', 'CONTACT', 'Domestic Apron'),
('YBBN', '5', 'Stand 5', -27.3845, 153.1195, 'DOM', 'CONTACT', 'Domestic Apron'),
('YBBN', '6', 'Stand 6', -27.3840, 153.1175, 'DOM', 'CONTACT', 'Domestic Apron'),
('YBBN', '7', 'Stand 7', -27.3840, 153.1180, 'DOM', 'CONTACT', 'Domestic Apron'),
('YBBN', '8', 'Stand 8', -27.3840, 153.1185, 'DOM', 'CONTACT', 'Domestic Apron'),
('YBBN', '9', 'Stand 9', -27.3840, 153.1190, 'DOM', 'CONTACT', 'Domestic Apron'),
('YBBN', '10', 'Stand 10', -27.3840, 153.1195, 'DOM', 'CONTACT', 'Domestic Apron'),
-- International Terminal stands
('YBBN', '71', 'Stand 71', -27.3825, 153.1155, 'INT', 'CONTACT', 'International Apron'),
('YBBN', '72', 'Stand 72', -27.3825, 153.1160, 'INT', 'CONTACT', 'International Apron'),
('YBBN', '73', 'Stand 73', -27.3825, 153.1165, 'INT', 'CONTACT', 'International Apron'),
('YBBN', '74', 'Stand 74', -27.3825, 153.1170, 'INT', 'CONTACT', 'International Apron'),
('YBBN', '75', 'Stand 75', -27.3825, 153.1175, 'INT', 'CONTACT', 'International Apron'),
('YBBN', '76', 'Stand 76', -27.3820, 153.1155, 'INT', 'PUSHBACK', 'International Apron'),
('YBBN', '77', 'Stand 77', -27.3820, 153.1160, 'INT', 'PUSHBACK', 'International Apron'),
('YBBN', '78', 'Stand 78', -27.3820, 153.1165, 'INT', 'PUSHBACK', 'International Apron'),
('YBBN', '79', 'Stand 79', -27.3820, 153.1170, 'INT', 'PUSHBACK', 'International Apron'),
('YBBN', '80', 'Stand 80', -27.3820, 153.1175, 'INT', 'PUSHBACK', 'International Apron');

-- =============================================================================
-- 6. Verify the data
-- =============================================================================
SELECT tenant_code, COUNT(*) as stand_count FROM stands GROUP BY tenant_code ORDER BY tenant_code;
