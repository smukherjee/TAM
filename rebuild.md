# TAM Infrastructure Rebuild Guide

Complete guide for rebuilding and populating the TAM system after a complete Docker teardown.

---

## Understanding the Data Initialization Flow

### Automatic Initialization (No Manual Steps Required!)

When you start Docker services, the following happens **automatically**:

1. **Database Schema Creation** (via Docker volume mount)
   - TimescaleDB runs all SQL files in [`infrastructure/db/init/`](file:///Users/sujoymukherjee/code/TAM/infrastructure/db/init) **in alphabetical order**
   - This creates tables, views, and seeds core data (tenants, users)

2. **NiFi Configuration** (via `dev-up` script)
   - Automatically configures data ingestion flows

3. **Superset Provisioning** (via sidecar container)
   - Automatically creates dashboards and charts

---

## Core Data Populated Automatically

The following **core/seed data** is populated automatically when Docker starts via SQL scripts in `infrastructure/db/init/`:

### Execution Order (Alphabetical by Filename)

| Order | File | What It Creates |
|-------|------|-----------------|
| 1 | [`01-core-platform.sql`](file:///Users/sujoymukherjee/code/TAM/infrastructure/db/init/01-core-platform.sql) | **Core Data**: Tenants (airports), Users, Audit logs, System metrics |
| 2 | [`02-aviation-domain.sql`](file:///Users/sujoymukherjee/code/TAM/infrastructure/db/init/02-aviation-domain.sql) | Tables: Flights, Vehicles (telemetry) |
| 3 | [`03-telemetry-domain.sql`](file:///Users/sujoymukherjee/code/TAM/infrastructure/db/init/03-telemetry-domain.sql) | Tables: Sensor alerts |
| 4 | [`04-turnaround-domain.sql`](file:///Users/sujoymukherjee/code/TAM/infrastructure/db/init/04-turnaround-domain.sql) | Tables: Turnaround events, sessions, tasks, alerts |
| 5 | [`05-asset-management.sql`](file:///Users/sujoymukherjee/code/TAM/infrastructure/db/init/05-asset-management.sql) | Tables: Assets, **Seeds sample assets** |
| 5b | [`05b-vehicle-asset-mapping.sql`](file:///Users/sujoymukherjee/code/TAM/infrastructure/db/init/05b-vehicle-asset-mapping.sql) | **Table: vehicle_asset_map** (links vehicles to assets) |
| 6 | [`06-asset-tracking-security.sql`](file:///Users/sujoymukherjee/code/TAM/infrastructure/db/init/06-asset-tracking-security.sql) | Tables: Restricted zones, Movement trail, Violations, Discrepancies |
| 7 | [`07-heatmap-views.sql`](file:///Users/sujoymukherjee/code/TAM/infrastructure/db/init/07-heatmap-views.sql) | Materialized views: Activity & violation heatmaps |
| 8 | [`08-admin-zones-paths.sql`](file:///Users/sujoymukherjee/code/TAM/infrastructure/db/init/08-admin-zones-paths.sql) | Tables: Zones, Vehicle paths, Vehicle types |
| 9 | [`09-superset-report-views.sql`](file:///Users/sujoymukherjee/code/TAM/infrastructure/db/init/09-superset-report-views.sql) | Views: All reporting views for Superset |
| 10 | [`10-predictive-tables.sql`](file:///Users/sujoymukherjee/code/TAM/infrastructure/db/init/10-predictive-tables.sql) | Tables: Predictive analytics tables |
| 11 | [`11-demo-seed.sql`](file:///Users/sujoymukherjee/code/TAM/infrastructure/db/init/11-demo-seed.sql) | **Seed Data**: Minimal demo data for charts |

### Core Seed Data Included

From `01-core-platform.sql`:

**Tenants (Airports)**:
- `VIDP` - Indira Gandhi International Airport (Delhi)
- `LIRN` - Naples International Airport
- `YBBN` - Brisbane International Airport

**Users** (9 users total):
- `admin_vidp` / `gh_vidp` / `user_vidp`
- `admin_lirn` / `gh_lirn` / `user_lirn`
- `admin_ybbn` / `gh_ybbn` / `user_ybbn`

---

## Quick Start (Recommended)

After running `make reset`, use this **single command** to rebuild everything:

```bash
make dev-up
```

**What this does automatically**:
1. ✅ Starts all Docker services
2. ✅ Creates database schema (via init scripts)
3. ✅ Seeds core data (tenants, users)
4. ✅ Waits for services to be healthy
5. ✅ Configures NiFi flows
6. ✅ Provisions Superset dashboards
7. ✅ Runs smoke tests

**Then populate test/demo data**:
```bash
bash infrastructure/db/master-data-setup.sh
```

**Total time**: ~3-5 minutes

---

## Step-by-Step Manual Process

If you need more control or want to understand each step:

### 1. Complete Teardown (Clean Slate)

```bash
# Stop all services and delete volumes
make reset
```

**What this does**:
- Stops all Docker containers
- Removes all volumes (deletes all data)
- Equivalent to: `docker-compose -f docker-compose.dev.yml down -v`

### 2. Rebuild and Start Services

```bash
# Option A: Start with rebuild (recommended after code changes)
make dev-up-build

# Option B: Start without rebuild (faster if no code changes)
make dev-up
```

**What happens automatically**:

#### 2a. Database Initialization
- TimescaleDB starts and runs all `infrastructure/db/init/*.sql` files
- Creates all tables, views, and indexes
- Seeds core data: tenants (airports), users
- **No manual intervention needed!**

#### 2b. NiFi Configuration
- `dev-up` script waits for NiFi to be ready
- Runs `infrastructure/nifi/setup-nifi.sh`
- Configures data ingestion flows

#### 2c. Superset Provisioning
- Superset sidecar container (`superset-bootstrap`) starts
- Waits for Superset to be healthy
- Runs `infrastructure/superset/provision_superset.py` (Python)
- Creates datasets, charts, dashboards, and seeds data directly

### 3. Populate Test/Demo Data

The system requires master data and mock telemetry to function correctly (`flights`, `vehicles` tables are seeded here).

```bash
# Run the master data setup script (from host)
./infrastructure/db/master-data-setup.sh
```

> **Note on NiFi**: If you see "NiFi flows not running" or empty Kafka topics, you can manually trigger the setup:
> ```bash
> make setup-nifi
> ```

**What this script does** (in order):

| Step | Script | What It Populates |
|------|--------|-------------------|
| 1 | [`fix_schema_and_add_stands.sql`](file:///Users/sujoymukherjee/code/TAM/infrastructure/db/fix_schema_and_add_stands.sql) | Stands, zones, schema fixes |
| 2 | [`populate_report_data.sql`](file:///Users/sujoymukherjee/code/TAM/infrastructure/db/populate_report_data.sql) | Zone violations, movement trails, discrepancies |
| 3 | [`update_asset_statuses.sql`](file:///Users/sujoymukherjee/code/TAM/infrastructure/db/update_asset_statuses.sql) | Asset statuses and locations |
| 4 | [`fix_turnaround_data.sql`](file:///Users/sujoymukherjee/code/TAM/infrastructure/db/fix_turnaround_data.sql) | Turnaround sessions and tasks |
| 5 | Materialized view refresh | Refreshes activity and violation heatmaps |

**Expected output**:
```
==========================================
TAM Master Data Setup Starting...
==========================================

Step 1/5: Checking schema and stands...
✓ Done

Step 2/5: Populating report data...
✓ Done

Step 3/5: Updating asset statuses...
✓ Done

Step 4/5: Fixing turnaround data...
✓ Done

Step 5/5: Refreshing materialized views...
✓ Materialized views refreshed

==========================================
Data Population Summary
==========================================
 table_name            | count
-----------------------+-------
 asset_movement_trail  | 1500
 assets                | 150
 movement_discrepancies| 45
 turnaround_sessions   | 30
 zone_violations       | 120
```

### 4. (Optional) Manually Provision Superset

If the automatic provisioning failed or you want to reprovision:

```bash
# Bootstrap Superset (creates dashboards)
make superset-bootstrap

# Or just provision reports
make superset-provision

# Reprovision (purge + create)
make superset-reprovision
```

### 5. Verify Services

Check that all services are running:

```bash
make dev-ps
```

**Expected services**:
- ✅ timescaledb (healthy)
- ✅ redpanda (healthy)
- ✅ backend (healthy)
- ✅ frontend (running)
- ✅ nifi (running)
- ✅ redis (healthy)
- ✅ prometheus (running)
- ✅ grafana (running)
- ✅ superset (running)

### 6. Test the Application

**Frontend**: http://localhost:3000
- Login: `admin_vidp` / `admin`
- Check map displays vehicles
- Verify assets table shows data

**Backend API**: http://localhost:8080
- Test endpoint: http://localhost:8080/api/vehicles

**NiFi**: http://localhost:8091
- Verify flows are running
- Check for data flowing through processors

**Superset**: http://localhost:8089
- Login: `admin` / `admin`
- Check dashboards are created
- **Verify "TAM Turnaround" dashboard shows data in "Stand Conflicts" chart**
- **Verify "TAM Predictive" dashboard shows data (no 404 errors)**

---

## Available Make Targets

### Infrastructure Management

| Command | Description |
|---------|-------------|
| `make dev-up` | ⭐ **Start all services + auto-configure NiFi + Superset** |
| `make dev-up-build` | Rebuild images and start all services |
| `make dev-up-only` | Start services without NiFi configuration |
| `make dev-down` | Stop all services (keeps volumes) |
| `make dev-ps` | Show running containers |
| `make dev-logs` | Tail logs from all services |
| `make dev-logs-backend` | Tail backend logs only |
| `make dev-logs-nifi` | Tail NiFi logs only |
| `make reset` | **Stop and delete all volumes** |
| `make db-reset` | Reset only database (preserves other services) |
| `make infra-up` | Start only infrastructure (no app) |

### Build Commands

| Command | Description |
|---------|-------------|
| `make backend-build` | Build backend JAR |
| `make frontend-build` | Build frontend dist |
| `make build-all` | Build both backend and frontend |
| `make clean` | Remove build artifacts |

### NiFi Management

| Command | Description |
|---------|-------------|
| `make setup-nifi` | Configure NiFi flows (auto-run by `dev-up`) |

### Superset Management

| Command | Description |
|---------|-------------|
| `make superset-bootstrap` | Bootstrap Superset (create admin + provision) |
| `make superset-provision` | Create/update all dashboards and charts |
| `make superset-purge` | Delete all dashboards and charts |
| `make superset-reprovision` | Purge + provision (clean slate) |
| `make superset-dedupe` | Remove duplicate charts |

### Utility Commands

| Command | Description |
|---------|-------------|
| `make topics` | Show Kafka topics |
| `make schema-audit` | Generate DB schema documentation |
| `make test-flight` | Send test flight message to Kafka |

---

## Common Workflows

### Workflow 1: Fresh Start After Code Changes

```bash
# 1. Teardown everything
make dev-down

# 2. Rebuild and start (includes auto-config)
make dev-up-build

# 3. Populate test data
bash infrastructure/db/master-data-setup.sh

# 4. Verify
make dev-ps
```

### Workflow 2: Quick Restart (No Code Changes)

```bash
# 1. Stop services
make dev-down

# 2. Start services (includes auto-config)
make dev-up

# Data persists in volumes - no need to repopulate
```

### Workflow 3: Complete Reset (Nuclear Option)

```bash
# 1. Delete everything
make reset

# 2. Rebuild from scratch (includes auto-config)
make dev-up-build

# 3. Populate test data
bash infrastructure/db/master-data-setup.sh
```

### Workflow 4: Database-Only Reset

```bash
# Reset only the database (faster than full reset)
make db-reset

# Wait 30 seconds for DB to initialize and run init scripts

# Populate test data
bash infrastructure/db/master-data-setup.sh
```

### Workflow 5: Reprovision Superset Dashboards

```bash
# If dashboards are broken or you want to start fresh
make superset-reprovision

# Or just update existing dashboards
make superset-provision
```

---

## Testing the New Vehicle Status Feature

After rebuild, test the status fix:

### 1. Check Backend API

```bash
# Test the new endpoint
curl -H "X-User-ICAO: VIDP" http://localhost:8080/api/vehicles/DEL-FT-05/asset-status
```

**Expected response**:
```json
{
  "status": "In Use",
  "assetId": "DEL-FT-05",
  "name": "Fuel Truck - DEL-FT-05",
  "category": "Fueling"
}
```

### 2. Test Map Popup

1. Open http://localhost:3000
2. Login as `admin_vidp` / `admin`
3. Navigate to Tracking page
4. Click on vehicle DEL-FT-05
5. Verify popup shows **"In Use"** (database status, not "idle")

### 3. Verify Consistency

1. Note status in map popup
2. Navigate to Assets page
3. Search for same vehicle
4. Confirm status matches

---

## Troubleshooting

### Services Won't Start

```bash
# Check logs
make dev-logs

# Check specific service
docker-compose -f docker-compose.dev.yml logs backend
docker-compose -f docker-compose.dev.yml logs timescaledb
```

### Database Connection Issues

```bash
# Reset database
make db-reset

# Check database is healthy
docker-compose -f docker-compose.dev.yml exec timescaledb pg_isready

# Check if init scripts ran
docker-compose -f docker-compose.dev.yml exec timescaledb psql -U postgres -d utam -c "SELECT code, name FROM tenants;"
```

**Expected output**:
```
 code |               name                
------+-----------------------------------
 VIDP | Indira Gandhi International Airport
 LIRN | Naples International Airport
 YBBN | Brisbane International Airport
```

### NiFi Not Configured

```bash
# Manually configure NiFi
make setup-nifi

# Verify NiFi is accessible
curl http://localhost:8091/nifi-api/flow/about
```

### Superset Dashboards Missing

```bash
# Check if Superset is healthy
curl http://localhost:8089/health

# Manually provision dashboards
make superset-provision

# Or reprovision from scratch
make superset-reprovision

# Check logs
docker-compose -f docker-compose.dev.yml logs superset-bootstrap
```

### Cache Issues

```bash
# Clear Redis cache
docker-compose -f docker-compose.dev.yml exec redis redis-cli FLUSHALL
```

### Port Conflicts

```bash
# Check what's using ports
lsof -i :8080  # Backend
lsof -i :3000  # Frontend
lsof -i :8091  # NiFi
lsof -i :8089  # Superset

# Kill conflicting processes or change ports in docker-compose.dev.yml
```

### Init Scripts Not Running

If database is empty after start:

```bash
# Check if volume exists
docker volume ls | grep timescaledb

# If volume exists from old deployment, remove it
docker volume rm tam_timescaledb_data

# Restart
make dev-up-build
```

### Data Population Errors

#### Error: `vehicle_asset_map` does not exist

**Symptom**:
```
ERROR:  relation "vehicle_asset_map" does not exist
```

**Cause**: The `vehicle_asset_map` table creation script was missing from the init directory.

**Fix**: ✅ **Already fixed!** The script is now at [`infrastructure/db/init/05b-vehicle-asset-mapping.sql`](file:///Users/sujoymukherjee/code/TAM/infrastructure/db/init/05b-vehicle-asset-mapping.sql)

**If you still see this error**:
```bash
# Manually create the table
docker-compose -f docker-compose.dev.yml exec -T timescaledb psql -U postgres -d utam < infrastructure/db/create_vehicle_asset_mapping.sql

# Verify
docker-compose -f docker-compose.dev.yml exec -T timescaledb psql -U postgres -d utam -c "\d vehicle_asset_map"
```

**For future rebuilds**: The table will be created automatically since the script is now in the `init/` directory.

#### Error: `column "oid" does not exist`

**Symptom**:
```
ERROR:  column "oid" does not exist
LINE 2:        pg_size_pretty(pg_relation_size(oid)) as size
```

**Cause**: PostgreSQL 12+ removed the `oid` column from system catalogs.

**Fix**: ✅ **Already fixed!** Updated [`master-data-setup.sh`](file:///Users/sujoymukherjee/code/TAM/infrastructure/db/master-data-setup.sh) to use `pg_total_relation_size()` instead.

**Note**: This error is harmless and doesn't affect data population. It only affects the final summary display.

---

## Service URLs Reference

| Service | URL | Credentials |
|---------|-----|-------------|
| Frontend | http://localhost:3000 | admin_vidp / admin |
| Backend API | http://localhost:8080 | - |
| NiFi | http://localhost:8091 | admin / admin123456789 |
| Redpanda Console | http://localhost:8090 | - |
| Superset | http://localhost:8089 | admin / admin |
| Grafana | http://localhost:3001 | admin / admin |
| Prometheus | http://localhost:9090 | - |
| MinIO Console | http://localhost:9001 | minioadmin / minioadmin |

---

## Data Population Scripts Reference

### Automatic (via Docker)

Located in [`infrastructure/db/init/`](file:///Users/sujoymukherjee/code/TAM/infrastructure/db/init):

| Script | Purpose | When It Runs |
|--------|---------|--------------|
| `01-core-platform.sql` | Creates tenants, users, audit tables | On first DB start |
| `02-aviation-domain.sql` | Creates flights, vehicles tables | On first DB start |
| `03-telemetry-domain.sql` | Creates sensor alerts table | On first DB start |
| `04-turnaround-domain.sql` | Creates turnaround tables | On first DB start |
| `05-asset-management.sql` | Creates assets tables | On first DB start |
| `06-asset-tracking-security.sql` | Creates zones, violations tables | On first DB start |
| `07-heatmap-views.sql` | Creates materialized views | On first DB start |
| `08-admin-zones-paths.sql` | Creates zones, paths tables | On first DB start |
| `09-superset-report-views.sql` | Creates reporting views | On first DB start |
| `10-predictive-tables.sql` | Creates ML/prediction tables | On first DB start |
| `11-demo-seed.sql` | Seeds minimal demo data | On first DB start |

### Manual (via Scripts)

Located in [`infrastructure/db/`](file:///Users/sujoymukherjee/code/TAM/infrastructure/db):

| Script | Purpose |
|--------|---------|
| [`master-data-setup.sh`](file:///Users/sujoymukherjee/code/TAM/infrastructure/db/master-data-setup.sh) | **Main script** - runs all others below |
| `fix_schema_and_add_stands.sql` | Creates stands and zones |
| `populate_report_data.sql` | Adds violations, trails, discrepancies |
| `update_asset_statuses.sql` | Sets asset statuses |
| `fix_turnaround_data.sql` | Creates turnaround sessions |

---

## Complete Rebuild Sequence

Here's the **exact order** of what happens during a complete rebuild:

### 1. Docker Compose Up
```bash
make dev-up-build
```

**Automatic steps**:
1. Build backend & frontend images
2. Start TimescaleDB
3. **TimescaleDB auto-runs init scripts** (01-11)
   - Creates all tables
   - Seeds tenants (VIDP, LIRN, YBBN)
   - Seeds users
   - Creates views
4. Start Redpanda, Redis, MinIO
5. Start Backend (waits for DB)
6. Start Frontend
7. Start NiFi
8. **`dev-up` script configures NiFi flows**
9. Start Superset
10. **Superset sidecar provisions dashboards**

### 2. Manual Data Population
```bash
bash infrastructure/db/master-data-setup.sh
```

**Manual steps**:
1. Add stands and zones
2. Populate violations, trails
3. Set asset statuses
4. Create turnaround sessions
5. Refresh materialized views

### 3. Verification
```bash
make dev-ps
```

---

## Quick Reference

**Most common command after teardown**:
```bash
make dev-up-build && bash infrastructure/db/master-data-setup.sh
```

This will:
1. Rebuild and start all services
2. Auto-create schema and seed core data
3. Auto-configure NiFi
4. Auto-provision Superset
5. Populate test/demo data
6. Run smoke tests

**Estimated time**: 3-5 minutes

---

## Key Takeaways

✅ **Core data (tenants, users) is seeded automatically** via Docker init scripts  
✅ **No separate "airport" or "tenant" population needed** - it's in `01-core-platform.sql`  
✅ **NiFi and Superset are configured automatically** by `dev-up`  
✅ **Only test/demo data needs manual population** via `master-data-setup.sh`  
✅ **Everything is idempotent** - safe to run multiple times
