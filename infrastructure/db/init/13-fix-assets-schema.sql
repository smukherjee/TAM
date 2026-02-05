-- Fix Assets Schema
-- Purpose: Add missing column required by Asset.java entity

ALTER TABLE assets ADD COLUMN IF NOT EXISTS telematics_source_name VARCHAR(100);
