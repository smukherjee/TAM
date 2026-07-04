-- TAM Database: Asset Management Module
-- Creates tables for assets, kits, categories, tags, and locations

-- 1. Assets Table
CREATE TABLE IF NOT EXISTS assets (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    asset_id VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    qr_id VARCHAR(50),
    status VARCHAR(20) DEFAULT 'Available',
    description VARCHAR(500),
    category VARCHAR(50),
    value DECIMAL(10, 2),
    tenant_code VARCHAR(4) REFERENCES tenants(code),
    location VARCHAR(100),
    company VARCHAR(100), -- owning ground handler; NULL = untagged/shared asset, visible to all GH views
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT uq_assets_asset_id_tenant UNIQUE (asset_id, tenant_code)
);

CREATE INDEX IF NOT EXISTS idx_assets_tenant ON assets (tenant_code);
CREATE INDEX IF NOT EXISTS idx_assets_status ON assets (status);
CREATE INDEX IF NOT EXISTS idx_assets_category ON assets (category);
CREATE INDEX IF NOT EXISTS idx_assets_company ON assets (company);
CREATE INDEX IF NOT EXISTS idx_assets_asset_tenant ON assets (asset_id, tenant_code);

-- 2. Seed Sample Assets for VIDP
INSERT INTO assets (asset_id, name, qr_id, status, description, category, value, tenant_code, location) VALUES
('SAM-0001', 'Fire Truck', 'h7wbp8n6po', 'Available', 'Fire Truck', 'Emergency', NULL, 'VIDP', 'Terminal 1'),
('SAM-0002', 'Pushback Tractors', 'moxh3ayxik', 'Available', 'Pushback Tractors', 'Ground Support', NULL, 'VIDP', 'Apron'),
('SAM-0003', 'Ground Power Unit -001', 'd6e8hrp1re', 'Available', 'Ground Power Unit', 'Power', NULL, 'VIDP', 'Terminal 2'),
('SAM-0004', 'Belt Loader-001', 'akj2hhqe5m', 'Available', 'Belt Loader', 'Cargo', NULL, 'VIDP', 'Cargo Area'),
('SAM-0005', 'Fuel Truck - 001', 'mx36v0s07n', 'Available', 'Fuel Trucks', 'Fueling', NULL, 'VIDP', 'Fuel Station'),
('SAM-0006', 'Potable Water Truck - 001', 'nxyumi5gw6', 'Available', 'Potable Water Trucks', 'Services', NULL, 'VIDP', 'Service Area'),
('SAM-0007', 'Apron Passenger Bus - 001', 'auslh2mrjz', 'Available', 'Apron Passenger Buses', 'Transport', NULL, 'VIDP', 'Terminal 3')
ON CONFLICT DO NOTHING;

-- 3. Seed Sample Assets for LIRN
INSERT INTO assets (asset_id, name, qr_id, status, description, category, value, tenant_code, location) VALUES
('NAP-0001', 'Fire Truck Naples', 'nap1fire01', 'Available', 'Fire Truck', 'Emergency', NULL, 'LIRN', 'Terminal A'),
('NAP-0002', 'Pushback Tractor Naples', 'nap1push01', 'Available', 'Pushback Tractors', 'Ground Support', NULL, 'LIRN', 'Apron'),
('NAP-0003', 'Ground Power Unit Naples', 'nap1gpu001', 'Available', 'Ground Power Unit', 'Power', NULL, 'LIRN', 'Terminal B'),
('NAP-0004', 'Belt Loader Naples', 'nap1belt01', 'Available', 'Belt Loader', 'Cargo', NULL, 'LIRN', 'Cargo Area')
ON CONFLICT DO NOTHING;

-- 4. Seed Sample Assets for YBBN
INSERT INTO assets (asset_id, name, qr_id, status, description, category, value, tenant_code, location) VALUES
('BNE-0001', 'Fire Truck Brisbane', 'bne1fire01', 'Available', 'Fire Truck', 'Emergency', NULL, 'YBBN', 'Terminal 1'),
('BNE-0002', 'Pushback Tractor Brisbane', 'bne1push01', 'Available', 'Pushback Tractors', 'Ground Support', NULL, 'YBBN', 'Apron'),
('BNE-0003', 'Ground Power Unit Brisbane', 'bne1gpu001', 'Available', 'Ground Power Unit', 'Power', NULL, 'YBBN', 'Terminal 2'),
('BNE-0004', 'Belt Loader Brisbane', 'bne1belt01', 'Available', 'Belt Loader', 'Cargo', NULL, 'YBBN', 'Cargo Area'),
('BNE-0005', 'Fuel Truck Brisbane', 'bne1fuel01', 'Available', 'Fuel Trucks', 'Fueling', NULL, 'YBBN', 'Fuel Station')
ON CONFLICT DO NOTHING;
