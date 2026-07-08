-- Low-Visibility Procedures (LVP) Events & Turnaround Delay Impact
-- Date: 2026-07-06
-- AOM Ch 3.4/3.5 (Low-visibility / adverse-weather operating procedures).
-- Same convention as 25-ppe-density-walkway-safety.sql: new table + deterministic
-- md5-backfill mock data anchored on NOW(), plus a reporting view.

BEGIN;

CREATE TABLE IF NOT EXISTS lvp_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_code VARCHAR(4) NOT NULL,
    category VARCHAR(10) NOT NULL,
    visibility_meters INT,
    rvr_meters INT,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_lvp_events_tenant ON lvp_events (tenant_code, start_time DESC);

COMMENT ON TABLE lvp_events IS 'CAT I/II/IIIA/IIIB low-visibility periods declared at the aerodrome, for AOM Ch 3.4/3.5 LVP reporting.';

CREATE OR REPLACE VIEW v_lvp_delay_impact AS
SELECT
    e.tenant_code,
    e.category,
    date_trunc('day', e.start_time) AS day,
    count(DISTINCT e.id) AS lvp_events,
    round(sum(extract(epoch FROM (coalesce(e.end_time, e.start_time + interval '2 hours') - e.start_time)) / 60.0)::numeric, 1) AS total_lvp_minutes,
    count(ts.id) AS affected_turnarounds,
    round(avg(extract(epoch FROM (ts.aobt - ts.tobt)) / 60.0)::numeric, 1) AS avg_delay_min
FROM lvp_events e
LEFT JOIN turnaround_sessions ts
    ON ts.tenant_code = e.tenant_code
    AND ts.tobt IS NOT NULL AND ts.aobt IS NOT NULL
    AND ts.aobt BETWEEN e.start_time AND coalesce(e.end_time, e.start_time + interval '2 hours')
GROUP BY 1, 2, 3;

COMMENT ON VIEW v_lvp_delay_impact IS 'Daily LVP declarations per category and their impact on turnaround on-time performance, for AOM Ch 3.4/3.5.';

COMMIT;

-- =====================================================================
-- Mock data: deterministic on a synthetic row key, idempotent to re-run.
-- Only VIDP gets a realistic fog-season pattern (winter early-morning fog is
-- the dominant real-world LVP driver at Delhi); LIRN/YBBN get sparse ad-hoc events.
-- =====================================================================

INSERT INTO lvp_events (tenant_code, category, visibility_meters, rvr_meters, start_time, end_time)
SELECT
    'VIDP',
    (ARRAY['CAT I', 'CAT II', 'CAT IIIA'])[1 + (gs % 3)],
    50 + (gs % 5) * 30,
    150 + (gs % 6) * 50,
    date_trunc('day', NOW()) - ((gs * 3) || ' days')::INTERVAL + INTERVAL '4 hours' + ((gs % 4) * 20 || ' minutes')::INTERVAL,
    date_trunc('day', NOW()) - ((gs * 3) || ' days')::INTERVAL + INTERVAL '4 hours' + ((gs % 4) * 20 || ' minutes')::INTERVAL + ((60 + (gs % 6) * 20) || ' minutes')::INTERVAL
FROM generate_series(1, 30) gs
WHERE NOT EXISTS (SELECT 1 FROM lvp_events WHERE tenant_code = 'VIDP');

INSERT INTO lvp_events (tenant_code, category, visibility_meters, rvr_meters, start_time, end_time)
SELECT
    t.tenant_code,
    'CAT I',
    150 + (gs % 3) * 40,
    300 + (gs % 4) * 50,
    date_trunc('day', NOW()) - ((gs * 11) || ' days')::INTERVAL + INTERVAL '5 hours',
    date_trunc('day', NOW()) - ((gs * 11) || ' days')::INTERVAL + INTERVAL '5 hours' + ((45 + (gs % 4) * 15) || ' minutes')::INTERVAL
FROM (VALUES ('LIRN'), ('YBBN')) AS t(tenant_code)
CROSS JOIN generate_series(1, 6) gs
WHERE NOT EXISTS (SELECT 1 FROM lvp_events WHERE tenant_code = t.tenant_code);
