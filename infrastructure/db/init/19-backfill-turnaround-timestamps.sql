-- =============================================================================
-- 19-backfill-turnaround-timestamps.sql
-- Backfills realistic SIRT, EIBT, AIBT, TOBT, TSAT and AOBT timestamps
-- on turnaround_sessions rows so the BI views produce data.
-- =============================================================================

DO $$
DECLARE
  base_time TIMESTAMPTZ;
BEGIN
  -- Only run if there are sessions and most of them have NULL sirt
  IF (SELECT COUNT(*) FROM turnaround_sessions WHERE sirt IS NULL) > 10 THEN
    RAISE NOTICE 'Backfilling turnaround timestamps...';

    UPDATE turnaround_sessions
    SET
      -- SIRT: Schedule In-block Runway Time (between 90 days ago and now)
      sirt  = created_at - (random() * interval '30 minutes'),
      -- EIBT: Estimated In-Block Time (sirt + 1-2 h flight time)
      eibt  = created_at - (random() * interval '30 minutes') + (interval '1 hour' + random() * interval '1 hour'),
      -- AIBT: Actual In-Block Time (close to eibt with small variance)
      aibt  = created_at - (random() * interval '30 minutes') + (interval '1 hour' + random() * interval '90 minutes'),
      -- TOBT: Target Off-Block Time (turnaround takes 45-90 min)
      tobt  = created_at + (interval '45 minutes' + random() * interval '45 minutes'),
      -- TSAT: Target Start-up Approval Time (10-15 min before TOBT)
      tsat  = created_at + (interval '35 minutes' + random() * interval '45 minutes'),
      -- AOBT: Actual Off-Block Time (close to TOBT)
      aobt  = created_at + (interval '50 minutes' + random() * interval '60 minutes'),
      -- Mark completed sessions
      status = CASE 
                 WHEN random() < 0.7 THEN 'COMPLETED'
                 WHEN random() < 0.5 THEN 'IN_PROGRESS'
                 ELSE status
               END,
      updated_at = NOW()
    WHERE sirt IS NULL;

    RAISE NOTICE 'Turnaround timestamps backfilled: % rows updated',
      (SELECT COUNT(*) FROM turnaround_sessions WHERE sirt IS NOT NULL);
  ELSE
    RAISE NOTICE 'Turnaround timestamps already set, skipping backfill.';
  END IF;
END $$;

-- Backfill Maintenance and Out-of-Service statuses into asset_movement_trail
-- so that v_maintenance_downtime_by_type returns data
DO $$
BEGIN
  IF (SELECT COUNT(*) FROM asset_movement_trail WHERE status IN ('Maintenance','Out of Service')) = 0 THEN
    RAISE NOTICE 'Adding maintenance statuses to asset_movement_trail...';

    UPDATE asset_movement_trail
    SET status = CASE 
                   WHEN random() < 0.08 THEN 'Maintenance'
                   WHEN random() < 0.04 THEN 'Out of Service'
                   ELSE status
                 END
    WHERE id IN (
      SELECT id FROM asset_movement_trail
      ORDER BY RANDOM()
      LIMIT 300
    );
    RAISE NOTICE 'Asset maintenance statuses added.';
  ELSE
    RAISE NOTICE 'Asset maintenance statuses already present, skipping.';
  END IF;
END $$;
