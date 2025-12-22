-- TAM Database Initialization: Layer 1 - Core Platform
-- Scope: Identity, Tenancy, Infrastructure, Audit, Metrics

-- 1. Enable Extensions
CREATE EXTENSION IF NOT EXISTS timescaledb CASCADE;
CREATE EXTENSION IF NOT EXISTS postgis CASCADE;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 2. Tenant Registry (Multi-Tenancy Root)
CREATE TABLE IF NOT EXISTS tenants (
    code VARCHAR(4) PRIMARY KEY, -- e.g., 'VIDP'
    name VARCHAR(100) NOT NULL,
    timezone VARCHAR(50) DEFAULT 'UTC',
    config JSONB DEFAULT '{}', -- Feature flags, limits, etc.
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Seed Tenants
INSERT INTO tenants (code, name, timezone) VALUES
('VIDP', 'Indira Gandhi International Airport', 'Asia/Kolkata'),
('LIRN', 'Naples International Airport', 'Europe/Rome')
ON CONFLICT (code) DO NOTHING;

-- 3. Users & Roles
CREATE TABLE IF NOT EXISTS users (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    tenant_code VARCHAR(4) REFERENCES tenants(code),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Seed Users
INSERT INTO users (username, password, role, tenant_code) VALUES
('admin_vidp', 'admin', 'ADMIN', 'VIDP'),
('gh_vidp', 'gh', 'GH', 'VIDP'),
('user_vidp', 'user', 'AIRPORT_USER', 'VIDP'),
('admin_lirn', 'admin', 'ADMIN', 'LIRN'),
('gh_lirn', 'gh', 'GH', 'LIRN'),
('user_lirn', 'user', 'AIRPORT_USER', 'LIRN')
ON CONFLICT (username) DO NOTHING;

-- 4. Audit Logs (Traceability)
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    tenant_code VARCHAR(4) REFERENCES tenants(code),
    user_id UUID REFERENCES users(id),
    action VARCHAR(50) NOT NULL, -- e.g., 'LOGIN', 'UPDATE_FLIGHT'
    resource_type VARCHAR(50),
    resource_id VARCHAR(100),
    details JSONB,
    ip_address VARCHAR(45),
    timestamp TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_tenant_time ON audit_logs (tenant_code, timestamp DESC);

-- 5. System Metrics (Platform Health)
CREATE TABLE IF NOT EXISTS system_metrics (
    timestamp TIMESTAMPTZ NOT NULL,
    service_name VARCHAR(50) NOT NULL, -- e.g., 'backend-api'
    metric_name VARCHAR(50) NOT NULL, -- e.g., 'cpu_usage'
    value DOUBLE PRECISION,
    tags JSONB
);

SELECT create_hypertable('system_metrics', 'timestamp', if_not_exists => TRUE);
CREATE INDEX IF NOT EXISTS idx_metrics_service ON system_metrics (service_name, timestamp DESC);

-- 6. Platform Settings (Archiving/Global Config)
CREATE TABLE IF NOT EXISTS platform_settings (
    key VARCHAR(50) PRIMARY KEY,
    value VARCHAR(255),
    description TEXT,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

INSERT INTO platform_settings (key, value, description) VALUES
('retention_days_raw', '7', 'Days to keep raw telemetry data'),
('retention_days_audit', '365', 'Days to keep audit logs')
ON CONFLICT (key) DO NOTHING;
