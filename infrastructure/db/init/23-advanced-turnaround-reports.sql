-- Advanced Turnaround, GHA/Airline SLA, Apron Compliance & Infra Health: schema + mock data
-- Date: 2026-07-04
-- Adds the columns/tables needed for new Superset reports (see
-- infrastructure/superset/create-all-reports.sh) that could not be built from
-- existing schema alone. Follows the same generate_series + CASE(gs % n)
-- mock-data convention as infrastructure/db/init/15-populate-report-data.sql.

BEGIN;

ALTER TABLE turnaround_sessions ADD COLUMN IF NOT EXISTS ground_handler VARCHAR(100);
ALTER TABLE turnaround_sessions ADD COLUMN IF NOT EXISTS aircraft_type VARCHAR(20);
ALTER TABLE turnaround_tasks ADD COLUMN IF NOT EXISTS aodb_manual_end TIMESTAMPTZ;
-- Lets zone_violations be joined to a real stand/session for pre-arrival,
-- post-departure and FOD-exposure-window reporting (mock data had no stand link).
ALTER TABLE zone_violations ADD COLUMN IF NOT EXISTS stand_id VARCHAR(10);

-- PPE & GSE clearance-zone compliance events, detected by apron cameras.
CREATE TABLE IF NOT EXISTS ppe_compliance_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_code VARCHAR(4) NOT NULL,
    stand_id VARCHAR(10),
    camera_zone VARCHAR(100) NOT NULL,
    personnel_detected INT NOT NULL,
    ppe_compliant_count INT NOT NULL,
    gse_position_violation BOOLEAN NOT NULL DEFAULT false,
    "timestamp" TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_ppe_compliance_tenant ON ppe_compliance_events (tenant_code, "timestamp" DESC);

-- Apron camera / edge-node telemetry: uptime, stream health, CV inference confidence.
CREATE TABLE IF NOT EXISTS camera_health_metrics (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_code VARCHAR(4) NOT NULL,
    camera_id VARCHAR(50) NOT NULL,
    stand_id VARCHAR(10),
    uptime_pct DOUBLE PRECISION,
    stream_fps DOUBLE PRECISION,
    frame_drop_rate DOUBLE PRECISION,
    edge_latency_ms DOUBLE PRECISION,
    inference_confidence DOUBLE PRECISION,
    manual_override BOOLEAN NOT NULL DEFAULT false,
    "timestamp" TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_camera_health_tenant ON camera_health_metrics (tenant_code, "timestamp" DESC);

COMMIT;

-- =====================================================================
-- Mock data: backfill new columns on existing rows, seed new tables.
-- Deterministic on id (md5-based) so re-running this script is idempotent.
-- =====================================================================

UPDATE turnaround_sessions
SET ground_handler = CASE WHEN ('x' || substr(md5(id::text), 1, 8))::bit(32)::int % 2 = 0
        THEN 'Bird Group' ELSE 'TajSAT' END
WHERE ground_handler IS NULL;

UPDATE turnaround_sessions
SET aircraft_type = CASE WHEN ('x' || substr(md5(id::text || 'ac'), 1, 8))::bit(32)::int % 3 = 0
        THEN 'Widebody' ELSE 'Narrowbody' END
WHERE aircraft_type IS NULL;

-- Simulated AODB manual entry vs computer-vision actual_end: mostly logged a
-- few minutes late (lag), occasionally logged early ("timestamp padding").
UPDATE turnaround_tasks
SET aodb_manual_end = actual_end
        + ((((('x' || substr(md5(id::text), 1, 8))::bit(32)::int % 21) - 5)) || ' minutes')::interval
WHERE actual_end IS NOT NULL AND aodb_manual_end IS NULL;

-- Assign each zone_violation to one of its tenant's real stands (deterministic on id).
WITH ranked_stands AS (
    SELECT tenant_code, stand_id,
           row_number() OVER (PARTITION BY tenant_code ORDER BY stand_id) AS rn,
           count(*) OVER (PARTITION BY tenant_code) AS cnt
    FROM stands
)
UPDATE zone_violations zv
SET stand_id = rs.stand_id
FROM ranked_stands rs
WHERE zv.stand_id IS NULL
  AND rs.tenant_code = zv.tenant_code
  AND rs.rn = (('x' || substr(md5(zv.id::text), 1, 8))::bit(32)::int % rs.cnt) + 1;

-- PPE / GSE compliance events: every ~12h per stand over the last 90 days.
-- Guarded by a per-stand existence check so re-running this file doesn't duplicate rows.
INSERT INTO ppe_compliance_events (
    tenant_code, stand_id, camera_zone, personnel_detected,
    ppe_compliant_count, gse_position_violation, "timestamp"
)
SELECT
    s.tenant_code,
    s.stand_id,
    s.stand_id || ' Clearance Zone',
    (3 + (gs % 6)) AS personnel_detected,
    GREATEST(0, (3 + (gs % 6)) - (CASE WHEN gs % 7 = 0 THEN 1 + (gs % 3) ELSE 0 END)) AS ppe_compliant_count,
    (gs % 11 = 0) AS gse_position_violation,
    NOW() - ((gs * 12) || ' hours')::INTERVAL
FROM stands s
CROSS JOIN generate_series(1, 180) gs
WHERE NOT EXISTS (
    SELECT 1 FROM ppe_compliance_events e
    WHERE e.tenant_code = s.tenant_code AND e.stand_id = s.stand_id
);

-- Camera / edge-node health: hourly per stand over the last 30 days.
-- Guarded by a per-stand existence check so re-running this file doesn't duplicate rows.
INSERT INTO camera_health_metrics (
    tenant_code, camera_id, stand_id, uptime_pct, stream_fps,
    frame_drop_rate, edge_latency_ms, inference_confidence, manual_override, "timestamp"
)
SELECT
    s.tenant_code,
    'CAM-' || s.stand_id,
    s.stand_id,
    CASE WHEN gs % 47 = 0 THEN 60 + random() * 30 ELSE 98 + random() * 2 END,
    CASE WHEN gs % 47 = 0 THEN 5 + random() * 10 ELSE 24 + random() * 6 END,
    CASE WHEN gs % 47 = 0 THEN 0.1 + random() * 0.3 ELSE random() * 0.02 END,
    CASE WHEN gs % 47 = 0 THEN 800 + random() * 2000 ELSE 150 + random() * 350 END,
    CASE WHEN gs % 23 = 0 THEN 0.55 + random() * 0.2 ELSE 0.85 + random() * 0.14 END,
    (gs % 23 = 0),
    NOW() - (gs || ' hours')::INTERVAL
FROM stands s
CROSS JOIN generate_series(1, 24 * 30) gs
WHERE NOT EXISTS (
    SELECT 1 FROM camera_health_metrics m
    WHERE m.tenant_code = s.tenant_code AND m.stand_id = s.stand_id
);
