-- =============================================================================
-- 22-normalize-and-enrich-seed-data.sql
-- Consolidates missing functionality from non-init scripts:
-- 1. Normalizes vehicle_types categories (from normalize_vehicle_categories.sql)
-- 2. Creates asset_dwell_heatmap materialized view (from create-heatmap-views.sql)
-- 3. Replaces turnaround seed with complete data: sessions (3 tenants),
--    7 task types with realistic delay probabilities, and 4 alert types
--    (supersedes init 17-fix-turnaround-data.sql and 19-backfill-turnaround-timestamps.sql)
-- =============================================================================

SET client_min_messages = 'warning';

-- -----------------------------------------------------------------------
-- 1. Normalize vehicle_types category names to uppercase with underscores
-- -----------------------------------------------------------------------
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM vehicle_types WHERE category IN ('Fueling','Power','Services','Emergency','Cargo','Transport') LIMIT 1) THEN
    RAISE NOTICE 'Normalizing vehicle_types categories...';
    UPDATE vehicle_types SET category = 'FUEL'      WHERE category = 'Fueling';
    UPDATE vehicle_types SET category = 'POWER'     WHERE category = 'Power';
    UPDATE vehicle_types SET category = 'SERVICE'   WHERE category = 'Services';
    UPDATE vehicle_types SET category = 'EMERGENCY' WHERE category = 'Emergency';
    UPDATE vehicle_types SET category = 'CARGO'     WHERE category = 'Cargo';
    UPDATE vehicle_types SET category = 'PASSENGER' WHERE category = 'Transport';
    UPDATE vehicle_types SET category = 'PUSHBACK'  WHERE code = 'PUSHBACK';
    UPDATE vehicle_types SET category = 'BAGGAGE'   WHERE code = 'BAGGAGE';
    UPDATE vehicle_types SET category = 'CATERING'  WHERE code = 'CATERING';
    UPDATE vehicle_types SET category = 'SERVICE'   WHERE code = 'LAVATORY';
    UPDATE vehicle_types SET category = 'PASSENGER' WHERE code = 'BUS';
    -- Remove duplicates (keep first occurrence per code)
    DELETE FROM vehicle_types WHERE id IN (
      SELECT id FROM (
        SELECT id, ROW_NUMBER() OVER (PARTITION BY code ORDER BY id) AS rn
        FROM vehicle_types
      ) t WHERE rn > 1
    );
    RAISE NOTICE 'vehicle_types categories normalized.';
  ELSE
    RAISE NOTICE 'vehicle_types already normalized, skipping.';
  END IF;
END $$;

-- -----------------------------------------------------------------------
-- 2. asset_dwell_heatmap materialized view (not in init 07 or 09)
--    Identifies locations where assets spend significant time (>1 min dwell)
-- -----------------------------------------------------------------------
CREATE MATERIALIZED VIEW IF NOT EXISTS asset_dwell_heatmap AS
WITH location_stays AS (
    SELECT
        asset_identifier,
        tenant_code,
        location,
        timestamp,
        LEAD(timestamp) OVER (PARTITION BY asset_identifier, location ORDER BY timestamp) AS next_timestamp,
        LAG(location)  OVER (PARTITION BY asset_identifier ORDER BY timestamp) AS prev_location
    FROM asset_movement_trail
    WHERE location IS NOT NULL
      AND timestamp >= NOW() - INTERVAL '30 days'
)
SELECT
    time_bucket('1 hour', timestamp) AS time_bucket,
    tenant_code,
    location AS grid_location,
    ST_Y(location) AS grid_latitude,
    ST_X(location) AS grid_longitude,
    COUNT(*) AS dwell_events,
    COUNT(DISTINCT asset_identifier) AS unique_assets,
    AVG(EXTRACT(EPOCH FROM (next_timestamp - timestamp))) AS avg_dwell_seconds,
    MAX(EXTRACT(EPOCH FROM (next_timestamp - timestamp))) AS max_dwell_seconds,
    MIN(timestamp) AS first_dwell,
    MAX(timestamp) AS last_dwell
FROM location_stays
WHERE next_timestamp IS NOT NULL
  AND prev_location IS DISTINCT FROM location
GROUP BY time_bucket, tenant_code, location
HAVING AVG(EXTRACT(EPOCH FROM (next_timestamp - timestamp))) > 60
WITH NO DATA;

CREATE INDEX IF NOT EXISTS idx_dwell_heatmap_tenant_time ON asset_dwell_heatmap (tenant_code, time_bucket DESC);
CREATE INDEX IF NOT EXISTS idx_dwell_heatmap_location    ON asset_dwell_heatmap USING GIST (grid_location);

-- -----------------------------------------------------------------------
-- 3. Complete turnaround seed: sessions, tasks, alerts for all 3 tenants
--    Only runs when sessions table is empty or has minimal data.
-- -----------------------------------------------------------------------
DO $$
DECLARE
  session_count INTEGER;
  task_count    INTEGER;
BEGIN
  SELECT COUNT(*) INTO session_count FROM turnaround_sessions;
  SELECT COUNT(*) INTO task_count    FROM turnaround_tasks;

  IF task_count < 50 THEN
    RAISE NOTICE 'Seeding turnaround sessions, tasks and alerts (found % tasks)...', task_count;

    -- Clear any partial data
    DELETE FROM turnaround_alerts;
    DELETE FROM turnaround_tasks;
    DELETE FROM turnaround_sessions;

    -- Helper function for random stand selection
    CREATE OR REPLACE FUNCTION get_random_stand_for_tenant(p_tenant_code VARCHAR)
    RETURNS VARCHAR AS $fn$
    DECLARE v_stand_id VARCHAR;
    BEGIN
      SELECT stand_id INTO v_stand_id FROM stands WHERE tenant_code = p_tenant_code ORDER BY random() LIMIT 1;
      RETURN COALESCE(v_stand_id, '1');
    END;
    $fn$ LANGUAGE plpgsql VOLATILE;

  ELSE
    RAISE NOTICE 'Turnaround data already present (% tasks), skipping full seed.', task_count;
    -- Still backfill SIRT for the Superset stand-occupancy view
    UPDATE turnaround_sessions
    SET
      sirt       = created_at - (random() * interval '30 minutes'),
      eibt       = created_at - (random() * interval '30 minutes') + (interval '1 hour' + random() * interval '1 hour'),
      aibt       = CASE WHEN aibt IS NULL THEN created_at + (interval '1 hour' + random() * interval '90 minutes') ELSE aibt END,
      tobt       = CASE WHEN tobt IS NULL THEN created_at + (interval '45 minutes' + random() * interval '45 minutes') ELSE tobt END,
      tsat       = CASE WHEN tsat IS NULL THEN created_at + (interval '35 minutes' + random() * interval '45 minutes') ELSE tsat END,
      aobt       = CASE WHEN aobt IS NULL THEN created_at + (interval '50 minutes' + random() * interval '60 minutes') ELSE aobt END,
      updated_at = NOW()
    WHERE sirt IS NULL;
    RETURN;
  END IF;
END $$;

-- VIDP Sessions (Delhi) — 40 sessions with truly random stands
DO $$
DECLARE
  i INTEGER; v_stand_id VARCHAR; v_flight_id VARCHAR; v_status VARCHAR;
  v_aibt TIMESTAMPTZ; v_aobt TIMESTAMPTZ;
  airlines TEXT[] := ARRAY['AI','UK','SG','G8','6E','QR','EK','BA','LH','AF'];
  statuses TEXT[] := ARRAY['SCHEDULED','ON_BLOCK','ON_BLOCK','ON_BLOCK','OFF_BLOCK','DEPARTED'];
BEGIN
  FOR i IN 1..40 LOOP
    SELECT stand_id INTO v_stand_id FROM stands WHERE tenant_code='VIDP' ORDER BY random() LIMIT 1;
    v_flight_id := airlines[1+floor(random()*10)::int] || (100+i)::text;
    v_status    := statuses[1+floor(random()*6)::int];
    v_aibt := CASE WHEN v_status != 'SCHEDULED' THEN NOW()-(random()*INTERVAL'60 minutes') ELSE NULL END;
    v_aobt := CASE WHEN v_status IN ('OFF_BLOCK','DEPARTED') THEN NOW()-(random()*INTERVAL'20 minutes') ELSE NULL END;
    INSERT INTO turnaround_sessions (id,tenant_code,flight_id,stand_id,status,created_at,updated_at,sirt,eibt,aibt,tobt,tsat,aobt)
    VALUES (uuid_generate_v4(),'VIDP',v_flight_id,COALESCE(v_stand_id,'A1'),v_status,
      NOW()-(random()*INTERVAL'3 hours'), NOW()-(random()*INTERVAL'2 hours'),
      NOW()-(random()*INTERVAL'40 minutes'),
      NOW()-(random()*INTERVAL'90 minutes')+(random()*INTERVAL'30 minutes'), v_aibt,
      NOW()+((30+floor(random()*60))::int*INTERVAL'1 minute'),
      NOW()+((25+floor(random()*55))::int*INTERVAL'1 minute'), v_aobt);
  END LOOP;
END $$;

-- LIRN Sessions (Naples) — 25 sessions
DO $$
DECLARE
  i INTEGER; v_stand_id VARCHAR; v_flight_id VARCHAR; v_status VARCHAR;
  v_aibt TIMESTAMPTZ; v_aobt TIMESTAMPTZ;
  airlines TEXT[] := ARRAY['AZ','FR','U2','VY','W6','LH','BA','KL'];
  statuses TEXT[] := ARRAY['SCHEDULED','ON_BLOCK','ON_BLOCK','ON_BLOCK','OFF_BLOCK','DEPARTED'];
BEGIN
  FOR i IN 1..25 LOOP
    SELECT stand_id INTO v_stand_id FROM stands WHERE tenant_code='LIRN' ORDER BY random() LIMIT 1;
    v_flight_id := airlines[1+floor(random()*8)::int] || (200+i)::text;
    v_status    := statuses[1+floor(random()*6)::int];
    v_aibt := CASE WHEN v_status != 'SCHEDULED' THEN NOW()-(random()*INTERVAL'60 minutes') ELSE NULL END;
    v_aobt := CASE WHEN v_status IN ('OFF_BLOCK','DEPARTED') THEN NOW()-(random()*INTERVAL'20 minutes') ELSE NULL END;
    INSERT INTO turnaround_sessions (id,tenant_code,flight_id,stand_id,status,created_at,updated_at,sirt,eibt,aibt,tobt,tsat,aobt)
    VALUES (uuid_generate_v4(),'LIRN',v_flight_id,COALESCE(v_stand_id,'L1'),v_status,
      NOW()-(random()*INTERVAL'3 hours'), NOW()-(random()*INTERVAL'2 hours'),
      NOW()-(random()*INTERVAL'40 minutes'),
      NOW()-(random()*INTERVAL'90 minutes')+(random()*INTERVAL'30 minutes'), v_aibt,
      NOW()+((30+floor(random()*60))::int*INTERVAL'1 minute'),
      NOW()+((25+floor(random()*55))::int*INTERVAL'1 minute'), v_aobt);
  END LOOP;
END $$;

-- YBBN Sessions (Brisbane) — 25 sessions
DO $$
DECLARE
  i INTEGER; v_stand_id VARCHAR; v_flight_id VARCHAR; v_status VARCHAR;
  v_aibt TIMESTAMPTZ; v_aobt TIMESTAMPTZ;
  airlines TEXT[] := ARRAY['QF','JQ','VA','EK','SQ','CX','NZ','MH'];
  statuses TEXT[] := ARRAY['SCHEDULED','ON_BLOCK','ON_BLOCK','ON_BLOCK','OFF_BLOCK','DEPARTED'];
BEGIN
  FOR i IN 1..25 LOOP
    SELECT stand_id INTO v_stand_id FROM stands WHERE tenant_code='YBBN' ORDER BY random() LIMIT 1;
    v_flight_id := airlines[1+floor(random()*8)::int] || (400+i)::text;
    v_status    := statuses[1+floor(random()*6)::int];
    v_aibt := CASE WHEN v_status != 'SCHEDULED' THEN NOW()-(random()*INTERVAL'60 minutes') ELSE NULL END;
    v_aobt := CASE WHEN v_status IN ('OFF_BLOCK','DEPARTED') THEN NOW()-(random()*INTERVAL'20 minutes') ELSE NULL END;
    INSERT INTO turnaround_sessions (id,tenant_code,flight_id,stand_id,status,created_at,updated_at,sirt,eibt,aibt,tobt,tsat,aobt)
    VALUES (uuid_generate_v4(),'YBBN',v_flight_id,COALESCE(v_stand_id,'B1'),v_status,
      NOW()-(random()*INTERVAL'3 hours'), NOW()-(random()*INTERVAL'2 hours'),
      NOW()-(random()*INTERVAL'40 minutes'),
      NOW()-(random()*INTERVAL'90 minutes')+(random()*INTERVAL'30 minutes'), v_aibt,
      NOW()+((30+floor(random()*60))::int*INTERVAL'1 minute'),
      NOW()+((25+floor(random()*55))::int*INTERVAL'1 minute'), v_aobt);
  END LOOP;
END $$;

-- ======================== TURNAROUND TASKS ========================
-- Tasks for all sessions: FUEL, CATERING, CLEANING, UNLOADING, LOADING, DISEMBARKATION, BOARDING, PUSHBACK
-- (Each INSERT only runs if task_count was < 50, handled by the outer DO block guard above)

-- FUEL tasks
INSERT INTO turnaround_tasks (tenant_code,session_id,task_type,status,planned_start,planned_end,actual_start,actual_end)
SELECT s.tenant_code, s.id, 'FUEL',
  CASE WHEN s.status='SCHEDULED' THEN 'PENDING'
       WHEN s.status='ON_BLOCK' THEN CASE WHEN random()<0.15 THEN 'DELAYED' WHEN random()<0.45 THEN 'COMPLETED' WHEN random()<0.75 THEN 'IN_PROGRESS' ELSE 'PENDING' END
       WHEN s.status IN ('OFF_BLOCK','DEPARTED') THEN CASE WHEN random()<0.92 THEN 'COMPLETED' ELSE 'DELAYED' END
       ELSE 'PENDING' END,
  COALESCE(s.aibt,s.eibt)+interval'5 minutes', COALESCE(s.aibt,s.eibt)+interval'20 minutes',
  CASE WHEN s.status!='SCHEDULED' THEN COALESCE(s.aibt,s.eibt)+(floor(random()*10)+5)*interval'1 minute' ELSE NULL END,
  CASE WHEN s.status IN ('OFF_BLOCK','DEPARTED') OR (s.status='ON_BLOCK' AND random()<0.4) THEN COALESCE(s.aibt,s.eibt)+(floor(random()*10)+18)*interval'1 minute' ELSE NULL END
FROM turnaround_sessions s
WHERE (SELECT COUNT(*) FROM turnaround_tasks) = 0;

-- CATERING tasks
INSERT INTO turnaround_tasks (tenant_code,session_id,task_type,status,planned_start,planned_end,actual_start,actual_end)
SELECT s.tenant_code, s.id, 'CATERING',
  CASE WHEN s.status='SCHEDULED' THEN 'PENDING'
       WHEN s.status='ON_BLOCK' THEN CASE WHEN random()<0.12 THEN 'DELAYED' WHEN random()<0.40 THEN 'COMPLETED' WHEN random()<0.70 THEN 'IN_PROGRESS' ELSE 'PENDING' END
       WHEN s.status IN ('OFF_BLOCK','DEPARTED') THEN CASE WHEN random()<0.90 THEN 'COMPLETED' ELSE 'DELAYED' END
       ELSE 'PENDING' END,
  COALESCE(s.aibt,s.eibt)+interval'10 minutes', COALESCE(s.aibt,s.eibt)+interval'25 minutes',
  CASE WHEN s.status!='SCHEDULED' AND random()>0.25 THEN COALESCE(s.aibt,s.eibt)+(floor(random()*8)+10)*interval'1 minute' ELSE NULL END,
  CASE WHEN s.status IN ('OFF_BLOCK','DEPARTED') THEN COALESCE(s.aibt,s.eibt)+(floor(random()*8)+22)*interval'1 minute' ELSE NULL END
FROM turnaround_sessions s
WHERE (SELECT COUNT(*) FROM turnaround_tasks WHERE task_type='CATERING') = 0;

-- CLEANING tasks
INSERT INTO turnaround_tasks (tenant_code,session_id,task_type,status,planned_start,planned_end,actual_start,actual_end)
SELECT s.tenant_code, s.id, 'CLEANING',
  CASE WHEN s.status='SCHEDULED' THEN 'PENDING'
       WHEN s.status='ON_BLOCK' THEN CASE WHEN random()<0.18 THEN 'DELAYED' WHEN random()<0.40 THEN 'COMPLETED' WHEN random()<0.70 THEN 'IN_PROGRESS' ELSE 'PENDING' END
       WHEN s.status IN ('OFF_BLOCK','DEPARTED') THEN CASE WHEN random()<0.88 THEN 'COMPLETED' ELSE 'DELAYED' END
       ELSE 'PENDING' END,
  COALESCE(s.aibt,s.eibt)+interval'8 minutes', COALESCE(s.aibt,s.eibt)+interval'28 minutes',
  CASE WHEN s.status!='SCHEDULED' THEN COALESCE(s.aibt,s.eibt)+(floor(random()*10)+8)*interval'1 minute' ELSE NULL END,
  CASE WHEN s.status IN ('OFF_BLOCK','DEPARTED') THEN COALESCE(s.aibt,s.eibt)+(floor(random()*10)+28)*interval'1 minute' ELSE NULL END
FROM turnaround_sessions s
WHERE (SELECT COUNT(*) FROM turnaround_tasks WHERE task_type='CLEANING') = 0;

-- UNLOADING tasks
INSERT INTO turnaround_tasks (tenant_code,session_id,task_type,status,planned_start,planned_end,actual_start,actual_end)
SELECT s.tenant_code, s.id, 'UNLOADING',
  CASE WHEN s.status='SCHEDULED' THEN 'PENDING'
       WHEN s.status='ON_BLOCK' THEN CASE WHEN random()<0.10 THEN 'DELAYED' WHEN random()<0.60 THEN 'COMPLETED' WHEN random()<0.85 THEN 'IN_PROGRESS' ELSE 'PENDING' END
       ELSE 'COMPLETED' END,
  COALESCE(s.aibt,s.eibt)+interval'3 minutes', COALESCE(s.aibt,s.eibt)+interval'18 minutes',
  CASE WHEN s.status!='SCHEDULED' THEN COALESCE(s.aibt,s.eibt)+(floor(random()*5)+3)*interval'1 minute' ELSE NULL END,
  CASE WHEN s.status IN ('OFF_BLOCK','DEPARTED') OR (s.status='ON_BLOCK' AND random()<0.5) THEN COALESCE(s.aibt,s.eibt)+(floor(random()*5)+16)*interval'1 minute' ELSE NULL END
FROM turnaround_sessions s
WHERE (SELECT COUNT(*) FROM turnaround_tasks WHERE task_type='UNLOADING') = 0;

-- LOADING tasks
INSERT INTO turnaround_tasks (tenant_code,session_id,task_type,status,planned_start,planned_end,actual_start,actual_end)
SELECT s.tenant_code, s.id, 'LOADING',
  CASE WHEN s.status='SCHEDULED' THEN 'PENDING'
       WHEN s.status='ON_BLOCK' THEN CASE WHEN random()<0.14 THEN 'DELAYED' WHEN random()<0.35 THEN 'IN_PROGRESS' WHEN random()<0.50 THEN 'COMPLETED' ELSE 'PENDING' END
       ELSE 'COMPLETED' END,
  COALESCE(s.aibt,s.eibt)+interval'22 minutes', COALESCE(s.aibt,s.eibt)+interval'38 minutes',
  CASE WHEN s.status='ON_BLOCK' AND random()>0.4 THEN COALESCE(s.aibt,s.eibt)+(floor(random()*8)+22)*interval'1 minute'
       WHEN s.status IN ('OFF_BLOCK','DEPARTED') THEN COALESCE(s.aibt,s.eibt)+(floor(random()*5)+22)*interval'1 minute'
       ELSE NULL END,
  CASE WHEN s.status IN ('OFF_BLOCK','DEPARTED') THEN COALESCE(s.aibt,s.eibt)+(floor(random()*5)+35)*interval'1 minute' ELSE NULL END
FROM turnaround_sessions s
WHERE (SELECT COUNT(*) FROM turnaround_tasks WHERE task_type='LOADING') = 0;

-- DISEMBARKATION tasks
INSERT INTO turnaround_tasks (tenant_code,session_id,task_type,status,planned_start,planned_end,actual_start,actual_end)
SELECT s.tenant_code, s.id, 'DISEMBARKATION',
  CASE WHEN s.status='SCHEDULED' THEN 'PENDING'
       WHEN s.status='ON_BLOCK' THEN CASE WHEN random()<0.08 THEN 'DELAYED' WHEN random()<0.75 THEN 'COMPLETED' ELSE 'IN_PROGRESS' END
       ELSE 'COMPLETED' END,
  COALESCE(s.aibt,s.eibt)+interval'2 minutes', COALESCE(s.aibt,s.eibt)+interval'15 minutes',
  CASE WHEN s.status!='SCHEDULED' THEN COALESCE(s.aibt,s.eibt)+(floor(random()*3)+2)*interval'1 minute' ELSE NULL END,
  CASE WHEN s.status IN ('OFF_BLOCK','DEPARTED') OR (s.status='ON_BLOCK' AND random()<0.7) THEN COALESCE(s.aibt,s.eibt)+(floor(random()*5)+12)*interval'1 minute' ELSE NULL END
FROM turnaround_sessions s
WHERE (SELECT COUNT(*) FROM turnaround_tasks WHERE task_type='DISEMBARKATION') = 0;

-- BOARDING tasks
INSERT INTO turnaround_tasks (tenant_code,session_id,task_type,status,planned_start,planned_end,actual_start,actual_end)
SELECT s.tenant_code, s.id, 'BOARDING',
  CASE WHEN s.status='SCHEDULED' THEN 'PENDING'
       WHEN s.status='ON_BLOCK' THEN CASE WHEN random()<0.16 THEN 'DELAYED' WHEN random()<0.35 THEN 'IN_PROGRESS' WHEN random()<0.45 THEN 'COMPLETED' ELSE 'PENDING' END
       ELSE 'COMPLETED' END,
  COALESCE(s.aibt,s.eibt)+interval'28 minutes', COALESCE(s.aibt,s.eibt)+interval'45 minutes',
  CASE WHEN s.status IN ('OFF_BLOCK','DEPARTED') THEN COALESCE(s.aibt,s.eibt)+(floor(random()*5)+28)*interval'1 minute'
       WHEN s.status='ON_BLOCK' AND random()<0.35 THEN COALESCE(s.aibt,s.eibt)+(floor(random()*8)+28)*interval'1 minute'
       ELSE NULL END,
  CASE WHEN s.status IN ('OFF_BLOCK','DEPARTED') THEN COALESCE(s.aibt,s.eibt)+(floor(random()*8)+42)*interval'1 minute' ELSE NULL END
FROM turnaround_sessions s
WHERE (SELECT COUNT(*) FROM turnaround_tasks WHERE task_type='BOARDING') = 0;

-- PUSHBACK tasks
INSERT INTO turnaround_tasks (tenant_code,session_id,task_type,status,planned_start,planned_end,actual_start,actual_end)
SELECT s.tenant_code, s.id, 'PUSHBACK',
  CASE WHEN s.status IN ('SCHEDULED','ON_BLOCK') THEN 'PENDING'
       WHEN s.status='OFF_BLOCK' THEN CASE WHEN random()<0.12 THEN 'DELAYED' ELSE 'IN_PROGRESS' END
       ELSE 'COMPLETED' END,
  COALESCE(s.aibt,s.eibt)+interval'45 minutes', COALESCE(s.aibt,s.eibt)+interval'50 minutes',
  CASE WHEN s.status IN ('OFF_BLOCK','DEPARTED') THEN COALESCE(s.aibt,s.eibt)+(floor(random()*5)+45)*interval'1 minute' ELSE NULL END,
  CASE WHEN s.status='DEPARTED' THEN COALESCE(s.aibt,s.eibt)+(floor(random()*3)+48)*interval'1 minute' ELSE NULL END
FROM turnaround_sessions s
WHERE (SELECT COUNT(*) FROM turnaround_tasks WHERE task_type='PUSHBACK') = 0;

-- ======================== TURNAROUND ALERTS ========================
-- Only insert if no alerts exist yet
INSERT INTO turnaround_alerts (id, tenant_code, session_id, severity, type, message, timestamp, is_active)
SELECT uuid_generate_v4(), s.tenant_code, s.id,
  CASE WHEN t.task_type IN ('PUSHBACK','BOARDING') THEN 'HIGH'
       WHEN t.task_type IN ('FUEL','CATERING')     THEN 'MEDIUM'
       ELSE 'LOW' END,
  t.task_type || '_DELAY',
  'Flight ' || s.flight_id || ' at Stand ' || s.stand_id || ': ' ||
  CASE t.task_type
    WHEN 'FUEL'           THEN 'Fuel truck delayed'
    WHEN 'CATERING'       THEN 'Catering service delayed'
    WHEN 'CLEANING'       THEN 'Cabin cleaning delayed'
    WHEN 'UNLOADING'      THEN 'Baggage unloading delayed'
    WHEN 'LOADING'        THEN 'Baggage loading delayed'
    WHEN 'DISEMBARKATION' THEN 'Passenger disembarkation delayed'
    WHEN 'BOARDING'       THEN 'Passenger boarding delayed'
    WHEN 'PUSHBACK'       THEN 'Pushback delayed - waiting for crew'
    ELSE t.task_type || ' delayed'
  END,
  NOW()-(random()*INTERVAL'15 minutes'), true
FROM turnaround_sessions s
JOIN turnaround_tasks t ON t.session_id = s.id
WHERE s.status='ON_BLOCK' AND t.status='DELAYED'
  AND NOT EXISTS (SELECT 1 FROM turnaround_alerts LIMIT 1);

-- SLA breach alerts for sessions with 2+ delayed tasks
INSERT INTO turnaround_alerts (id, tenant_code, session_id, severity, type, message, timestamp, is_active)
SELECT uuid_generate_v4(), s.tenant_code, s.id, 'CRITICAL', 'SLA_BREACH_WARNING',
  'Flight ' || s.flight_id || ' at Stand ' || s.stand_id || ': Turnaround SLA at risk - multiple task delays',
  NOW()-(random()*INTERVAL'5 minutes'), true
FROM turnaround_sessions s
WHERE s.status='ON_BLOCK'
  AND (SELECT COUNT(*) FROM turnaround_tasks t WHERE t.session_id=s.id AND t.status='DELAYED') >= 2
  AND NOT EXISTS (SELECT 1 FROM turnaround_alerts WHERE type='SLA_BREACH_WARNING' LIMIT 1);

-- ======================== SUMMARY ========================
DO $$
DECLARE
  s_count INT; t_count INT; a_count INT;
BEGIN
  SELECT COUNT(*) INTO s_count FROM turnaround_sessions;
  SELECT COUNT(*) INTO t_count FROM turnaround_tasks;
  SELECT COUNT(*) INTO a_count FROM turnaround_alerts;
  RAISE NOTICE 'Script 22 complete: % sessions, % tasks, % alerts', s_count, t_count, a_count;
END $$;
