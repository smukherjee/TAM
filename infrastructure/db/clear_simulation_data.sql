-- =============================================================================
-- TAM Clear Simulation Data Script
-- =============================================================================
-- Clears simulation data for a specific tenant or all tenants.
-- 
-- Usage:
--   psql -U postgres -d utam -v tenant_code="'VIDP'" -f clear_simulation_data.sql
--   psql -U postgres -d utam -v tenant_code="'ALL'" -f clear_simulation_data.sql
-- =============================================================================

\echo '============================================='
\echo 'TAM Clear Simulation Data'
\echo '============================================='

-- Set tenant code from variable (default to ALL if not provided)
\set tenant :tenant_code

DO $$
DECLARE
    v_tenant_code TEXT := :tenant;
    v_deleted_count INTEGER := 0;
    v_total_deleted INTEGER := 0;
BEGIN
    RAISE NOTICE 'Clearing simulation data for tenant: %', v_tenant_code;
    
    -- simulation_alerts
    IF v_tenant_code = 'ALL' THEN
        DELETE FROM simulation_alerts;
    ELSE
        DELETE FROM simulation_alerts WHERE tenant_code = v_tenant_code;
    END IF;
    GET DIAGNOSTICS v_deleted_count = ROW_COUNT;
    v_total_deleted := v_total_deleted + v_deleted_count;
    RAISE NOTICE 'Deleted % rows from simulation_alerts', v_deleted_count;
    
    -- simulation_asset_positions
    IF v_tenant_code = 'ALL' THEN
        DELETE FROM simulation_asset_positions;
    ELSE
        DELETE FROM simulation_asset_positions WHERE tenant_code = v_tenant_code;
    END IF;
    GET DIAGNOSTICS v_deleted_count = ROW_COUNT;
    v_total_deleted := v_total_deleted + v_deleted_count;
    RAISE NOTICE 'Deleted % rows from simulation_asset_positions', v_deleted_count;
    
    -- simulation_assets
    IF v_tenant_code = 'ALL' THEN
        DELETE FROM simulation_assets;
    ELSE
        DELETE FROM simulation_assets WHERE tenant_code = v_tenant_code;
    END IF;
    GET DIAGNOSTICS v_deleted_count = ROW_COUNT;
    v_total_deleted := v_total_deleted + v_deleted_count;
    RAISE NOTICE 'Deleted % rows from simulation_assets', v_deleted_count;
    
    -- simulation_discrepancies
    IF v_tenant_code = 'ALL' THEN
        DELETE FROM simulation_discrepancies;
    ELSE
        DELETE FROM simulation_discrepancies WHERE tenant_code = v_tenant_code;
    END IF;
    GET DIAGNOSTICS v_deleted_count = ROW_COUNT;
    v_total_deleted := v_total_deleted + v_deleted_count;
    RAISE NOTICE 'Deleted % rows from simulation_discrepancies', v_deleted_count;
    
    -- simulation_dispatches
    IF v_tenant_code = 'ALL' THEN
        DELETE FROM simulation_dispatches;
    ELSE
        DELETE FROM simulation_dispatches WHERE tenant_code = v_tenant_code;
    END IF;
    GET DIAGNOSTICS v_deleted_count = ROW_COUNT;
    v_total_deleted := v_total_deleted + v_deleted_count;
    RAISE NOTICE 'Deleted % rows from simulation_dispatches', v_deleted_count;
    
    -- simulation_financial_metrics
    IF v_tenant_code = 'ALL' THEN
        DELETE FROM simulation_financial_metrics;
    ELSE
        DELETE FROM simulation_financial_metrics WHERE tenant_code = v_tenant_code;
    END IF;
    GET DIAGNOSTICS v_deleted_count = ROW_COUNT;
    v_total_deleted := v_total_deleted + v_deleted_count;
    RAISE NOTICE 'Deleted % rows from simulation_financial_metrics', v_deleted_count;
    
    -- simulation_turnarounds
    IF v_tenant_code = 'ALL' THEN
        DELETE FROM simulation_turnarounds;
    ELSE
        DELETE FROM simulation_turnarounds WHERE tenant_code = v_tenant_code;
    END IF;
    GET DIAGNOSTICS v_deleted_count = ROW_COUNT;
    v_total_deleted := v_total_deleted + v_deleted_count;
    RAISE NOTICE 'Deleted % rows from simulation_turnarounds', v_deleted_count;
    
    -- simulation_vehicles
    IF v_tenant_code = 'ALL' THEN
        DELETE FROM simulation_vehicles;
    ELSE
        DELETE FROM simulation_vehicles WHERE tenant_code = v_tenant_code;
    END IF;
    GET DIAGNOSTICS v_deleted_count = ROW_COUNT;
    v_total_deleted := v_total_deleted + v_deleted_count;
    RAISE NOTICE 'Deleted % rows from simulation_vehicles', v_deleted_count;
    
    -- simulation_violations
    IF v_tenant_code = 'ALL' THEN
        DELETE FROM simulation_violations;
    ELSE
        DELETE FROM simulation_violations WHERE tenant_code = v_tenant_code;
    END IF;
    GET DIAGNOSTICS v_deleted_count = ROW_COUNT;
    v_total_deleted := v_total_deleted + v_deleted_count;
    RAISE NOTICE 'Deleted % rows from simulation_violations', v_deleted_count;
    
    -- Also clear from core tables that have simulation data
    -- asset_movement_trail
    IF v_tenant_code = 'ALL' THEN
        DELETE FROM asset_movement_trail;
    ELSE
        DELETE FROM asset_movement_trail WHERE tenant_code = v_tenant_code;
    END IF;
    GET DIAGNOSTICS v_deleted_count = ROW_COUNT;
    v_total_deleted := v_total_deleted + v_deleted_count;
    RAISE NOTICE 'Deleted % rows from asset_movement_trail', v_deleted_count;
    
    -- asset_location_register
    IF v_tenant_code = 'ALL' THEN
        DELETE FROM asset_location_register;
    ELSE
        DELETE FROM asset_location_register WHERE tenant_code = v_tenant_code;
    END IF;
    GET DIAGNOSTICS v_deleted_count = ROW_COUNT;
    v_total_deleted := v_total_deleted + v_deleted_count;
    RAISE NOTICE 'Deleted % rows from asset_location_register', v_deleted_count;
    
    -- zone_violations
    IF v_tenant_code = 'ALL' THEN
        DELETE FROM zone_violations;
    ELSE
        DELETE FROM zone_violations WHERE tenant_code = v_tenant_code;
    END IF;
    GET DIAGNOSTICS v_deleted_count = ROW_COUNT;
    v_total_deleted := v_total_deleted + v_deleted_count;
    RAISE NOTICE 'Deleted % rows from zone_violations', v_deleted_count;
    
    RAISE NOTICE '=============================================';
    RAISE NOTICE 'Total rows deleted: %', v_total_deleted;
    RAISE NOTICE '=============================================';
END $$;

\echo 'Data clearing complete!'
