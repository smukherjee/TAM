-- Views for PPE Compliance Density Index (by GHA) & Passenger Walkway Safety Audit
-- Date: 2026-07-05
-- Same convention as 09-superset-report-views.sql / 24-advanced-turnaround-reports-views.sql.

BEGIN;

-- PPE Compliance Density Index: compliance rate per ground handler and apron zone.
-- Aggregate-only (no personnel identity), matching the WHS/privacy requirement.
CREATE OR REPLACE VIEW v_ppe_compliance_density_index AS
SELECT tenant_code,
       ground_handler,
       camera_zone AS apron_zone,
       date_trunc('day', "timestamp") AS day,
       sum(personnel_detected) AS personnel_detected,
       sum(ppe_compliant_count) AS ppe_compliant,
       round(100.0 * sum(ppe_compliant_count) / nullif(sum(personnel_detected), 0), 1) AS compliance_density_pct,
       sum(CASE WHEN gse_position_violation THEN 1 ELSE 0 END) AS gse_position_violations,
       count(*) AS checks
FROM ppe_compliance_events
WHERE "timestamp" >= now() - interval '90 days'
GROUP BY tenant_code, ground_handler, apron_zone, day;

COMMENT ON VIEW v_ppe_compliance_density_index IS 'PPE compliance density (compliant/detected personnel) per ground handling agent and apron zone, for BAC Airside Safety / WHS reporting.';

-- Passenger Safety Walkway & Boarding Corridor Audit.
CREATE OR REPLACE VIEW v_walkway_safety_audit AS
SELECT tenant_code,
       stand_id,
       corridor_zone,
       date_trunc('day', "timestamp") AS day,
       count(*) AS checks,
       sum(CASE WHEN obstruction_detected THEN 1 ELSE 0 END) AS obstruction_count,
       round(100.0 * sum(CASE WHEN obstruction_detected THEN 1 ELSE 0 END) / count(*), 1) AS obstruction_rate_pct,
       sum(pedestrian_excursion_count) AS pedestrian_excursions
FROM walkway_safety_events
WHERE "timestamp" >= now() - interval '90 days'
GROUP BY tenant_code, stand_id, corridor_zone, day;

COMMENT ON VIEW v_walkway_safety_audit IS 'Boarding-corridor machinery obstructions and passenger excursions outside marked walkways, per stand/day.';

COMMIT;
