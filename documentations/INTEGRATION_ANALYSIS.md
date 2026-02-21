# TAM Code Integration Analysis Report
**Generated**: 2026-01-28  
**Scope**: Universal Asset Map (Feature 005) vs Existing Map Implementation  
**Severity**: CRITICAL - Major feature regression

---

## Executive Summary

The new Universal Asset Map implementation ([AirsideMapPage.tsx](frontend/src/pages/AirsideMapPage.tsx)) has **completely replaced** the root route (`/`), causing a **CRITICAL loss of existing functionality**:

- ❌ **Flight tracking** (live aircraft visualization)
- ❌ **Vehicle tracking** (ground support equipment monitoring) 
- ❌ **Real-time alerts** (EnhancedAlertList component)
- ❌ **Map theming** (dark/light mode switching)
- ❌ **Layer controls** (MapControlPanel)
- ❌ **Simulation integration** (simulate_*.sh scripts → NiFi → cv-event-ingest)
- ❌ **Airport boundary circles** (spatial context visualization)

The old implementation ([MapPage.tsx](frontend/src/pages/MapPage.tsx)) is now only accessible at `/map/turnaround`, creating a **fragmented user experience** and **breaking existing workflows**.

---

## Detailed Analysis

### 1. CRITICAL: Complete Feature Override

| ID | Category | Severity | Location(s) | Summary | Impact |
|----|----------|----------|-------------|---------|--------|
| **A1** | **Routing** | **CRITICAL** | [App.tsx#L42-52](frontend/src/App.tsx#L42-L52) | Root route `/` changed from MapPage to AirsideMapPage | **100% of users** now see asset tracking instead of turnaround monitoring |
| **A2** | **Data Flow** | **CRITICAL** | AirsideMapPage, MapPage | Two separate data pipelines with **zero integration** | Vehicle data not visible in asset map; asset data not in turnaround map |
| **A3** | **WebSocket** | **HIGH** | [AirsideMapPage.tsx#L77](frontend/src/pages/AirsideMapPage.tsx#L77), [MapPage.tsx#L61](frontend/src/pages/MapPage.tsx#L61) | Different WebSocket topics: `/topic/asset-tracking/{tenant}` vs `/topic/vehicles/{icao}` | Real-time updates only work per page, not unified |
| **A4** | **Simulation** | **CRITICAL** | simulate_*.sh scripts | Scripts POST to `cv-event-ingest` (port 8094) but **endpoint doesn't exist** | **All simulation workflows broken** |
| **A5** | **API Mismatch** | **HIGH** | Backend controllers | Old APIs: `/api/vehicles`, `/api/vehicle-alerts`, `/api/flights` vs New: `/api/tracking/assets/live` | **API duplication**, unclear which is canonical |

---

### 2. Lost Features Analysis

#### 2.1 Flight Tracking Layer
**Component**: [FlightLayer.tsx](frontend/src/components/Map/FlightLayer.tsx)

**Capabilities Lost**:
- Live aircraft position visualization with custom aircraft icons
- Flight status indicators (AIRBORNE, TAXIING, LANDED, ALERT)
- WebSocket real-time updates from `/topic/flights/{icao}`
- Flight info cards with callsign, speed, altitude, heading
- Aircraft icon rotation based on heading
- Color-coded by status with pulse animation for alerts

**Backend Support**:
- ✅ [FlightController.java](backend/src/main/java/com/utam/controller/FlightController.java) exists
- ✅ `/api/flights` endpoint returns `FlightDisplay[]`
- ✅ WebSocket publishing to `/topic/flights/*` active

**User Story Impact**:  
> "As an air traffic controller, I need to see live aircraft positions to coordinate ground operations" — **NO LONGER SUPPORTED**

---

#### 2.2 Vehicle Tracking Layer  
**Component**: [VehicleLayer.tsx](frontend/src/components/Map/VehicleLayer.tsx)

**Capabilities Lost**:
- Ground support equipment (GSE) visualization
- Vehicle type detection: bus, fuel_truck, tug, belt_loader, catering
- Status-based coloring: active (moving), idle, warning (speeding), alert
- Vehicle info cards with speed, location, last update time
- WebSocket real-time updates from `/topic/vehicles/{icao}`
- Integration with simulate_*.sh generated data

**Backend Support**:
- ✅ [VehicleController.java](backend/src/main/java/com/utam/controller/VehicleController.java) exists
- ✅ [VehicleService.java](backend/src/main/java/com/utam/service/VehicleService.java) with Kafka integration
- ✅ `/api/vehicles` endpoint returns `Vehicle[]`
- ✅ WebSocket publishing to `/topic/vehicles/*` active

**Data Flow (BROKEN)**:
```
simulate_*.sh → NiFi (port 8094/cv-event-ingest) → Kafka (turnaround-raw-json) 
  → VehicleService → PostgreSQL (vehicles table) → WebSocket → MapPage ❌
```

**User Story Impact**:  
> "As a ramp supervisor, I need to track all ground vehicles to optimize resource allocation" — **NO LONGER SUPPORTED**

---

#### 2.3 Real-Time Alerts System
**Component**: [EnhancedAlertList.tsx](frontend/src/components/EnhancedAlertList.tsx)

**Capabilities Lost**:
- Draggable alert panel (UX enhancement)
- Collapsible/expandable UI
- Alert severity filtering (critical, warning, info)
- Color-coded by severity (red, amber, blue)
- Alert grouping and count badges
- Alert dismissal functionality
- WebSocket real-time alert streaming
- Throttled rendering (max 2 updates/sec to prevent UI lag)

**Backend Support**:
- ✅ [VehicleAlertController.java](backend/src/main/java/com/utam/controller/VehicleAlertController.java) exists
- ✅ `/api/vehicle-alerts` endpoint returns alerts
- ✅ WebSocket publishing to `/topic/alerts/*` active

**User Story Impact**:  
> "As operations manager, I need immediate notification of safety violations" — **NO LONGER SUPPORTED**

---

#### 2.4 Map Control Panel
**Component**: [MapControlPanel.tsx](frontend/src/components/MapControlPanel.tsx)

**Capabilities Lost**:
- Draggable control panel
- Layer toggles: vehicles, flights, alerts (show/hide individual layers)
- Theme switching: dark mode (CartoDB Dark) ↔ light mode (OpenStreetMap)
- Collapsible UI to maximize map space
- Future-ready vehicle type filters (bus, tug, fuel truck, etc.)
- Time range controls (reserved for future implementation)

**User Story Impact**:  
> "As a user, I need to customize what I see on the map based on my role" — **NO LONGER SUPPORTED**

---

#### 2.5 Map Theming & Visual Context
**Component**: [MapComponent.tsx](frontend/src/components/Map/MapComponent.tsx)

**Capabilities Lost**:
- Dark/light theme switching
- Theme-aware tile layers:
  - Dark: CartoDB Dark (optimized for nighttime operations)
  - Light: OpenStreetMap (daytime operations)
- Airport boundary circles (3 concentric circles):
  - 18.52 km inner radius (10 NM, airfield operations zone)
  - 74.08 km middle radius (40 NM, approach/departure zone)
  - 129.64 km outer radius (70 NM, terminal control area)
- Theme-aware circle colors (blue gradient in dark, darker blue in light)
- Dashed boundary lines for visual clarity
- Canvas rendering optimization (`preferCanvas: true`)

**Technical Details**:
```tsx
// LOST: Dark theme with boundary circles
<TileLayer url="https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png" />
<Circle center={center} radius={18520} pathOptions={{ color: '#3b82f6', opacity: 0.6, dashArray: '5, 5' }} />
<Circle center={center} radius={74080} pathOptions={{ color: '#6366f1', opacity: 0.4, dashArray: '10, 5' }} />
<Circle center={center} radius={129640} pathOptions={{ color: '#8b5cf6', opacity: 0.3, dashArray: '15, 10' }} />
```

**User Story Impact**:  
> "As a night shift operator, I need dark mode to reduce eye strain during 12-hour shifts" — **NO LONGER SUPPORTED**

---

### 3. Simulation Integration Breakdown

#### 3.1 Missing NiFi Endpoint
**Issue**: Scripts reference `http://localhost:8094/cv-event-ingest` but **no backend endpoint** exists.

**Evidence**:
```bash
# simulate_ba249.sh, simulate_ek500.sh, simulate_qf401_ybbn.sh
BASE_URL="http://localhost:8094/cv-event-ingest"
```

**Expected Flow** (from NiFi setup):
```
simulate_*.sh → NiFi ListenHTTP (port 8094, path /cv-event-ingest) 
  → Kafka (turnaround-raw-json) → VehicleService → PostgreSQL → WebSocket → Frontend
```

**Current State**:
- ❌ NiFi flow **not configured** (manual setup required per [setup-nifi.sh](infrastructure/nifi/setup-nifi.sh))
- ❌ ListenHTTP processor **not created**
- ❌ Port 8094 **not listening**
- ❌ All simulation scripts **fail silently** (no error handling)

**Impact**: 
- **Zero demo capability** for turnaround simulation
- **Cannot test** vehicle tracking, flight arrivals, baggage operations, fueling, catering
- **Manual data entry required** for any demo

---

#### 3.2 API Endpoint Confusion
**Problem**: Two parallel API structures with **no integration**

**Old API** (MapPage):
| Endpoint | Controller | Data Model | WebSocket Topic |
|----------|-----------|------------|-----------------|
| `/api/vehicles` | [VehicleController](backend/src/main/java/com/utam/controller/VehicleController.java) | `Vehicle` (gps_actual_time, vehicle_type, ign, speed) | `/topic/vehicles/{icao}` |
| `/api/vehicle-alerts` | [VehicleAlertController](backend/src/main/java/com/utam/controller/VehicleAlertController.java) | `Alert` (type, severity, entity_id) | `/topic/alerts/{icao}` |
| `/api/flights` | [FlightController](backend/src/main/java/com/utam/controller/FlightController.java) | `FlightDisplay` (callsign, lat, lon, altitude) | `/topic/flights/{icao}` |

**New API** (AirsideMapPage):
| Endpoint | Controller | Data Model | WebSocket Topic |
|----------|-----------|------------|-----------------|
| `/api/tracking/assets/live` | [AssetLocationController](backend/src/main/java/com/utam/asset/controller/AssetLocationController.java) | `AssetLocation` (asset_id, category, zone_status, has_violation) | `/topic/asset-tracking/{tenant}` |

**Schema Mismatch**:
```java
// OLD: vehicles table (from IngestionController)
Vehicle {
    vehicle_id: String      // e.g., "BNE-TRUCK-001"
    vehicle_type: String    // e.g., "fuel_truck"
    latitude: Double
    longitude: Double
    speed: Double
    ign: String            // "ON" / "OFF"
    status: String
    timestamp: Instant
}

// NEW: asset_location_register table  
AssetLocation {
    asset_id: UUID                     // UUID from assets table
    asset_identifier: String           // e.g., "BNE-0001"
    name: String                       // e.g., "Fire Truck Brisbane"
    category: String                   // e.g., "Emergency", "Fueling"
    tenant_code: String                // e.g., "YBBN"
    qr_id: String
    zone_status: Enum                  // IN_RESTRICTED_ZONE, OUTSIDE_ZONES
    has_violation: Boolean
}
```

**No Data Bridge**:
- Vehicle data ❌ does NOT populate asset_location_register
- Assets ❌ do NOT appear in vehicles table
- **Two separate universes** of tracking data

---

### 4. Constitution Alignment Check

**Constitution Principle Violated**:
> "New features MUST NOT break existing functionality without explicit migration plan and user notification"

**Violation Evidence**:
1. No deprecation notice for old MapPage functionality
2. No data migration from vehicles → asset_location_register
3. No backward compatibility layer
4. No feature flag to toggle between implementations
5. Routing change (`/` → AirsideMapPage) applied **without** preserving old default

**Severity**: CRITICAL — Constitution MUST principle broken

---

## Coverage Analysis

### 4.1 Requirements Coverage

**From Old MapPage** (assumed from implementation):
| Requirement | Old MapPage | AirsideMapPage | Status |
|-------------|------------|----------------|---------|
| REQ-MAP-01: Display live aircraft positions | ✅ FlightLayer | ❌ Not implemented | **LOST** |
| REQ-MAP-02: Display ground vehicles | ✅ VehicleLayer | ❌ Not implemented | **LOST** |
| REQ-MAP-03: Show real-time alerts | ✅ EnhancedAlertList | ❌ Not implemented | **LOST** |
| REQ-MAP-04: Provide layer controls | ✅ MapControlPanel | ❌ Not implemented | **LOST** |
| REQ-MAP-05: Support dark/light themes | ✅ MapComponent | ❌ Only OpenStreetMap | **PARTIAL LOSS** |
| REQ-MAP-06: WebSocket real-time updates | ✅ 3 topics (vehicles, flights, alerts) | ✅ 1 topic (asset-tracking) | **PARTIAL** |

**From spec.md (Feature 005)**: 
| Requirement | Tasks | Status |
|-------------|-------|--------|
| US5: Universal Airside Map | T036-T040 | ✅ **COMPLETE** |
| Show all assets on map | T037 | ✅ UniversalAssetMap component |
| Filter by category/status/zone | T038 | ✅ AssetFilterPanel component |
| Real-time position updates | T039 | ✅ WebSocket + polling |
| Color-code by violation status | T040 | ✅ hasViolation flag |

**Finding**: Feature 005 requirements are **100% met**, but at the **cost of breaking existing requirements**.

---

### 4.2 Task Coverage vs Implementation

**Completed** (from tasks.md):
- ✅ T036: Create AirsideMapPage route component
- ✅ T037: Implement UniversalAssetMap with Leaflet
- ✅ T038: Build AssetFilterPanel
- ✅ T039: WebSocket integration for real-time updates
- ✅ T040: Color-code markers by violation status

**NOT Completed** (relevant to integration):
- ❌ T023a: NiFi flow for Asset Position Polling (manual config needed)
- ❌ **NO TASK** for integrating old MapPage features
- ❌ **NO TASK** for data migration vehicles → assets
- ❌ **NO TASK** for preserving flight/vehicle/alert tracking
- ❌ **NO TASK** for unified map component

**Root Cause**: **Tasks.md did NOT include integration requirements**. This is a **specification gap**.

---

## Metrics Summary

| Metric | Value | Notes |
|--------|-------|-------|
| **Total Components Lost** | 5 | FlightLayer, VehicleLayer, EnhancedAlertList, MapControlPanel, MapComponent theming |
| **Requirements with Zero Coverage** | 5 | Aircraft display, vehicle tracking, alerts, layer controls, theme switching |
| **API Endpoints Orphaned** | 3 | `/api/vehicles`, `/api/vehicle-alerts`, `/api/flights` |
| **WebSocket Topics Unused** | 3 | `/topic/vehicles/*`, `/topic/flights/*`, `/topic/alerts/*` |
| **Simulation Scripts Broken** | 3 | simulate_ba249.sh, simulate_ek500.sh, simulate_qf401_ybbn.sh |
| **Critical Issues** | 4 | A1 (routing), A2 (data flow), A4 (simulation), constitution violation |
| **High Issues** | 2 | A3 (WebSocket fragmentation), A5 (API duplication) |
| **Medium Issues** | 0 | None identified |
| **Coverage % (Old Features)** | 0% | Zero old features preserved in new implementation |

---

## Recommended Remediation Plan

### Phase 1: Immediate Fixes (1-2 days)

#### 1.1 Restore Old MapPage as Default
**Priority**: CRITICAL  
**Effort**: 2 hours

**Changes**:
```tsx
// frontend/src/App.tsx
<Route path="/" element={
  <ProtectedRoute>
    <MainLayout>
      <MapPage />  {/* RESTORE OLD DEFAULT */}
    </MainLayout>
  </ProtectedRoute>
} />
<Route path="/tracking/assets" element={  {/* NEW PATH FOR ASSET MAP */}
  <ProtectedRoute>
    <MainLayout>
      <AirsideMapPage />
    </MainLayout>
  </ProtectedRoute>
} />
```

**Rationale**: Preserves existing user workflows while making new feature accessible

---

#### 1.2 Fix NiFi Integration
**Priority**: CRITICAL  
**Effort**: 4 hours

**Tasks**:
1. Run [setup-nifi.sh](infrastructure/nifi/setup-nifi.sh) to create NiFi flows
2. Create ListenHTTP processor on port 8094, path `/cv-event-ingest`
3. Configure Kafka publishing to `turnaround-raw-json` topic
4. Test with `simulate_ba249.sh`
5. Verify WebSocket updates reach MapPage

**Validation**:
```bash
# Test NiFi endpoint
curl -X POST http://localhost:8094/cv-event-ingest \
  -H 'Content-Type: application/json' \
  -d '{"flight_id":"TEST123","event_type":"bridge_connect","timestamp":"2026-01-28T10:00:00Z","icao_code":"YBBN"}'

# Verify Kafka message
docker exec tam-redpanda-1 rpk topic consume turnaround-raw-json --num 1
```

---

### Phase 2: Integration (3-5 days)

#### 2.1 Create Unified Map Component
**Priority**: HIGH  
**Effort**: 2 days

**Design**: Merge MapPage and AirsideMapPage into single component with feature toggles

```tsx
// frontend/src/pages/UnifiedMapPage.tsx
interface UnifiedMapPageProps {
  enableFlights?: boolean;     // Default: true
  enableVehicles?: boolean;    // Default: true
  enableAssets?: boolean;      // Default: true
  enableAlerts?: boolean;      // Default: true
  enableThemes?: boolean;      // Default: true
  defaultView?: 'turnaround' | 'tracking' | 'unified';  // Default: 'unified'
}

const UnifiedMapPage: React.FC<UnifiedMapPageProps> = ({ 
  enableFlights = true,
  enableVehicles = true,
  enableAssets = true,
  enableAlerts = true,
  enableThemes = true,
  defaultView = 'unified'
}) => {
  const [activeView, setActiveView] = useState(defaultView);
  const [layers, setLayers] = useState({
    flights: enableFlights,
    vehicles: enableVehicles,
    assets: enableAssets,
    alerts: enableAlerts
  });
  const [mapTheme, setMapTheme] = useState<'dark' | 'light'>('dark');

  return (
    <div className="h-full flex flex-col">
      {/* View Switcher */}
      <ViewSwitcher 
        activeView={activeView} 
        onViewChange={setActiveView}
        options={[
          { id: 'turnaround', label: 'Turnaround Ops', enabled: enableVehicles || enableFlights },
          { id: 'tracking', label: 'Asset Tracking', enabled: enableAssets },
          { id: 'unified', label: 'Unified View', enabled: true }
        ]}
      />

      {/* Unified Map */}
      <div className="flex-1 relative">
        <MapComponent center={getCenter()} zoom={14} theme={mapTheme}>
          {/* Conditional Layers */}
          {layers.flights && <FlightLayer />}
          {layers.vehicles && <VehicleLayer vehicles={vehicles} />}
          {layers.assets && <AssetMarkersLayer assets={assets} />}
        </MapComponent>

        {/* Unified Control Panel */}
        <MapControlPanel
          layers={layers}
          onLayerToggle={handleLayerToggle}
          theme={enableThemes ? mapTheme : undefined}
          onThemeToggle={enableThemes ? handleThemeToggle : undefined}
        />

        {/* Conditional Alert List */}
        {layers.alerts && (
          <EnhancedAlertList alerts={enhancedAlerts} onDismiss={handleAlertDismiss} />
        )}
      </div>
    </div>
  );
};
```

**Benefits**:
- Single source of truth for map visualization
- Feature flags enable gradual migration
- All layers work together harmoniously
- User can toggle between focused views

---

#### 2.2 Create Data Bridge
**Priority**: HIGH  
**Effort**: 2 days

**Backend**: Create synchronization between `vehicles` and `asset_location_register`

```java
// backend/src/main/java/com/utam/service/AssetVehicleSyncService.java
@Service
public class AssetVehicleSyncService {
    
    @Scheduled(fixedDelay = 5000) // Sync every 5 seconds
    public void syncVehiclesToAssets() {
        List<Vehicle> recentVehicles = vehicleRepository.findUpdatedSince(
            Instant.now().minus(10, ChronoUnit.SECONDS)
        );
        
        for (Vehicle vehicle : recentVehicles) {
            // Map vehicle to asset
            AssetLocation asset = assetLocationRepository
                .findByVehicleIdentifier(vehicle.getVehicleId())
                .orElseGet(() -> createAssetFromVehicle(vehicle));
            
            // Update position
            asset.setLatitude(vehicle.getLatitude());
            asset.setLongitude(vehicle.getLongitude());
            asset.setSpeed(vehicle.getSpeed());
            asset.setHeading(vehicle.getHeading());
            asset.setStatus(mapVehicleStatus(vehicle.getStatus()));
            asset.setLastSeen(vehicle.getTimestamp());
            
            assetLocationRepository.save(asset);
            
            // Publish to unified WebSocket topic
            messagingTemplate.convertAndSend(
                "/topic/unified-tracking/" + asset.getTenantCode(),
                asset
            );
        }
    }
    
    private AssetLocation createAssetFromVehicle(Vehicle vehicle) {
        // Auto-register vehicle as asset
        Asset asset = new Asset();
        asset.setAssetIdentifier(vehicle.getVehicleId());
        asset.setName(vehicle.getVehicleType() + " " + vehicle.getVehicleId());
        asset.setCategory(mapVehicleTypeToCategory(vehicle.getVehicleType()));
        asset.setTenantCode(vehicle.getIcaoCode());
        // ... set other fields
        return assetService.registerAsset(asset);
    }
}
```

**Database**: Add mapping column
```sql
-- Migration: Add vehicle_id reference to assets table
ALTER TABLE assets ADD COLUMN vehicle_identifier VARCHAR(50);
CREATE INDEX idx_assets_vehicle_id ON assets(vehicle_identifier);
```

**Benefits**:
- Vehicles automatically become trackable assets
- Unified WebSocket topic for all position updates
- No manual data entry required

---

#### 2.3 Unified WebSocket Topic
**Priority**: MEDIUM  
**Effort**: 1 day

**Backend**: Create aggregated topic that combines all position updates

```java
// backend/src/main/java/com/utam/service/UnifiedTrackingService.java
@Service
public class UnifiedTrackingService {
    
    @KafkaListener(topics = {"vehicle-raw-json", "asset-positions-json", "flight-raw-json"})
    public void handlePositionUpdate(String message) {
        // Parse and normalize to unified format
        UnifiedPosition position = normalizePosition(message);
        
        // Publish to unified topic
        messagingTemplate.convertAndSend(
            "/topic/unified-tracking/" + position.getTenantCode(),
            position
        );
    }
}
```

**Frontend**: Subscribe to single topic in UnifiedMapPage
```typescript
webSocketService.subscribe(
  `/topic/unified-tracking/${tenantCode}`,
  (update: UnifiedPosition) => {
    if (update.entityType === 'FLIGHT') {
      updateFlights(update);
    } else if (update.entityType === 'VEHICLE') {
      updateVehicles(update);
    } else if (update.entityType === 'ASSET') {
      updateAssets(update);
    }
  }
);
```

**Benefits**:
- Single WebSocket connection (reduced overhead)
- Consistent real-time updates across all entity types
- Easier to add new tracked entity types in future

---

### Phase 3: Polish & Documentation (1-2 days)

#### 3.1 Update Routing Table
**Priority**: MEDIUM  
**Effort**: 2 hours

**Routes**:
```tsx
<Route path="/" element={<UnifiedMapPage defaultView="unified" />} />
<Route path="/map/turnaround" element={<UnifiedMapPage defaultView="turnaround" enableAssets={false} />} />
<Route path="/tracking/assets" element={<UnifiedMapPage defaultView="tracking" enableFlights={false} enableVehicles={false} />} />
```

**Navigation Menu**:
```tsx
<NavItem icon={MapPin} label="Unified Map" path="/" />
<NavItem icon={Plane} label="Turnaround Ops" path="/map/turnaround" />
<NavItem icon={Package} label="Asset Tracking" path="/tracking/assets" />
```

---

#### 3.2 Create Migration Guide
**Priority**: LOW  
**Effort**: 4 hours

**Document**: `docs/MIGRATION_GUIDE_UNIFIED_MAP.md`

Contents:
1. What changed and why
2. Backward compatibility notes
3. New unified features
4. How to use feature toggles
5. API changes (deprecated vs new endpoints)
6. WebSocket topic migration
7. FAQ for existing users

---

#### 3.3 Update Tasks.md
**Priority**: LOW  
**Effort**: 2 hours

Add missing integration tasks:
```markdown
### Phase 2B: Feature Integration

- [ ] T041 [FE] Create UnifiedMapPage component
- [ ] T042 [FE] Integrate FlightLayer into UnifiedMapPage
- [ ] T043 [FE] Integrate VehicleLayer into UnifiedMapPage  
- [ ] T044 [FE] Integrate AssetMarkersLayer into UnifiedMapPage
- [ ] T045 [FE] Add ViewSwitcher component
- [ ] T046 [BE] Create AssetVehicleSyncService
- [ ] T047 [BE] Create UnifiedTrackingService
- [ ] T048 [BE] Add vehicle_identifier column to assets table
- [ ] T049 [DB] Create data migration script vehicles → asset_location_register
- [ ] T050 [TEST] Test unified WebSocket updates
- [ ] T051 [TEST] Test view switching performance
- [ ] T052 [DOC] Create MIGRATION_GUIDE_UNIFIED_MAP.md
```

---

## Alternative Approaches

### Option A: Keep Separate (Status Quo)
**Pros**: No integration work needed  
**Cons**: 
- Fragmented UX
- Duplicate API maintenance
- Lost features never restored
- Users must know two different URLs

**Verdict**: ❌ Not recommended — poor user experience

---

### Option B: Feature Flag Toggle
**Implementation**: Add global setting to switch default map

```tsx
// frontend/src/context/FeatureFlags.tsx
const FeatureFlags = {
  useUnifiedMap: process.env.REACT_APP_UNIFIED_MAP === 'true'
};

// App.tsx
<Route path="/" element={
  FeatureFlags.useUnifiedMap 
    ? <UnifiedMapPage /> 
    : <MapPage />
} />
```

**Pros**: 
- Gradual rollout
- Easy rollback
- A/B testing possible

**Cons**: 
- Doesn't solve feature loss
- Still need to build UnifiedMapPage

**Verdict**: ✅ Good interim step, but still need full integration

---

### Option C: Deprecate Old MapPage Entirely
**Implementation**: Force all users to new AirsideMapPage, rebuild lost features as widgets

**Pros**: 
- Single code path
- Clean architecture

**Cons**: 
- Requires rebuilding FlightLayer, VehicleLayer, EnhancedAlertList from scratch
- Higher effort than integration
- Risk of missing nuances from old implementation

**Verdict**: ⚠️ Only if business decides to sunset turnaround monitoring entirely

---

## Next Steps

### Immediate Actions (Today)

1. **CRITICAL**: Revert routing change  
   - Change `App.tsx` root route back to MapPage
   - Move AirsideMapPage to `/tracking/assets`
   - **Owner**: Frontend lead
   - **ETA**: 1 hour

2. **CRITICAL**: Configure NiFi flows
   - Run `infrastructure/nifi/setup-nifi.sh`
   - Test with `simulate_ba249.sh`
   - **Owner**: DevOps/Infrastructure lead  
   - **ETA**: 3 hours

3. **HIGH**: Document current state
   - Update README with route changes
   - Create issue in tracker: "Integrate MapPage and AirsideMapPage"
   - Link to this analysis report
   - **Owner**: Tech lead
   - **ETA**: 1 hour

---

### Short-term Actions (This Week)

4. **HIGH**: Spike on UnifiedMapPage approach
   - Prototype with FlightLayer + AssetMarkersLayer together
   - Validate no performance degradation
   - **Owner**: Senior frontend engineer
   - **ETA**: 1 day

5. **HIGH**: Design data bridge architecture
   - Document vehicles ↔ assets sync strategy
   - Get DBA review on sync performance
   - **Owner**: Backend lead + DBA
   - **ETA**: 1 day

6. **MEDIUM**: Stakeholder communication
   - Present findings to product team
   - Get decision on integration approach
   - **Owner**: Engineering manager
   - **ETA**: 1 meeting

---

### Medium-term Actions (Next Sprint)

7. **HIGH**: Implement UnifiedMapPage (Phase 2.1)
8. **HIGH**: Implement data bridge (Phase 2.2)
9. **MEDIUM**: Implement unified WebSocket (Phase 2.3)
10. **LOW**: Documentation and polish (Phase 3)

---

## Appendix: File Inventory

### Frontend Files Affected

| File | Status | Purpose |
|------|--------|---------|
| [frontend/src/pages/MapPage.tsx](frontend/src/pages/MapPage.tsx) | ✅ Intact | Old turnaround map with flights, vehicles, alerts |
| [frontend/src/pages/AirsideMapPage.tsx](frontend/src/pages/AirsideMapPage.tsx) | ✅ New | Asset tracking map |
| [frontend/src/components/Map/FlightLayer.tsx](frontend/src/components/Map/FlightLayer.tsx) | ⚠️ Orphaned | No longer rendered in default view |
| [frontend/src/components/Map/VehicleLayer.tsx](frontend/src/components/Map/VehicleLayer.tsx) | ⚠️ Orphaned | No longer rendered in default view |
| [frontend/src/components/EnhancedAlertList.tsx](frontend/src/components/EnhancedAlertList.tsx) | ⚠️ Orphaned | No longer rendered in default view |
| [frontend/src/components/MapControlPanel.tsx](frontend/src/components/MapControlPanel.tsx) | ⚠️ Orphaned | No longer rendered in default view |
| [frontend/src/components/Map/MapComponent.tsx](frontend/src/components/Map/MapComponent.tsx) | ⚠️ Partially used | Themes not available in AirsideMapPage |
| [frontend/src/components/Tracking/UniversalAssetMap.tsx](frontend/src/components/Tracking/UniversalAssetMap.tsx) | ✅ New | Core asset map component |
| [frontend/src/components/Tracking/AssetFilterPanel.tsx](frontend/src/components/Tracking/AssetFilterPanel.tsx) | ✅ New | Asset filtering UI |
| [frontend/src/App.tsx](frontend/src/App.tsx) | ⚠️ Modified | Routing changed (root = AirsideMapPage) |

### Backend Files Affected

| File | Status | Purpose |
|------|--------|---------|
| [backend/.../VehicleController.java](backend/src/main/java/com/utam/controller/VehicleController.java) | ⚠️ Orphaned | `/api/vehicles` endpoint not used by AirsideMapPage |
| [backend/.../FlightController.java](backend/src/main/java/com/utam/controller/FlightController.java) | ⚠️ Orphaned | `/api/flights` endpoint not used by AirsideMapPage |
| [backend/.../VehicleAlertController.java](backend/src/main/java/com/utam/controller/VehicleAlertController.java) | ⚠️ Orphaned | `/api/vehicle-alerts` endpoint not used by AirsideMapPage |
| [backend/.../AssetLocationController.java](backend/src/main/java/com/utam/asset/controller/AssetLocationController.java) | ✅ New | `/api/tracking/assets/live` for asset tracking |
| [backend/.../AssetLocationService.java](backend/src/main/java/com/utam/asset/service/AssetLocationService.java) | ✅ New (fixed) | Asset tracking business logic |
| [backend/.../IngestionController.java](backend/src/main/java/com/utam/controller/IngestionController.java) | ⚠️ Partially broken | `/api/cv/events` exists but NiFi not configured |

### Infrastructure Files

| File | Status | Purpose |
|------|--------|---------|
| [simulate_ba249.sh](simulate_ba249.sh) | ❌ Broken | POSTs to missing NiFi endpoint |
| [simulate_ek500.sh](simulate_ek500.sh) | ❌ Broken | POSTs to missing NiFi endpoint |
| [simulate_qf401_ybbn.sh](simulate_qf401_ybbn.sh) | ❌ Broken | POSTs to missing NiFi endpoint |
| [infrastructure/nifi/setup-nifi.sh](infrastructure/nifi/setup-nifi.sh) | ⚠️ Not run | Needs manual execution |
| [infrastructure/db/seed-asset-locations.sql](infrastructure/db/seed-asset-locations.sql) | ✅ Executed | 5 YBBN assets seeded |

---

## Summary

**Total Issues**: 6 (4 Critical, 2 High)  
**Lost Components**: 5 major UI features  
**Broken Workflows**: Simulation, demo flows  
**Estimated Fix Effort**: 5-8 days  

**Recommended Path**: Implement UnifiedMapPage (Option from Phase 2) to preserve all features while adding new asset tracking capabilities.

**Critical Next Step**: Restore old MapPage as default route **immediately** to prevent production impact.

---

**End of Report**
