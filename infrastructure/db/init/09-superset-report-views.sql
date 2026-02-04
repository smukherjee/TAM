-- Superset Report Views: Static and Aggregated Datasets
-- Date: 2026-02-04
-- Purpose: Provide SQL views backing 20+ Superset reports across Ops, Safety, Turnaround, Assets, Pipeline
-- Notes:
--  - Idempotent CREATE VIEW IF NOT EXISTS where possible; for updates, DROP/CREATE may be needed.
--  - Assumes existing tables: flights, vehicles, asset_location_register, asset_movement_trail,
--    turnaround_sessions, turnaround_tasks, zone_violations, movement_discrepancies, sensor_alerts,
--    and materialized views: asset_activity_heatmap, violation_heatmap, asset_dwell_heatmap.

BEGIN;

-- 1) Operations Overview (Daily) per tenant
CREATE OR REPLACE VIEW v_ops_overview_daily AS
WITH flights_daily AS (
  SELECT tenant_code, date_trunc('day', timestamp) AS day,
       count(DISTINCT flight_number) AS flights
  FROM flights
  WHERE timestamp >= now() - interval '90 days'
  GROUP BY tenant_code, day
), vehicles_daily AS (
  SELECT tenant_code, date_trunc('day', timestamp) AS day,
       count(*) AS vehicles
  FROM vehicles
  WHERE timestamp >= now() - interval '90 days'
  GROUP BY tenant_code, day
), alerts_daily AS (
  SELECT tenant_code, date_trunc('day', timestamp) AS day,
       count(*) AS alerts
  FROM sensor_alerts
  WHERE timestamp >= now() - interval '90 days'
  GROUP BY tenant_code, day
), violations_daily AS (
  SELECT tenant_code, date_trunc('day', timestamp) AS day,
       count(*) AS violations
  FROM zone_violations
  WHERE timestamp >= now() - interval '90 days'
  GROUP BY tenant_code, day
)
SELECT f.tenant_code,
     f.day,
     f.flights,
     coalesce(v.vehicles, 0) AS vehicles,
     coalesce(a.alerts, 0) AS alerts,
     coalesce(z.violations, 0) AS violations
FROM flights_daily f
LEFT JOIN vehicles_daily v ON v.tenant_code = f.tenant_code AND v.day = f.day
LEFT JOIN alerts_daily a   ON a.tenant_code = f.tenant_code AND a.day = f.day
LEFT JOIN violations_daily z ON z.tenant_code = f.tenant_code AND z.day = f.day;

COMMENT ON VIEW v_ops_overview_daily IS 'Daily operations overview per tenant: flights, vehicles, alerts, violations.';

-- 2) Flight Movements by Hour
CREATE OR REPLACE VIEW v_flight_movements_hourly AS
SELECT tenant_code,
       date_trunc('hour', timestamp) AS hour,
       count(*) AS positions
FROM flights
WHERE timestamp >= now() - interval '30 days'
GROUP BY tenant_code, hour;

COMMENT ON VIEW v_flight_movements_hourly IS 'Hourly flight position counts per tenant (movement proxy).';

-- 3) Ground Vehicle Activity Summary (Daily)
CREATE OR REPLACE VIEW v_vehicle_activity_summary_daily AS
SELECT tenant_code,
       date_trunc('day', timestamp) AS day,
       vehicle_type,
       count(*) AS telemetry_points,
       avg(speed) AS avg_speed
FROM vehicles
WHERE timestamp >= now() - interval '30 days'
GROUP BY tenant_code, day, vehicle_type;

COMMENT ON VIEW v_vehicle_activity_summary_daily IS 'Daily vehicle activity summary by type: points and avg speed.';

-- 4) Stand/Gate Occupancy and Avg Turnaround
CREATE OR REPLACE VIEW v_stand_gate_occupancy AS
SELECT tenant_code,
       stand_id,
       count(*) AS sessions,
       avg(extract(epoch FROM (aobt - sirt))/60.0) AS avg_turnaround_min
FROM turnaround_sessions
WHERE sirt IS NOT NULL AND aobt IS NOT NULL
  AND sirt >= now() - interval '90 days'
GROUP BY tenant_code, stand_id;

COMMENT ON VIEW v_stand_gate_occupancy IS 'Stand occupancy: session count and average turnaround duration (minutes).';

-- 5) Turnaround SLA Compliance (per task type)
CREATE OR REPLACE VIEW v_turnaround_sla_compliance AS
SELECT t.tenant_code,
       t.task_type,
       avg(extract(epoch FROM (t.actual_end - t.planned_end))/60.0) AS avg_delay_min,
       sum(CASE WHEN t.actual_end <= t.planned_end THEN 1 ELSE 0 END)::float / nullif(count(*),0) AS on_time_ratio,
       count(*) AS tasks
FROM turnaround_tasks t
WHERE t.actual_end IS NOT NULL AND t.planned_end IS NOT NULL
  AND t.planned_end >= now() - interval '90 days'
GROUP BY t.tenant_code, t.task_type;

COMMENT ON VIEW v_turnaround_sla_compliance IS 'SLA compliance per task type: avg delay and on-time ratio.';

-- 6) Delay Root Causes (task types with positive delays)
CREATE OR REPLACE VIEW v_delay_root_causes AS
SELECT t.tenant_code,
       t.task_type AS cause,
       sum(GREATEST(extract(epoch FROM (t.actual_end - t.planned_end))/60.0, 0)) AS total_delay_min,
       count(*) AS affected_tasks
FROM turnaround_tasks t
WHERE t.actual_end IS NOT NULL AND t.planned_end IS NOT NULL
  AND t.actual_end > t.planned_end
  AND t.planned_end >= now() - interval '90 days'
GROUP BY t.tenant_code, t.task_type
ORDER BY total_delay_min DESC;

COMMENT ON VIEW v_delay_root_causes IS 'Aggregated delay minutes per task type (Pareto analysis).';

-- 7) Speed/Zone Violations by Zone and Severity
CREATE OR REPLACE VIEW v_speed_violations_by_zone AS
SELECT tenant_code,
       zone_name,
       severity,
       count(*) AS violations
FROM zone_violations
WHERE timestamp >= now() - interval '90 days'
GROUP BY tenant_code, zone_name, severity;

COMMENT ON VIEW v_speed_violations_by_zone IS 'Violations count grouped by zone and severity.';

-- 8) Restricted Zone Breach Dwell Stats
CREATE OR REPLACE VIEW v_restricted_zone_breach_dwell AS
SELECT tenant_code,
       zone_name,
       zone_type,
       avg(duration_seconds) AS avg_dwell_sec,
       max(duration_seconds) AS max_dwell_sec,
       count(*) AS breaches
FROM zone_violations
WHERE duration_seconds IS NOT NULL
  AND timestamp >= now() - interval '90 days'
GROUP BY tenant_code, zone_name, zone_type;

COMMENT ON VIEW v_restricted_zone_breach_dwell IS 'Dwell time stats inside restricted zones by zone/type.';

-- 9) Movement Discrepancy Trends (Daily)
CREATE OR REPLACE VIEW v_discrepancy_trends_daily AS
SELECT tenant_code,
       discrepancy_type,
       date_trunc('day', timestamp) AS day,
       count(*) AS discrepancies
FROM movement_discrepancies
WHERE timestamp >= now() - interval '90 days'
GROUP BY tenant_code, discrepancy_type, day;

COMMENT ON VIEW v_discrepancy_trends_daily IS 'Daily discrepancy counts by type per tenant.';

-- 10) Asset Utilization by Status
-- Latest status per asset from movement trail, joined to tenant
CREATE OR REPLACE VIEW v_asset_utilization_status_counts AS
WITH latest_status AS (
  SELECT amt.asset_identifier,
       amt.status,
       amt.tenant_code,
       amt.timestamp,
       row_number() OVER (PARTITION BY amt.asset_identifier ORDER BY amt.timestamp DESC) AS rn
  FROM asset_movement_trail amt
  WHERE amt.status IS NOT NULL
)
SELECT ls.tenant_code,
     ls.status,
     count(*) AS assets
FROM latest_status ls
WHERE ls.rn = 1
GROUP BY ls.tenant_code, ls.status;

COMMENT ON VIEW v_asset_utilization_status_counts IS 'Latest asset status counts per tenant (from movement trail).';

-- 11) Maintenance Downtime by Status
CREATE OR REPLACE VIEW v_maintenance_downtime_by_type AS
WITH latest_status AS (
  SELECT amt.asset_identifier,
       amt.status,
       amt.tenant_code,
       amt.timestamp,
       row_number() OVER (PARTITION BY amt.asset_identifier ORDER BY amt.timestamp DESC) AS rn
  FROM asset_movement_trail amt
  WHERE amt.status IS NOT NULL
)
SELECT ls.tenant_code,
     ls.status,
     count(*) AS count
FROM latest_status ls
WHERE ls.rn = 1 AND ls.status IN ('Maintenance','Out of Service')
GROUP BY ls.tenant_code, ls.status;

COMMENT ON VIEW v_maintenance_downtime_by_type IS 'Assets currently in Maintenance / Out of Service based on latest trail status.';

-- 12) Dwell Proxy by Zone (Hourly movement counts)
CREATE OR REPLACE VIEW v_dwell_proxy_by_zone_hourly AS
SELECT tenant_code,
       zone,
       date_trunc('hour', timestamp) AS hour,
       count(*) AS movement_points
FROM asset_movement_trail
WHERE timestamp >= now() - interval '30 days' AND zone IS NOT NULL
GROUP BY tenant_code, zone, hour;

COMMENT ON VIEW v_dwell_proxy_by_zone_hourly IS 'Proxy dwell via movement points per zone per hour.';

-- 13) Alerts Summary by Type and Hour (Sensor Alerts)
CREATE OR REPLACE VIEW v_alerts_summary_type_hour AS
SELECT tenant_code,
       type,
       date_trunc('hour', timestamp) AS hour,
       count(*) AS alerts
FROM sensor_alerts
WHERE timestamp >= now() - interval '30 days'
GROUP BY tenant_code, type, hour;

COMMENT ON VIEW v_alerts_summary_type_hour IS 'Hourly sensor alerts grouped by type and tenant.';

-- 14) Repeat Offenders (Assets with most violations)
CREATE OR REPLACE VIEW v_repeat_offenders_assets AS
SELECT tenant_code,
       asset_identifier,
       count(*) AS violations
FROM zone_violations
WHERE timestamp >= now() - interval '180 days'
GROUP BY tenant_code, asset_identifier
ORDER BY violations DESC;

COMMENT ON VIEW v_repeat_offenders_assets IS 'Top assets by violation count in last 6 months.';

-- 15) Throughput & Ops Volume (Today)
CREATE OR REPLACE VIEW v_throughput_ops_volume_today AS
SELECT tenant_code,
       (SELECT count(DISTINCT flight_number) FROM flights f WHERE f.tenant_code = x.tenant_code AND f.timestamp::date = current_date) AS flights_today,
       (SELECT count(*) FROM turnaround_tasks t WHERE t.tenant_code = x.tenant_code AND t.actual_end::date = current_date) AS tasks_done_today,
       (SELECT count(*) FROM zone_violations v WHERE v.tenant_code = x.tenant_code AND v.acknowledged = true AND v.acknowledged_at::date = current_date) AS alerts_closed_today
FROM (SELECT DISTINCT tenant_code FROM flights) x;

COMMENT ON VIEW v_throughput_ops_volume_today IS 'Today''s totals: flights handled, tasks completed, acknowledged violations per tenant.';

-- 16) Pipeline Health: Events per Minute (Trail)
CREATE OR REPLACE VIEW v_pipeline_health_events_per_minute AS
SELECT tenant_code,
       date_trunc('minute', timestamp) AS minute,
       count(*) AS events
FROM asset_movement_trail
WHERE timestamp >= now() - interval '24 hours'
GROUP BY tenant_code, minute;

COMMENT ON VIEW v_pipeline_health_events_per_minute IS 'Events per minute from movement trail (freshness/volume proxy).';

-- 17) Latest Activity Heatmap (24h)
CREATE OR REPLACE VIEW v_activity_heatmap_latest AS
SELECT *
FROM asset_activity_heatmap
WHERE time_bucket >= now() - interval '24 hours';

-- 18) Latest Violation Heatmap (7d)
CREATE OR REPLACE VIEW v_violation_heatmap_latest AS
SELECT *
FROM violation_heatmap
WHERE time_bucket >= now() - interval '7 days';

-- 19) Latest Dwell Heatmap (24h)
-- Note: asset_dwell_heatmap not available in init; use v_dwell_proxy_by_zone_hourly for dwell proxy.

-- 20) Stand Conflicts (Sessions overlapping per stand)
CREATE OR REPLACE VIEW v_stand_conflicts AS
WITH sessions AS (
    SELECT tenant_code, stand_id, sirt, aobt
    FROM turnaround_sessions
    WHERE sirt IS NOT NULL AND aobt IS NOT NULL
      AND sirt >= now() - interval '30 days'
)
SELECT s1.tenant_code,
       s1.stand_id,
       count(*) AS overlapping_pairs
FROM sessions s1
JOIN sessions s2
  ON s1.stand_id = s2.stand_id
 AND s1.tenant_code = s2.tenant_code
 AND s1.sirt < s2.aobt
 AND s2.sirt < s1.aobt
 AND s1.sirt <> s2.sirt -- exclude self-join exact same rows
GROUP BY s1.tenant_code, s1.stand_id
HAVING count(*) > 0
ORDER BY overlapping_pairs DESC;

COMMENT ON VIEW v_stand_conflicts IS 'Approximate count of overlapping turnaround sessions per stand (conflict indicator).';

COMMIT;
