-- Asset/Vehicle Certification & Fitness Expiry, and Asset Downtime/Maintenance Log
-- Date: 2026-07-06
-- AOM Ch 3.1 (airside vehicle/equipment permit & fitness compliance) and
-- Ch 7 (aerodrome/equipment maintenance). Same convention as
-- 25-ppe-density-walkway-safety.sql: new tables + deterministic md5-backfill mock data.
--
-- Note: v_maintenance_downtime_by_type (09-superset-report-views.sql) already reports a
-- point-in-time snapshot of assets currently in "Maintenance" status. asset_maintenance_log
-- complements it with an actual historical event log (start/end, reason, duration).

BEGIN;

CREATE TABLE IF NOT EXISTS asset_certifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_code VARCHAR(4) NOT NULL,
    asset_id UUID NOT NULL REFERENCES assets(id),
    cert_type VARCHAR(50) NOT NULL,
    issued_date DATE NOT NULL,
    expiry_date DATE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_asset_cert_tenant ON asset_certifications (tenant_code, expiry_date);
CREATE INDEX IF NOT EXISTS idx_asset_cert_asset ON asset_certifications (asset_id);

COMMENT ON TABLE asset_certifications IS 'Airside driving permits, fitness certificates, insurance and calibration certificates per GSE/vehicle asset, for AOM Ch 3.1 compliance.';

CREATE TABLE IF NOT EXISTS asset_maintenance_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_code VARCHAR(4) NOT NULL,
    asset_id UUID NOT NULL REFERENCES assets(id),
    maintenance_type VARCHAR(20) NOT NULL,
    reason VARCHAR(100) NOT NULL,
    downtime_start TIMESTAMPTZ NOT NULL,
    downtime_end TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_asset_maint_tenant ON asset_maintenance_log (tenant_code, downtime_start DESC);
CREATE INDEX IF NOT EXISTS idx_asset_maint_asset ON asset_maintenance_log (asset_id);

COMMENT ON TABLE asset_maintenance_log IS 'Historical GSE/vehicle downtime events (scheduled service vs breakdown), for AOM Ch 7 maintenance reporting.';

CREATE OR REPLACE VIEW v_asset_certification_status AS
SELECT
    c.tenant_code,
    a.asset_id,
    a.name AS asset_name,
    a.category,
    c.cert_type,
    c.issued_date,
    c.expiry_date,
    CASE
        WHEN c.expiry_date < CURRENT_DATE THEN 'EXPIRED'
        WHEN c.expiry_date < CURRENT_DATE + INTERVAL '30 days' THEN 'EXPIRING_SOON'
        ELSE 'VALID'
    END AS cert_status
FROM asset_certifications c
JOIN assets a ON a.id = c.asset_id;

COMMENT ON VIEW v_asset_certification_status IS 'Certificate/permit expiry status per asset, for AOM Ch 3.1 fitness-to-operate compliance.';

CREATE OR REPLACE VIEW v_asset_maintenance_log AS
SELECT
    m.tenant_code,
    a.category,
    m.maintenance_type,
    m.reason,
    count(*) AS events,
    round(avg(extract(epoch FROM (coalesce(m.downtime_end, now()) - m.downtime_start)) / 3600.0)::numeric, 1) AS avg_downtime_hours,
    round(sum(extract(epoch FROM (coalesce(m.downtime_end, now()) - m.downtime_start)) / 3600.0)::numeric, 1) AS total_downtime_hours
FROM asset_maintenance_log m
JOIN assets a ON a.id = m.asset_id
GROUP BY 1, 2, 3, 4;

COMMENT ON VIEW v_asset_maintenance_log IS 'GSE/vehicle downtime hours by category and reason, for AOM Ch 7 maintenance reporting.';

COMMIT;

-- =====================================================================
-- Mock data: deterministic on asset id (md5-based), idempotent to re-run.
-- Certification: every airside-operational asset gets 1-2 certs, ~10% pre-expired
-- and ~10% expiring within 30 days (so the status view is never all-green).
-- Maintenance log: ~1 in 6 assets currently in "Maintenance" gets an open downtime
-- event; plus a trailing history of closed events over the last 90 days.
-- =====================================================================

INSERT INTO asset_certifications (tenant_code, asset_id, cert_type, issued_date, expiry_date)
SELECT
    a.tenant_code,
    a.id,
    ct.cert_type,
    (CURRENT_DATE - INTERVAL '2 years') + ((('x' || substr(md5(a.id::text || ct.cert_type || 'issued'), 1, 8))::bit(32)::int % 400) || ' days')::INTERVAL,
    CASE ('x' || substr(md5(a.id::text || ct.cert_type || 'bucket'), 1, 8))::bit(32)::int % 10
        WHEN 0 THEN CURRENT_DATE - ((('x' || substr(md5(a.id::text || ct.cert_type || 'past'), 1, 8))::bit(32)::int % 60 + 1) || ' days')::INTERVAL
        WHEN 1 THEN CURRENT_DATE + ((('x' || substr(md5(a.id::text || ct.cert_type || 'soon'), 1, 8))::bit(32)::int % 29 + 1) || ' days')::INTERVAL
        ELSE CURRENT_DATE + ((('x' || substr(md5(a.id::text || ct.cert_type || 'future'), 1, 8))::bit(32)::int % 700 + 31) || ' days')::INTERVAL
    END
FROM assets a
CROSS JOIN (VALUES ('Airside Driving Permit'), ('Fitness Certificate')) AS ct(cert_type)
WHERE a.category IN ('Ground Support', 'Transport', 'Fueling', 'De-icing', 'Baggage', 'Catering', 'Power')
  AND NOT EXISTS (SELECT 1 FROM asset_certifications e WHERE e.asset_id = a.id AND e.cert_type = ct.cert_type);

INSERT INTO asset_maintenance_log (tenant_code, asset_id, maintenance_type, reason, downtime_start, downtime_end)
SELECT
    a.tenant_code,
    a.id,
    'UNSCHEDULED',
    (ARRAY['Hydraulic Fault', 'Battery Failure', 'Accident Damage', 'Engine Warning'])[1 + abs(('x' || substr(md5(a.id::text || 'reason'), 1, 8))::bit(32)::int % 4)],
    now() - ((('x' || substr(md5(a.id::text || 'age'), 1, 8))::bit(32)::int % 48 + 1) || ' hours')::INTERVAL,
    NULL
FROM assets a
WHERE a.status = 'Maintenance'
  AND NOT EXISTS (SELECT 1 FROM asset_maintenance_log l WHERE l.asset_id = a.id AND l.downtime_end IS NULL);

INSERT INTO asset_maintenance_log (tenant_code, asset_id, maintenance_type, reason, downtime_start, downtime_end)
SELECT
    a.tenant_code,
    a.id,
    CASE WHEN gs % 3 = 0 THEN 'UNSCHEDULED' ELSE 'SCHEDULED' END,
    CASE WHEN gs % 3 = 0
        THEN (ARRAY['Hydraulic Fault', 'Battery Failure', 'Tyre Puncture', 'Electrical Fault'])[1 + (gs % 4)]
        ELSE (ARRAY['Scheduled Service', 'Calibration Check', 'Tyre Rotation'])[1 + (gs % 3)]
    END,
    now() - ((gs * 240 + (('x' || substr(md5(a.id::text || gs::text), 1, 8))::bit(32)::int % 48)) || ' hours')::INTERVAL,
    now() - ((gs * 240 + (('x' || substr(md5(a.id::text || gs::text), 1, 8))::bit(32)::int % 48) - (2 + gs % 6)) || ' hours')::INTERVAL
FROM assets a
CROSS JOIN generate_series(1, 3) gs
WHERE ('x' || substr(md5(a.id::text || 'history'), 1, 8))::bit(32)::int % 4 = 0
  AND NOT EXISTS (
      SELECT 1 FROM asset_maintenance_log l
      WHERE l.asset_id = a.id AND l.downtime_end IS NOT NULL
  );
