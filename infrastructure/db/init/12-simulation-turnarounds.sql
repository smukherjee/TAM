-- Simulation Tables
-- Purpose: Support backend simulation engine (FR-071)

CREATE TABLE IF NOT EXISTS simulation_turnarounds (
    id UUID PRIMARY KEY,
    tenant_code VARCHAR(10) NOT NULL,
    flight_number VARCHAR(10) NOT NULL,
    stand_code VARCHAR(10) NOT NULL,
    aircraft_type VARCHAR(10) NOT NULL,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ,
    planned_duration_minutes INT NOT NULL,
    actual_duration_minutes INT,
    status VARCHAR(20) NOT NULL, -- SCHEDULED, IN_PROGRESS, COMPLETED, DELAYED
    current_phase VARCHAR(20) NOT NULL, -- ARRIVAL, DEBOARDING, SERVICING, BOARDING, DEPARTURE
    delay_minutes INT,
    delay_reason VARCHAR(200),
    last_updated TIMESTAMPTZ,
    arrival_time TIMESTAMPTZ,
    deboarding_start_time TIMESTAMPTZ,
    servicing_start_time TIMESTAMPTZ,
    boarding_start_time TIMESTAMPTZ,
    departure_time TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_turnaround_tenant ON simulation_turnarounds (tenant_code);
CREATE INDEX IF NOT EXISTS idx_turnaround_stand ON simulation_turnarounds (stand_code);
CREATE INDEX IF NOT EXISTS idx_turnaround_flight ON simulation_turnarounds (flight_number);
CREATE INDEX IF NOT EXISTS idx_turnaround_status ON simulation_turnarounds (status);
CREATE INDEX IF NOT EXISTS idx_turnaround_start ON simulation_turnarounds (start_time);
