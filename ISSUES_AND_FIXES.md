# Issues and Fixes Log

## 1. Superset Provisioning: API Authentication Failures

**Issue**: `create-all-reports.sh` failed repeatedly with "Fatal error" and HTTP 500 responses when calling Superset API (`/api/v1/dashboard/`).
**Root Cause**: The API interaction via `curl` was brittle. It faced issues with JWT token maintenance, session cookies, and user context (`AnonymousUserMixin` errors) because the bootstrap container was external to the Superset flask application context.
**Fix**:
- **Architecture Change**: Abandoned the external API approach.
- **New Solution**: Created `provision_superset.py` which runs *inside* the Superset container environment. It uses Superset's internal Python classes (`SupersetApp`, `Database`, `Slice`, `Dashboard` models) to directly insert metadata into the database.
- **Outcome**: 100% reliable provisioning without HTTP/Auth overhead.

## 2. Superset Provisioning: Missing Data / "Table Not Found"

**Issue**: Dashboards were created but charts showed errors or no data, and "Table not found" errors appeared in logs.
**Root Cause**: Superset was defaulting to an internal **SQLite** database for its metadata (dashboards, slices) because `SQLALCHEMY_DATABASE_URI` was not explicitly set to PostgreSQL in `superset_config.py`. This meant the bootstrap container and the main Superset container were using different, isolated SQLite files.
**Fix**:
- **Configuration**: Updated `superset_config.py` to point `SQLALCHEMY_DATABASE_URI` to the shared `timescaledb` service (`postgresql://...`).
- **Outcome**: Both containers now share the same persistent metadata store.

## 3. Superset Health Check Parsing Error

**Issue**: `sidecar-bootstrap.sh` failed with `jq: parse error` because Superset's `/health` endpoint returned plain text "OK" instead of JSON.
**Fix**:
- **Script Update**: Updated `sidecar-bootstrap.sh` to use Python's `urllib` to check for status 200, removing the dependency on `jq` and `curl` output parsing.

## 4. Missing Chart Data (Stand Conflicts, Utilization)

**Issue**: Charts "Stand Conflicts" and others were empty. Predictive charts showed "404 Not Found".
**Root Cause**:
- **Stand Conflicts**: The seed data (`11-demo-seed.sql`) did not contain overlapping turnaround sessions, so the `v_stand_conflicts` view returned 0 rows.
- **Predictive Charts**: Likely due to restrictive default filters or incomplete dataset metadata.
**Fix**:
- **Data**: Updated `11-demo-seed.sql` to insert overlapping `turnaround_sessions`.
- **Controls**: Updated `provision_superset.py` to add `time_range="No filter"` to all table charts, ensuring no data is hidden by default. Added "Include Search" control to table charts.

## 5. Vehicle Status Discrepancy (Previous Task)

**Issue**: Map popup showed calculated "idle" status while Assets table showed "In Use".
**Fix**:
- **Frontend**: Updated `VehicleLayer` and `VehicleInfoCard` to fetch and display the authoritative database status.

## 6. Missing Flights & Vehicles Data (Step 6 Verification)

**Issue**: The "Verify Services" step showed empty Flights and Vehicles tables, even after running all setup scripts.
**Root Cause**: The `flights` and `vehicles` tables were designed to be populated solely by real-time NiFi ingestion. There was no static seed data for them in `populate_report_data.sql`. If NiFi wasn't running or hadn't ingested data yet, the UI appeared broken.
**Fix**:
- **Data Seeding**: Updated `infrastructure/db/populate_report_data.sql` to explicitly insert 50 mock flights and 50 mock vehicles. This ensures immediate data availability for UI testing, independent of NiFi status.
- **NiFi Automation**: Clarified in `rebuild_guide.md` that `make setup-nifi` might need to be run manually if the automatic `dev-up` flow times out or fails silently.
