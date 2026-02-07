-- Migration: Add delay tracking to turnaround_sessions
-- Date: 2026-02-07
-- Description: Adds delay_minutes and delay_reason columns to support alert generation for delayed turnarounds

-- Add delay_minutes column
ALTER TABLE turnaround_sessions 
ADD COLUMN IF NOT EXISTS delay_minutes INTEGER;

-- Add delay_reason column
ALTER TABLE turnaround_sessions 
ADD COLUMN IF NOT EXISTS delay_reason VARCHAR(255);

-- Add index for efficient querying of delayed sessions
CREATE INDEX IF NOT EXISTS idx_turnaround_sessions_delay 
ON turnaround_sessions(delay_minutes) 
WHERE delay_minutes > 0;

-- Add comment for documentation
COMMENT ON COLUMN turnaround_sessions.delay_minutes IS 'Number of minutes the turnaround is delayed';
COMMENT ON COLUMN turnaround_sessions.delay_reason IS 'Reason for the turnaround delay';
