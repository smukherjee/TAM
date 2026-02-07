-- TAM Database: Simulation Operational Flows (Layer 15)
-- Scope: Dispatches, Alerts, Discrepancies, Violations, Financial Metrics

-- ============================================
-- 1. Create simulation_dispatches table
-- ============================================
CREATE TABLE IF NOT EXISTS simulation_dispatches (
    id UUID PRIMARY KEY,
    tenant_code VARCHAR(10) NOT NULL REFERENCES tenants(code),
    vehicle_id UUID,
    vehicle_type VARCHAR(20),
    stand_code VARCHAR(10),
    turnaround_id UUID,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    dispatched_at TIMESTAMP WITH TIME ZONE,
    arrived_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    estimated_duration_seconds INTEGER,
    actual_duration_seconds INTEGER,
    delay_seconds INTEGER,
    priority VARCHAR(20),
    notes VARCHAR(200)
);

CREATE INDEX IF NOT EXISTS idx_sim_dispatch_tenant ON simulation_dispatches(tenant_code);
CREATE INDEX IF NOT EXISTS idx_sim_dispatch_status ON simulation_dispatches(status);

-- ============================================
-- 2. Create simulation_alerts table
-- ============================================
CREATE TABLE IF NOT EXISTS simulation_alerts (
    id UUID PRIMARY KEY,
    tenant_code VARCHAR(10) NOT NULL REFERENCES tenants(code),
    alert_type VARCHAR(30) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    acknowledged_at TIMESTAMP WITH TIME ZONE,
    resolved_at TIMESTAMP WITH TIME ZONE,
    entity_type VARCHAR(30),
    entity_id UUID,
    entity_reference VARCHAR(50),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    message TEXT
);

CREATE INDEX IF NOT EXISTS idx_sim_alert_tenant ON simulation_alerts(tenant_code);
CREATE INDEX IF NOT EXISTS idx_sim_alert_status ON simulation_alerts(status);

-- ============================================
-- 3. Create simulation_discrepancies table
-- ============================================
CREATE TABLE IF NOT EXISTS simulation_discrepancies (
    id UUID PRIMARY KEY,
    tenant_code VARCHAR(10) NOT NULL REFERENCES tenants(code),
    discrepancy_type VARCHAR(30) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    entity_type VARCHAR(20) NOT NULL,
    description TEXT,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    detected_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sim_discrepancy_tenant ON simulation_discrepancies(tenant_code);

-- ============================================
-- 4. Create simulation_violations table
-- ============================================
CREATE TABLE IF NOT EXISTS simulation_violations (
    id UUID PRIMARY KEY,
    tenant_code VARCHAR(10) NOT NULL REFERENCES tenants(code),
    violation_type VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    severity VARCHAR(20) NOT NULL,
    priority_score INTEGER,
    entity_id VARCHAR(100) NOT NULL,
    entity_type VARCHAR(20) NOT NULL,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    zone_id UUID,
    zone_name VARCHAR(100),
    detected_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sim_violation_tenant ON simulation_violations(tenant_code);

-- ============================================
-- 5. Create simulation_financial_metrics table
-- ============================================
CREATE TABLE IF NOT EXISTS simulation_financial_metrics (
    id UUID PRIMARY KEY,
    tenant_code VARCHAR(10) NOT NULL REFERENCES tenants(code),
    metric_type VARCHAR(30) NOT NULL,
    report_date DATE NOT NULL,
    stand_code VARCHAR(10),
    vehicle_type VARCHAR(20),
    flight_number VARCHAR(10),
    turnaround_count INTEGER DEFAULT 0,
    delay_minutes INTEGER DEFAULT 0,
    total_delay_cost DOUBLE PRECISION DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sim_financial_tenant ON simulation_financial_metrics(tenant_code);
CREATE INDEX IF NOT EXISTS idx_sim_financial_date ON simulation_financial_metrics(report_date DESC);
