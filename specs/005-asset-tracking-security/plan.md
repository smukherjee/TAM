# Implementation Plan: Asset Tracking & Security Module

**Feature ID**: 005  
**Created**: 2026-01-28  
**Estimated Duration**: 18-24 days  
**Team Size**: 1 Full-Stack Developer

---

## Architecture Overview

### System Components

```
┌─────────────────────────────────────────────────────────────┐
│                     Frontend (React/TypeScript)              │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────────────┐  ┌──────────────────┐  ┌────────────┐ │
│  │ Zone Violations  │  │   Discrepancy    │  │  Movement  │ │
│  │  Report Page     │  │   Report Page    │  │ Trail Page │ │
│  └──────────────────┘  └──────────────────┘  └────────────┘ │
│           │                     │                   │        │
│           └─────────────────────┼───────────────────┘        │
│                                 ↓                            │
│  ┌──────────────────────────────────────────────────────┐   │
│  │          Tracking Service (trackingService.ts)       │   │
│  │    + WebSocket Integration (WebSocketService.ts)     │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                               ↕ HTTP/WebSocket
┌─────────────────────────────────────────────────────────────┐
│                Backend (Spring Boot / Java 21)              │
├─────────────────────────────────────────────────────────────┤
│  REST Controllers                                            │
│  ┌──────────────────┐  ┌──────────────────┐  ┌────────────┐ │
│  │ ZoneViolation    │  │ Discrepancy      │  │ Movement   │ │
│  │ Controller       │  │ Controller       │  │ Controller │ │
│  └──────────────────┘  └──────────────────┘  └────────────┘ │
│           ↓                     ↓                   ↓        │
│  ┌──────────────────────────────────────────────────────┐   │
│  │                  Service Layer                       │   │
│  │  ┌──────────────┐  ┌───────────────┐  ┌───────────┐ │   │
│  │  │ Zone         │  │ Discrepancy   │  │ Movement  │ │   │
│  │  │ Service      │  │ Service       │  │ Service   │ │   │
│  │  └──────────────┘  └───────────────┘  └───────────┘ │   │
│  │                                                      │   │
│  │  ┌──────────────────────────────────────────────┐   │   │
│  │  │  MovementTrailIngestionService (Scheduled)   │   │   │
│  │  │  - Reads from vehicles table every 5 sec     │   │   │
│  │  │  - Detects zone violations                   │   │   │
│  │  │  - Detects movement discrepancies            │   │   │
│  │  │  - Writes to asset_movement_trail            │   │   │
│  │  └──────────────────────────────────────────────┘   │   │
│  └──────────────────────────────────────────────────────┘   │
│           ↓                                                  │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              Repository Layer (JPA)                  │   │
│  │  - RestrictedZoneRepository                          │   │
│  │  - AssetMovementTrailRepository                      │   │
│  │  - ZoneViolationRepository                           │   │
│  │  - MovementDiscrepancyRepository                     │   │
│  │  - AssetLocationRegisterRepository                   │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                               ↕ JDBC
┌─────────────────────────────────────────────────────────────┐
│         PostgreSQL 16 + TimescaleDB + PostGIS               │
├─────────────────────────────────────────────────────────────┤
│  Tables:                                                     │
│  - restricted_zones (zone definitions)                       │
│  - asset_movement_trail (hypertable, time-series)           │
│  - zone_violations (hypertable, time-series)                │
│  - movement_discrepancies (hypertable, time-series)         │
│  - asset_location_register (current state snapshot)         │
│                                                              │
│  Continuous Aggregates:                                      │
│  - zone_violations_hourly                                    │
│  - movement_discrepancies_daily                              │
│                                                              │
│  Spatial Functions:                                          │
│  - ST_Contains (point in polygon)                           │
│  - ST_Distance (deviation calculation)                       │
└─────────────────────────────────────────────────────────────┘
```

---

## Data Flow

### 1. Movement Trail Ingestion (Real-time)

```
┌──────────────┐      5 sec      ┌────────────────────────┐
│   vehicles   │ ←─────────────  │ MovementTrail          │
│   table      │                 │ IngestionService       │
│ (live data)  │                 │ (@Scheduled)           │
└──────────────┘                 └────────────────────────┘
                                           │
                                           ↓
                          ┌────────────────────────────────┐
                          │ For each vehicle position:     │
                          │ 1. Match vehicle_id → asset    │
                          │ 2. Check zone (ST_Contains)    │
                          │ 3. Detect violations           │
                          │ 4. Detect discrepancies        │
                          │ 5. Write to movement_trail     │
                          │ 6. Broadcast WebSocket event   │
                          └────────────────────────────────┘
                                           │
                    ┌──────────────────────┼──────────────────────┐
                    ↓                      ↓                      ↓
        ┌───────────────────┐  ┌───────────────────┐  ┌─────────────────┐
        │ asset_movement    │  │ zone_violations   │  │ movement_       │
        │ _trail            │  │ (if unauthorized) │  │ discrepancies   │
        │ (always)          │  └───────────────────┘  │ (if anomaly)    │
        └───────────────────┘                         └─────────────────┘
```

### 2. Zone Violation Detection

```
Asset Position → ST_Contains(zone.geometry, asset.location)
                             │
                ┌────────────┴───────────┐
                │ In Restricted Zone?    │
                └────────────┬───────────┘
                             │ YES
                             ↓
                ┌────────────────────────────────┐
                │ Check Authorization:           │
                │ - Zone type PROHIBITED?        │
                │   → VIOLATION (CRITICAL)       │
                │ - Asset category in allowed?   │
                │   → AUTHORIZED                 │
                │ - Asset ID in exceptions?      │
                │   → AUTHORIZED                 │
                │ - Else → VIOLATION             │
                └────────────────────────────────┘
                             │
                             ↓
                ┌────────────────────────────────┐
                │ Create zone_violations record  │
                │ - Calculate severity           │
                │ - Record entry time/location   │
                │ - Broadcast WebSocket event    │
                └────────────────────────────────┘
```

### 3. Discrepancy Detection

```
Asset Movement → Compare against expected behavior
                             │
        ┌────────────────────┼────────────────────┐
        │                    │                    │
        ↓                    ↓                    ↓
┌──────────────┐  ┌──────────────────┐  ┌──────────────────┐
│ Status Check │  │ Location Check   │  │ Speed Check      │
│ In Use?      │  │ vs Register      │  │ vs Category Max  │
│ Maintenance? │  │ Distance > 100m? │  │ Exceeded?        │
└──────────────┘  └──────────────────┘  └──────────────────┘
        │                    │                    │
        └────────────────────┼────────────────────┘
                             ↓
                 ┌───────────────────────────┐
                 │ Create discrepancy record │
                 │ - Calculate deviation     │
                 │ - Assign severity         │
                 │ - Broadcast event         │
                 └───────────────────────────┘
```

---

## Database Schema Strategy

### Vehicle ↔ Asset Mapping

**Problem**: `vehicles.vehicle_id` (VARCHAR) vs `assets.id` (UUID)

**Solution**: Use QR code as common identifier
- Match `vehicles.vehicle_id` to `assets.qr_id`
- QR codes are unique identifiers printed on physical assets
- If no match found: Log as unmatched, allow manual mapping later

**Implementation**:
```sql
-- Add mapping lookup function
CREATE OR REPLACE FUNCTION get_asset_id_from_vehicle(vehicle_identifier VARCHAR)
RETURNS UUID AS $$
DECLARE
    asset_uuid UUID;
BEGIN
    SELECT id INTO asset_uuid
    FROM assets
    WHERE qr_id = vehicle_identifier OR asset_id = vehicle_identifier;
    
    RETURN asset_uuid;
END;
$$ LANGUAGE plpgsql;
```

### Spatial Indexing

All geographic queries use GIST indexes:
```sql
CREATE INDEX idx_restricted_zones_geom ON restricted_zones USING GIST (geometry);
CREATE INDEX idx_movement_trail_location ON asset_movement_trail USING GIST (location);
```

Query pattern:
```sql
-- Check if position is in any restricted zone
SELECT z.id, z.zone_name, z.zone_type, z.authorized_asset_categories
FROM restricted_zones z
WHERE z.tenant_code = $1
  AND z.is_active = TRUE
  AND ST_Contains(z.geometry, ST_SetSRID(ST_MakePoint($longitude, $latitude), 4326));
```

---

## API Contracts

### REST Endpoints

#### 1. GET /api/tracking/zones/violations

**Query Parameters:**
- `tenantCode` (required)
- `startDate` (ISO 8601, default: now - 24h)
- `endDate` (ISO 8601, default: now)
- `zoneType` (optional: PROHIBITED, RESTRICTED, CONTROLLED, MAINTENANCE)
- `assetCategory` (optional)
- `severity` (optional: CRITICAL, HIGH, MEDIUM, LOW)
- `acknowledged` (optional: true, false)
- `page` (default: 0)
- `size` (default: 50)
- `sort` (default: timestamp,desc)

**Response:**
```json
{
  "content": [
    {
      "id": "uuid",
      "violationId": "VIO-20260128-001",
      "assetId": "uuid",
      "assetIdentifier": "SAM-0005",
      "assetName": "Fuel Truck - 001",
      "assetCategory": "Fueling",
      "restrictedZoneId": "uuid",
      "zoneName": "Runway 09/27 Safety Zone",
      "zoneType": "PROHIBITED",
      "violationType": "UNAUTHORIZED_ENTRY",
      "entryLatitude": 28.5555,
      "entryLongitude": 77.0900,
      "durationSeconds": 120,
      "severity": "CRITICAL",
      "acknowledged": false,
      "acknowledgedBy": null,
      "acknowledgedAt": null,
      "resolutionNotes": null,
      "timestamp": "2026-01-28T14:30:00Z"
    }
  ],
  "totalElements": 15,
  "totalPages": 1,
  "size": 50,
  "number": 0
}
```

#### 2. POST /api/tracking/zones/violations/{id}/acknowledge

**Request Body:**
```json
{
  "resolutionNotes": "Driver error - retraining scheduled for 2026-01-30",
  "acknowledgedBy": "user-uuid"
}
```

**Response:** 200 OK with updated violation object

#### 3. GET /api/tracking/discrepancies

**Query Parameters:** (Same as violations)

**Response:**
```json
{
  "content": [
    {
      "id": "uuid",
      "discrepancyId": "DISC-20260128-005",
      "assetId": "uuid",
      "assetIdentifier": "BNE-0002",
      "assetName": "Pushback Tractor Brisbane",
      "assetCategory": "Ground Support",
      "discrepancyType": "LOCATION_MISMATCH",
      "expectedLocation": "Terminal 2",
      "actualLocation": "Cargo Area",
      "expectedLatitude": -27.3950,
      "expectedLongitude": 153.1150,
      "actualLatitude": -27.3920,
      "actualLongitude": 153.1180,
      "distanceDeviationMeters": 450.5,
      "description": "Asset register shows Terminal 2 but GPS shows Cargo Area",
      "severity": "MEDIUM",
      "acknowledged": false,
      "timestamp": "2026-01-28T14:45:00Z"
    }
  ],
  "totalElements": 8,
  "totalPages": 1
}
```

#### 4. GET /api/tracking/trail/{assetId}

**Query Parameters:**
- `startDate` (ISO 8601, required)
- `endDate` (ISO 8601, required, max 30 days from start)

**Response:**
```json
{
  "assetId": "uuid",
  "assetIdentifier": "SAM-0002",
  "assetName": "Pushback Tractors",
  "startDate": "2026-01-28T00:00:00Z",
  "endDate": "2026-01-28T23:59:59Z",
  "trailPoints": [
    {
      "timestamp": "2026-01-28T06:00:00Z",
      "latitude": 28.5560,
      "longitude": 77.0850,
      "speed": 0,
      "heading": 90,
      "zone": "Terminal 1 Apron",
      "restrictedZoneId": null,
      "status": "In Use"
    },
    {
      "timestamp": "2026-01-28T06:05:00Z",
      "latitude": 28.5565,
      "longitude": 77.0855,
      "speed": 12.5,
      "heading": 95,
      "zone": "Taxiway Alpha",
      "restrictedZoneId": null,
      "status": "In Use"
    }
  ],
  "zoneEntries": [
    {
      "zoneName": "Maintenance Hangar Area",
      "zoneType": "RESTRICTED",
      "entryTime": "2026-01-28T08:30:00Z",
      "exitTime": "2026-01-28T10:15:00Z",
      "dwellSeconds": 6300,
      "authorized": true
    }
  ],
  "summary": {
    "totalPoints": 288,
    "totalDistanceKm": 25.3,
    "avgSpeed": 8.7,
    "maxSpeed": 18.2,
    "zoneViolationCount": 0,
    "restrictedZoneTimeSeconds": 6300
  }
}
```

#### 5. GET /api/tracking/zones

**Query Parameters:**
- `tenantCode` (required)

**Response:**
```json
{
  "zones": [
    {
      "id": "uuid",
      "zoneId": "VIDP-RZ-001",
      "zoneName": "Runway 09/27 Safety Zone",
      "zoneType": "PROHIBITED",
      "description": "Active runway area",
      "authorizedAssetCategories": [],
      "isActive": true,
      "geometry": {
        "type": "Polygon",
        "coordinates": [...]
      }
    }
  ]
}
```

### WebSocket Events

**Connection**: `ws://localhost:8080/ws/tracking`

**Subscribe**: `/topic/violations/{tenantCode}`  
**Subscribe**: `/topic/discrepancies/{tenantCode}`  
**Subscribe**: `/topic/trail/{assetId}`

**Event Format**:
```json
{
  "eventType": "ZONE_VIOLATION",
  "tenantCode": "VIDP",
  "severity": "CRITICAL",
  "timestamp": "2026-01-28T14:30:00Z",
  "data": {
    "violationId": "VIO-20260128-001",
    "assetName": "Fuel Truck - 001",
    "zoneName": "Runway 09/27 Safety Zone",
    "zoneType": "PROHIBITED"
  }
}
```

---

## Technology Stack

### Backend
- **Java 21** with Spring Boot 3.2
- **Spring Data JPA** for database access
- **Spring WebSocket** for real-time events
- **Hibernate Spatial** for PostGIS integration
- **JTS (Java Topology Suite)** for geometric calculations
- **Lombok** for boilerplate reduction
- **MapStruct** for DTO mapping

### Frontend
- **React 18** with TypeScript
- **React Router** for navigation
- **Leaflet** for map visualization
- **TanStack Query (React Query)** for data fetching
- **SockJS + STOMP** for WebSocket
- **Recharts** for analytics charts
- **Tailwind CSS** for styling

### Database
- **PostgreSQL 16**
- **TimescaleDB 2.13** for time-series data
- **PostGIS 3.4** for spatial queries

---

## Implementation Phases

### Phase 0: Foundation (2 days)

**Tasks:**
- [x] Create spec.md
- [x] Create plan.md (this document)
- [x] Create tasks.md
- [ ] Create data-model.md
- [x] Set up git branch (005-asset-tracking-security)

---

### Phase 1: Database Layer (3 days) ✅ COMPLETE

**Tasks:**
- [x] Apply SQL migration (06-asset-tracking-security.sql)
- [x] Verify PostGIS extension installed
- [x] Seed restricted zones for all tenants
- [x] Create spatial indexes
- [x] Test zone containment queries
- [x] Create continuous aggregates
- [x] Test data retention policies

**Acceptance:**
- ✅ All tables created successfully
- ✅ Spatial queries return correct results (<100ms)
- ✅ TimescaleDB hypertables configured
- ✅ 8 restricted zones seeded for VIDP, LIRN, YBBN

---

### Phase 2A: Demo Flow Enhancements - Universal Map & Heatmap (3-4 days)

**Priority**: HIGH - Required for complete Demo Flow coverage (100%)

**Objective**: Implement US5 (Universal Airside Map) and US6 (Hotspot Analysis) to address Demo Flow gaps identified in requirements analysis.

**New API Endpoints:**

#### Universal Asset Map (US5)
```http
GET /api/tracking/assets/live?tenantCode={code}&category={cat}&status={status}&zone={zone}
```
Returns all current asset positions from `asset_location_register`.

**Response:**
```json
{
  "assets": [
    {
      "assetId": "uuid",
      "assetIdentifier": "SAM-0005",
      "name": "Fuel Truck - 001",
      "category": "Fueling",
      "latitude": 28.5555,
      "longitude": 77.0900,
      "status": "IN_USE",
      "currentZone": "Apron Stand 5",
      "speed": 12.5,
      "lastUpdated": "2026-01-28T14:30:00Z",
      "owner": "VIDP"
    }
  ],
  "total": 45,
  "filtered": 12
}
```

```http
GET /api/tracking/assets/live/{assetId}
```
Returns single asset details.

#### Heatmap Analysis (US6)
```http
GET /api/tracking/heatmap/activity?tenantCode={code}&startDate={date}&endDate={date}&gridSize={size}
```
Returns activity density heatmap.

**Parameters:**
- `gridSize`: 10m, 25m, 50m, 100m (converted to degrees: 0.0001°, 0.00025°, 0.0005°, 0.001°)

**Response:**
```json
{
  "mode": "ACTIVITY",
  "gridSize": "10m",
  "timeRange": {
    "start": "2026-01-27T14:30:00Z",
    "end": "2026-01-28T14:30:00Z"
  },
  "data": [
    {
      "latitude": 28.5555,
      "longitude": 77.0900,
      "intensity": 0.85,
      "metadata": {
        "activityCount": 120,
        "uniqueAssets": 15,
        "avgSpeed": 8.5
      }
    }
  ],
  "statistics": {
    "totalCells": 1250,
    "hotspotCells": 45,
    "maxIntensity": 1.0,
    "avgIntensity": 0.32
  }
}
```

```http
GET /api/tracking/heatmap/violations?tenantCode={code}&startDate={date}&endDate={date}&gridSize={size}
```
Returns violation density heatmap.

```http
GET /api/tracking/heatmap/dwell?tenantCode={code}&startDate={date}&endDate={date}&gridSize={size}
```
Returns dwell time heatmap (time assets spent stationary in each cell).

```http
GET /api/tracking/heatmap/hotspot?latitude={lat}&longitude={lng}&mode={mode}&startDate={date}&endDate={date}
```
Returns detailed breakdown for a specific hotspot cell.

**Response:**
```json
{
  "location": {
    "latitude": 28.5555,
    "longitude": 77.0900
  },
  "mode": "ACTIVITY",
  "activityCount": 120,
  "uniqueAssets": 15,
  "assets": [
    {
      "assetIdentifier": "SAM-0005",
      "name": "Fuel Truck - 001",
      "visits": 25,
      "totalDuration": "2h 15m"
    }
  ],
  "timeDistribution": [
    {"hour": "00:00", "count": 5},
    {"hour": "01:00", "count": 3},
    {"hour": "14:00", "count": 45}
  ]
}
```

**WebSocket Events:**

```javascript
// Subscribe to live asset updates
STOMP.subscribe('/topic/assets/live/{tenantCode}', (message) => {
  {
    "eventType": "ASSET_POSITION_UPDATE",
    "assetId": "uuid",
    "latitude": 28.5556,
    "longitude": 77.0901,
    "speed": 13.2,
    "timestamp": "2026-01-28T14:30:05Z"
  }
});
```

**Database Additions:**

```sql
-- Heatmap materialized views
CREATE MATERIALIZED VIEW asset_activity_heatmap AS
SELECT 
    ST_SnapToGrid(location, 0.0001) as grid_location,
    tenant_code,
    time_bucket('1 hour', timestamp) as time_bucket,
    COUNT(*) as activity_count,
    COUNT(DISTINCT asset_identifier) as unique_assets,
    AVG(speed) as avg_speed
FROM asset_movement_trail
WHERE timestamp > NOW() - INTERVAL '30 days'
GROUP BY grid_location, tenant_code, time_bucket;

CREATE MATERIALIZED VIEW violation_heatmap AS
SELECT 
    ST_SnapToGrid(entry_location, 0.0001) as grid_location,
    tenant_code,
    time_bucket('1 hour', timestamp) as time_bucket,
    COUNT(*) as violation_count,
    COUNT(*) FILTER (WHERE severity = 'CRITICAL') as critical_count
FROM zone_violations
WHERE timestamp > NOW() - INTERVAL '30 days'
GROUP BY grid_location, tenant_code, time_bucket;
```

**Frontend Components:**

#### Universal Airside Map Page (US5)
- **Page**: `AirsideMapPage.tsx`
- **Components**:
  - `UniversalAssetMap.tsx`: Leaflet map with all assets
  - `AssetMarker.tsx`: Custom markers color-coded by category
  - `AssetPopup.tsx`: Asset details on click
  - `AssetFilterPanel.tsx`: Multi-filter controls
  - `ZoneBoundariesLayer.tsx`: Restricted zone polygons
  - `AssetSearchBar.tsx`: Search and zoom to asset
  - `MapLegend.tsx`: Color/status legend
- **Features**:
  - Real-time WebSocket updates
  - Marker clustering (react-leaflet-cluster)
  - Smooth marker animation
  - Category/status/zone filtering
  - Owner metadata display

#### Hotspot Analysis Page (US6)
- **Page**: `HotspotAnalysisPage.tsx`
- **Components**:
  - `HeatmapView.tsx`: Leaflet.heat integration
  - `HeatmapControls.tsx`: Mode/resolution/time selectors
  - `HotspotDetailModal.tsx`: Cell detail breakdown
  - `HeatmapLegend.tsx`: Gradient legend
- **Features**:
  - 3 modes: Activity, Violation, Dwell
  - 4 grid resolutions: 10m/25m/50m/100m
  - Time range selector
  - Intensity slider
  - Export PNG/CSV/PDF
  - Comparison mode (stretch goal)

**Tasks:**
- [ ] Create heatmap materialized views
- [ ] Create AssetLocationService and endpoints
- [ ] Create HeatmapService and endpoints
- [ ] Enhance WebSocket for live asset broadcasts
- [ ] Create Universal Airside Map page
- [ ] Create Hotspot Analysis page
- [ ] Install Leaflet.heat plugin
- [ ] Update navigation menu
- [ ] Integration testing
- [ ] Performance testing (500+ assets)

**Acceptance:**
- ✅ Universal asset map shows all assets in real-time (<3 sec load)
- ✅ Asset markers update via WebSocket (<1 sec latency)
- ✅ Filters work correctly
- ✅ Marker clustering prevents overlap
- ✅ Heatmap displays all 3 modes correctly
- ✅ Hotspot click shows detailed breakdown
- ✅ Export functionality works
- ✅ Performance acceptable with 500+ assets
- ✅ Demo Flow requirements 1 (Universal Airside Visibility) and 3 (Hotspot Identification) fully satisfied

**Estimated Duration**: 3-4 days

---

### Phase 2: Backend Domain Models (2 days)

**Tasks:**
- [ ] Create `RestrictedZone` entity
- [ ] Create `AssetMovementTrail` entity  
- [ ] Create `ZoneViolation` entity
- [ ] Create `MovementDiscrepancy` entity
- [ ] Create `AssetLocationRegister` entity
- [ ] Configure Hibernate Spatial
- [ ] Add Lombok annotations
- [ ] Test entity persistence

**Acceptance:**
- All entities mapped to tables
- Spatial types (Point, Polygon) working
- JPA relationships configured
- Unit tests pass

---

### Phase 3: Backend Repositories (1 day)

**Tasks:**
- [ ] Create `RestrictedZoneRepository`
- [ ] Create `AssetMovementTrailRepository`
- [ ] Create `ZoneViolationRepository`
- [ ] Create `MovementDiscrepancyRepository`
- [ ] Create `AssetLocationRegisterRepository`
- [ ] Add custom queries (spatial, time-range)
- [ ] Add pagination support

**Acceptance:**
- All CRUD operations working
- Custom queries tested
- Pagination working

---

### Phase 4: Movement Trail Ingestion Service (4 days)

**Tasks:**
- [ ] Create `MovementTrailIngestionService`
- [ ] Implement scheduled task (every 5 seconds)
- [ ] Read from `vehicles` table
- [ ] Match vehicle_id to asset (via qr_id)
- [ ] Calculate zone containment (ST_Contains)
- [ ] Detect zone violations
- [ ] Detect movement discrepancies
- [ ] Write to `asset_movement_trail`
- [ ] Update `asset_location_register`
- [ ] Broadcast WebSocket events
- [ ] Add error handling
- [ ] Add logging
- [ ] Test with simulated data

**Acceptance:**
- Trail data captured every 5 seconds
- Violations detected within 5 seconds
- Discrepancies detected correctly
- WebSocket events broadcast
- No memory leaks
- <10% CPU usage

---

### Phase 5: Backend Services (3 days)

**Tasks:**
- [ ] Create `ZoneViolationService`
  - [ ] Query violations with filters
  - [ ] Acknowledge violation
  - [ ] Calculate statistics
- [ ] Create `MovementDiscrepancyService`
  - [ ] Query discrepancies with filters
  - [ ] Acknowledge discrepancy
  - [ ] Calculate statistics
- [ ] Create `MovementTrailService`
  - [ ] Get trail for asset
  - [ ] Calculate zone entries/exits
  - [ ] Calculate summary statistics
  - [ ] Export trail data
- [ ] Create `RestrictedZoneService`
  - [ ] CRUD operations
  - [ ] Validate zone geometry
- [ ] Add DTOs for all responses
- [ ] Add MapStruct mappers

**Acceptance:**
- All business logic working
- Unit tests >80% coverage
- DTOs properly mapped

---

### Phase 6: Backend Controllers (2 days)

**Tasks:**
- [ ] Create `ZoneViolationController`
  - [ ] GET /violations (with filters, pagination)
  - [ ] POST /violations/{id}/acknowledge
- [ ] Create `MovementDiscrepancyController`
  - [ ] GET /discrepancies (with filters, pagination)
  - [ ] POST /discrepancies/{id}/acknowledge
- [ ] Create `MovementTrailController`
  - [ ] GET /trail/{assetId}
  - [ ] GET /trail/{assetId}/export
- [ ] Create `RestrictedZoneController`
  - [ ] GET /zones
  - [ ] POST /zones (ADMIN only)
  - [ ] PUT /zones/{id}
  - [ ] DELETE /zones/{id}
- [ ] Add role-based authorization
- [ ] Add tenant isolation
- [ ] Add API documentation (Swagger)

**Acceptance:**
- All endpoints working
- Authorization enforced
- Tenant isolation working
- API docs generated

---

### Phase 7: Frontend Services (1 day)

**Tasks:**
- [ ] Create `trackingService.ts`
  - [ ] fetchZoneViolations()
  - [ ] acknowledgeViolation()
  - [ ] fetchMovementDiscrepancies()
  - [ ] acknowledgeDiscrepancy()
  - [ ] fetchMovementTrail()
  - [ ] fetchRestrictedZones()
- [ ] Integrate with existing `api.ts`
- [ ] Add TypeScript interfaces for all DTOs
- [ ] Update `WebSocketService.ts` for tracking events

**Acceptance:**
- All API calls working
- TypeScript types defined
- Error handling implemented

---

### Phase 8: Frontend - Zone Violations Report (2 days)

**Tasks:**
- [ ] Create `RestrictedZoneReportPage.tsx`
- [ ] Create `ViolationTable` component
- [ ] Create `ViolationFilters` component
- [ ] Create `AcknowledgeViolationModal` component
- [ ] Implement pagination
- [ ] Implement sorting
- [ ] Implement filtering
- [ ] Add real-time updates (WebSocket)
- [ ] Add export button
- [ ] Add loading states
- [ ] Add error states

**Acceptance:**
- Table displays violations
- Filters working
- Sorting working
- Pagination working
- Acknowledge modal working
- Real-time updates working
- Responsive design

---

### Phase 9: Frontend - Movement Discrepancy Report (2 days)

**Tasks:**
- [ ] Create `MovementDiscrepancyReportPage.tsx`
- [ ] Create `DiscrepancyTable` component
- [ ] Create `DiscrepancyFilters` component
- [ ] Create `DiscrepancyMapView` component (Leaflet)
- [ ] Create `AcknowledgeDiscrepancyModal` component
- [ ] Implement table/map view toggle
- [ ] Add real-time updates
- [ ] Add export button

**Acceptance:**
- Table view working
- Map view showing expected vs actual
- Toggle between views
- Real-time updates working

---

### Phase 10: Frontend - Movement Trail Visualization (3 days)

**Tasks:**
- [ ] Create `MovementTrailPage.tsx`
- [ ] Create `AssetSelector` component (autocomplete)
- [ ] Create `TrailMap` component
  - [ ] Polyline with color coding
  - [ ] Zone entry/exit markers
  - [ ] Current position marker
- [ ] Create `TrailTimeline` component
  - [ ] Timeline scrubber
  - [ ] Playback controls
  - [ ] Speed selector
- [ ] Create `TrailInfoPanel` component
- [ ] Implement playback animation
- [ ] Add export trail data
- [ ] Add date range selector

**Acceptance:**
- Asset search working
- Map displays trail with colors
- Timeline navigation working
- Playback animation smooth
- Zone markers displayed
- Export working

---

### Phase 11: Navigation Integration (1 day)

**Tasks:**
- [ ] Update `MainLayout.tsx` - Add "Security Reports" menu section
- [ ] Add "Zone Violations" link
- [ ] Add "Movement Discrepancies" link
- [ ] Add "Movement Trail" link
- [ ] Update `App.tsx` - Add routes
- [ ] Configure role-based access (GH, ADMIN)
- [ ] Add icons to menu items

**Acceptance:**
- Menu items visible for authorized roles
- Routes working
- Unauthorized users see 403

---

### Phase 12: Testing & Quality (3 days)

**Tasks:**
- [ ] Backend unit tests
  - [ ] Service layer tests
  - [ ] Repository tests
  - [ ] Detection algorithm tests
- [ ] Backend integration tests
  - [ ] API endpoint tests
  - [ ] WebSocket tests
- [ ] Frontend component tests
- [ ] E2E tests (Playwright/Cypress)
  - [ ] View violations report
  - [ ] Acknowledge violation
  - [ ] View movement trail
- [ ] Performance testing
  - [ ] Load test ingestion service
  - [ ] Test with 10K+ trail points
- [ ] Security testing
  - [ ] Test tenant isolation
  - [ ] Test authorization

**Acceptance:**
- >80% backend code coverage
- >70% frontend code coverage
- All E2E scenarios pass
- Performance meets SLAs
- No security vulnerabilities

---

### Phase 13: Documentation & Deployment (1 day)

**Tasks:**
- [ ] Update README with feature documentation
- [ ] Create user guide
- [ ] Create admin configuration guide
- [ ] Update API documentation
- [ ] Test deployment in dev environment
- [ ] Verify database migrations
- [ ] Create demo data script
- [ ] Record demo video

**Acceptance:**
- Documentation complete
- Deployment successful
- Demo ready

---

## Testing Strategy

### Unit Tests
- All service methods
- All detection algorithms
- All repository custom queries
- All utility functions

### Integration Tests
- API endpoints with test database
- WebSocket event flow
- Database spatial queries
- Scheduled task execution

### E2E Tests
- User workflows (view reports, acknowledge, export)
- Real-time updates
- Map interactions
- Multi-tenant scenarios

### Performance Tests
- Ingestion service under load (500+ assets)
- Report queries with 10K+ rows
- Map rendering with 1000+ trail points
- Concurrent user access (100 users)

---

## Deployment Strategy

### Database Migration
1. Apply 06-asset-tracking-security.sql
2. Verify PostGIS extension
3. Run data seeding scripts
4. Test spatial indexes
5. Verify continuous aggregates

### Backend Deployment
1. Build JAR: `mvn clean package`
2. Update environment variables
3. Deploy to application server
4. Verify scheduled tasks running
5. Monitor CPU/memory usage

### Frontend Deployment
1. Build: `npm run build`
2. Update API endpoints (production URLs)
3. Deploy to nginx/CDN
4. Verify WebSocket connections

---

## Rollback Plan

If critical issues found after deployment:
1. Stop scheduled ingestion service
2. Revert database migration (drop new tables)
3. Revert backend code
4. Remove frontend routes
5. Notify users of rollback

---

## Monitoring & Observability

### Metrics to Track
- Ingestion rate (positions/second)
- Violation detection rate (violations/hour)
- Discrepancy detection rate (discrepancies/hour)
- API response times (p50, p95, p99)
- WebSocket connection count
- Database query performance
- Error rates

### Alerts
- Ingestion service stopped (>1 min no data)
- High violation rate (>50 violations/hour)
- API errors >5% in 5 minutes
- Database query >5 seconds
- CPU usage >80% for 10 minutes

### Logging
- All violations logged at INFO level
- All discrepancies logged at WARN level
- Acknowledgments logged in audit log
- Errors logged with stack traces

---

## Success Metrics

**Day 1 Post-Launch:**
- Ingestion service running without errors
- At least 1 violation detected and displayed
- All 3 report pages accessible

**Week 1 Post-Launch:**
- 50+ violations detected
- 20+ discrepancies detected
- 10+ users accessed reports
- Average API response time <2 seconds

**Month 1 Post-Launch:**
- 80% of GH managers using reports weekly
- >95% violation detection accuracy
- <5% unmatched vehicle-asset mappings
- 50% reduction in manual incident reports

---

## Known Limitations

1. **GPS Accuracy**: Dependent on GPS quality (±10-50m typical)
2. **Zone Boundaries**: Complex polygons may impact performance
3. **Real-time Latency**: 5-10 second delay due to polling interval
4. **Historical Data**: Only 90 days retained in hot storage
5. **Concurrent Editing**: No conflict resolution for zone configuration

---

## Future Enhancements

**Phase 2 (Future)**:
- ML-based anomaly detection
- Predictive zone violation alerts
- Mobile app for field acknowledgment
- CCTV integration for visual verification
- Automated driver notifications
- Integration with maintenance scheduling
- Advanced analytics dashboard
- Custom zone shapes (circles, routes)
