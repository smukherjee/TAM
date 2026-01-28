# Quickstart Guide: Asset Tracking & Security Module

**Feature**: 005  
**Target Audience**: Developers, QA Engineers  
**Estimated Setup Time**: 30 minutes

---

## Prerequisites

- Docker Desktop 4.25+ installed and running
- Git repository cloned: `/Users/sujoymukherjee/code/TAM`
- Minimum 8GB RAM available for containers
- Ports available: 5432 (PostgreSQL), 9092 (Kafka), 8080/8081 (NiFi), 3000 (Frontend), 8090 (Backend)

---

## Quick Start (5 Minutes)

### 1. Start Infrastructure

```bash
cd /Users/sujoymukherjee/code/TAM

# Start all containers
docker-compose up -d

# Verify services are running
docker-compose ps

# Expected output:
# tam-postgres         Up      0.0.0.0:5432->5432/tcp
# tam-kafka            Up      0.0.0.0:9092->9092/tcp
# tam-nifi             Up      0.0.0.0:8080->8080/tcp, 8081/tcp
# tam-backend          Up      0.0.0.0:8090->8090/tcp
# tam-frontend         Up      0.0.0.0:3000->3000/tcp
```

### 2. Verify Database Schema

```bash
# Connect to PostgreSQL
docker exec -it tam-postgres psql -U postgres -d utam

# Check if tracking tables exist
\dt *zone*
\dt *movement*

# Expected output:
# restricted_zones
# zone_violations
# asset_movement_trail
# movement_discrepancies
# asset_location_register
```

### 3. Seed Test Data

```bash
# Run mock data generators
./simulate_ba249.sh     # British Airways flight BA249 assets
./simulate_ek500.sh     # Emirates flight EK500 assets
./simulate_qf401_ybbn.sh # Qantas flight QF401 at Brisbane

# Verify vehicles are being tracked
docker exec -it tam-postgres psql -U postgres -d utam -c \
  "SELECT COUNT(*) FROM vehicles WHERE updated_at > NOW() - INTERVAL '1 minute';"
```

### 4. Access Application

Open your browser:
- **Frontend**: http://localhost:3000
- **NiFi**: http://localhost:8080/nifi (credentials: admin/ctsBtRBKHRAx69EqUghvvgEvjnaLjFEB)
- **Backend API**: http://localhost:8090/swagger-ui.html

---

## Feature Walkthrough (15 Minutes)

### Step 1: View Restricted Zones

1. Navigate to **http://localhost:3000**
2. Login with test credentials (use existing VIDP user)
3. Go to **Tracking** → **Zone Configuration**
4. Verify 8 restricted zones are displayed:
   - VIDP: Runway Safety, Fuel Storage, Cargo Security, Maintenance Hangar
   - LIRN: Runway Safety, VIP Terminal
   - YBBN: Runway Safety, International Secure

### Step 2: Simulate Zone Violation

```bash
# Update a fuel truck position to enter runway (PROHIBITED zone)
docker exec -it tam-postgres psql -U postgres -d utam <<EOF
UPDATE vehicles 
SET latitude = 28.5561, longitude = 77.1000, updated_at = NOW()
WHERE vehicle_id = 'FUEL-TRUCK-001';
EOF

# Wait 10 seconds for NiFi to poll and process
sleep 10

# Check if violation was detected
docker exec -it tam-postgres psql -U postgres -d utam -c \
  "SELECT violation_id, asset_name, zone_name, severity 
   FROM zone_violations 
   ORDER BY timestamp DESC LIMIT 5;"
```

**Expected Output**:
```
 violation_id    |    asset_name     |        zone_name         | severity
-----------------+-------------------+--------------------------+----------
 VIO-20260128-001| Fuel Truck - 001  | Runway 09/27 Safety Zone | CRITICAL
```

### Step 3: View Violation Report

1. Go to **Tracking** → **Zone Violations Report**
2. Verify **VIO-20260128-001** appears in the table
3. Click on the violation row
4. View details: Entry location, duration, severity
5. Click **Acknowledge** button
6. Add resolution notes: "Driver error - retraining scheduled"
7. Verify violation is marked as acknowledged

### Step 4: View Movement Trail

1. Go to **Tracking** → **Movement Trail**
2. Search for asset: "Fuel Truck - 001"
3. Set date range: Last 24 hours
4. Click **View Trail**
5. Verify map displays:
   - Blue path for normal zones
   - Red path for restricted zone entry
   - Entry/exit markers
   - Dwell time annotations

### Step 5: Trigger Movement Discrepancy

```bash
# Set asset to Maintenance status but move it (UNEXPECTED_MOVEMENT)
docker exec -it tam-postgres psql -U postgres -d utam <<EOF
-- First update asset status to Maintenance
UPDATE assets SET status = 'Maintenance' WHERE qr_id = 'BELT-LOADER-002';

-- Then move the vehicle significantly
UPDATE vehicles 
SET latitude = latitude + 0.001, 
    longitude = longitude + 0.001,
    updated_at = NOW()
WHERE vehicle_id = 'BELT-LOADER-002';
EOF

# Wait for processing
sleep 10

# Check discrepancy detection
docker exec -it tam-postgres psql -U postgres -d utam -c \
  "SELECT discrepancy_id, discrepancy_type, asset_name, severity 
   FROM movement_discrepancies 
   ORDER BY timestamp DESC LIMIT 5;"
```

**Expected Output**:
```
 discrepancy_id  | discrepancy_type     |     asset_name      | severity
-----------------+----------------------+---------------------+----------
 DIS-20260128-001| UNEXPECTED_MOVEMENT  | Belt Loader - 002   | MEDIUM
```

### Step 6: View Universal Airside Map (US5)

1. Go to **Airside Operations** → **Live Asset Map**
2. Verify all assets are displayed with color-coded markers:
   - Red: Emergency vehicles
   - Orange: Fuel trucks
   - Blue: Cargo equipment
   - Green: Ground support
   - Purple: Passenger buses
   - Yellow: Power units
   - Teal: Service vehicles
3. Apply filters:
   - Category: Fueling
   - Status: In Use
4. Verify marker count badge updates: "Showing 3 of 45 assets"
5. Click on a marker to view asset popup
6. Click **View Trail** to navigate to movement trail

### Step 7: View Hotspot Heatmap (US6)

1. Go to **Airside Operations** → **Hotspot Analysis**
2. Select mode: **Activity Density**
3. Select time range: **Last 24 hours**
4. Select grid resolution: **25m**
5. Verify heatmap displays with color gradient (blue → red)
6. Click on a red hotspot cell
7. Verify hotspot detail modal shows:
   - Activity count
   - Unique assets
   - Time distribution chart
8. Click **View Assets** to switch to asset map
9. Export heatmap:
   - Click Export → PNG
   - Verify download: `heatmap_activity_20260128.png`

---

## NiFi Flow Configuration (10 Minutes)

### Verify Asset Position Polling Flow

1. Open **http://localhost:8080/nifi**
2. Login with credentials
3. Navigate to **Asset Tracking Security** process group
4. Verify processors:
   - ✅ **ExecuteSQLRecord**: Queries vehicles table every 5 seconds
   - ✅ **ConvertRecord**: Database rows → JSON
   - ✅ **PublishKafkaRecord**: Sends to asset-positions-json topic
5. Check processor statistics:
   - ExecuteSQLRecord: Should show "FlowFiles In/Out" incrementing every 5 sec
   - PublishKafkaRecord: Should show "Bytes Sent" to Kafka

### Test Flow Manually

1. Right-click **ExecuteSQLRecord** → **Run Once**
2. View data provenance:
   - Right-click processor → **View Data Provenance**
   - Click on latest event → **View Content**
   - Verify JSON format:
```json
{
  "vehicleId": "FUEL-TRUCK-001",
  "latitude": 28.5555,
  "longitude": 77.0900,
  "speed": 12.5,
  "heading": 180,
  "status": "RUNNING",
  "timestamp": "2026-01-28T14:30:00Z"
}
```

---

## Troubleshooting

### Issue: No violations detected

**Symptoms**: Vehicles move into restricted zones but no violations appear in report

**Diagnosis**:
```bash
# Check if NiFi is publishing to Kafka
docker logs tam-nifi | grep "PublishKafka"

# Check if Kafka topic exists
docker exec -it tam-kafka kafka-topics --list --bootstrap-server localhost:9092 | grep asset-positions

# Check if backend consumer is running
docker logs tam-backend | grep "MovementTrailProcessor"

# Check database for recent trail entries
docker exec -it tam-postgres psql -U postgres -d utam -c \
  "SELECT COUNT(*) FROM asset_movement_trail WHERE timestamp > NOW() - INTERVAL '5 minutes';"
```

**Solutions**:
- If topic missing: Create manually with `docker exec tam-kafka kafka-topics --create --topic asset-positions-json`
- If consumer not running: Restart backend with `docker-compose restart tam-backend`
- If no trail entries: Check NiFi flow is started

### Issue: Frontend shows "No assets found"

**Diagnosis**:
```bash
# Check asset_location_register has data
docker exec -it tam-postgres psql -U postgres -d utam -c \
  "SELECT COUNT(*) FROM asset_location_register;"

# Check API endpoint
curl http://localhost:8090/api/tracking/assets/live?tenantCode=VIDP
```

**Solutions**:
- Run mock data generators: `./simulate_ba249.sh`
- Verify asset-vehicle mapping: Check `assets.qr_id` matches `vehicles.vehicle_id`

### Issue: Heatmap shows "No data available"

**Diagnosis**:
```bash
# Check materialized view has data
docker exec -it tam-postgres psql -U postgres -d utam -c \
  "SELECT COUNT(*) FROM asset_activity_heatmap;"

# Refresh materialized view manually
docker exec -it tam-postgres psql -U postgres -d utam -c \
  "REFRESH MATERIALIZED VIEW asset_activity_heatmap;"
```

**Solutions**:
- Wait 6 hours for auto-refresh OR refresh manually
- Ensure movement trail has data for last 30 days

---

## Performance Validation

### Expected Metrics

Run these queries to validate performance meets SLAs:

```sql
-- Violation detection latency (should be <10 seconds)
SELECT 
    AVG(EXTRACT(EPOCH FROM (created_at - timestamp))) AS avg_latency_seconds
FROM zone_violations
WHERE created_at > NOW() - INTERVAL '1 hour';

-- Spatial query performance (should be <100ms)
EXPLAIN ANALYZE
SELECT id, name FROM restricted_zones 
WHERE ST_DWithin(geometry, ST_SetSRID(ST_MakePoint(77.0900, 28.5555), 4326), 50)
AND tenant_code = 'VIDP';

-- Asset map load time (should return <3 seconds)
EXPLAIN ANALYZE
SELECT * FROM asset_location_register WHERE tenant_code = 'VIDP';

-- Heatmap aggregation (should return <5 seconds for 30 days)
EXPLAIN ANALYZE
SELECT * FROM asset_activity_heatmap 
WHERE tenant_code = 'VIDP' 
AND time_bucket > NOW() - INTERVAL '30 days';
```

**Success Criteria**:
- ✅ Violation detection: <10 sec avg latency
- ✅ Spatial queries: <100ms execution time
- ✅ Asset map: <3 sec response time for 500 assets
- ✅ Heatmap: <5 sec aggregation for 30-day data

---

## Next Steps

### For Developers

1. **Review API Contracts**: See `contracts/violations.openapi.yaml`
2. **Implement Phase 2**: Backend domain models (tasks T024-T032)
3. **Run Unit Tests**: `./mvnw test -Dtest=ZoneViolationServiceTest`
4. **Setup IDE**: Import project into IntelliJ/VS Code

### For QA Engineers

1. **Review Test Plan**: See `checklists/acceptance-testing.md`
2. **Execute Acceptance Scenarios**: Test all 3 scenarios in spec.md
3. **Performance Testing**: Use JMeter to simulate 500 concurrent assets
4. **Security Testing**: Verify role-based access control

### For Product Owners

1. **Demo Walkthrough**: Follow Steps 1-7 above for stakeholder demo
2. **Review Reports**: Export sample violation/discrepancy reports as PDF
3. **Validate Business Rules**: Confirm zone authorization logic matches requirements

---

## Cleanup

```bash
# Stop all containers
docker-compose down

# Remove volumes (WARNING: deletes all data)
docker-compose down -v

# Remove mock data
docker exec -it tam-postgres psql -U postgres -d utam -c \
  "TRUNCATE asset_movement_trail, zone_violations, movement_discrepancies CASCADE;"
```

---

## Support

- **Documentation**: `specs/005-asset-tracking-security/`
- **Architecture**: `plan.md`
- **Task Tracking**: `tasks.md`
- **Issues**: Create GitHub issue with label `feature-005`

**Status**: ✅ Quickstart guide complete  
**Last Updated**: 2026-01-28
