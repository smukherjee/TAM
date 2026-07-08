-- Emergency/RFFS Drill & Readiness Log
-- Date: 2026-07-06
-- AOM Ch 4/5 (Aerodrome Emergency Plan / Rescue & Fire Fighting Services readiness).
-- Same convention as 25-ppe-density-walkway-safety.sql: new table + deterministic
-- md5-backfill mock data. Cadence mirrors ICAO Annex 14 practice: full-scale
-- exercise every 2 years, partial/table-top exercise annually, RFFS drills monthly.

BEGIN;

CREATE TABLE IF NOT EXISTS emergency_drills (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_code VARCHAR(4) NOT NULL,
    drill_type VARCHAR(50) NOT NULL,
    scheduled_date DATE NOT NULL,
    conducted_date DATE,
    participants_count INT,
    response_time_seconds INT,
    outcome VARCHAR(20),
    next_due_date DATE,
    created_at TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_emergency_drills_tenant ON emergency_drills (tenant_code, scheduled_date DESC);

COMMENT ON TABLE emergency_drills IS 'Full-scale/partial emergency exercises and RFFS response drills, for AOM Ch 4/5 emergency-readiness reporting.';

CREATE OR REPLACE VIEW v_emergency_readiness AS
SELECT
    tenant_code,
    drill_type,
    count(*) AS drills_scheduled,
    sum(CASE WHEN conducted_date IS NOT NULL THEN 1 ELSE 0 END) AS drills_conducted,
    round(100.0 * sum(CASE WHEN conducted_date IS NOT NULL THEN 1 ELSE 0 END) / count(*), 1) AS compliance_pct,
    round(avg(response_time_seconds)::numeric, 0) AS avg_response_time_sec,
    max(conducted_date) AS last_conducted,
    min(next_due_date) AS next_due
FROM emergency_drills
GROUP BY 1, 2;

COMMENT ON VIEW v_emergency_readiness IS 'Drill compliance rate and RFFS response times, for AOM Ch 4/5 emergency-readiness reporting.';

COMMIT;

-- =====================================================================
-- Mock data: deterministic on a synthetic row key, idempotent to re-run.
-- ~1 in 8 drills is left un-conducted (still due) so the readiness view is
-- never 100% across the board.
-- =====================================================================

INSERT INTO emergency_drills (tenant_code, drill_type, scheduled_date, conducted_date, participants_count, response_time_seconds, outcome, next_due_date)
SELECT
    t.tenant_code,
    d.drill_type,
    CURRENT_DATE - (gs * d.cadence_days || ' days')::INTERVAL,
    CASE WHEN ('x' || substr(md5(t.tenant_code || d.drill_type || gs::text || 'held'), 1, 8))::bit(32)::int % 8 = 0
        THEN NULL
        ELSE CURRENT_DATE - (gs * d.cadence_days - (('x' || substr(md5(t.tenant_code || d.drill_type || gs::text || 'slip'), 1, 8))::bit(32)::int % 5) || ' days')::INTERVAL
    END,
    d.min_participants + (('x' || substr(md5(t.tenant_code || d.drill_type || gs::text || 'part'), 1, 8))::bit(32)::int % 20),
    CASE WHEN d.drill_type = 'RFFS Response Drill'
        THEN 120 + (('x' || substr(md5(t.tenant_code || d.drill_type || gs::text || 'rt'), 1, 8))::bit(32)::int % 90)
        ELSE NULL
    END,
    CASE ('x' || substr(md5(t.tenant_code || d.drill_type || gs::text || 'outcome'), 1, 8))::bit(32)::int % 5
        WHEN 0 THEN 'NEEDS_IMPROVEMENT'
        ELSE 'SATISFACTORY'
    END,
    CURRENT_DATE - ((gs - 1) * d.cadence_days || ' days')::INTERVAL
FROM (VALUES ('VIDP'), ('LIRN'), ('YBBN')) AS t(tenant_code)
CROSS JOIN (VALUES
    ('Full-Scale Emergency Exercise', 730, 40),
    ('Partial/Table-Top Exercise', 365, 15),
    ('RFFS Response Drill', 30, 6)
) AS d(drill_type, cadence_days, min_participants)
CROSS JOIN generate_series(1, 4) gs
WHERE NOT EXISTS (
    SELECT 1 FROM emergency_drills e WHERE e.tenant_code = t.tenant_code AND e.drill_type = d.drill_type
);
