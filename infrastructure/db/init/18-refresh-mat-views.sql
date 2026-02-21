-- Refresh Materialized Views after all seed data is populated
DO $$
BEGIN
    -- Refresh asset_activity_heatmap
    IF EXISTS (SELECT 1 FROM pg_matviews WHERE matviewname = 'asset_activity_heatmap') THEN
        REFRESH MATERIALIZED VIEW asset_activity_heatmap;
        RAISE NOTICE 'Refreshed asset_activity_heatmap';
    END IF;
    
    -- Refresh violation_heatmap
    IF EXISTS (SELECT 1 FROM pg_matviews WHERE matviewname = 'violation_heatmap') THEN
        REFRESH MATERIALIZED VIEW violation_heatmap;
        RAISE NOTICE 'Refreshed violation_heatmap';
    END IF;
    
    -- Refresh activity_heatmap (legacy)
    IF EXISTS (SELECT 1 FROM pg_matviews WHERE matviewname = 'activity_heatmap') THEN
        REFRESH MATERIALIZED VIEW activity_heatmap;
        RAISE NOTICE 'Refreshed activity_heatmap';
    END IF;
    
    RAISE NOTICE 'All materialized views refreshed';
EXCEPTION WHEN OTHERS THEN
    RAISE WARNING 'Error refreshing materialized views: %', SQLERRM;
END $$;
