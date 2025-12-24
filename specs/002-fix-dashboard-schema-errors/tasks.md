# Tasks: Fix Analytics Dashboard Schema Errors

**Date**: 2025-12-24  
**Purpose**: Fix remaining schema inconsistencies in analytics dashboards after database migration

## Issue Summary

Multiple analytics dashboards are failing with schema errors:
1. ✅ **FIXED**: Column 'icao_code' → 'tenant_code' (flights, turnaround_events)
2. ✅ **FIXED**: Column 'time' → 'timestamp' (flights table)
3. ⚠️ **PENDING**: Column 'vehicle_no' → 'vehicle_id' (vehicles table)
4. ⚠️ **PENDING**: Column 'type' → 'vehicle_type' (vehicles table)
5. ⚠️ **PENDING**: Relation 'turnarounds' → 'turnaround_sessions' (anomaly detection queries)

## Tasks

### Phase 1: Fix Ground Vehicle Tracking Dashboard

- [x] T001 Update active vehicles count query in 04-ground-vehicle-tracking.json
  - Replace `vehicle_no` with `vehicle_id`
  - Path: infrastructure/grafana/dashboards/04-ground-vehicle-tracking.json

- [x] T002 Update vehicle type breakdown query in 04-ground-vehicle-tracking.json
  - Replace `vehicle_no` with `vehicle_id`
  - Replace `type` with `vehicle_type`
  - Path: infrastructure/grafana/dashboards/04-ground-vehicle-tracking.json

- [x] T003 Update recent vehicle activity query in 04-ground-vehicle-tracking.json
  - Replace `vehicle_no` with `vehicle_id`
  - Replace `type` with `vehicle_type`
  - Remove `location` column (doesn't exist, use latitude/longitude)
  - Path: infrastructure/grafana/dashboards/04-ground-vehicle-tracking.json

### Phase 2: Fix Anomaly Detection Dashboard (Turnaround Queries)

- [x] T004 Update turnaround time outliers query in 07-anomaly-detection.json
  - Replace `FROM turnarounds` with `FROM turnaround_sessions`
  - Replace `arrival_time` with appropriate timestamp column
  - Replace `actual_departure` with `aobt` (Actual Off-Block Time)
  - Replace `flight_number` with `flight_id`
  - Path: infrastructure/grafana/dashboards/07-anomaly-detection.json

- [x] T005 Update unexpected delays query in 07-anomaly-detection.json
  - Replace `FROM turnarounds` with `FROM turnaround_sessions`
  - Replace `actual_departure` with `aobt`
  - Replace `scheduled_departure` with `tobt` (Target Off-Block Time)
  - Replace `flight_number` with `flight_id`
  - Path: infrastructure/grafana/dashboards/07-anomaly-detection.json

- [x] T006 Update SLA violations count query in 07-anomaly-detection.json
  - Replace `FROM turnarounds` with `FROM turnaround_sessions`
  - Replace `actual_departure` with `aobt`
  - Replace `scheduled_departure` with `tobt`
  - Replace `arrival_time` with `aibt` (Actual In-Block Time)
  - Path: infrastructure/grafana/dashboards/07-anomaly-detection.json

### Phase 3: Testing & Validation

- [x] T007 Test Ground Vehicle Tracking dashboard queries in PostgreSQL
  - Run each query manually to verify schema compatibility
  - Verify results are returned without errors

- [x] T008 Test Anomaly Detection dashboard queries in PostgreSQL
  - Run each turnaround query manually
  - Verify turnaround_sessions data is properly queried

- [x] T009 Restart Grafana to reload dashboard configurations
  - Command: `docker-compose -f docker-compose.dev.yml restart grafana`

- [ ] T010 Verify all dashboards display without errors
  - Check Ground Vehicle Tracking dashboard
  - Check Anomaly Detection dashboard
  - Verify all panels load data successfully

### Phase 4: Documentation

- [ ] T011 Update sanitytest.md with dashboard error checks
  - Add specific checks for vehicle and turnaround dashboard queries
  - Document expected column names for each table

- [ ] T012 Document schema changes in CODEBASE_ANALYSIS.md
  - List all column name changes
  - Document table name changes (turnarounds → turnaround_sessions)

## Schema Reference

### Vehicles Table Columns
- ✅ `id` (UUID)
- ✅ `tenant_code` (VARCHAR)
- ✅ `vehicle_id` (VARCHAR) ← was `vehicle_no`
- ✅ `vehicle_type` (VARCHAR) ← was `type`
- ✅ `vehicle_name` (VARCHAR)
- ✅ `latitude`, `longitude`, `speed`, `heading` (DOUBLE PRECISION)
- ✅ `zone`, `status` (VARCHAR)
- ✅ `timestamp`, `created_at` (TIMESTAMPTZ)

### Turnaround Sessions Table Columns
- ✅ `id` (UUID)
- ✅ `tenant_code` (VARCHAR)
- ✅ `flight_id` (VARCHAR) ← was `flight_number`
- ✅ `stand_id` (VARCHAR)
- ✅ `sirt` (Scheduled In-Block Time)
- ✅ `eibt` (Estimated In-Block Time)
- ✅ `aibt` (Actual In-Block Time)
- ✅ `tobt` (Target Off-Block Time)
- ✅ `tsat` (Target Start-up Approval Time)
- ✅ `aobt` (Actual Off-Block Time)
- ✅ `status` (VARCHAR)
- ✅ `created_at`, `updated_at` (TIMESTAMPTZ)

## Completion Criteria

- All dashboard panels load without errors
- All queries return data successfully
- No "column does not exist" or "relation does not exist" errors
- Grafana dashboards refresh and display data properly