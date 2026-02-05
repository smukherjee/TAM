-- =====================================================================
-- TAM MASTER DATA SETUP SCRIPT
-- =====================================================================
-- Purpose: Single script to run all data generation for a fresh deployment
-- 
-- Run from HOST machine (not inside Docker):
--   docker exec -i tam-timescaledb-1 psql -U postgres -d utam < infrastructure/db/master-data-setup.sql
--
-- This approach pipes the SQL via stdin, not \i includes.
-- =====================================================================
-- 
-- Order of operations (all in this single file):
-- 1. Schema fixes and stand data
-- 2. Report/simulation data (violations, movement trail, discrepancies)
-- 3. Asset status updates
-- 4. Turnaround data fixes
-- 5. Refresh materialized views
-- =====================================================================

SET client_min_messages = 'warning';

DO $$ BEGIN RAISE NOTICE ''; END $$;
DO $$ BEGIN RAISE NOTICE '=========================================='; END $$;
DO $$ BEGIN RAISE NOTICE 'TAM Master Data Setup Starting...'; END $$;
DO $$ BEGIN RAISE NOTICE '=========================================='; END $$;
DO $$ BEGIN RAISE NOTICE ''; END $$;

-- =====================================================================
-- INDIVIDUAL SCRIPTS SHOULD BE RUN SEQUENTIALLY
-- Use this shell command to run all:
-- =====================================================================
-- 
-- cat infrastructure/db/fix_schema_and_add_stands.sql \
--     infrastructure/db/populate_report_data.sql \
--     infrastructure/db/update_asset_statuses.sql \
--     infrastructure/db/fix_turnaround_data.sql \
--     | docker exec -i tam-timescaledb-1 psql -U postgres -d utam
--
-- OR run this master script which calls them individually:
--
-- ./infrastructure/db/regenerate-all-data.sh
-- =====================================================================

DO $$ BEGIN RAISE NOTICE 'Step 1/5: Checking schema and stands...'; END $$;
-- (Include fix_schema_and_add_stands.sql content here when piping)

DO $$ BEGIN RAISE NOTICE 'Step 2/5: Populating report data (violations, trails, discrepancies, predictions)...'; END $$;
-- (Include populate_report_data.sql content here when piping)

DO $$ BEGIN RAISE NOTICE 'Step 3/5: Updating asset statuses...'; END $$;
-- (Include update_asset_statuses.sql content here when piping)

DO $$ BEGIN RAISE NOTICE 'Step 4/5: Fixing turnaround data...'; END $$;
-- (Include fix_turnaround_data.sql content here when piping)

-- =====================================================================
-- Step 5: Refresh materialized views (ALWAYS RUNS)
-- =====================================================================
DO $$ BEGIN RAISE NOTICE 'Step 5/5: Refreshing materialized views...'; END $$;

-- Refresh asset activity heatmap
DO $$
BEGIN
    PERFORM 1 FROM pg_matviews WHERE matviewname = 'asset_activity_heatmap';
    IF FOUND THEN
        REFRESH MATERIALIZED VIEW asset_activity_heatmap;
        RAISE NOTICE 'Refreshed asset_activity_heatmap';
    ELSE
        RAISE NOTICE 'Materialized view asset_activity_heatmap does not exist';
    END IF;
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'Could not refresh asset_activity_heatmap: %', SQLERRM;
END $$;

-- Refresh violation heatmap
DO $$
BEGIN
    PERFORM 1 FROM pg_matviews WHERE matviewname = 'violation_heatmap';
    IF FOUND THEN
        REFRESH MATERIALIZED VIEW violation_heatmap;
        RAISE NOTICE 'Refreshed violation_heatmap';
    ELSE
        RAISE NOTICE 'Materialized view violation_heatmap does not exist';
    END IF;
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'Could not refresh violation_heatmap: %', SQLERRM;
END $$;

-- Refresh activity heatmap (legacy name)
DO $$
BEGIN
    PERFORM 1 FROM pg_matviews WHERE matviewname = 'activity_heatmap';
    IF FOUND THEN
        REFRESH MATERIALIZED VIEW activity_heatmap;
        RAISE NOTICE 'Refreshed activity_heatmap';
    END IF;
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'Note: activity_heatmap not found or could not refresh';
END $$;

-- =====================================================================
-- Summary
-- =====================================================================
DO $$ BEGIN RAISE NOTICE ''; END $$;
DO $$ BEGIN RAISE NOTICE '=========================================='; END $$;
DO $$ BEGIN RAISE NOTICE 'Data Population Summary'; END $$;
DO $$ BEGIN RAISE NOTICE '=========================================='; END $$;

SELECT 'zone_violations' as table_name, COUNT(*) as count FROM zone_violations
UNION ALL SELECT 'movement_discrepancies', COUNT(*) FROM movement_discrepancies
UNION ALL SELECT 'asset_movement_trail', COUNT(*) FROM asset_movement_trail
UNION ALL SELECT 'turnaround_sessions', COUNT(*) FROM turnaround_sessions
UNION ALL SELECT 'turnaround_tasks', COUNT(*) FROM turnaround_tasks
UNION ALL SELECT 'sensor_alerts', COUNT(*) FROM sensor_alerts
UNION ALL SELECT 'assets', COUNT(*) FROM assets
UNION ALL SELECT 'stands', COUNT(*) FROM stands
UNION ALL SELECT 'pred_turnaround_risk', COUNT(*) FROM pred_turnaround_risk
UNION ALL SELECT 'pred_congestion', COUNT(*) FROM pred_congestion
ORDER BY table_name;

DO $$ BEGIN RAISE NOTICE ''; END $$;
DO $$ BEGIN RAISE NOTICE 'Heatmap Materialized Views:'; END $$;

SELECT matviewname, 
       pg_size_pretty(pg_relation_size(oid)) as size
FROM pg_matviews 
WHERE schemaname = 'public' 
AND matviewname LIKE '%heatmap%'
ORDER BY matviewname;

DO $$ BEGIN RAISE NOTICE ''; END $$;
DO $$ BEGIN RAISE NOTICE '=========================================='; END $$;
DO $$ BEGIN RAISE NOTICE 'TAM Master Data Setup Complete!'; END $$;
DO $$ BEGIN RAISE NOTICE '=========================================='; END $$;
DO $$ BEGIN RAISE NOTICE ''; END $$;
DO $$ BEGIN RAISE NOTICE 'Data includes:'; END $$;
DO $$ BEGIN RAISE NOTICE '  • Zone violations (Safety reports)'; END $$;
DO $$ BEGIN RAISE NOTICE '  • Movement discrepancies (Discrepancy reports)'; END $$;
DO $$ BEGIN RAISE NOTICE '  • Asset movement trail (Activity Heatmap)'; END $$;
DO $$ BEGIN RAISE NOTICE '  • Turnaround sessions/tasks (SLA Compliance)'; END $$;
DO $$ BEGIN RAISE NOTICE '  • Prediction tables (Forecasting)'; END $$;
DO $$ BEGIN RAISE NOTICE ''; END $$;
DO $$ BEGIN RAISE NOTICE 'Materialized views refreshed:'; END $$;
DO $$ BEGIN RAISE NOTICE '  • asset_activity_heatmap'; END $$;
DO $$ BEGIN RAISE NOTICE '  • violation_heatmap'; END $$;
DO $$ BEGIN RAISE NOTICE ''; END $$;
