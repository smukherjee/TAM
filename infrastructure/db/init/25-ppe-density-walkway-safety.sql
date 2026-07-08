-- PPE Compliance Density Index (by GHA) & Passenger Safety Walkway/Boarding Corridor Audit
-- Date: 2026-07-05
-- Follows the same convention as 23-advanced-turnaround-reports.sql: ALTER + deterministic
-- md5-backfill for existing rows, new table + generate_series mock data for new telemetry.
-- Note: "CV Model Drift & Confidence Drift Audit" and "Edge Node Ingestion Latency & Frame
-- Pipeline Diagnostics" are NOT added here - both are already fully covered by the existing
-- v_camera_infra_health view (avg_inference_confidence/manual_overrides and
-- avg_edge_latency_ms/avg_frame_drop_rate respectively).

BEGIN;

-- Lets PPE compliance be broken out by ground handling agent, not just by stand.
ALTER TABLE ppe_compliance_events ADD COLUMN IF NOT EXISTS ground_handler VARCHAR(100);

-- Passenger walkway / boarding corridor camera events: machinery obstructions on the
-- walkway and passengers detected outside the marked safe corridor. Aggregate counts
-- only (no individual tracking), consistent with the privacy stance already taken for
-- PPE compliance monitoring.
CREATE TABLE IF NOT EXISTS walkway_safety_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_code VARCHAR(4) NOT NULL,
    stand_id VARCHAR(10),
    corridor_zone VARCHAR(100) NOT NULL,
    obstruction_detected BOOLEAN NOT NULL DEFAULT false,
    obstruction_source VARCHAR(50),
    pedestrian_excursion_count INT NOT NULL DEFAULT 0,
    "timestamp" TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_walkway_safety_tenant ON walkway_safety_events (tenant_code, "timestamp" DESC);

COMMIT;

-- =====================================================================
-- Mock data: backfill new column, seed new table.
-- Deterministic on id (md5-based) so re-running this script is idempotent.
-- =====================================================================

UPDATE ppe_compliance_events
SET ground_handler = CASE WHEN ('x' || substr(md5(id::text || 'gh'), 1, 8))::bit(32)::int % 2 = 0
        THEN 'Bird Group' ELSE 'TajSAT' END
WHERE ground_handler IS NULL;

-- Boarding-corridor checks: every ~2h per stand over the last 60 days.
INSERT INTO walkway_safety_events (
    tenant_code, stand_id, corridor_zone, obstruction_detected,
    obstruction_source, pedestrian_excursion_count, "timestamp"
)
SELECT
    s.tenant_code,
    s.stand_id,
    s.stand_id || ' Boarding Corridor',
    (gs % 13 = 0) AS obstruction_detected,
    CASE WHEN gs % 13 = 0 THEN (ARRAY['GSE','BAGGAGE_CART','CATERING_TRUCK'])[1 + (gs % 3)] ELSE NULL END,
    CASE WHEN gs % 9 = 0 THEN 1 + (gs % 4) ELSE 0 END AS pedestrian_excursion_count,
    NOW() - ((gs * 2) || ' hours')::INTERVAL
FROM stands s
CROSS JOIN generate_series(1, 12 * 60) gs
WHERE NOT EXISTS (
    SELECT 1 FROM walkway_safety_events e
    WHERE e.tenant_code = s.tenant_code AND e.stand_id = s.stand_id
);
