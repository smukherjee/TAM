-- =====================================================================
-- TAM Mock Data Population Script for Superset Reports
-- This script populates all tables needed for the 28 Superset charts
-- Run: docker exec -i tam-timescaledb-1 psql -U postgres -d utam < infrastructure/db/populate_report_data.sql
-- =====================================================================

-- Set timezone
SET timezone = 'UTC';

-- Get tenant codes for reference
DO $$
DECLARE
    v_tenant_vidp VARCHAR(4) := 'VIDP';
    v_tenant_lirn VARCHAR(4) := 'LIRN';
    v_tenant_ybbn VARCHAR(4) := 'YBBN';
BEGIN
    RAISE NOTICE 'Populating mock data for tenants: %, %, %', v_tenant_vidp, v_tenant_lirn, v_tenant_ybbn;
END $$;

-- =====================================================================
-- 1. ZONE VIOLATIONS (for Safety & Security reports)
-- =====================================================================
INSERT INTO zone_violations (
    violation_id, asset_identifier, asset_name, asset_category,
    zone_name, zone_type, tenant_code, violation_type,
    entry_latitude, entry_longitude, duration_seconds, severity, timestamp
)
SELECT
    'VIOL-' || TO_CHAR(gs, 'FM00000'),
    'GSE-' || (100 + (gs % 50))::TEXT,
    CASE (gs % 5)
        WHEN 0 THEN 'Baggage Tractor'
        WHEN 1 THEN 'Fuel Truck'
        WHEN 2 THEN 'Catering Vehicle'
        WHEN 3 THEN 'Ground Power Unit'
        ELSE 'Pushback Tug'
    END,
    CASE (gs % 4)
        WHEN 0 THEN 'BAGGAGE'
        WHEN 1 THEN 'FUEL'
        WHEN 2 THEN 'CATERING'
        ELSE 'GSE'
    END,
    CASE (gs % 6)
        WHEN 0 THEN 'Runway 24'
        WHEN 1 THEN 'Taxiway Alpha'
        WHEN 2 THEN 'Terminal Apron A'
        WHEN 3 THEN 'Fuel Farm Zone'
        WHEN 4 THEN 'Maintenance Hangar'
        ELSE 'Cargo Area'
    END,
    CASE (gs % 3)
        WHEN 0 THEN 'RUNWAY'
        WHEN 1 THEN 'TAXIWAY'
        ELSE 'RESTRICTED'
    END,
    CASE (gs % 3)
        WHEN 0 THEN 'VIDP'
        WHEN 1 THEN 'LIRN'
        ELSE 'YBBN'
    END,
    CASE (gs % 3)
        WHEN 0 THEN 'UNAUTHORIZED_ENTRY'
        WHEN 1 THEN 'UNAUTHORIZED_CATEGORY'
        ELSE 'PROHIBITED_ZONE'
    END,
    -- Tenant-specific coordinates: VIDP=Delhi, LIRN=Naples, YBBN=Brisbane
    CASE (gs % 3)
        WHEN 0 THEN 28.5665 + (random() - 0.5) * 0.01  -- VIDP (Delhi)
        WHEN 1 THEN 40.8844 + (random() - 0.5) * 0.01  -- LIRN (Naples)
        ELSE -27.3842 + (random() - 0.5) * 0.01        -- YBBN (Brisbane)
    END,
    CASE (gs % 3)
        WHEN 0 THEN 77.1031 + (random() - 0.5) * 0.01  -- VIDP (Delhi)
        WHEN 1 THEN 14.2908 + (random() - 0.5) * 0.01  -- LIRN (Naples)
        ELSE 153.1175 + (random() - 0.5) * 0.01        -- YBBN (Brisbane)
    END,
    (30 + random() * 300)::INT,
    CASE (gs % 4)
        WHEN 0 THEN 'LOW'
        WHEN 1 THEN 'MEDIUM'
        WHEN 2 THEN 'HIGH'
        ELSE 'CRITICAL'
    END,
    NOW() - (gs || ' minutes')::INTERVAL
FROM generate_series(1, 500) gs
ON CONFLICT DO NOTHING;

-- =====================================================================
-- 2. MOVEMENT DISCREPANCIES (for Safety & Discrepancy reports)
-- =====================================================================
INSERT INTO movement_discrepancies (
    discrepancy_id, asset_identifier, asset_name, asset_category,
    tenant_code, discrepancy_type, expected_location, actual_location,
    distance_deviation_meters, severity, timestamp
)
SELECT
    'DISC-' || TO_CHAR(gs, 'FM00000'),
    'GSE-' || (100 + (gs % 50))::TEXT,
    CASE (gs % 5)
        WHEN 0 THEN 'Baggage Tractor'
        WHEN 1 THEN 'Fuel Truck'
        WHEN 2 THEN 'Catering Vehicle'
        WHEN 3 THEN 'Ground Power Unit'
        ELSE 'Pushback Tug'
    END,
    CASE (gs % 4)
        WHEN 0 THEN 'BAGGAGE'
        WHEN 1 THEN 'FUEL'
        WHEN 2 THEN 'CATERING'
        ELSE 'GSE'
    END,
    CASE (gs % 3)
        WHEN 0 THEN 'VIDP'
        WHEN 1 THEN 'LIRN'
        ELSE 'YBBN'
    END,
    CASE (gs % 5)
        WHEN 0 THEN 'UNEXPECTED_MOVEMENT'
        WHEN 1 THEN 'LOCATION_MISMATCH'
        WHEN 2 THEN 'SPEED_ANOMALY'
        WHEN 3 THEN 'MISSING_TRACKING'
        ELSE 'DUPLICATE_SIGNAL'
    END,
    'Stand A' || (gs % 20 + 1)::TEXT,
    'Stand B' || (gs % 20 + 1)::TEXT,
    50 + random() * 500,
    CASE (gs % 4)
        WHEN 0 THEN 'LOW'
        WHEN 1 THEN 'MEDIUM'
        WHEN 2 THEN 'HIGH'
        ELSE 'CRITICAL'
    END,
    NOW() - (gs || ' minutes')::INTERVAL
FROM generate_series(1, 300) gs
ON CONFLICT DO NOTHING;

-- =====================================================================
-- 3. ASSET MOVEMENT TRAIL (for Activity Heatmap)
-- =====================================================================
INSERT INTO asset_movement_trail (
    asset_identifier, tenant_code, latitude, longitude,
    speed, heading, zone, status, timestamp
)
SELECT
    'GSE-' || (100 + (gs % 100))::TEXT,
    CASE (gs % 3)
        WHEN 0 THEN 'VIDP'
        WHEN 1 THEN 'LIRN'
        ELSE 'YBBN'
    END,
    CASE (gs % 3)
        WHEN 0 THEN 28.5665 + (random() - 0.5) * 0.05  -- Delhi
        WHEN 1 THEN 40.8844 + (random() - 0.5) * 0.05  -- Naples
        ELSE -27.3842 + (random() - 0.5) * 0.05        -- Brisbane
    END,
    CASE (gs % 3)
        WHEN 0 THEN 77.1031 + (random() - 0.5) * 0.05  -- Delhi
        WHEN 1 THEN 14.2908 + (random() - 0.5) * 0.05  -- Naples
        ELSE 153.1175 + (random() - 0.5) * 0.05        -- Brisbane
    END,
    5 + random() * 35,  -- Speed 5-40 km/h
    random() * 360,     -- Heading 0-360
    CASE (gs % 8)
        WHEN 0 THEN 'Terminal A'
        WHEN 1 THEN 'Terminal B'
        WHEN 2 THEN 'Cargo Area'
        WHEN 3 THEN 'Fuel Farm'
        WHEN 4 THEN 'Maintenance'
        WHEN 5 THEN 'Apron East'
        WHEN 6 THEN 'Apron West'
        ELSE 'General Aviation'
    END,
    CASE (gs % 4)
        WHEN 0 THEN 'ACTIVE'
        WHEN 1 THEN 'IDLE'
        WHEN 2 THEN 'IN_TRANSIT'
        ELSE 'PARKED'
    END,
    NOW() - ((gs * 2) || ' minutes')::INTERVAL
FROM generate_series(1, 2000) gs
ON CONFLICT DO NOTHING;

-- =====================================================================
-- 4. TURNAROUND TASKS (for SLA Compliance and Delay reports)
-- =====================================================================
INSERT INTO turnaround_tasks (tenant_code, session_id, task_type, status, planned_start, planned_end, actual_start, actual_end)
SELECT 
    s.tenant_code, s.id, 'FUEL', 'COMPLETED', 
    s.created_at + interval '5 minutes', s.created_at + interval '15 minutes',
    s.created_at + interval '7 minutes', s.created_at + interval '17 minutes'
FROM turnaround_sessions s
ON CONFLICT DO NOTHING;

INSERT INTO turnaround_tasks (tenant_code, session_id, task_type, status, planned_start, planned_end, actual_start, actual_end)
SELECT 
    s.tenant_code, s.id, 'CATERING', 'COMPLETED', 
    s.created_at + interval '10 minutes', s.created_at + interval '20 minutes',
    s.created_at + interval '12 minutes', s.created_at + interval '22 minutes'
FROM turnaround_sessions s
ON CONFLICT DO NOTHING;

INSERT INTO turnaround_tasks (tenant_code, session_id, task_type, status, planned_start, planned_end, actual_start, actual_end)
SELECT 
    s.tenant_code, s.id, 'CLEANING', 'DELAYED', 
    s.created_at + interval '15 minutes', s.created_at + interval '25 minutes',
    s.created_at + interval '20 minutes', s.created_at + interval '35 minutes'
FROM turnaround_sessions s
ON CONFLICT DO NOTHING;

INSERT INTO turnaround_tasks (tenant_code, session_id, task_type, status, planned_start, planned_end, actual_start, actual_end)
SELECT 
    s.tenant_code, s.id, 'BAGGAGE_LOAD', 'COMPLETED', 
    s.created_at + interval '20 minutes', s.created_at + interval '30 minutes',
    s.created_at + interval '22 minutes', s.created_at + interval '32 minutes'
FROM turnaround_sessions s
ON CONFLICT DO NOTHING;

INSERT INTO turnaround_tasks (tenant_code, session_id, task_type, status, planned_start, planned_end, actual_start, actual_end)
SELECT 
    s.tenant_code, s.id, 'PASSENGER_BOARD', 'IN_PROGRESS', 
    s.created_at + interval '25 minutes', s.created_at + interval '35 minutes',
    s.created_at + interval '27 minutes', NULL
FROM turnaround_sessions s
ON CONFLICT DO NOTHING;

INSERT INTO turnaround_tasks (tenant_code, session_id, task_type, status, planned_start, planned_end, actual_start, actual_end)
SELECT 
    s.tenant_code, s.id, 'PUSHBACK', 'PENDING', 
    s.created_at + interval '35 minutes', s.created_at + interval '40 minutes',
    NULL, NULL
FROM turnaround_sessions s
ON CONFLICT DO NOTHING;

-- =====================================================================
-- 5. SENSOR ALERTS (for Alerts reports)
-- =====================================================================
INSERT INTO sensor_alerts (
    alert_id, tenant_code, type, entity_id, value,
    latitude, longitude, timestamp
)
SELECT
    uuid_generate_v4(),
    CASE (gs % 3)
        WHEN 0 THEN 'VIDP'
        WHEN 1 THEN 'LIRN'
        ELSE 'YBBN'
    END,
    CASE (gs % 6)
        WHEN 0 THEN 'SPEED_VIOLATION'
        WHEN 1 THEN 'ZONE_BREACH'
        WHEN 2 THEN 'EQUIPMENT_FAULT'
        WHEN 3 THEN 'PROXIMITY_WARNING'
        WHEN 4 THEN 'BATTERY_LOW'
        ELSE 'SIGNAL_LOST'
    END,
    'GSE-' || (100 + (gs % 50))::TEXT,
    50 + random() * 100,
    CASE (gs % 3)
        WHEN 0 THEN 40.8844 + (random() - 0.5) * 0.05  -- Naples
        WHEN 1 THEN -27.3842 + (random() - 0.5) * 0.05 -- Brisbane
        ELSE 40.6413 + (random() - 0.5) * 0.05         -- JFK
    END,
    CASE (gs % 3)
        WHEN 0 THEN 14.2908 + (random() - 0.5) * 0.05  -- Naples
        WHEN 1 THEN 153.1175 + (random() - 0.5) * 0.05 -- Brisbane
        ELSE -73.7781 + (random() - 0.5) * 0.05        -- JFK
    END,
    NOW() - (gs || ' minutes')::INTERVAL
FROM generate_series(1, 500) gs
ON CONFLICT DO NOTHING;

-- =====================================================================
-- 6. PREDICTION TABLES
-- =====================================================================

-- Turnaround Risk Predictions
INSERT INTO pred_turnaround_risk (
    tenant_code, flight_id, stand_id, risk_score, risk_band, as_of
)
SELECT
    CASE (gs % 3)
        WHEN 0 THEN 'VIDP'
        WHEN 1 THEN 'LIRN'
        ELSE 'YBBN'
    END,
    CASE (gs % 3)
        WHEN 0 THEN 'AZ' || (100 + gs)::TEXT
        WHEN 1 THEN 'QF' || (400 + gs)::TEXT
        ELSE 'AA' || (700 + gs)::TEXT
    END,
    'Stand A' || (gs % 25 + 1)::TEXT,
    random() * 100,
    CASE 
        WHEN random() < 0.3 THEN 'LOW'
        WHEN random() < 0.6 THEN 'MEDIUM'
        WHEN random() < 0.85 THEN 'HIGH'
        ELSE 'CRITICAL'
    END,
    NOW() - ((gs * 10) || ' minutes')::INTERVAL
FROM generate_series(1, 100) gs
ON CONFLICT DO NOTHING;

-- Congestion Predictions
INSERT INTO pred_congestion (
    tenant_code, zone, forecast_time, expected_density
)
SELECT
    CASE (gs % 3)
        WHEN 0 THEN 'VIDP'
        WHEN 1 THEN 'LIRN'
        ELSE 'YBBN'
    END,
    CASE (gs % 6)
        WHEN 0 THEN 'Terminal A Apron'
        WHEN 1 THEN 'Terminal B Apron'
        WHEN 2 THEN 'Cargo Area'
        WHEN 3 THEN 'Fuel Farm'
        WHEN 4 THEN 'Taxiway Alpha'
        ELSE 'Runway Threshold'
    END,
    NOW() + ((gs * 30) || ' minutes')::INTERVAL,
    10 + random() * 90
FROM generate_series(1, 50) gs
ON CONFLICT DO NOTHING;

-- Zone Breach Predictions
INSERT INTO pred_zone_breach (
    tenant_code, zone_id, horizon_minutes, probability, 
    top_asset_categories, as_of
)
SELECT 
    rz.tenant_code,
    rz.id,
    (ARRAY[15, 30, 45, 60])[1 + (gs % 4)],
    random(),
    ARRAY['FUEL', 'BAGGAGE', 'CATERING'],
    NOW() - (gs || ' minutes')::INTERVAL
FROM restricted_zones rz
CROSS JOIN generate_series(1, 10) gs
ON CONFLICT DO NOTHING;

-- Asset Violation Risk
INSERT INTO pred_asset_violation_risk (
    tenant_code, asset_identifier, probability, expected_severity, as_of
)
SELECT
    CASE (gs % 3)
        WHEN 0 THEN 'VIDP'
        WHEN 1 THEN 'LIRN'
        ELSE 'YBBN'
    END,
    'GSE-' || (100 + gs)::TEXT,
    random(),
    CASE (gs % 4)
        WHEN 0 THEN 'LOW'
        WHEN 1 THEN 'MEDIUM'
        WHEN 2 THEN 'HIGH'
        ELSE 'CRITICAL'
    END,
    NOW() - ((gs * 5) || ' minutes')::INTERVAL
FROM generate_series(1, 100) gs
ON CONFLICT DO NOTHING;

-- Violations Forecast
INSERT INTO forecast_violations_hourly (
    tenant_code, hour, expected_count, lower, upper
)
SELECT
    CASE (gs % 3)
        WHEN 0 THEN 'VIDP'
        WHEN 1 THEN 'LIRN'
        ELSE 'YBBN'
    END,
    DATE_TRUNC('hour', NOW()) + ((gs - 24) || ' hours')::INTERVAL,
    (5 + random() * 20)::INT,
    (2 + random() * 5)::INT,
    (20 + random() * 30)::INT
FROM generate_series(1, 72) gs  -- 72 hours of forecast
ON CONFLICT DO NOTHING;

-- =====================================================================
-- 7. UPDATE ASSETS STATUS for Utilization report
-- =====================================================================
UPDATE assets
SET status = CASE (random() * 4)::INT
    WHEN 0 THEN 'ACTIVE'
    WHEN 1 THEN 'IDLE'
    WHEN 2 THEN 'MAINTENANCE'
    ELSE 'OFFLINE'
END
WHERE status IS NULL OR status = '';

-- =====================================================================
-- 8. SYSTEM METRICS (for Pipeline Health)
-- =====================================================================
INSERT INTO system_metrics (
    timestamp, service_name, metric_name, value, tags
)
SELECT
    NOW() - (gs || ' minutes')::INTERVAL,
    CASE (gs % 4)
        WHEN 0 THEN 'kafka-consumer'
        WHEN 1 THEN 'nifi-processor'
        WHEN 2 THEN 'ksqldb-engine'
        ELSE 'api-gateway'
    END,
    CASE (gs % 5)
        WHEN 0 THEN 'events_per_minute'
        WHEN 1 THEN 'kafka_lag'
        WHEN 2 THEN 'processing_latency_ms'
        WHEN 3 THEN 'api_requests_per_min'
        ELSE 'active_connections'
    END,
    CASE (gs % 5)
        WHEN 0 THEN 100 + random() * 500
        WHEN 1 THEN random() * 100
        WHEN 2 THEN 10 + random() * 200
        WHEN 3 THEN 50 + random() * 200
        ELSE 10 + random() * 50
    END,
    jsonb_build_object(
        'tenant_code', 
        CASE (gs % 3)
            WHEN 0 THEN 'VIDP'
            WHEN 1 THEN 'LIRN'
            ELSE 'YBBN'
        END
    )
FROM generate_series(1, 500) gs
ON CONFLICT DO NOTHING;

-- =====================================================================
-- VERIFICATION QUERIES
-- =====================================================================

-- Fix geometry columns from lat/lng
UPDATE asset_movement_trail 
SET location = ST_SetSRID(ST_MakePoint(longitude, latitude), 4326) 
WHERE location IS NULL;

UPDATE zone_violations 
SET entry_location = ST_SetSRID(ST_MakePoint(entry_longitude, entry_latitude), 4326) 
WHERE entry_location IS NULL AND entry_latitude IS NOT NULL AND entry_longitude IS NOT NULL;

-- Refresh materialized views
REFRESH MATERIALIZED VIEW asset_activity_heatmap;
REFRESH MATERIALIZED VIEW violation_heatmap;

DO $$
DECLARE
    cnt_violations INT;
    cnt_discrepancies INT;
    cnt_trail INT;
    cnt_tasks INT;
    cnt_alerts INT;
    cnt_pred_risk INT;
    cnt_pred_cong INT;
    cnt_forecast INT;
    cnt_metrics INT;
BEGIN
    SELECT COUNT(*) INTO cnt_violations FROM zone_violations;
    SELECT COUNT(*) INTO cnt_discrepancies FROM movement_discrepancies;
    SELECT COUNT(*) INTO cnt_trail FROM asset_movement_trail;
    SELECT COUNT(*) INTO cnt_tasks FROM turnaround_tasks;
    SELECT COUNT(*) INTO cnt_alerts FROM sensor_alerts;
    SELECT COUNT(*) INTO cnt_pred_risk FROM pred_turnaround_risk;
    SELECT COUNT(*) INTO cnt_pred_cong FROM pred_congestion;
    SELECT COUNT(*) INTO cnt_forecast FROM forecast_violations_hourly;
    SELECT COUNT(*) INTO cnt_metrics FROM system_metrics;
    
    RAISE NOTICE '========================================';
    RAISE NOTICE 'Mock Data Population Complete!';
    RAISE NOTICE '========================================';
    RAISE NOTICE 'Zone Violations:        %', cnt_violations;
    RAISE NOTICE 'Movement Discrepancies: %', cnt_discrepancies;
    RAISE NOTICE 'Asset Movement Trail:   %', cnt_trail;
    RAISE NOTICE 'Turnaround Tasks:       %', cnt_tasks;
    RAISE NOTICE 'Sensor Alerts:          %', cnt_alerts;
    RAISE NOTICE 'Turnaround Risk Pred:   %', cnt_pred_risk;
    RAISE NOTICE 'Congestion Pred:        %', cnt_pred_cong;
    RAISE NOTICE 'Violations Forecast:    %', cnt_forecast;
    RAISE NOTICE 'System Metrics:         %', cnt_metrics;
    RAISE NOTICE '========================================';
END $$;

-- Show sample data from key views
SELECT 'v_ops_overview_daily' as view_name, COUNT(*) as rows FROM v_ops_overview_daily
UNION ALL SELECT 'v_flight_movements_hourly', COUNT(*) FROM v_flight_movements_hourly
UNION ALL SELECT 'v_vehicle_activity_summary_daily', COUNT(*) FROM v_vehicle_activity_summary_daily
UNION ALL SELECT 'v_stand_gate_occupancy', COUNT(*) FROM v_stand_gate_occupancy
UNION ALL SELECT 'v_turnaround_sla_compliance', COUNT(*) FROM v_turnaround_sla_compliance
UNION ALL SELECT 'v_delay_root_causes', COUNT(*) FROM v_delay_root_causes
UNION ALL SELECT 'v_speed_violations_by_zone', COUNT(*) FROM v_speed_violations_by_zone
UNION ALL SELECT 'v_restricted_zone_breach_dwell', COUNT(*) FROM v_restricted_zone_breach_dwell
UNION ALL SELECT 'v_discrepancy_trends_daily', COUNT(*) FROM v_discrepancy_trends_daily
UNION ALL SELECT 'v_asset_utilization_status_counts', COUNT(*) FROM v_asset_utilization_status_counts
UNION ALL SELECT 'v_alerts_summary_type_hour', COUNT(*) FROM v_alerts_summary_type_hour
UNION ALL SELECT 'v_repeat_offenders_assets', COUNT(*) FROM v_repeat_offenders_assets
UNION ALL SELECT 'v_activity_heatmap_latest', COUNT(*) FROM v_activity_heatmap_latest
UNION ALL SELECT 'v_violation_heatmap_latest', COUNT(*) FROM v_violation_heatmap_latest
ORDER BY view_name;
