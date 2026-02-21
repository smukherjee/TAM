-- ============================================================================
-- FIX TURNAROUND DATA: Proper stand distribution and varied statuses
-- ============================================================================
-- This script:
-- 1. Clears existing turnaround sessions with hardcoded A1 stand
-- 2. Regenerates sessions with random stands from actual stands table
-- 3. Creates varied task statuses instead of all CLEANING DELAYED
-- ============================================================================

-- Step 1: Clear existing bad data
DELETE FROM turnaround_tasks;
DELETE FROM turnaround_sessions;

-- Step 2: Create turnaround sessions with proper distribution
-- For each tenant, create sessions across different stands with varied statuses

-- VIDP Sessions (Delhi) - 40 sessions
WITH vidp_stands AS (
    SELECT stand_id, row_number() OVER (ORDER BY random()) as rn
    FROM stands WHERE tenant_code = 'VIDP'
),
session_data AS (
    SELECT 
        uuid_generate_v4() as id,
        'VIDP' as tenant_code,
        'AI' || (100 + gs)::text || CASE WHEN gs % 2 = 0 THEN 'D' ELSE 'A' END as flight_id,
        (SELECT stand_id FROM vidp_stands WHERE rn = ((gs % 20) + 1)) as stand_id,
        CASE 
            WHEN gs % 5 = 0 THEN 'SCHEDULED'  -- 20% scheduled (blank status on UI)
            WHEN gs % 5 = 1 THEN 'ON_BLOCK'   -- 20% on block (in progress)
            WHEN gs % 5 = 2 THEN 'ON_BLOCK'   -- 20% on block (different progress)
            WHEN gs % 5 = 3 THEN 'OFF_BLOCK'  -- 20% off block (completing)
            ELSE 'DEPARTED'                    -- 20% departed (completed)
        END as status,
        NOW() - (gs * INTERVAL '25 minutes') as created_at,
        NOW() - (gs * INTERVAL '25 minutes') as updated_at,
        -- EIBT: Expected In-Block Time
        CASE 
            WHEN gs % 5 = 0 THEN NOW() + ((gs % 60 + 30) * INTERVAL '1 minute')  -- Future for scheduled
            ELSE NOW() - ((gs * 25 + 5) * INTERVAL '1 minute')  -- Past for arrived
        END as eibt,
        -- AIBT: Actual In-Block Time (null for scheduled)
        CASE 
            WHEN gs % 5 = 0 THEN NULL  -- Not yet arrived
            ELSE NOW() - (gs * 25 * INTERVAL '1 minute')  -- Already arrived
        END as aibt,
        -- TOBT: Target Off-Block Time
        NOW() + ((60 - gs) * INTERVAL '1 minute') as tobt,
        -- TSAT: Target Start-up Approval Time
        NOW() + ((55 - gs) * INTERVAL '1 minute') as tsat,
        -- AOBT: Actual Off-Block Time (only for departed)
        CASE 
            WHEN gs % 5 >= 3 THEN NOW() - ((gs * 5) * INTERVAL '1 minute')
            ELSE NULL
        END as aobt
    FROM generate_series(1, 40) gs
)
INSERT INTO turnaround_sessions (id, tenant_code, flight_id, stand_id, status, created_at, updated_at, eibt, aibt, tobt, tsat, aobt)
SELECT id, tenant_code, flight_id, stand_id, status, created_at, updated_at, eibt, aibt, tobt, tsat, aobt
FROM session_data;

-- LIRN Sessions (Naples) - 30 sessions
WITH lirn_stands AS (
    SELECT stand_id, row_number() OVER (ORDER BY random()) as rn
    FROM stands WHERE tenant_code = 'LIRN'
),
session_data AS (
    SELECT 
        uuid_generate_v4() as id,
        'LIRN' as tenant_code,
        'AZ' || (200 + gs)::text || CASE WHEN gs % 2 = 0 THEN 'D' ELSE 'A' END as flight_id,
        (SELECT stand_id FROM lirn_stands WHERE rn = ((gs % 15) + 1)) as stand_id,
        CASE 
            WHEN gs % 5 = 0 THEN 'SCHEDULED'
            WHEN gs % 5 = 1 THEN 'ON_BLOCK'
            WHEN gs % 5 = 2 THEN 'ON_BLOCK'
            WHEN gs % 5 = 3 THEN 'OFF_BLOCK'
            ELSE 'DEPARTED'
        END as status,
        NOW() - (gs * INTERVAL '30 minutes') as created_at,
        NOW() - (gs * INTERVAL '30 minutes') as updated_at,
        CASE 
            WHEN gs % 5 = 0 THEN NOW() + ((gs % 45 + 20) * INTERVAL '1 minute')
            ELSE NOW() - ((gs * 30 + 5) * INTERVAL '1 minute')
        END as eibt,
        CASE 
            WHEN gs % 5 = 0 THEN NULL
            ELSE NOW() - (gs * 30 * INTERVAL '1 minute')
        END as aibt,
        NOW() + ((50 - gs) * INTERVAL '1 minute') as tobt,
        NOW() + ((45 - gs) * INTERVAL '1 minute') as tsat,
        CASE 
            WHEN gs % 5 >= 3 THEN NOW() - ((gs * 4) * INTERVAL '1 minute')
            ELSE NULL
        END as aobt
    FROM generate_series(1, 30) gs
)
INSERT INTO turnaround_sessions (id, tenant_code, flight_id, stand_id, status, created_at, updated_at, eibt, aibt, tobt, tsat, aobt)
SELECT id, tenant_code, flight_id, stand_id, status, created_at, updated_at, eibt, aibt, tobt, tsat, aobt
FROM session_data;

-- YBBN Sessions (Brisbane) - 30 sessions
WITH ybbn_stands AS (
    SELECT stand_id, row_number() OVER (ORDER BY random()) as rn
    FROM stands WHERE tenant_code = 'YBBN'
),
session_data AS (
    SELECT 
        uuid_generate_v4() as id,
        'YBBN' as tenant_code,
        'QF' || (400 + gs)::text || CASE WHEN gs % 2 = 0 THEN 'D' ELSE 'A' END as flight_id,
        (SELECT stand_id FROM ybbn_stands WHERE rn = ((gs % 15) + 1)) as stand_id,
        CASE 
            WHEN gs % 5 = 0 THEN 'SCHEDULED'
            WHEN gs % 5 = 1 THEN 'ON_BLOCK'
            WHEN gs % 5 = 2 THEN 'ON_BLOCK'
            WHEN gs % 5 = 3 THEN 'OFF_BLOCK'
            ELSE 'DEPARTED'
        END as status,
        NOW() - (gs * INTERVAL '28 minutes') as created_at,
        NOW() - (gs * INTERVAL '28 minutes') as updated_at,
        CASE 
            WHEN gs % 5 = 0 THEN NOW() + ((gs % 50 + 25) * INTERVAL '1 minute')
            ELSE NOW() - ((gs * 28 + 5) * INTERVAL '1 minute')
        END as eibt,
        CASE 
            WHEN gs % 5 = 0 THEN NULL
            ELSE NOW() - (gs * 28 * INTERVAL '1 minute')
        END as aibt,
        NOW() + ((55 - gs) * INTERVAL '1 minute') as tobt,
        NOW() + ((50 - gs) * INTERVAL '1 minute') as tsat,
        CASE 
            WHEN gs % 5 >= 3 THEN NOW() - ((gs * 6) * INTERVAL '1 minute')
            ELSE NULL
        END as aobt
    FROM generate_series(1, 30) gs
)
INSERT INTO turnaround_sessions (id, tenant_code, flight_id, stand_id, status, created_at, updated_at, eibt, aibt, tobt, tsat, aobt)
SELECT id, tenant_code, flight_id, stand_id, status, created_at, updated_at, eibt, aibt, tobt, tsat, aobt
FROM session_data;

-- Step 3: Create tasks with varied statuses based on session status
-- For SCHEDULED sessions: tasks should be PENDING (no status shown)
-- For ON_BLOCK sessions: tasks should have mixed progress (some COMPLETED, some IN_PROGRESS, some PENDING)
-- For OFF_BLOCK/DEPARTED: most tasks COMPLETED

-- Task types: FUEL, CATERING, CLEANING, BAGGAGE_LOAD, PASSENGER_BOARD, PUSHBACK

-- FUEL tasks
INSERT INTO turnaround_tasks (tenant_code, session_id, task_type, status, planned_start, planned_end, actual_start, actual_end)
SELECT 
    s.tenant_code, 
    s.id, 
    'FUEL', 
    CASE 
        WHEN s.status = 'SCHEDULED' THEN 'PENDING'
        WHEN s.status = 'ON_BLOCK' THEN 
            CASE WHEN random() < 0.5 THEN 'COMPLETED' ELSE 'IN_PROGRESS' END
        ELSE 'COMPLETED'
    END,
    COALESCE(s.aibt, s.eibt) + interval '5 minutes',
    COALESCE(s.aibt, s.eibt) + interval '20 minutes',
    CASE WHEN s.status != 'SCHEDULED' THEN COALESCE(s.aibt, s.eibt) + interval '7 minutes' ELSE NULL END,
    CASE WHEN s.status IN ('OFF_BLOCK', 'DEPARTED') OR (s.status = 'ON_BLOCK' AND random() < 0.5) 
         THEN COALESCE(s.aibt, s.eibt) + interval '18 minutes' ELSE NULL END
FROM turnaround_sessions s;

-- CATERING tasks
INSERT INTO turnaround_tasks (tenant_code, session_id, task_type, status, planned_start, planned_end, actual_start, actual_end)
SELECT 
    s.tenant_code, 
    s.id, 
    'CATERING', 
    CASE 
        WHEN s.status = 'SCHEDULED' THEN 'PENDING'
        WHEN s.status = 'ON_BLOCK' THEN 
            CASE 
                WHEN random() < 0.3 THEN 'COMPLETED'
                WHEN random() < 0.6 THEN 'IN_PROGRESS'
                ELSE 'PENDING'
            END
        ELSE 'COMPLETED'
    END,
    COALESCE(s.aibt, s.eibt) + interval '10 minutes',
    COALESCE(s.aibt, s.eibt) + interval '25 minutes',
    CASE WHEN s.status != 'SCHEDULED' AND random() > 0.3 THEN COALESCE(s.aibt, s.eibt) + interval '12 minutes' ELSE NULL END,
    CASE WHEN s.status IN ('OFF_BLOCK', 'DEPARTED') THEN COALESCE(s.aibt, s.eibt) + interval '24 minutes' ELSE NULL END
FROM turnaround_sessions s;

-- CLEANING tasks - with some DELAYED for variety but not all
INSERT INTO turnaround_tasks (tenant_code, session_id, task_type, status, planned_start, planned_end, actual_start, actual_end)
SELECT 
    s.tenant_code, 
    s.id, 
    'CLEANING', 
    CASE 
        WHEN s.status = 'SCHEDULED' THEN 'PENDING'
        WHEN s.status = 'ON_BLOCK' THEN 
            CASE 
                WHEN random() < 0.2 THEN 'COMPLETED'
                WHEN random() < 0.5 THEN 'IN_PROGRESS'
                WHEN random() < 0.7 THEN 'DELAYED'  -- Only 20% delayed for ON_BLOCK
                ELSE 'PENDING'
            END
        WHEN s.status IN ('OFF_BLOCK', 'DEPARTED') THEN
            CASE WHEN random() < 0.9 THEN 'COMPLETED' ELSE 'DELAYED' END  -- 10% delayed even when completed
        ELSE 'PENDING'
    END,
    COALESCE(s.aibt, s.eibt) + interval '15 minutes',
    COALESCE(s.aibt, s.eibt) + interval '30 minutes',
    CASE WHEN s.status != 'SCHEDULED' THEN COALESCE(s.aibt, s.eibt) + interval '18 minutes' ELSE NULL END,
    CASE WHEN s.status IN ('OFF_BLOCK', 'DEPARTED') 
         THEN COALESCE(s.aibt, s.eibt) + interval '32 minutes' ELSE NULL END
FROM turnaround_sessions s;

-- BAGGAGE_LOAD tasks
INSERT INTO turnaround_tasks (tenant_code, session_id, task_type, status, planned_start, planned_end, actual_start, actual_end)
SELECT 
    s.tenant_code, 
    s.id, 
    'BAGGAGE_LOAD', 
    CASE 
        WHEN s.status = 'SCHEDULED' THEN 'PENDING'
        WHEN s.status = 'ON_BLOCK' THEN 
            CASE 
                WHEN random() < 0.4 THEN 'IN_PROGRESS'
                WHEN random() < 0.7 THEN 'PENDING'
                ELSE 'COMPLETED'
            END
        ELSE 'COMPLETED'
    END,
    COALESCE(s.aibt, s.eibt) + interval '20 minutes',
    COALESCE(s.aibt, s.eibt) + interval '35 minutes',
    CASE WHEN s.status = 'ON_BLOCK' AND random() > 0.5 THEN COALESCE(s.aibt, s.eibt) + interval '22 minutes' 
         WHEN s.status IN ('OFF_BLOCK', 'DEPARTED') THEN COALESCE(s.aibt, s.eibt) + interval '21 minutes'
         ELSE NULL END,
    CASE WHEN s.status IN ('OFF_BLOCK', 'DEPARTED') 
         THEN COALESCE(s.aibt, s.eibt) + interval '34 minutes' ELSE NULL END
FROM turnaround_sessions s;

-- PASSENGER_BOARD tasks
INSERT INTO turnaround_tasks (tenant_code, session_id, task_type, status, planned_start, planned_end, actual_start, actual_end)
SELECT 
    s.tenant_code, 
    s.id, 
    'PASSENGER_BOARD', 
    CASE 
        WHEN s.status = 'SCHEDULED' THEN 'PENDING'
        WHEN s.status = 'ON_BLOCK' THEN 
            CASE 
                WHEN random() < 0.3 THEN 'IN_PROGRESS'
                ELSE 'PENDING'
            END
        WHEN s.status = 'OFF_BLOCK' THEN 'COMPLETED'
        ELSE 'COMPLETED'
    END,
    COALESCE(s.aibt, s.eibt) + interval '25 minutes',
    COALESCE(s.aibt, s.eibt) + interval '40 minutes',
    CASE WHEN s.status IN ('OFF_BLOCK', 'DEPARTED') THEN COALESCE(s.aibt, s.eibt) + interval '27 minutes' 
         WHEN s.status = 'ON_BLOCK' AND random() < 0.3 THEN COALESCE(s.aibt, s.eibt) + interval '28 minutes'
         ELSE NULL END,
    CASE WHEN s.status IN ('OFF_BLOCK', 'DEPARTED') 
         THEN COALESCE(s.aibt, s.eibt) + interval '38 minutes' ELSE NULL END
FROM turnaround_sessions s;

-- PUSHBACK tasks
INSERT INTO turnaround_tasks (tenant_code, session_id, task_type, status, planned_start, planned_end, actual_start, actual_end)
SELECT 
    s.tenant_code, 
    s.id, 
    'PUSHBACK', 
    CASE 
        WHEN s.status IN ('SCHEDULED', 'ON_BLOCK') THEN 'PENDING'
        WHEN s.status = 'OFF_BLOCK' THEN 'IN_PROGRESS'
        ELSE 'COMPLETED'
    END,
    COALESCE(s.aibt, s.eibt) + interval '40 minutes',
    COALESCE(s.aibt, s.eibt) + interval '45 minutes',
    CASE WHEN s.status IN ('OFF_BLOCK', 'DEPARTED') THEN COALESCE(s.aibt, s.eibt) + interval '42 minutes' ELSE NULL END,
    CASE WHEN s.status = 'DEPARTED' THEN COALESCE(s.aibt, s.eibt) + interval '44 minutes' ELSE NULL END
FROM turnaround_sessions s;

-- Verify results
SELECT 'Sessions by tenant and status:' as info;
SELECT tenant_code, status, COUNT(*) as count 
FROM turnaround_sessions 
GROUP BY tenant_code, status 
ORDER BY tenant_code, status;

SELECT 'Sessions by stand (sample):' as info;
SELECT tenant_code, stand_id, COUNT(*) as count 
FROM turnaround_sessions 
GROUP BY tenant_code, stand_id 
ORDER BY tenant_code, count DESC
LIMIT 20;

SELECT 'Tasks by type and status:' as info;
SELECT task_type, status, COUNT(*) as count 
FROM turnaround_tasks 
GROUP BY task_type, status 
ORDER BY task_type, status;
