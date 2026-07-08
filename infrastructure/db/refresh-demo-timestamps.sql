-- Refresh Stale Demo Timestamps
-- Date: 2026-07-06
--
-- turnaround_sessions/turnaround_tasks/turnaround_alerts and camera_health_metrics
-- are one-time seeded (no live generator/producer touches them - see
-- infrastructure/db/init/23-advanced-turnaround-reports.sql and 17-fix-turnaround-data.sql).
-- Their timestamps drift into the past relative to NOW() the longer the demo environment
-- runs, which makes any "last N hours" dashboard query against them return nothing.
--
-- This shifts every row forward by a uniform delta so the most recent row lands at NOW(),
-- preserving all relative spacing/relationships (session durations, task/alert ordering,
-- on-time vs delayed patterns). Safe to re-run any time the data goes stale again.

BEGIN;

DO $$
DECLARE
    turnaround_delta INTERVAL;
    camera_delta INTERVAL;
BEGIN
    SELECT NOW() - MAX(latest) INTO turnaround_delta
    FROM (
        SELECT GREATEST(aobt, tobt, aibt, eibt, sirt, tsat) AS latest
        FROM turnaround_sessions
        ORDER BY latest DESC NULLS LAST
        LIMIT 1
    ) t;

    IF turnaround_delta IS NOT NULL AND turnaround_delta > INTERVAL '0' THEN
        UPDATE turnaround_sessions SET
            sirt = sirt + turnaround_delta,
            eibt = eibt + turnaround_delta,
            aibt = aibt + turnaround_delta,
            tobt = tobt + turnaround_delta,
            tsat = tsat + turnaround_delta,
            aobt = aobt + turnaround_delta,
            created_at = created_at + turnaround_delta,
            updated_at = updated_at + turnaround_delta;

        UPDATE turnaround_tasks SET
            planned_start = planned_start + turnaround_delta,
            planned_end = planned_end + turnaround_delta,
            actual_start = actual_start + turnaround_delta;

        UPDATE turnaround_alerts SET
            "timestamp" = "timestamp" + turnaround_delta;

        RAISE NOTICE 'Shifted turnaround_sessions/tasks/alerts forward by %', turnaround_delta;
    ELSE
        RAISE NOTICE 'turnaround_sessions already current, no shift needed';
    END IF;

    SELECT NOW() - MAX("timestamp") INTO camera_delta FROM camera_health_metrics;

    IF camera_delta IS NOT NULL AND camera_delta > INTERVAL '0' THEN
        UPDATE camera_health_metrics SET
            "timestamp" = "timestamp" + camera_delta,
            created_at = created_at + camera_delta;

        RAISE NOTICE 'Shifted camera_health_metrics forward by %', camera_delta;
    ELSE
        RAISE NOTICE 'camera_health_metrics already current, no shift needed';
    END IF;
END $$;

-- Same staleness problem applies to the AOM-gap report tables added in
-- infrastructure/db/init/27,28,30,31,32.sql: their mock data is anchored to
-- NOW()/CURRENT_DATE at insert time, so "last N days" dashboard queries against
-- them go stale too. asset_certifications and inventory_stock are excluded:
-- their status views (v_asset_certification_status, v_inventory_stock_status)
-- compare against CURRENT_DATE/reorder_level live, not against a fixed insert-time
-- window, so they don't decay the same way.
DO $$
DECLARE
    lvp_delta INTERVAL;
    maint_delta INTERVAL;
    wildlife_delta INTERVAL;
    occurrence_delta INTERVAL;
    drill_delta INTERVAL;
BEGIN
    SELECT NOW() - MAX(COALESCE(end_time, start_time)) INTO lvp_delta FROM lvp_events;
    IF lvp_delta IS NOT NULL AND lvp_delta > INTERVAL '0' THEN
        UPDATE lvp_events SET
            start_time = start_time + lvp_delta,
            end_time = end_time + lvp_delta;
        RAISE NOTICE 'Shifted lvp_events forward by %', lvp_delta;
    ELSE
        RAISE NOTICE 'lvp_events already current, no shift needed';
    END IF;

    SELECT NOW() - MAX(COALESCE(downtime_end, downtime_start)) INTO maint_delta FROM asset_maintenance_log;
    IF maint_delta IS NOT NULL AND maint_delta > INTERVAL '0' THEN
        UPDATE asset_maintenance_log SET
            downtime_start = downtime_start + maint_delta,
            downtime_end = downtime_end + maint_delta;
        RAISE NOTICE 'Shifted asset_maintenance_log forward by %', maint_delta;
    ELSE
        RAISE NOTICE 'asset_maintenance_log already current, no shift needed';
    END IF;

    SELECT NOW() - MAX("timestamp") INTO wildlife_delta FROM wildlife_events;
    IF wildlife_delta IS NOT NULL AND wildlife_delta > INTERVAL '0' THEN
        UPDATE wildlife_events SET "timestamp" = "timestamp" + wildlife_delta;
        RAISE NOTICE 'Shifted wildlife_events forward by %', wildlife_delta;
    ELSE
        RAISE NOTICE 'wildlife_events already current, no shift needed';
    END IF;

    SELECT NOW() - MAX(occurrence_date) INTO occurrence_delta FROM occurrence_reports;
    IF occurrence_delta IS NOT NULL AND occurrence_delta > INTERVAL '0' THEN
        UPDATE occurrence_reports SET
            occurrence_date = occurrence_date + occurrence_delta,
            reported_at = reported_at + occurrence_delta;
        RAISE NOTICE 'Shifted occurrence_reports forward by %', occurrence_delta;
    ELSE
        RAISE NOTICE 'occurrence_reports already current, no shift needed';
    END IF;

    SELECT NOW() - MAX(GREATEST(scheduled_date, COALESCE(conducted_date, scheduled_date), next_due_date))
        INTO drill_delta FROM emergency_drills;
    IF drill_delta IS NOT NULL AND drill_delta > INTERVAL '0' THEN
        UPDATE emergency_drills SET
            scheduled_date = (scheduled_date + drill_delta)::date,
            conducted_date = (conducted_date + drill_delta)::date,
            next_due_date = (next_due_date + drill_delta)::date;
        RAISE NOTICE 'Shifted emergency_drills forward by %', drill_delta;
    ELSE
        RAISE NOTICE 'emergency_drills already current, no shift needed';
    END IF;
END $$;

COMMIT;
