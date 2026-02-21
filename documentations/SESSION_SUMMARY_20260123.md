# TAM Development Session Summary
**Date:** January 23, 2026 (7:00 AM IST onwards)  
**Branch:** feature/ux-improvements (branched from feature/add-ybbn-tenant)

---

## 🎯 Overview
This session focused on fixing critical UX issues with the map interface, resolving backend data pipeline issues, and ensuring proper architectural patterns for data ingestion through NiFi.

---

## 📋 Actions Taken

### 1. **Map UI/UX Professional Overhaul**
**Problem:** Map icons (emoji-based) looked unprofessional, double popups appearing, and overall UI lacked polish.

**Solutions Implemented:**

#### A. Professional SVG Icons Created
- **File:** `frontend/src/components/MapIcons.tsx` (NEW)
  - Aircraft icon with rotation based on heading (0-360°)
  - Vehicle type-specific icons: bus, fuel truck, tug, belt loader, catering
  - Status-based coloring: blue (active/airborne), red (alert), amber (warning), gray (idle/landed), green (taxiing)
  - Reusable icon generation functions

- **File:** `frontend/src/components/MapIcons.css` (NEW)
  - Pulse animation for alerts
  - Glassmorphism effects
  - Dark theme styling
  - Smooth transitions and hover effects

#### B. Enhanced Map Components
- **File:** `frontend/src/components/InfoCards.tsx` (NEW)
  - `FlightInfoCard`: Shows callsign, altitude, speed, heading, status
  - `VehicleInfoCard`: Shows vehicle type, speed, status, zone
  - Glassmorphism design with dark theme

- **File:** `frontend/src/components/EnhancedAlertList.tsx` (NEW)
  - Severity-grouped alerts (critical/high/medium/low)
  - Collapsible sections
  - Bounce-in animations
  - Alert count badges

- **File:** `frontend/src/components/MapControlPanel.tsx` (NEW)
  - Layer toggles for flights, vehicles, alerts
  - Glassmorphism panel design
  - Icon-based controls

#### C. Flight Layer Major Rewrite
- **File:** `frontend/src/components/Map/FlightLayer.tsx` (MODIFIED)
  - **Removed:** Leaflet Popup component (was causing double popups)
  - **Added:** Hover-based info cards using mouseover/mouseout events
  - **Fixed:** Flexible property handling for WebSocket messages (handles both `CallSign` and `callsign`)
  - **Added:** Stale flight pruning (removes flights older than 2 minutes)
  - Uses professional aircraft SVG icons with rotation

#### D. Vehicle Layer Updates
- **File:** `frontend/src/components/Map/VehicleLayer.tsx` (MODIFIED)
  - Integrated professional SVG vehicle icons
  - Added VehicleInfoCard on hover
  - Status-based icon colors

#### E. Map Page Integration
- **File:** `frontend/src/pages/MapPage.tsx` (MODIFIED)
  - Integrated EnhancedAlertList
  - Integrated MapControlPanel
  - Layer state management (show/hide flights, vehicles, alerts)
  - Professional dark theme layout

---

### 2. **Backend Data Pipeline Fix (NiFi Downgrade)**
**Problem:** After UX improvements, flights stopped appearing. Investigation revealed NiFi 1.25.0 ListenHTTP processors were returning 503 errors, preventing MockAdsbGenerators from sending data.

**Root Cause:** NiFi 1.25.0 has known issues with ListenHTTP processor not binding to ports properly.

**Solution Implemented:**

#### A. NiFi Version Downgrade
- **File:** `docker-compose.dev.yml` (MODIFIED)
  - Changed: `image: apache/nifi:1.25.0` → `image: apache/nifi:1.18.0`
  - NiFi 1.18.0 is a stable LTS version with reliable ListenHTTP functionality

#### B. Verification Steps Completed
1. ✅ NiFi 1.18.0 container running
2. ✅ ListenHTTP ports bound correctly (8092, 8093, 8094)
3. ✅ HTTP POST test returns 200 (not 503)
4. ✅ MockAdsbGenerators successfully sending data to NiFi
5. ✅ Kafka receiving messages (verified via FlightConsumer logs)
6. ✅ Database persistence working (10,554+ flights in TimescaleDB)
7. ✅ WebSocket broadcasting active
8. ✅ REST API returning flights correctly

#### C. Temporary Workaround Removed
- **File:** `backend/src/main/java/com/utam/controller/FlightTestController.java` (DELETED)
  - This was a temporary controller that bypassed NiFi for testing
  - No longer needed now that NiFi pipeline is working

---

### 3. **WebSocket Service Updates**
- **File:** `frontend/src/services/WebSocketService.ts` (MODIFIED)
  - Improved connection handling
  - Better error recovery
  - Maintains STOMP subscriptions across reconnections

---

## 🏗️ Architecture Pattern Maintained

The proper architectural pattern is now fully functional:

```
Data Sources (MockAdsbGenerators)
    ↓ HTTP POST
NiFi ListenHTTP (ports 8092-8094)
    ↓ Process & Validate
Kafka Topics (flight-raw-json, vehicle-raw-json, cv-event-raw-json)
    ↓ Consume
Backend Spring Boot (FlightConsumer, VehicleConsumer)
    ↓ Split
    ├─→ TimescaleDB (Persistence)
    └─→ WebSocket Broadcast (/topic/flights/{icao})
         ↓
Frontend (React)
    ├─→ REST API Load (on page load/refresh)
    └─→ WebSocket Updates (real-time)
```

**Key Benefits:**
- ✅ Data persists to database (survives refresh)
- ✅ NiFi provides data validation layer
- ✅ Kafka provides reliable message bus
- ✅ WebSocket provides real-time updates
- ✅ No shortcuts or architectural compromises

---

## 📦 Files Modified/Created

### Frontend (11 files)
```
MODIFIED:
- frontend/src/components/Map/FlightLayer.tsx
- frontend/src/components/Map/VehicleLayer.tsx
- frontend/src/pages/MapPage.tsx
- frontend/src/services/WebSocketService.ts

NEW:
- frontend/src/components/MapIcons.tsx
- frontend/src/components/MapIcons.css
- frontend/src/components/InfoCards.tsx
- frontend/src/components/EnhancedAlertList.tsx
- frontend/src/components/MapControlPanel.tsx
```

### Backend (1 file)
```
DELETED:
- backend/src/main/java/com/utam/controller/FlightTestController.java
```

### Infrastructure (2 files)
```
MODIFIED:
- docker-compose.dev.yml
- infrastructure/nifi/setup-nifi.sh
```

---

## 🔄 Persistence & Reproducibility

### ✅ What IS Persisted (Survives Container Restart)

#### Database Schema & Data
- **Location:** Docker volume `tam_timescaledb_data`
- **Init Scripts:** `infrastructure/db/init/*.sql` (run on first start)
  - `01-core-platform.sql` - Tenants, users, roles
  - `02-aviation-domain.sql` - Flights, vehicles tables
  - `03-telemetry-domain.sql` - Telemetry tables
  - `04-turnaround-domain.sql` - Turnaround operations
  - `05-asset-management.sql` - Assets, kits, categories, tags, locations
- **Persistence:** All data persists via volume mount

#### NiFi Configuration
- **Volumes:**
  - `tam_nifi_data` - Flow files and state
  - `tam_nifi_conf` - Configuration files
  - `tam_nifi_logs` - Logs
  - `tam_nifi_provenance` - Provenance data
- **Note:** NiFi flows need to be set up via `infrastructure/nifi/setup-nifi.sh` after first start

#### Kafka/Redpanda Data
- **Volume:** `tam_redpanda_data`
- **Topics:** Created automatically by backend on first message

#### MinIO Buckets
- **Volume:** `tam_minio_data`
- **Buckets:** Created by `minio-init` service:
  - `tam-raw-data`
  - `tam-processed-data`
  - `tam-reports`

#### Redis Cache
- **Volume:** `tam_redis_data`
- **Data:** In-memory cache with RDB persistence

### ✅ What IS Auto-Generated (No Manual Setup Needed)

#### Mock Data Generators
- **Location:** `backend/src/main/java/com/utam/simulation/`
- **Files:**
  - `MockAdsbGenerator.java` (VIDP - Delhi)
  - `MockAdsbGenerator_LIRN.java` (LIRN - Naples)
  - `MockAdsbGenerator_YBBN.java` (YBBN - Brisbane)
  - `MockTelitGenerator.java` (VIDP)
  - `MockTelitGenerator_YBBN.java` (YBBN)
  - `MockCvEventGenerator_YBBN.java` (YBBN)
- **Behavior:** Start automatically when backend starts
- **Data Flow:** Generate data → POST to NiFi HTTP endpoints → Kafka → DB

#### Backend API Endpoints
- **Controllers:** Auto-registered by Spring Boot
- **Endpoints:**
  - `/api/flights` - Flight data (with X-User-ICAO header)
  - `/api/vehicles` - Vehicle tracking
  - `/api/assets` - Asset management
  - `/api/kits` - Kit management (stub)
  - `/api/categories` - Categories (stub)
  - `/api/tags` - Tags (stub)
  - `/api/locations` - Locations (stub)

#### Kafka Topics
- **Auto-created by:**
  - `flight-raw-json` - FlightConsumer
  - `vehicle-raw-json` - VehicleConsumer
  - `cv-event-raw-json` - CVEventConsumer

### ⚠️ What NEEDS Manual Setup (After Fresh Start)

#### NiFi Flows
- **Required:** Run `infrastructure/nifi/setup-nifi.sh` after NiFi starts
- **What it does:**
  - Creates ListenHTTP processors for ports 8092, 8093, 8094
  - Creates PublishKafka processors for topics
  - Creates HttpContextMap controller service
  - Connects all processors
  - Starts all flows
- **When:** Only needed on first NiFi start or after volume deletion

---

## 🧪 Testing & Verification Commands

### Check Services Running
```bash
docker-compose -f docker-compose.dev.yml ps
```

### Check NiFi ListenHTTP Ports
```bash
netstat -an | grep -E "8092|8093|8094"
# Should show: tcp46 *.8092 LISTEN (and 8093, 8094)
```

### Test NiFi HTTP Endpoint
```bash
curl -X POST http://localhost:8092/adsb-ingest \
  -H "Content-Type: application/json" \
  -d '[{"test":"data"}]' \
  -w "\nHTTP Status: %{http_code}\n"
# Should return: HTTP Status: 200
```

### Check Flight Data in Database
```bash
docker exec tam-timescaledb-1 psql -U postgres -d utam -c \
  "SELECT COUNT(*) as flight_count, tenant_code FROM flights GROUP BY tenant_code;"
# Should show counts for YBBN, LIRN, VIDP
```

### Check Recent Flights
```bash
docker exec tam-timescaledb-1 psql -U postgres -d utam -c \
  "SELECT tenant_code, callsign, flight_number, status, \
   TO_CHAR(timestamp, 'HH24:MI:SS') as time \
   FROM flights \
   WHERE timestamp > NOW() - INTERVAL '30 seconds' \
   ORDER BY timestamp DESC LIMIT 10;"
```

### Check Backend Logs
```bash
docker logs tam-backend-1 --tail 50 | grep -E "FlightConsumer|MockAdsb"
# Should see: "Received N flights from Kafka" and "Generated flight data"
```

### Test REST API
```bash
curl -H "X-User-ICAO: YBBN" http://localhost:8080/api/flights | jq '.data | length'
# Should return: number of active flights (typically 10)
```

### Check Kafka Topics
```bash
# Open Redpanda Console: http://localhost:8090
# Check topics: flight-raw-json, vehicle-raw-json, cv-event-raw-json
```

---

## 🚀 Fresh Start Procedure

If you bring down containers and start fresh:

### 1. Stop and Remove Containers
```bash
docker-compose -f docker-compose.dev.yml down
```

### 2. (Optional) Remove Volumes for Clean State
```bash
# WARNING: This deletes ALL data
docker volume rm tam_timescaledb_data tam_nifi_data tam_nifi_conf \
  tam_redpanda_data tam_minio_data tam_redis_data
```

### 3. Start All Services
```bash
docker-compose -f docker-compose.dev.yml up -d
```

### 4. Wait for Services to be Healthy
```bash
# Check status (wait until all healthy)
docker-compose -f docker-compose.dev.yml ps
```

### 5. Setup NiFi Flows (REQUIRED if volumes were removed)
```bash
cd infrastructure/nifi
./setup-nifi.sh
```

### 6. Verify Data Flow
```bash
# Wait 30 seconds for mock generators to run
sleep 30

# Check database
docker exec tam-timescaledb-1 psql -U postgres -d utam -c \
  "SELECT COUNT(*) FROM flights;"
# Should show increasing count
```

### 7. Access Frontend
```bash
# Open browser: http://localhost:3000
# Login: admin_ybbn / admin123
# Navigate to Map page
# Should see flights, vehicles, alerts
```

---

## 📊 Current State

### Database Stats (as of session end)
```
YBBN (Brisbane): 4,990 flights
LIRN (Naples):   3,609 flights
VIDP (Delhi):    1,955 flights
Total:          10,554 flights
```

### Docker Volumes
```
tam_timescaledb_data      - 10.5K+ flight records
tam_nifi_data             - Flow files and state
tam_nifi_conf             - NiFi 1.18.0 configuration
tam_redpanda_data         - Kafka topics and messages
tam_minio_data            - Raw data archives
tam_redis_data            - Cached flight/vehicle data
```

### Active Services
```
✅ TimescaleDB     - port 5432
✅ Redpanda        - port 9092 (Kafka API)
✅ Redpanda Console- port 8090
✅ NiFi 1.18.0     - port 8091 (UI), 8092-8094 (HTTP)
✅ Backend         - port 8080
✅ Frontend        - port 3000
✅ Redis           - port 6379
✅ MinIO           - port 9000 (API), 9001 (Console)
```

---

## 🔧 Known Issues & Limitations

### Resolved Issues
- ✅ NiFi 1.25.0 ListenHTTP 503 errors (fixed by downgrade to 1.18.0)
- ✅ Double popups on map (removed Leaflet Popup, using hover cards)
- ✅ Flights disappearing on refresh (data now persists to DB)
- ✅ Unprofessional emoji icons (replaced with SVG icons)

### Current Limitations
- ⚠️ NiFi flows must be set up manually after fresh start
- ⚠️ Asset management endpoints are stubs (return empty arrays)
- ⚠️ No authentication beyond basic login (tokens not enforced)
- ⚠️ Mock generators run continuously (no start/stop control)

---

## 📝 Git Status

### Committed (in feature/add-ybbn-tenant branch)
```
✅ Asset management controllers (Kit, Category, Tag, Location)
✅ MockAdsbGenerator_YBBN.java
✅ Frontend asset management pages
✅ Basic YBBN tenant support
```

### Uncommitted Changes (need to commit to feature/ux-improvements)
```
📝 docker-compose.dev.yml (NiFi 1.18.0)
📝 MapIcons.tsx, MapIcons.css (NEW)
📝 InfoCards.tsx (NEW)
📝 EnhancedAlertList.tsx (NEW)
📝 MapControlPanel.tsx (NEW)
📝 FlightLayer.tsx (major rewrite)
📝 VehicleLayer.tsx (icon updates)
📝 MapPage.tsx (component integration)
📝 WebSocketService.ts (improvements)
📝 infrastructure/nifi/setup-nifi.sh (updates)
```

### Suggested Commit
```bash
git add -A
git commit -m "feat: Professional map UX with SVG icons and NiFi 1.18.0 downgrade

- Replace emoji icons with professional SVG aircraft/vehicle icons
- Add glassmorphism UI components (InfoCards, EnhancedAlertList, MapControlPanel)
- Fix double popup issue with hover-based info cards
- Rewrite FlightLayer with flexible WebSocket property handling
- Downgrade NiFi from 1.25.0 to 1.18.0 to fix ListenHTTP 503 errors
- Remove temporary FlightTestController workaround
- Verify complete data pipeline: NiFi → Kafka → DB → WebSocket → Frontend
- Add stale flight pruning (2-minute timeout)
- Implement layer toggles for flights/vehicles/alerts

Technical:
- NiFi 1.18.0 LTS with reliable ListenHTTP
- 10,554+ flights persisted to TimescaleDB
- WebSocket + REST API dual data loading
- Dark theme with animations and transitions"
```

---

## 🎯 Next Steps (Recommended)

1. **Commit UX Changes**
   - Commit all map UX improvements to feature/ux-improvements branch
   - Create PR to merge into main

2. **NiFi Flow Persistence**
   - Document NiFi flow setup in setup-nifi.sh
   - Consider NiFi flow.json.gz export for version control

3. **Asset Management Implementation**
   - Implement real repository/service for Kits, Categories, Tags, Locations
   - Create database tables in `05-asset-management.sql`
   - Add CRUD operations

4. **Authentication Hardening**
   - Enforce JWT tokens on all endpoints
   - Add role-based access control

5. **Mock Generator Controls**
   - Add start/stop endpoints for mock generators
   - Add rate control (messages per second)

---

## 📚 Documentation References

- **Architecture:** `documentations/architecture_dev_20251220.mermaid`
- **Database Schema:** `infrastructure/db/init/*.sql`
- **Setup Guide:** `SETUP_GUIDE.md`
- **NiFi Setup:** `infrastructure/nifi/setup-nifi.sh`
- **Docker Compose:** `docker-compose.dev.yml`

---

**Session Duration:** ~4 hours  
**Primary Focus:** Map UX overhaul + NiFi data pipeline fix  
**Result:** ✅ Professional map UI + Fully functional data architecture
