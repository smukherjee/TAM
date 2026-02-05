-- ============================================================================
-- COMPLETE TURNAROUND FIX: Proper stands, varied delays, randomization & alerts
-- ============================================================================
-- This script:
-- 1. Clears existing turnaround sessions with hardcoded A1 stand
-- 2. Regenerates sessions with RANDOM stands from actual stands table
-- 3. Creates varied task statuses with MULTIPLE delay types (not just cleaning)
-- 4. Adds ALERTS for delayed tasks on landed flights (ON_BLOCK status)
-- 5. Uses proper randomization for realistic data distribution
-- ============================================================================

-- Step 1: Clear existing bad data
DELETE FROM turnaround_alerts;
DELETE FROM turnaround_tasks;
DELETE FROM turnaround_sessions;

-- Create function for random stand selection (VOLATILE ensures fresh random each call)
CREATE OR REPLACE FUNCTION get_random_stand_for_tenant(p_tenant_code VARCHAR) 
RETURNS VARCHAR AS $$
DECLARE
    v_stand_id VARCHAR;
BEGIN
    SELECT stand_id INTO v_stand_id 
    FROM stands 
    WHERE tenant_code = p_tenant_code 
    ORDER BY random() 
    LIMIT 1;
    RETURN COALESCE(v_stand_id, '1');
END;
$$ LANGUAGE plpgsql VOLATILE;

-- ============================================================================
-- Step 2: Create turnaround sessions with RANDOM stand distribution using PL/pgSQL
-- ============================================================================

-- VIDP Sessions (Delhi) - 40 sessions with truly random stands
DO $$
DECLARE
    i INTEGER;
    v_stand_id VARCHAR;
    v_flight_id VARCHAR;
    v_status VARCHAR;
    v_aibt TIMESTAMP WITH TIME ZONE;
    v_aobt TIMESTAMP WITH TIME ZONE;
    airlines TEXT[] := ARRAY['AI', 'UK', 'SG', 'G8', '6E', 'QR', 'EK', 'BA', 'LH', 'AF'];
    statuses TEXT[] := ARRAY['SCHEDULED', 'ON_BLOCK', 'ON_BLOCK', 'ON_BLOCK', 'OFF_BLOCK', 'DEPARTED'];
BEGIN
    FOR i IN 1..40 LOOP
        SELECT stand_id INTO v_stand_id FROM stands WHERE tenant_code = 'VIDP' ORDER BY random() LIMIT 1;
        v_flight_id := airlines[1 + floor(random() * 10)::int] || (100 + i)::text;
        v_status := statuses[1 + floor(random() * 6)::int];
        IF v_status != 'SCHEDULED' THEN v_aibt := NOW() - (random() * INTERVAL '60 minutes'); ELSE v_aibt := NULL; END IF;
        IF v_status IN ('OFF_BLOCK', 'DEPARTED') THEN v_aobt := NOW() - (random() * INTERVAL '20 minutes'); ELSE v_aobt := NULL; END IF;
        
        INSERT INTO turnaround_sessions (id, tenant_code, flight_id, stand_id, status, created_at, updated_at, eibt, aibt, tobt, tsat, aobt)
        VALUES (uuid_generate_v4(), 'VIDP', v_flight_id, v_stand_id, v_status,
            NOW() - (random() * INTERVAL '3 hours'), NOW() - (random() * INTERVAL '2 hours'),
            NOW() - (random() * INTERVAL '90 minutes') + (random() * INTERVAL '30 minutes'), v_aibt,
            NOW() + ((30 + floor(random() * 60))::int * INTERVAL '1 minute'),
            NOW() + ((25 + floor(random() * 55))::int * INTERVAL '1 minute'), v_aobt);
    END LOOP;
END $$;

-- LIRN Sessions (Naples) - 25 sessions
DO $$
DECLARE
    i INTEGER; v_stand_id VARCHAR; v_flight_id VARCHAR; v_status VARCHAR;
    v_aibt TIMESTAMP WITH TIME ZONE; v_aobt TIMESTAMP WITH TIME ZONE;
    airlines TEXT[] := ARRAY['AZ', 'FR', 'U2', 'VY', 'W6', 'LH', 'BA', 'KL'];
    statuses TEXT[] := ARRAY['SCHEDULED', 'ON_BLOCK', 'ON_BLOCK', 'ON_BLOCK', 'OFF_BLOCK', 'DEPARTED'];
BEGIN
    FOR i IN 1..25 LOOP
        SELECT stand_id INTO v_stand_id FROM stands WHERE tenant_code = 'LIRN' ORDER BY random() LIMIT 1;
        v_flight_id := airlines[1 + floor(random() * 8)::int] || (200 + i)::text;
        v_status := statuses[1 + floor(random() * 6)::int];
        IF v_status != 'SCHEDULED' THEN v_aibt := NOW() - (random() * INTERVAL '60 minutes'); ELSE v_aibt := NULL; END IF;
        IF v_status IN ('OFF_BLOCK', 'DEPARTED') THEN v_aobt := NOW() - (random() * INTERVAL '20 minutes'); ELSE v_aobt := NULL; END IF;
        
        INSERT INTO turnaround_sessions (id, tenant_code, flight_id, stand_id, status, created_at, updated_at, eibt, aibt, tobt, tsat, aobt)
        VALUES (uuid_generate_v4(), 'LIRN', v_flight_id, v_stand_id, v_status,
            NOW() - (random() * INTERVAL '3 hours'), NOW() - (random() * INTERVAL '2 hours'),
            NOW() - (random() * INTERVAL '90 minutes') + (random() * INTERVAL '30 minutes'), v_aibt,
            NOW() + ((30 + floor(random() * 60))::int * INTERVAL '1 minute'),
            NOW() + ((25 + floor(random() * 55))::int * INTERVAL '1 minute'), v_aobt);
    END LOOP;
END $$;

-- YBBN Sessions (Brisbane) - 25 sessions
DO $$
DECLARE
    i INTEGER; v_stand_id VARCHAR; v_flight_id VARCHAR; v_status VARCHAR;
    v_aibt TIMESTAMP WITH TIME ZONE; v_aobt TIMESTAMP WITH TIME ZONE;
    airlines TEXT[] := ARRAY['QF', 'JQ', 'VA', 'EK', 'SQ', 'CX', 'NZ', 'MH'];
    statuses TEXT[] := ARRAY['SCHEDULED', 'ON_BLOCK', 'ON_BLOCK', 'ON_BLOCK', 'OFF_BLOCK', 'DEPARTED'];
BEGIN
    FOR i IN 1..25 LOOP
        SELECT stand_id INTO v_stand_id FROM stands WHERE tenant_code = 'YBBN' ORDER BY random() LIMIT 1;
        v_flight_id := airlines[1 + floor(random() * 8)::int] || (400 + i)::text;
        v_status := statuses[1 + floor(random() * 6)::int];
        IF v_status != 'SCHEDULED' THEN v_aibt := NOW() - (random() * INTERVAL '60 minutes'); ELSE v_aibt := NULL; END IF;
        IF v_status IN ('OFF_BLOCK', 'DEPARTED') THEN v_aobt := NOW() - (random() * INTERVAL '20 minutes'); ELSE v_aobt := NULL; END IF;
        
        INSERT INTO turnaround_sessions (id, tenant_code, flight_id, stand_id, status, created_at, updated_at, eibt, aibt, tobt, tsat, aobt)
        VALUES (uuid_generate_v4(), 'YBBN', v_flight_id, v_stand_id, v_status,
            NOW() - (random() * INTERVAL '3 hours'), NOW() - (random() * INTERVAL '2 hours'),
            NOW() - (random() * INTERVAL '90 minutes') + (random() * INTERVAL '30 minutes'), v_aibt,
            NOW() + ((30 + floor(random() * 60))::int * INTERVAL '1 minute'),
            NOW() + ((25 + floor(random() * 55))::int * INTERVAL '1 minute'), v_aobt);
    END LOOP;
END $$;

-- ============================================================================
-- Step 3: Create tasks with MULTIPLE delay types (not just cleaning)
-- Delay types: FUEL_DELAYED, CATERING_DELAYED, CLEANING_DELAYED, 
--              BAGGAGE_DELAYED, BOARDING_DELAYED, PUSHBACK_DELAYED
-- ============================================================================

-- FUEL tasks - with possible delays
INSERT INTO turnaround_tasks (tenant_code, session_id, task_type, status, planned_start, planned_end, actual_start, actual_end)
SELECT 
    s.tenant_code, 
    s.id, 
    'FUEL', 
    CASE 
        WHEN s.status = 'SCHEDULED' THEN 'PENDING'
        WHEN s.status = 'ON_BLOCK' THEN 
            CASE 
                WHEN random() < 0.15 THEN 'DELAYED'      -- 15% delayed
                WHEN random() < 0.45 THEN 'COMPLETED'    -- 30% completed
                WHEN random() < 0.75 THEN 'IN_PROGRESS'  -- 30% in progress
                ELSE 'PENDING'                            -- 25% pending
            END
        WHEN s.status IN ('OFF_BLOCK', 'DEPARTED') THEN
            CASE WHEN random() < 0.92 THEN 'COMPLETED' ELSE 'DELAYED' END
        ELSE 'PENDING'
    END,
    COALESCE(s.aibt, s.eibt) + interval '5 minutes',
    COALESCE(s.aibt, s.eibt) + interval '20 minutes',
    CASE WHEN s.status != 'SCHEDULED' THEN 
        COALESCE(s.aibt, s.eibt) + (floor(random() * 10) + 5) * interval '1 minute' 
    ELSE NULL END,
    CASE WHEN s.status IN ('OFF_BLOCK', 'DEPARTED') OR (s.status = 'ON_BLOCK' AND random() < 0.4) 
         THEN COALESCE(s.aibt, s.eibt) + (floor(random() * 10) + 18) * interval '1 minute' 
    ELSE NULL END
FROM turnaround_sessions s;

-- CATERING tasks - with possible delays
INSERT INTO turnaround_tasks (tenant_code, session_id, task_type, status, planned_start, planned_end, actual_start, actual_end)
SELECT 
    s.tenant_code, 
    s.id, 
    'CATERING', 
    CASE 
        WHEN s.status = 'SCHEDULED' THEN 'PENDING'
        WHEN s.status = 'ON_BLOCK' THEN 
            CASE 
                WHEN random() < 0.12 THEN 'DELAYED'      -- 12% delayed
                WHEN random() < 0.40 THEN 'COMPLETED'
                WHEN random() < 0.70 THEN 'IN_PROGRESS'
                ELSE 'PENDING'
            END
        WHEN s.status IN ('OFF_BLOCK', 'DEPARTED') THEN
            CASE WHEN random() < 0.90 THEN 'COMPLETED' ELSE 'DELAYED' END
        ELSE 'PENDING'
    END,
    COALESCE(s.aibt, s.eibt) + interval '10 minutes',
    COALESCE(s.aibt, s.eibt) + interval '25 minutes',
    CASE WHEN s.status != 'SCHEDULED' AND random() > 0.25 THEN 
        COALESCE(s.aibt, s.eibt) + (floor(random() * 8) + 10) * interval '1 minute' 
    ELSE NULL END,
    CASE WHEN s.status IN ('OFF_BLOCK', 'DEPARTED') THEN 
        COALESCE(s.aibt, s.eibt) + (floor(random() * 8) + 22) * interval '1 minute' 
    ELSE NULL END
FROM turnaround_sessions s;

-- CLEANING tasks - with possible delays  
INSERT INTO turnaround_tasks (tenant_code, session_id, task_type, status, planned_start, planned_end, actual_start, actual_end)
SELECT 
    s.tenant_code, 
    s.id, 
    'CLEANING', 
    CASE 
        WHEN s.status = 'SCHEDULED' THEN 'PENDING'
        WHEN s.status = 'ON_BLOCK' THEN 
            CASE 
                WHEN random() < 0.18 THEN 'DELAYED'      -- 18% delayed (cleaning delays common)
                WHEN random() < 0.40 THEN 'COMPLETED'
                WHEN random() < 0.70 THEN 'IN_PROGRESS'
                ELSE 'PENDING'
            END
        WHEN s.status IN ('OFF_BLOCK', 'DEPARTED') THEN
            CASE WHEN random() < 0.88 THEN 'COMPLETED' ELSE 'DELAYED' END
        ELSE 'PENDING'
    END,
    COALESCE(s.aibt, s.eibt) + interval '8 minutes',
    COALESCE(s.aibt, s.eibt) + interval '28 minutes',
    CASE WHEN s.status != 'SCHEDULED' THEN 
        COALESCE(s.aibt, s.eibt) + (floor(random() * 10) + 8) * interval '1 minute' 
    ELSE NULL END,
    CASE WHEN s.status IN ('OFF_BLOCK', 'DEPARTED') 
         THEN COALESCE(s.aibt, s.eibt) + (floor(random() * 10) + 28) * interval '1 minute' 
    ELSE NULL END
FROM turnaround_sessions s;

-- UNLOADING tasks - baggage unload, with possible delays
INSERT INTO turnaround_tasks (tenant_code, session_id, task_type, status, planned_start, planned_end, actual_start, actual_end)
SELECT 
    s.tenant_code, 
    s.id, 
    'UNLOADING', 
    CASE 
        WHEN s.status = 'SCHEDULED' THEN 'PENDING'
        WHEN s.status = 'ON_BLOCK' THEN 
            CASE 
                WHEN random() < 0.10 THEN 'DELAYED'      -- 10% delayed
                WHEN random() < 0.60 THEN 'COMPLETED'    -- Usually done early
                WHEN random() < 0.85 THEN 'IN_PROGRESS'
                ELSE 'PENDING'
            END
        ELSE 'COMPLETED'
    END,
    COALESCE(s.aibt, s.eibt) + interval '3 minutes',
    COALESCE(s.aibt, s.eibt) + interval '18 minutes',
    CASE WHEN s.status != 'SCHEDULED' THEN 
        COALESCE(s.aibt, s.eibt) + (floor(random() * 5) + 3) * interval '1 minute' 
    ELSE NULL END,
    CASE WHEN s.status IN ('OFF_BLOCK', 'DEPARTED') OR (s.status = 'ON_BLOCK' AND random() < 0.5) 
         THEN COALESCE(s.aibt, s.eibt) + (floor(random() * 5) + 16) * interval '1 minute' 
    ELSE NULL END
FROM turnaround_sessions s;

-- LOADING tasks - baggage load, with possible delays
INSERT INTO turnaround_tasks (tenant_code, session_id, task_type, status, planned_start, planned_end, actual_start, actual_end)
SELECT 
    s.tenant_code, 
    s.id, 
    'LOADING', 
    CASE 
        WHEN s.status = 'SCHEDULED' THEN 'PENDING'
        WHEN s.status = 'ON_BLOCK' THEN 
            CASE 
                WHEN random() < 0.14 THEN 'DELAYED'      -- 14% delayed
                WHEN random() < 0.35 THEN 'IN_PROGRESS'
                WHEN random() < 0.50 THEN 'COMPLETED'
                ELSE 'PENDING'
            END
        ELSE 'COMPLETED'
    END,
    COALESCE(s.aibt, s.eibt) + interval '22 minutes',
    COALESCE(s.aibt, s.eibt) + interval '38 minutes',
    CASE WHEN s.status = 'ON_BLOCK' AND random() > 0.4 THEN 
        COALESCE(s.aibt, s.eibt) + (floor(random() * 8) + 22) * interval '1 minute' 
         WHEN s.status IN ('OFF_BLOCK', 'DEPARTED') THEN 
        COALESCE(s.aibt, s.eibt) + (floor(random() * 5) + 22) * interval '1 minute'
         ELSE NULL END,
    CASE WHEN s.status IN ('OFF_BLOCK', 'DEPARTED') 
         THEN COALESCE(s.aibt, s.eibt) + (floor(random() * 5) + 35) * interval '1 minute' 
    ELSE NULL END
FROM turnaround_sessions s;

-- DISEMBARKATION tasks - passengers off
INSERT INTO turnaround_tasks (tenant_code, session_id, task_type, status, planned_start, planned_end, actual_start, actual_end)
SELECT 
    s.tenant_code, 
    s.id, 
    'DISEMBARKATION', 
    CASE 
        WHEN s.status = 'SCHEDULED' THEN 'PENDING'
        WHEN s.status = 'ON_BLOCK' THEN 
            CASE 
                WHEN random() < 0.08 THEN 'DELAYED'      -- 8% delayed
                WHEN random() < 0.75 THEN 'COMPLETED'    -- Usually done first
                ELSE 'IN_PROGRESS'
            END
        ELSE 'COMPLETED'
    END,
    COALESCE(s.aibt, s.eibt) + interval '2 minutes',
    COALESCE(s.aibt, s.eibt) + interval '15 minutes',
    CASE WHEN s.status != 'SCHEDULED' THEN 
        COALESCE(s.aibt, s.eibt) + (floor(random() * 3) + 2) * interval '1 minute' 
    ELSE NULL END,
    CASE WHEN s.status IN ('OFF_BLOCK', 'DEPARTED') OR (s.status = 'ON_BLOCK' AND random() < 0.7) 
         THEN COALESCE(s.aibt, s.eibt) + (floor(random() * 5) + 12) * interval '1 minute' 
    ELSE NULL END
FROM turnaround_sessions s;

-- BOARDING tasks - passengers on
INSERT INTO turnaround_tasks (tenant_code, session_id, task_type, status, planned_start, planned_end, actual_start, actual_end)
SELECT 
    s.tenant_code, 
    s.id, 
    'BOARDING', 
    CASE 
        WHEN s.status = 'SCHEDULED' THEN 'PENDING'
        WHEN s.status = 'ON_BLOCK' THEN 
            CASE 
                WHEN random() < 0.16 THEN 'DELAYED'      -- 16% delayed (boarding delays common)
                WHEN random() < 0.35 THEN 'IN_PROGRESS'
                WHEN random() < 0.45 THEN 'COMPLETED'
                ELSE 'PENDING'
            END
        WHEN s.status = 'OFF_BLOCK' THEN 'COMPLETED'
        ELSE 'COMPLETED'
    END,
    COALESCE(s.aibt, s.eibt) + interval '28 minutes',
    COALESCE(s.aibt, s.eibt) + interval '45 minutes',
    CASE WHEN s.status IN ('OFF_BLOCK', 'DEPARTED') THEN 
        COALESCE(s.aibt, s.eibt) + (floor(random() * 5) + 28) * interval '1 minute' 
         WHEN s.status = 'ON_BLOCK' AND random() < 0.35 THEN 
        COALESCE(s.aibt, s.eibt) + (floor(random() * 8) + 28) * interval '1 minute'
         ELSE NULL END,
    CASE WHEN s.status IN ('OFF_BLOCK', 'DEPARTED') 
         THEN COALESCE(s.aibt, s.eibt) + (floor(random() * 8) + 42) * interval '1 minute' 
    ELSE NULL END
FROM turnaround_sessions s;

-- PUSHBACK tasks
INSERT INTO turnaround_tasks (tenant_code, session_id, task_type, status, planned_start, planned_end, actual_start, actual_end)
SELECT 
    s.tenant_code, 
    s.id, 
    'PUSHBACK', 
    CASE 
        WHEN s.status IN ('SCHEDULED', 'ON_BLOCK') THEN 'PENDING'
        WHEN s.status = 'OFF_BLOCK' THEN 
            CASE WHEN random() < 0.12 THEN 'DELAYED' ELSE 'IN_PROGRESS' END
        ELSE 'COMPLETED'
    END,
    COALESCE(s.aibt, s.eibt) + interval '45 minutes',
    COALESCE(s.aibt, s.eibt) + interval '50 minutes',
    CASE WHEN s.status IN ('OFF_BLOCK', 'DEPARTED') THEN 
        COALESCE(s.aibt, s.eibt) + (floor(random() * 5) + 45) * interval '1 minute' 
    ELSE NULL END,
    CASE WHEN s.status = 'DEPARTED' THEN 
        COALESCE(s.aibt, s.eibt) + (floor(random() * 3) + 48) * interval '1 minute' 
    ELSE NULL END
FROM turnaround_sessions s;

-- ============================================================================
-- Step 4: Create ALERTS for delayed tasks on landed flights (ON_BLOCK status)
-- Alert types: FUEL_DELAY, CATERING_DELAY, CLEANING_DELAY, BAGGAGE_DELAY, 
--              BOARDING_DELAY, PUSHBACK_DELAY, GROUND_CREW_DELAY
-- ============================================================================

-- Create alerts for all DELAYED tasks on ON_BLOCK sessions
INSERT INTO turnaround_alerts (id, tenant_code, session_id, severity, type, message, timestamp, is_active)
SELECT 
    uuid_generate_v4() as id,
    s.tenant_code,
    s.id as session_id,
    CASE 
        WHEN t.task_type IN ('PUSHBACK', 'BOARDING') THEN 'HIGH'
        WHEN t.task_type IN ('FUEL', 'CATERING') THEN 'MEDIUM'
        ELSE 'LOW'
    END as severity,
    t.task_type || '_DELAY' as type,
    'Flight ' || s.flight_id || ' at Stand ' || s.stand_id || ': ' || 
    CASE t.task_type
        WHEN 'FUEL' THEN 'Fuel truck delayed - fuel service behind schedule'
        WHEN 'CATERING' THEN 'Catering service delayed - waiting for catering truck'
        WHEN 'CLEANING' THEN 'Cabin cleaning delayed - cleaning crew behind schedule'
        WHEN 'UNLOADING' THEN 'Baggage unloading delayed - waiting for baggage handlers'
        WHEN 'LOADING' THEN 'Baggage loading delayed - cargo loading behind schedule'
        WHEN 'DISEMBARKATION' THEN 'Passenger disembarkation delayed - bridge connection issue'
        WHEN 'BOARDING' THEN 'Passenger boarding delayed - boarding process behind schedule'
        WHEN 'PUSHBACK' THEN 'Pushback delayed - waiting for pushback crew'
        ELSE t.task_type || ' delayed'
    END as message,
    NOW() - (random() * INTERVAL '15 minutes') as timestamp,
    true as is_active
FROM turnaround_sessions s
JOIN turnaround_tasks t ON t.session_id = s.id
WHERE s.status = 'ON_BLOCK' AND t.status = 'DELAYED';

-- Add some additional operational alerts for variety
INSERT INTO turnaround_alerts (id, tenant_code, session_id, severity, type, message, timestamp, is_active)
SELECT 
    uuid_generate_v4() as id,
    s.tenant_code,
    s.id as session_id,
    'MEDIUM' as severity,
    'GROUND_CREW_DELAY' as type,
    'Flight ' || s.flight_id || ' at Stand ' || s.stand_id || ': Ground crew allocation pending',
    NOW() - (random() * INTERVAL '20 minutes') as timestamp,
    true as is_active
FROM turnaround_sessions s
WHERE s.status = 'ON_BLOCK' 
  AND random() < 0.15;  -- 15% of ON_BLOCK flights get a ground crew delay alert

-- Add gate conflict alerts for some sessions
INSERT INTO turnaround_alerts (id, tenant_code, session_id, severity, type, message, timestamp, is_active)
SELECT 
    uuid_generate_v4() as id,
    s.tenant_code,
    s.id as session_id,
    'HIGH' as severity,
    'GATE_CONFLICT' as type,
    'Flight ' || s.flight_id || ' at Stand ' || s.stand_id || ': Potential gate conflict with scheduled arrival',
    NOW() - (random() * INTERVAL '10 minutes') as timestamp,
    true as is_active
FROM turnaround_sessions s
WHERE s.status = 'ON_BLOCK' 
  AND random() < 0.08;  -- 8% of ON_BLOCK flights get a gate conflict alert

-- Add SLA breach warnings for significantly delayed operations
INSERT INTO turnaround_alerts (id, tenant_code, session_id, severity, type, message, timestamp, is_active)
SELECT 
    uuid_generate_v4() as id,
    s.tenant_code,
    s.id as session_id,
    'CRITICAL' as severity,
    'SLA_BREACH_WARNING' as type,
    'Flight ' || s.flight_id || ' at Stand ' || s.stand_id || ': Turnaround SLA at risk - multiple task delays detected',
    NOW() - (random() * INTERVAL '5 minutes') as timestamp,
    true as is_active
FROM turnaround_sessions s
WHERE s.status = 'ON_BLOCK'
  AND (SELECT COUNT(*) FROM turnaround_tasks t WHERE t.session_id = s.id AND t.status = 'DELAYED') >= 2;

-- ============================================================================
-- Verification Queries
-- ============================================================================

SELECT '=== Sessions by tenant and status ===' as info;
SELECT tenant_code, status, COUNT(*) as count 
FROM turnaround_sessions 
GROUP BY tenant_code, status 
ORDER BY tenant_code, status;

SELECT '=== Sessions by stand (VIDP top 15) - should be varied, NOT all A1 ===' as info;
SELECT stand_id, COUNT(*) as count 
FROM turnaround_sessions 
WHERE tenant_code = 'VIDP'
GROUP BY stand_id 
ORDER BY count DESC
LIMIT 15;

SELECT '=== Tasks by type and status ===' as info;
SELECT task_type, status, COUNT(*) as count 
FROM turnaround_tasks 
GROUP BY task_type, status 
ORDER BY task_type, status;

SELECT '=== Delayed tasks breakdown ===' as info;
SELECT tenant_code, task_type, COUNT(*) as delayed_count
FROM turnaround_tasks 
WHERE status = 'DELAYED'
GROUP BY tenant_code, task_type
ORDER BY tenant_code, delayed_count DESC;

SELECT '=== Alerts by type ===' as info;
SELECT type, severity, COUNT(*) as count
FROM turnaround_alerts
GROUP BY type, severity
ORDER BY severity, type;

SELECT '=== Sample: ON_BLOCK flights with alerts ===' as info;
SELECT 
    s.flight_id, 
    s.stand_id,
    s.status,
    a.type as alert_type,
    a.severity,
    a.message
FROM turnaround_sessions s
JOIN turnaround_alerts a ON a.session_id = s.id
WHERE s.status = 'ON_BLOCK'
ORDER BY a.severity DESC, s.flight_id
LIMIT 15;
