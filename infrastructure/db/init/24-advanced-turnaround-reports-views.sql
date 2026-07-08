-- Advanced Turnaround, GHA/Airline SLA, Apron Compliance & Infra Health Views
-- Date: 2026-07-04
-- Backs the new Superset reports added in infrastructure/superset/create-all-reports.sh.
-- Same conventions as infrastructure/db/init/09-superset-report-views.sql:
--  - CREATE OR REPLACE VIEW, COMMENT ON VIEW, 90-day rolling windows.
-- Deduplication notes (per request): "Safety Zone Infringement Heatmaps" is
-- already covered by the existing v_violation_heatmap_latest (grid x zone_type);
-- no new view added for it. PBB/cone-placement/GPU-connection latency has no
-- backing telemetry anywhere in this system and is intentionally not modeled.

BEGIN;

-- 1) Turnaround Milestone Efficiency: actual vs baseline target per task type
CREATE OR REPLACE VIEW v_turnaround_milestone_efficiency AS
SELECT t.tenant_code,
       t.task_type,
       (CASE t.task_type
           WHEN 'FUEL' THEN 15 WHEN 'CATERING' THEN 15 WHEN 'CLEANING' THEN 10
           WHEN 'BAGGAGE_LOAD' THEN 15 WHEN 'UNLOADING' THEN 15 WHEN 'LOADING' THEN 15
           WHEN 'PASSENGER_BOARD' THEN 20 WHEN 'BOARDING' THEN 20
           WHEN 'DISEMBARKATION' THEN 15 WHEN 'PUSHBACK' THEN 10 ELSE 15
       END)::numeric AS baseline_target_min,
       avg(extract(epoch FROM (t.actual_end - t.actual_start)) / 60.0) AS actual_avg_min,
       avg(extract(epoch FROM (t.actual_end - t.actual_start)) / 60.0) - (CASE t.task_type
           WHEN 'FUEL' THEN 15 WHEN 'CATERING' THEN 15 WHEN 'CLEANING' THEN 10
           WHEN 'BAGGAGE_LOAD' THEN 15 WHEN 'UNLOADING' THEN 15 WHEN 'LOADING' THEN 15
           WHEN 'PASSENGER_BOARD' THEN 20 WHEN 'BOARDING' THEN 20
           WHEN 'DISEMBARKATION' THEN 15 WHEN 'PUSHBACK' THEN 10 ELSE 15
       END) AS variance_min,
       count(*) AS tasks
FROM turnaround_tasks t
WHERE t.actual_start IS NOT NULL AND t.actual_end IS NOT NULL
  AND t.planned_end >= now() - interval '90 days'
GROUP BY t.tenant_code, t.task_type;

COMMENT ON VIEW v_turnaround_milestone_efficiency IS 'Actual vs baseline target duration per turnaround milestone (task type).';

-- 2) Critical Path / Milestone Concurrency Ratio
CREATE OR REPLACE VIEW v_turnaround_concurrency_ratio AS
WITH task_durs AS (
    SELECT session_id, sum(extract(epoch FROM (actual_end - actual_start))) AS task_seconds
    FROM turnaround_tasks
    WHERE actual_start IS NOT NULL AND actual_end IS NOT NULL
    GROUP BY session_id
)
SELECT s.tenant_code,
       s.flight_id,
       s.stand_id,
       td.task_seconds / 60.0 AS sum_task_minutes,
       extract(epoch FROM (s.aobt - s.aibt)) / 60.0 AS total_turnaround_minutes,
       td.task_seconds / nullif(extract(epoch FROM (s.aobt - s.aibt)), 0) AS concurrency_ratio
FROM turnaround_sessions s
JOIN task_durs td ON td.session_id = s.id
WHERE s.aibt IS NOT NULL AND s.aobt IS NOT NULL AND s.aobt > s.aibt
  AND s.aibt >= now() - interval '90 days';

COMMENT ON VIEW v_turnaround_concurrency_ratio IS 'Sum of milestone durations vs total turnaround duration; low ratio = sequential/inefficient handling.';

-- 3) First-Wave Departure Readiness (early-morning TOBT window)
CREATE OR REPLACE VIEW v_first_wave_departure_readiness AS
SELECT tenant_code,
       date_trunc('day', tobt) AS day,
       count(*) AS first_wave_flights,
       avg(extract(epoch FROM (aobt - tobt)) / 60.0) AS avg_delay_min,
       sum(CASE WHEN aobt <= tobt THEN 1 ELSE 0 END)::float / count(*) AS on_time_ratio
FROM turnaround_sessions
WHERE tobt IS NOT NULL AND aobt IS NOT NULL
  AND extract(hour FROM tobt) BETWEEN 4 AND 8
  AND tobt >= now() - interval '90 days'
GROUP BY tenant_code, day;

COMMENT ON VIEW v_first_wave_departure_readiness IS 'On-time readiness of early-morning (04:00-08:00 TOBT) first-wave departures.';

-- 4) Ground Handling Delay Root-Cause (per session, mapped to the lagging milestone)
CREATE OR REPLACE VIEW v_delay_root_cause_by_session AS
WITH task_delay AS (
    SELECT session_id, task_type,
           extract(epoch FROM (actual_end - planned_end)) / 60.0 AS delay_min,
           row_number() OVER (PARTITION BY session_id ORDER BY (actual_end - planned_end) DESC) AS rn
    FROM turnaround_tasks
    WHERE actual_end IS NOT NULL AND planned_end IS NOT NULL
)
SELECT s.tenant_code,
       s.flight_id,
       s.stand_id,
       extract(epoch FROM (s.aobt - s.tobt)) / 60.0 AS std_breach_min,
       td.task_type AS lagging_milestone,
       td.delay_min AS lagging_milestone_delay_min
FROM turnaround_sessions s
JOIN task_delay td ON td.session_id = s.id AND td.rn = 1
WHERE s.aobt IS NOT NULL AND s.tobt IS NOT NULL AND s.aobt > s.tobt
  AND s.tobt >= now() - interval '90 days';

COMMENT ON VIEW v_delay_root_cause_by_session IS 'Turnarounds that missed their target off-block time, mapped to the specific milestone that ran latest.';

-- 5) TOBT Volatility Index (per tenant / ground handler)
CREATE OR REPLACE VIEW v_tobt_volatility AS
SELECT tenant_code,
       ground_handler,
       avg(extract(epoch FROM (aobt - tobt)) / 60.0) AS avg_tobt_volatility_min,
       sum(CASE WHEN abs(extract(epoch FROM (aobt - tobt))) > 15 * 60 THEN 1 ELSE 0 END) AS unannounced_delay_count,
       count(*) AS sessions
FROM turnaround_sessions
WHERE tobt IS NOT NULL AND aobt IS NOT NULL
  AND tobt >= now() - interval '90 days'
GROUP BY tenant_code, ground_handler;

COMMENT ON VIEW v_tobt_volatility IS 'TOBT volatility (AOBT - TOBT) by ground handler; flags unannounced delays beyond 15 minutes.';

-- 6) AODB Manual Log vs Computer Vision Audit
CREATE OR REPLACE VIEW v_aodb_cv_discrepancy AS
SELECT tenant_code,
       task_type,
       avg(extract(epoch FROM (aodb_manual_end - actual_end)) / 60.0) AS avg_logging_latency_min,
       sum(CASE WHEN aodb_manual_end < actual_end THEN 1 ELSE 0 END) AS timestamp_padding_count,
       count(*) AS tasks
FROM turnaround_tasks
WHERE actual_end IS NOT NULL AND aodb_manual_end IS NOT NULL
  AND planned_end >= now() - interval '90 days'
GROUP BY tenant_code, task_type;

COMMENT ON VIEW v_aodb_cv_discrepancy IS 'Manual AODB entry time vs computer-vision-captured actual time per milestone; negative latency = timestamp padding.';

-- 7) IATA Delay Code Auto-Attribution
CREATE OR REPLACE VIEW v_iata_delay_code_attribution AS
SELECT tenant_code,
       task_type,
       (CASE task_type
           WHEN 'BAGGAGE_LOAD' THEN '33 - Baggage loading'
           WHEN 'UNLOADING' THEN '32 - Baggage unloading'
           WHEN 'FUEL' THEN '84 - Fueling'
           WHEN 'CATERING' THEN '55 - Catering'
           WHEN 'CLEANING' THEN '56 - Cabin cleaning'
           WHEN 'PASSENGER_BOARD' THEN '63 - Passenger boarding'
           WHEN 'BOARDING' THEN '63 - Passenger boarding'
           WHEN 'DISEMBARKATION' THEN '61 - Passenger disembarkation'
           WHEN 'PUSHBACK' THEN '44 - Aircraft pushback'
           WHEN 'LOADING' THEN '43 - Ramp handling'
           ELSE '99 - Other'
       END) AS iata_delay_code,
       count(*) AS delayed_tasks,
       sum(extract(epoch FROM (actual_end - planned_end)) / 60.0) AS total_delay_min
FROM turnaround_tasks
WHERE actual_end IS NOT NULL AND planned_end IS NOT NULL AND actual_end > planned_end
  AND planned_end >= now() - interval '90 days'
GROUP BY tenant_code, task_type;

COMMENT ON VIEW v_iata_delay_code_attribution IS 'Delayed turnaround tasks mapped to their nearest IATA delay code, for airline/GHA delay-attribution disputes.';

-- 8) ERA / Red-Zone Pre-Arrival & Post-Departure Encroachment
CREATE OR REPLACE VIEW v_era_redzone_encroachment AS
SELECT zv.tenant_code,
       zv.stand_id,
       (CASE
           WHEN zv.timestamp < s.aibt THEN 'PRE_ARRIVAL'
           WHEN zv.timestamp > s.aobt THEN 'POST_DEPARTURE'
           ELSE 'IN_TURNAROUND'
       END) AS encroachment_phase,
       zv.severity,
       count(*) AS encroachments
FROM zone_violations zv
JOIN turnaround_sessions s
  ON s.tenant_code = zv.tenant_code AND s.stand_id = zv.stand_id
 AND zv.timestamp BETWEEN s.aibt - interval '2 hours' AND s.aobt + interval '2 hours'
WHERE zv.zone_type IN ('RESTRICTED', 'PROHIBITED')
  AND s.aibt IS NOT NULL AND s.aobt IS NOT NULL
  AND s.aibt >= now() - interval '90 days'
GROUP BY zv.tenant_code, zv.stand_id, encroachment_phase, zv.severity;

COMMENT ON VIEW v_era_redzone_encroachment IS 'Red-zone/ERA infringements classified as pre-arrival, in-turnaround or post-departure relative to chocks-on/chocks-off (AIBT/AOBT).';

-- 9) FOD Exposure Window (unmonitored gap between successive stand occupations)
CREATE OR REPLACE VIEW v_fod_exposure_window AS
WITH stand_sessions AS (
    SELECT tenant_code, stand_id, aibt, aobt,
           lead(aibt) OVER (PARTITION BY tenant_code, stand_id ORDER BY aobt) AS next_aibt
    FROM turnaround_sessions
    WHERE aibt IS NOT NULL AND aobt IS NOT NULL
      AND aibt >= now() - interval '90 days'
)
SELECT ss.tenant_code,
       ss.stand_id,
       ss.aobt AS chocks_off,
       ss.next_aibt AS next_chocks_on,
       extract(epoch FROM (ss.next_aibt - ss.aobt)) / 60.0 AS fod_exposure_window_min,
       EXISTS (
           SELECT 1 FROM zone_violations zv
           WHERE zv.tenant_code = ss.tenant_code AND zv.stand_id = ss.stand_id
             AND zv.timestamp BETWEEN ss.aobt AND ss.next_aibt
       ) AS unmonitored_breach_detected
FROM stand_sessions ss
WHERE ss.next_aibt IS NOT NULL AND ss.next_aibt > ss.aobt;

COMMENT ON VIEW v_fod_exposure_window IS 'Time window between one aircraft chocks-off and the next chocks-on at the same stand, flagged when no clearance/verification breach check occurred.';

-- 10) PPE & GSE Compliance
CREATE OR REPLACE VIEW v_ppe_gse_compliance AS
SELECT tenant_code,
       stand_id,
       date_trunc('day', "timestamp") AS day,
       sum(personnel_detected) AS personnel_detected,
       sum(ppe_compliant_count) AS ppe_compliant,
       round(100.0 * sum(ppe_compliant_count) / nullif(sum(personnel_detected), 0), 1) AS ppe_compliance_pct,
       sum(CASE WHEN gse_position_violation THEN 1 ELSE 0 END) AS gse_position_violations,
       count(*) AS events
FROM ppe_compliance_events
WHERE "timestamp" >= now() - interval '90 days'
GROUP BY tenant_code, stand_id, day;

COMMENT ON VIEW v_ppe_gse_compliance IS 'Daily PPE compliance rate and GSE clearance-zone positioning violations per stand.';

-- 11) GHA Performance Scorecard
CREATE OR REPLACE VIEW v_gha_performance_scorecard AS
SELECT s.tenant_code,
       s.ground_handler,
       t.task_type,
       (CASE t.task_type
           WHEN 'FUEL' THEN 15 WHEN 'CATERING' THEN 15 WHEN 'CLEANING' THEN 10
           WHEN 'BAGGAGE_LOAD' THEN 15 WHEN 'UNLOADING' THEN 15 WHEN 'LOADING' THEN 15
           WHEN 'PASSENGER_BOARD' THEN 20 WHEN 'BOARDING' THEN 20
           WHEN 'DISEMBARKATION' THEN 15 WHEN 'PUSHBACK' THEN 10 ELSE 15
       END)::numeric AS target_baseline_min,
       avg(extract(epoch FROM (t.actual_end - t.actual_start)) / 60.0) AS actual_avg_min,
       round(100.0 * sum(CASE WHEN t.actual_end <= t.planned_end THEN 1 ELSE 0 END) / count(*), 1) AS sla_compliance_pct,
       count(*) AS tasks
FROM turnaround_tasks t
JOIN turnaround_sessions s ON s.id = t.session_id
WHERE t.actual_start IS NOT NULL AND t.actual_end IS NOT NULL AND t.planned_end IS NOT NULL
  AND s.ground_handler IS NOT NULL
  AND t.planned_end >= now() - interval '90 days'
GROUP BY s.tenant_code, s.ground_handler, t.task_type;

COMMENT ON VIEW v_gha_performance_scorecard IS 'Ground handling agent performance vs baseline target and SLA compliance %, per milestone.';

-- 12) Airline Turnaround Profile Analytics
CREATE OR REPLACE VIEW v_airline_turnaround_profile AS
SELECT s.tenant_code,
       regexp_replace(s.flight_id, '[0-9].*$', '') AS airline_code,
       s.aircraft_type,
       (CASE st.terminal_id
           WHEN 'INT' THEN 'International' WHEN 'T3' THEN 'International'
           WHEN 'DOM' THEN 'Domestic' WHEN 'T1' THEN 'Domestic'
           WHEN 'GA' THEN 'General Aviation' ELSE 'Unknown'
       END) AS terminal_category,
       avg(extract(epoch FROM (s.aobt - s.aibt)) / 60.0) AS avg_turnaround_min,
       count(*) AS sessions
FROM turnaround_sessions s
LEFT JOIN stands st ON st.tenant_code = s.tenant_code AND st.stand_id = s.stand_id
WHERE s.aibt IS NOT NULL AND s.aobt IS NOT NULL
  AND s.aibt >= now() - interval '90 days'
GROUP BY s.tenant_code, airline_code, s.aircraft_type, terminal_category;

COMMENT ON VIEW v_airline_turnaround_profile IS 'Average turnaround time by airline, aircraft body type (narrow/wide) and terminal (domestic/international).';

-- 13) Stand Dead-Time & Overstay Utilization
CREATE OR REPLACE VIEW v_stand_dead_time_overstay AS
WITH stand_sessions AS (
    SELECT tenant_code, stand_id, aibt, aobt, tobt,
           lead(aibt) OVER (PARTITION BY tenant_code, stand_id ORDER BY aobt) AS next_aibt
    FROM turnaround_sessions
    WHERE aibt IS NOT NULL AND aobt IS NOT NULL
      AND aibt >= now() - interval '90 days'
)
SELECT tenant_code,
       stand_id,
       count(*) AS sessions,
       avg(GREATEST(extract(epoch FROM (aobt - tobt)) / 60.0, 0)) AS avg_overstay_min,
       sum(CASE WHEN next_aibt IS NOT NULL THEN GREATEST(extract(epoch FROM (next_aibt - aobt)) / 60.0, 0) ELSE 0 END) AS total_dead_time_min
FROM stand_sessions
GROUP BY tenant_code, stand_id;

COMMENT ON VIEW v_stand_dead_time_overstay IS 'Per-stand overstay beyond TOBT and accumulated dead-stand time between successive occupations.';

-- 14) Camera Infrastructure Health, Edge Latency & CV Model Drift
CREATE OR REPLACE VIEW v_camera_infra_health AS
SELECT tenant_code,
       camera_id,
       stand_id,
       avg(uptime_pct) AS avg_uptime_pct,
       avg(stream_fps) AS avg_stream_fps,
       avg(frame_drop_rate) AS avg_frame_drop_rate,
       avg(edge_latency_ms) AS avg_edge_latency_ms,
       avg(inference_confidence) AS avg_inference_confidence,
       sum(CASE WHEN manual_override THEN 1 ELSE 0 END) AS manual_overrides,
       count(*) AS readings
FROM camera_health_metrics
WHERE "timestamp" >= now() - interval '30 days'
GROUP BY tenant_code, camera_id, stand_id;

COMMENT ON VIEW v_camera_infra_health IS 'Camera/edge-node uptime, stream quality, latency and CV inference confidence/manual-override rate per stand.';

COMMIT;
