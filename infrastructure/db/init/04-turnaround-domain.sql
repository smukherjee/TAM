-- TAM Database Initialization: Layer 4 - Turnaround Domain
-- Scope: Turnaround Sessions, Tasks, Business Events

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. CV EVENTS (Raw Data Ingestion for this domain)
CREATE TABLE IF NOT EXISTS turnaround_events (
    event_unique_id VARCHAR(255) NOT NULL,
    tenant_code VARCHAR(4),
    camera_id VARCHAR(255),
    camera_name VARCHAR(255),
    activity_type VARCHAR(255),
    event_type INTEGER, -- 0=Start, 1=Stop
    event_time_stamp TIMESTAMPTZ NOT NULL,
    stand VARCHAR(255),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (event_unique_id, event_time_stamp)
);

SELECT create_hypertable('turnaround_events', 'event_time_stamp', if_not_exists => TRUE);
CREATE INDEX IF NOT EXISTS idx_turnaround_tenant ON turnaround_events (tenant_code);
CREATE INDEX IF NOT EXISTS idx_turnaround_stand ON turnaround_events (stand);

-- 2. TURNAROUND SESSIONS (Business Logic)
CREATE TABLE IF NOT EXISTS turnaround_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_code VARCHAR(4) NOT NULL,
    flight_id VARCHAR(20) NOT NULL,
    stand_id VARCHAR(10) NOT NULL,
    sirt TIMESTAMPTZ, -- Scheduled In-Block
    eibt TIMESTAMPTZ, -- Estimated In-Block
    aibt TIMESTAMPTZ, -- Actual In-Block
    tobt TIMESTAMPTZ, -- Target Off-Block
    tsat TIMESTAMPTZ, -- Target Startup Approval
    aobt TIMESTAMPTZ, -- Actual Off-Block
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_turnaround_sessions_tenant ON turnaround_sessions(tenant_code);
CREATE INDEX IF NOT EXISTS idx_turnaround_sessions_status ON turnaround_sessions(status);

-- 3. TURNAROUND TASKS (Process Steps)
CREATE TABLE IF NOT EXISTS turnaround_tasks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_code VARCHAR(4) NOT NULL,
    session_id UUID REFERENCES turnaround_sessions(id),
    task_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    planned_start TIMESTAMPTZ,
    planned_end TIMESTAMPTZ,
    actual_start TIMESTAMPTZ,
    actual_end TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_turnaround_tasks_session ON turnaround_tasks(session_id);

-- 4. TURNAROUND ALERTS (Business Notifications)
CREATE TABLE IF NOT EXISTS turnaround_alerts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_code VARCHAR(4) NOT NULL,
    session_id UUID REFERENCES turnaround_sessions(id),
    severity VARCHAR(20) NOT NULL,
    type VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    timestamp TIMESTAMPTZ DEFAULT NOW(),
    is_active BOOLEAN DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_turnaround_alerts_session ON turnaround_alerts(session_id);
