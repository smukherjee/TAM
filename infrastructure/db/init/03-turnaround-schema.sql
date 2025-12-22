-- Turnaround Management Schema
-- Created: 2025-12-21
-- Feature: 001-turnaround-ui-overhaul

CREATE TABLE IF NOT EXISTS turnaround_sessions (
    id UUID PRIMARY KEY,
    icao_code VARCHAR(4) NOT NULL,
    flight_id VARCHAR(20) NOT NULL,
    stand_id VARCHAR(10) NOT NULL,
    sirt TIMESTAMPTZ,
    eibt TIMESTAMPTZ,
    aibt TIMESTAMPTZ,
    tobt TIMESTAMPTZ,
    tsat TIMESTAMPTZ,
    aobt TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS turnaround_tasks (
    id UUID PRIMARY KEY,
    icao_code VARCHAR(4) NOT NULL,
    session_id UUID REFERENCES turnaround_sessions(id),
    task_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    planned_start TIMESTAMPTZ,
    planned_end TIMESTAMPTZ,
    actual_start TIMESTAMPTZ,
    actual_end TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS turnaround_alerts (
    id UUID PRIMARY KEY,
    icao_code VARCHAR(4) NOT NULL,
    session_id UUID REFERENCES turnaround_sessions(id),
    severity VARCHAR(20) NOT NULL,
    type VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    timestamp TIMESTAMPTZ DEFAULT NOW(),
    is_active BOOLEAN DEFAULT TRUE
);

-- Indexes for performance
CREATE INDEX idx_turnaround_sessions_icao ON turnaround_sessions(icao_code);
CREATE INDEX idx_turnaround_sessions_status ON turnaround_sessions(status);
CREATE INDEX idx_turnaround_tasks_session ON turnaround_tasks(session_id);
CREATE INDEX idx_turnaround_alerts_session ON turnaround_alerts(session_id);
CREATE INDEX idx_turnaround_alerts_active ON turnaround_alerts(is_active);
