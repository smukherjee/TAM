-- Mandatory Occurrence Report (MOR) Packaging
-- Date: 2026-07-06
-- AOM Ch 6 (SMS occurrence reporting, regulator-facing). Runs after
-- 30-wildlife-hazard.sql since it packages HIGH/CRITICAL zone_violations and
-- HIGH-severity wildlife_events into formal, regulator-submittable occurrence
-- records (status + corrective action + regulator reference), rather than
-- raw telemetry. Same md5-deterministic backfill convention as elsewhere.

BEGIN;

CREATE TABLE IF NOT EXISTS occurrence_reports (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_code VARCHAR(4) NOT NULL,
    source_type VARCHAR(20) NOT NULL,
    source_id UUID NOT NULL,
    occurrence_category VARCHAR(50) NOT NULL,
    description VARCHAR(300) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    occurrence_date TIMESTAMPTZ NOT NULL,
    reported_at TIMESTAMPTZ DEFAULT now(),
    corrective_action VARCHAR(300),
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    regulator_reference VARCHAR(50),
    created_at TIMESTAMPTZ DEFAULT now()
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_occurrence_source ON occurrence_reports (source_type, source_id);
CREATE INDEX IF NOT EXISTS idx_occurrence_tenant ON occurrence_reports (tenant_code, occurrence_date DESC);

COMMENT ON TABLE occurrence_reports IS 'Regulator-facing Mandatory Occurrence Reports packaged from HIGH/CRITICAL safety events, for AOM Ch 6 SMS occurrence reporting.';

CREATE OR REPLACE VIEW v_mandatory_occurrence_report AS
SELECT
    tenant_code,
    occurrence_category,
    severity,
    status,
    date_trunc('month', occurrence_date) AS month,
    count(*) AS occurrences,
    sum(CASE WHEN status IN ('OPEN', 'UNDER_REVIEW') THEN 1 ELSE 0 END) AS open_occurrences,
    sum(CASE WHEN status = 'SUBMITTED_TO_REGULATOR' THEN 1 ELSE 0 END) AS submitted_to_regulator
FROM occurrence_reports
GROUP BY 1, 2, 3, 4, 5;

COMMENT ON VIEW v_mandatory_occurrence_report IS 'Monthly MOR volume and disposition status, for AOM Ch 6 regulator reporting.';

COMMIT;

-- =====================================================================
-- Mock data: one occurrence_report per HIGH/CRITICAL zone_violation and per
-- HIGH-severity wildlife_event, deterministic on source id (md5-based status
-- bucket) so re-running is idempotent (unique index on source_type/source_id
-- doubles as the guard).
-- =====================================================================

INSERT INTO occurrence_reports (tenant_code, source_type, source_id, occurrence_category, description, severity, occurrence_date, corrective_action, status, regulator_reference)
SELECT
    v.tenant_code,
    'ZONE_VIOLATION',
    v.id,
    CASE v.violation_type
        WHEN 'UNAUTHORIZED_ENTRY' THEN 'APRON_SAFETY'
        ELSE 'RUNWAY_INCURSION'
    END,
    'Unauthorized entry into ' || v.zone_name || ' by asset near stand ' || coalesce(v.stand_id, 'N/A'),
    v.severity,
    v."timestamp",
    CASE ('x' || substr(md5(v.id::text || 'action'), 1, 8))::bit(32)::int % 3
        WHEN 0 THEN 'Zone signage and lighting reviewed; refresher briefing issued to GHA'
        WHEN 1 THEN 'Access control barrier repositioned; incident logged with GHA supervisor'
        ELSE 'Under investigation'
    END,
    CASE ('x' || substr(md5(v.id::text || 'status'), 1, 8))::bit(32)::int % 4
        WHEN 0 THEN 'OPEN'
        WHEN 1 THEN 'UNDER_REVIEW'
        WHEN 2 THEN 'CLOSED'
        ELSE 'SUBMITTED_TO_REGULATOR'
    END,
    CASE WHEN ('x' || substr(md5(v.id::text || 'status'), 1, 8))::bit(32)::int % 4 = 3
        THEN 'MOR-' || v.tenant_code || '-' || to_char(v."timestamp", 'YYYY') || '-' || substr(v.id::text, 1, 8)
        ELSE NULL
    END
FROM zone_violations v
WHERE v.severity IN ('HIGH', 'CRITICAL')
  AND NOT EXISTS (SELECT 1 FROM occurrence_reports r WHERE r.source_type = 'ZONE_VIOLATION' AND r.source_id = v.id);

INSERT INTO occurrence_reports (tenant_code, source_type, source_id, occurrence_category, description, severity, occurrence_date, corrective_action, status, regulator_reference)
SELECT
    w.tenant_code,
    'WILDLIFE_STRIKE',
    w.id,
    'WILDLIFE_STRIKE',
    w.event_type || ' involving ' || w.species || ' at ' || w.zone,
    w.severity,
    w."timestamp",
    'Dispersal team dispatched; habitat/attractant review scheduled',
    CASE ('x' || substr(md5(w.id::text || 'status'), 1, 8))::bit(32)::int % 4
        WHEN 0 THEN 'OPEN'
        WHEN 1 THEN 'UNDER_REVIEW'
        WHEN 2 THEN 'CLOSED'
        ELSE 'SUBMITTED_TO_REGULATOR'
    END,
    CASE WHEN ('x' || substr(md5(w.id::text || 'status'), 1, 8))::bit(32)::int % 4 = 3
        THEN 'MOR-' || w.tenant_code || '-' || to_char(w."timestamp", 'YYYY') || '-' || substr(w.id::text, 1, 8)
        ELSE NULL
    END
FROM wildlife_events w
WHERE w.severity = 'HIGH'
  AND NOT EXISTS (SELECT 1 FROM occurrence_reports r WHERE r.source_type = 'WILDLIFE_STRIKE' AND r.source_id = w.id);
