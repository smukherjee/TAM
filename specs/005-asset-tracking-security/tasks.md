# Implementation Tasks: Asset Tracking & Security Module

**Feature ID**: 005  
**Created**: 2026-01-28  
**Updated**: 2026-01-28 (Added Phase 2A for Demo Flow enhancements)  
**Status**: In Progress  
**Estimated Total Effort**: 21-28 days (revised from 18-24 days)

**Task Summary**:
- **Total Tasks**: 233 (was 162, added 71 for US5 & US6)
- **Completed**: 23 (Phase 0 & Phase 1)
- **Remaining**: 210
- **New in Phase 2A**: 48 tasks for Universal Airside Map (US5) and Hotspot Analysis (US6)

---

## Task Categories

- **[P]** - Platform/Infrastructure
- **[BE]** - Backend
- **[FE]** - Frontend
- **[DB]** - Database
- **[TEST]** - Testing
- **[DOC]** - Documentation

---

## Phase 0: Foundation ✅

- [x] T001 [P] Create feature branch `005-asset-tracking-security`
- [x] T002 [DOC] Create spec.md with requirements and user stories
- [x] T003 [DOC] Create plan.md with architecture and implementation plan
- [x] T004 [DOC] Create tasks.md (this file)
- [x] T005 [DOC] Create PHASE_2A_SUMMARY.md with enhancement details

---

## Phase 1: Database Layer ✅ COMPLETE

- [x] T006 [DB] Create 06-asset-tracking-security.sql migration file
- [x] T007 [DB] Create `restricted_zones` table with PostGIS POLYGON
- [x] T008 [DB] Create `asset_movement_trail` hypertable
- [x] T009 [DB] Create `zone_violations` hypertable
- [x] T010 [DB] Create `movement_discrepancies` hypertable
- [x] T011 [DB] Create `asset_location_register` table
- [x] T012 [DB] Add spatial indexes (GIST) for all geometry columns
- [x] T013 [DB] Add time-series indexes for hypertables
- [x] T014 [DB] Create `zone_violations_hourly` continuous aggregate
- [x] T015 [DB] Create `movement_discrepancies_daily` continuous aggregate
- [x] T016 [DB] Seed restricted zones for VIDP (4 zones)
- [x] T017 [DB] Seed restricted zones for LIRN (2 zones)
- [x] T018 [DB] Seed restricted zones for YBBN (2 zones)
- [x] T019 [DB] Create helper function `get_asset_id_from_vehicle()`
- [x] T020 [DB] Test spatial queries (ST_Contains, ST_Distance)
- [x] T021 [DB] Verify TimescaleDB compression policies
- [x] T022 [DB] Apply migration to local database
- [x] T023 [DB] Verify all tables created successfully
- [ ] T023a [P] Create NiFi flow: Asset Position Polling
  - [ ] ExecuteSQLRecord processor to query vehicles table every 5 seconds
  - [ ] Query: SELECT vehicle_id, latitude, longitude, speed, heading, status, timestamp FROM vehicles WHERE updated_at > ${last_poll_time}
  - [ ] ConvertRecord processor: Database rows → JSON
  - [ ] PublishKafkaRecord processor: Send to asset-positions-json topic
  - [ ] UpdateAttribute: Track last_poll_time
  - [ ] Configure error handling and retry logic
- [ ] T023b [P] Create Kafka topic: asset-positions-json
  - [ ] Partitions: 3 (for parallel processing)
  - [ ] Replication factor: 1 (single node for MVP)
  - [ ] Retention: 24 hours
  - [ ] Compression: gzip
- [ ] T023c [P] Test NiFi flow with mock vehicle data
  - [ ] Run simulate_ba249.sh to generate test positions
  - [ ] Verify NiFi polls and publishes to Kafka
  - [ ] Verify JSON format matches expected schema
  - [ ] Monitor flow performance (<1 sec latency)

**Acceptance Criteria:**
- ✅ All tables exist with correct schema
- ✅ Spatial queries execute in <100ms
- ✅ 8 restricted zones inserted across 3 tenants
- ✅ Continuous aggregates configured
- ✅ NiFi flow ingests vehicle positions every 5 seconds
- ✅ Kafka topic receives position events with <1 sec latency

---

## Phase 2A: Demo Flow Enhancements (US5 & US6)

**Priority**: HIGH - Required for complete Demo Flow coverage (100%)

### Database Enhancements

- [ ] T024 [DB] Create `asset_activity_heatmap` materialized view
  - [ ] Grid-based aggregation using ST_SnapToGrid (10m resolution = 0.0001°)
  - [ ] Time-bucketed by hour for last 30 days
  - [ ] Count activity, unique assets, avg speed per cell
  - [ ] Create GIST spatial index on grid_location
  - [ ] Create index on (tenant_code, time_bucket)
- [ ] T025 [DB] Create `violation_heatmap` materialized view
  - [ ] Grid-based violation density aggregation
  - [ ] Count violations by severity per cell
  - [ ] Time-bucketed by hour
  - [ ] Create spatial and temporal indexes
- [ ] T026 [DB] Create refresh policy for heatmap views
  - [ ] Auto-refresh every 6 hours
  - [ ] Create manual refresh function `refresh_heatmaps()`
  - [ ] Test refresh performance (<10 seconds)

### NiFi & Kafka Integration

- [ ] T026a [BE] Create `AssetPositionEvent.java` (Kafka message model)
  - [ ] Fields: vehicleId, assetId, latitude, longitude, speed, heading, status, timestamp, tenantCode
  - [ ] Add @Data, @Builder, @NoArgsConstructor, @AllArgsConstructor
  - [ ] Add Jackson annotations for JSON deserialization
- [ ] T026b [BE] Create `MovementTrailProcessor.java` (Kafka consumer)
  - [ ] Annotation: @KafkaListener(topics = "asset-positions-json")
  - [ ] Consume AssetPositionEvent from Kafka
  - [ ] Match vehicle_id → asset using qr_id
  - [ ] Check zone containment using ST_DWithin (50m buffer)
  - [ ] Detect zone violations (call ZoneViolationService)
  - [ ] Detect movement discrepancies (call DiscrepancyService)
  - [ ] Write to asset_movement_trail and asset_location_register
  - [ ] Broadcast WebSocket event for real-time updates
  - [ ] Error handling: Log unmapped vehicles, dead letter queue for failed messages
- [ ] T026c [BE] Configure Kafka consumer properties
  - [ ] application.yml: spring.kafka.consumer settings
  - [ ] Group ID: asset-tracking-consumer-group
  - [ ] Auto-offset-reset: earliest
  - [ ] Enable JSON deserialization
  - [ ] Concurrency: 3 (match Kafka partitions)

### Backend API - Universal Asset Map (US5)

- [ ] T027 [BE] Create `AssetLocationDTO.java`
  - [ ] Fields: assetId, assetIdentifier, name, category, latitude, longitude
  - [ ] Fields: status, currentZone, speed, lastUpdated, owner
  - [ ] Add @Data, @Builder, @AllArgsConstructor annotations
  - [ ] Add validation annotations
- [ ] T028 [BE] Create `AssetLocationService.java`
  - [ ] Method: `getAllLiveAssets(tenantCode, filters)` - Query asset_location_register
  - [ ] Method: `getLiveAssetById(assetId)` - Single asset details
  - [ ] Method: `getAssetsInZone(zoneId)` - Filter by zone
  - [ ] Method: `getAssetsByCategory(category)` - Filter by category
  - [ ] Implement caching with @Cacheable (5 second TTL)
  - [ ] Optimize query with JOIN to assets and zones tables
- [ ] T029 [BE] Create `AssetLocationController.java`
  - [ ] `GET /api/tracking/assets/live` - All live assets
  - [ ] Query params: tenantCode, category, status, zone
  - [ ] Response: AssetLocationResponse with assets array + count
  - [ ] `GET /api/tracking/assets/live/{assetId}` - Single asset
  - [ ] Add @PreAuthorize for role-based access
  - [ ] Add @ApiOperation Swagger docs
- [ ] T030 [BE] Enhance WebSocket for live asset updates
  - [ ] Create `AssetLocationWebSocketService.java`
  - [ ] Broadcast to `/topic/assets/live/{tenantCode}`
  - [ ] Message payload: AssetPositionUpdateEvent
  - [ ] Include all asset position changes (not just violations)
  - [ ] Throttle to 1 update per asset per 5 seconds
  - [ ] Integrate in MovementTrailIngestionService

### Backend API - Heatmap (US6)

- [ ] T031 [BE] Create `HeatmapDataDTO.java`
  - [ ] Fields: latitude, longitude, intensity (0-1 normalized)
  - [ ] Fields: metadata (activityCount, uniqueAssets, avgSpeed)
  - [ ] Add nested class for statistics
- [ ] T032 [BE] Create `HotspotDetailDTO.java`
  - [ ] Fields: location, mode, activityCount, uniqueAssets
  - [ ] Fields: assets (list of contributing assets)
  - [ ] Fields: violations (if violation mode)
  - [ ] Fields: timeDistribution (hourly breakdown chart data)
- [ ] T033 [BE] Create `HeatmapService.java`
  - [ ] Method: `getActivityHeatmap(tenantCode, startDate, endDate, gridSize)`
  - [ ] Method: `getViolationHeatmap(tenantCode, startDate, endDate, gridSize)`
  - [ ] Method: `getDwellHeatmap(tenantCode, startDate, endDate, gridSize)`
  - [ ] Method: `getHotspotDetails(lat, lng, mode, dateRange)`
  - [ ] Grid size conversion: 10m=0.0001°, 25m=0.00025°, 50m=0.0005°, 100m=0.001°
  - [ ] Normalize intensity values to 0-1 scale (percentile-based)
  - [ ] Implement caching for frequently requested ranges
- [ ] T034 [BE] Create `HeatmapController.java`
  - [ ] `GET /api/tracking/heatmap/activity` - Activity density data
  - [ ] `GET /api/tracking/heatmap/violations` - Violation density data
  - [ ] `GET /api/tracking/heatmap/dwell` - Dwell time data
  - [ ] `GET /api/tracking/heatmap/hotspot` - Hotspot detail for clicked cell
  - [ ] All endpoints support: tenantCode, startDate, endDate, gridSize params
  - [ ] Add Swagger documentation
  - [ ] Add validation for date ranges (max 30 days)
- [ ] T035 [BE] Create heatmap export service
  - [ ] Method: `exportHeatmapDataCSV(heatmapData)` - CSV export
  - [ ] Method: `generateHeatmapReportPDF(heatmapData, metadata)` - PDF summary
  - [ ] CSV format: latitude, longitude, intensity, activityCount
  - [ ] PDF includes: heatmap summary stats, top 10 hotspots table

### Frontend - Universal Asset Map (US5)

- [ ] T036 [FE] Create `frontend/src/pages/AirsideMapPage.tsx`
  - [ ] Page layout: map (100% width) + filter panel (collapsible sidebar)
  - [ ] Fetch live assets on mount using React Query
  - [ ] WebSocket subscription for real-time updates
  - [ ] Handle loading/error states with spinners/messages
  - [ ] Pass filtered assets to UniversalAssetMap component
- [ ] T037 [FE] Create `frontend/src/components/Tracking/UniversalAssetMap.tsx`
  - [ ] Leaflet map component (react-leaflet)
  - [ ] Set initial center based on tenant (VIDP, LIRN, YBBN)
  - [ ] Render asset markers from props
  - [ ] Implement marker clustering (react-leaflet-cluster) for zoom < 15
  - [ ] Zone boundaries layer with toggle control
  - [ ] Handle marker click → show AssetPopup
  - [ ] Smooth marker position animation using react-spring
  - [ ] Zoom controls, scale bar, attribution
- [ ] T038 [FE] Create `frontend/src/components/Tracking/AssetMarker.tsx`
  - [ ] Custom SVG markers color-coded by category
  - [ ] Colors: Emergency=Red, Fueling=Orange, Cargo=Blue, GSE=Green, Transport=Purple, Power=Yellow, Services=Teal
  - [ ] Marker states: solid (In Use), hollow (Available), gray (Maintenance), black w/ X (Out of Service)
  - [ ] Size scales with zoom level (12px at z14, 24px at z18)
  - [ ] Pulse animation for moving assets (speed > 0)
  - [ ] Use DivIcon for custom HTML/SVG content
- [ ] T039 [FE] Create `frontend/src/components/Tracking/AssetPopup.tsx`
  - [ ] Leaflet Popup component
  - [ ] Display: Asset ID, Name, Category badge, Status badge
  - [ ] Display: Current Zone, Speed (if moving), Last Updated (relative time)
  - [ ] \"View Movement Trail\" button → navigate to TrailPage with assetId
  - [ ] \"View in Register\" button → navigate to AssetDetails page
  - [ ] Styling with Tailwind CSS
- [ ] T040 [FE] Create `frontend/src/components/Tracking/AssetFilterPanel.tsx`
  - [ ] Collapsible sidebar panel
  - [ ] Category multi-select checkboxes (Emergency, Fueling, etc.)
  - [ ] Owner/tenant dropdown (visible for ADMIN role only)
  - [ ] Zone dropdown (fetched from zones API)
  - [ ] Status multi-select (In Use, Available, Maintenance, Out of Service)
  - [ ] \"Clear All Filters\" button
  - [ ] Asset count badge: \"Showing X of Y assets\"
  - [ ] Apply filters on change, debounced by 300ms
- [ ] T041 [FE] Create `frontend/src/components/Tracking/ZoneBoundariesLayer.tsx`
  - [ ] Fetch restricted zones from GET /api/zones endpoint
  - [ ] Render as Leaflet Polygon layers
  - [ ] Color by zone type: PROHIBITED=rgba(255,0,0,0.3), RESTRICTED=rgba(255,165,0,0.3), CONTROLLED=rgba(255,255,0,0.3), MAINTENANCE=rgba(0,0,255,0.3)
  - [ ] Solid border (2px), semi-transparent fill
  - [ ] Tooltip on hover showing zone name and type
  - [ ] Toggle visibility button in map controls (eye icon)
  - [ ] Store visibility state in localStorage
- [ ] T042 [FE] Create `frontend/src/components/Tracking/AssetSearchBar.tsx`
  - [ ] Search input with autocomplete (Combobox from Headless UI)
  - [ ] Search by asset ID or name (case-insensitive)
  - [ ] Fetch suggestions on typing (debounced 300ms)
  - [ ] On select: zoom to asset location, highlight marker (pulse animation)
  - [ ] Recent searches dropdown (store in localStorage, max 5)
  - [ ] Clear button
- [ ] T043 [FE] Create `frontend/src/components/Tracking/MapLegend.tsx`
  - [ ] Collapsible panel (default: expanded)
  - [ ] Section 1: Marker colors (category legend)
  - [ ] Section 2: Status indicators (solid/hollow/gray/black)
  - [ ] Section 3: Zone colors (PROHIBITED/RESTRICTED/etc.)
  - [ ] Toggle collapse with arrow icon
  - [ ] Position: bottom-right corner, absolute positioning
- [ ] T044 [FE] Implement WebSocket real-time updates
  - [ ] Create custom hook: `useAssetLiveUpdates(tenantCode)`
  - [ ] Subscribe to `/topic/assets/live/{tenantCode}` using SockJS + STOMP
  - [ ] On event receive: update asset state in React Query cache
  - [ ] Use react-spring for smooth marker position animation
  - [ ] Add new markers for new assets (fade in animation)
  - [ ] Remove markers for inactive assets (fade out after 30 sec)
  - [ ] Handle reconnection on disconnect
- [ ] T045 [FE] Create `frontend/src/services/assetLocationService.ts`
  - [ ] `fetchLiveAssets(filters: AssetFilter)` - GET /api/tracking/assets/live
  - [ ] `fetchAssetById(assetId: string)` - GET /api/tracking/assets/live/{id}
  - [ ] `subscribeToLiveUpdates(tenantCode, callback)` - WebSocket helper
  - [ ] Error handling with axios interceptors
- [ ] T046 [FE] Add TypeScript interfaces
  - [ ] `LiveAsset` interface (matches AssetLocationDTO)
  - [ ] `AssetFilter` interface (category, status, zone, owner)
  - [ ] `ZoneBoundary` interface (id, name, type, geometry)
  - [ ] `AssetPositionUpdateEvent` interface (WebSocket payload)

### Frontend - Hotspot Analysis (US6)

- [ ] T047 [FE] Install Leaflet.heat plugin
  - [ ] `npm install leaflet.heat @types/leaflet.heat --save`
  - [ ] Verify compatibility with react-leaflet v4
  - [ ] Add to package.json dependencies
- [ ] T048 [FE] Create `frontend/src/pages/HotspotAnalysisPage.tsx`
  - [ ] Layout: Map (70% width) + Controls sidebar (30% width)
  - [ ] State: mode (Activity/Violation/Dwell), gridSize, timeRange
  - [ ] Fetch heatmap data based on selected mode using React Query
  - [ ] Toggle button: \"Asset View\" ↔ \"Heatmap View\"
  - [ ] Pass heatmap data to HeatmapView component
  - [ ] Handle mode/time range changes → refetch data
- [ ] T049 [FE] Create `frontend/src/components/Tracking/HeatmapView.tsx`
  - [ ] Leaflet map with base layer
  - [ ] Use Leaflet.heat to render heat layer overlay
  - [ ] Configure gradient: {0.0: 'blue', 0.25: 'green', 0.5: 'yellow', 0.75: 'orange', 1.0: 'red'}
  - [ ] Adjust intensity based on slider value prop (0-100 → 0-1)
  - [ ] Adjust radius based on grid size (10m=5px, 25m=10px, 50m=15px, 100m=20px)
  - [ ] Click cell → determine lat/lng → trigger hotspot detail modal
  - [ ] Overlay zone boundaries (optional toggle)
- [ ] T050 [FE] Create `frontend/src/components/Tracking/HeatmapControls.tsx`
  - [ ] Mode selector: Radio button group (Activity / Violations / Dwell)
  - [ ] Grid resolution dropdown: Select (10m / 25m / 50m / 100m)
  - [ ] Time range preset buttons: 1h, 24h, 7d, 30d
  - [ ] Custom date range picker (react-datepicker)
  - [ ] Intensity slider: Range input (0-100) with label
  - [ ] Auto-refresh toggle switch (refresh every 60 seconds when enabled)
  - [ ] \"Switch to Asset View\" button → navigate to AirsideMapPage
  - [ ] Export dropdown menu: PNG / CSV / PDF
  - [ ] Trigger export on selection
- [ ] T051 [FE] Create `frontend/src/components/Tracking/HotspotDetailModal.tsx`
  - [ ] Modal dialog (Headless UI Dialog)
  - [ ] Triggered on heatmap cell click, receives lat/lng and mode
  - [ ] Fetch hotspot details from GET /api/tracking/heatmap/hotspot
  - [ ] Display: Location (lat/long with copy button), Intensity value
  - [ ] Activity mode: Total movements, Unique assets, Avg speed
  - [ ] Violation mode: Total violations, Breakdown by severity (CRITICAL/HIGH/MEDIUM/LOW), Asset list
  - [ ] Dwell mode: Total dwell time, Avg dwell time, Asset list
  - [ ] Time distribution chart: Recharts BarChart (hourly breakdown)
  - [ ] \"View Assets\" button → switch to AirsideMapPage with location filter (bounding box)
  - [ ] \"View Violations\" button → navigate to ViolationReportPage with location filter
  - [ ] Close button, click outside to close
- [ ] T052 [FE] Create `frontend/src/components/Tracking/HeatmapLegend.tsx`
  - [ ] Color gradient bar (vertical, 200px height)
  - [ ] Intensity labels: Low (0) → High (100)
  - [ ] Current mode indicator: \"Showing: Activity Density\"
  - [ ] Grid size indicator: \"Grid: 10m\"
  - [ ] Time range display: \"Period: Last 24 hours\"
  - [ ] Position: bottom-left corner
- [ ] T053 [FE] Implement heatmap data fetching
  - [ ] Fetch on mode/time range/grid size change
  - [ ] Transform API response to Leaflet.heat format: [[lat, lng, intensity], ...]
  - [ ] Normalize intensity values to 0-1 scale (divide by max)
  - [ ] Cache previous results in React Query for quick mode switching
  - [ ] Handle loading state with skeleton/spinner
  - [ ] Handle empty data (show \"No data available\" message)
- [ ] T054 [FE] Implement export functionality
  - [ ] PNG: Use html2canvas to capture map div, download as image
  - [ ] CSV: Format heatmap data with headers (Latitude, Longitude, Intensity, Activity Count)
  - [ ] CSV: Use Papa Parse library for CSV generation
  - [ ] PDF: Generate summary report with jsPDF
  - [ ] PDF includes: Title, metadata (mode, time range), embedded map image, statistics table, top 10 hotspots table
  - [ ] Download with descriptive filename: `heatmap_{mode}_{date}.{ext}`
  - [ ] Show success toast notification on download
- [ ] T055 [FE] Create `frontend/src/services/heatmapService.ts`
  - [ ] `fetchActivityHeatmap(filters: HeatmapFilters)` - GET /api/tracking/heatmap/activity
  - [ ] `fetchViolationHeatmap(filters: HeatmapFilters)` - GET /api/tracking/heatmap/violations
  - [ ] `fetchDwellHeatmap(filters: HeatmapFilters)` - GET /api/tracking/heatmap/dwell
  - [ ] `fetchHotspotDetails(lat, lng, mode, dateRange)` - GET /api/tracking/heatmap/hotspot
  - [ ] `exportHeatmapData(data, format: 'png' | 'csv' | 'pdf')` - Trigger export
  - [ ] Error handling and retry logic
- [ ] T056 [FE] Add TypeScript interfaces
  - [ ] `HeatmapPoint` interface: {latitude, longitude, intensity, metadata}
  - [ ] `HotspotDetail` interface: {location, mode, activityCount, assets, violations, timeDistribution}
  - [ ] `HeatmapFilters` interface: {tenantCode, startDate, endDate, gridSize}
  - [ ] `HeatmapMode` enum: ACTIVITY | VIOLATION | DWELL
  - [ ] `HeatmapStatistics` interface: {totalCells, hotspotCells, maxIntensity, avgIntensity}

### Navigation & Integration

- [ ] T057 [FE] Update `frontend/src/components/Layout/MainLayout.tsx`
  - [ ] Add new menu section: \"Airside Operations\" (between Dashboard and Reports)
  - [ ] Add \"Live Asset Map\" menu item (icon: MapIcon, route: `/tracking/airside-map`)
  - [ ] Add \"Hotspot Analysis\" menu item (icon: FireIcon, route: `/tracking/hotspot-analysis`)
  - [ ] Show section for roles: ADMIN, GH, AIRPORT_USER
  - [ ] Highlight active route
- [ ] T058 [FE] Update `frontend/src/App.tsx`
  - [ ] Add route: `/tracking/airside-map` → AirsideMapPage
  - [ ] Add route: `/tracking/hotspot-analysis` → HotspotAnalysisPage
  - [ ] Wrap in ProtectedRoute with role check: ['ADMIN', 'GH', 'AIRPORT_USER']
  - [ ] Add lazy loading with React.lazy and Suspense
- [ ] T059 [FE] Cross-linking between pages
  - [ ] Asset map → Movement trail: \"View Trail\" button in AssetPopup
  - [ ] Hotspot analysis → Asset map: \"View Assets\" button in HotspotDetailModal
  - [ ] Hotspot analysis → Violation report: \"View Violations\" button in HotspotDetailModal
  - [ ] Asset map → Asset register: \"View in Register\" button in AssetPopup
  - [ ] Preserve filter state when navigating between pages (use URL query params)

### Testing & Quality

- [ ] T060 [TEST] Backend unit tests for new services
  - [ ] Test AssetLocationService.getAllLiveAssets() with filters
  - [ ] Test HeatmapService.getActivityHeatmap() grid calculations
  - [ ] Test heatmap intensity normalization (percentile-based)
  - [ ] Test grid size conversion (10m=0.0001°, etc.)
  - [ ] Mock repository calls with @MockBean
- [ ] T061 [TEST] Backend integration tests
  - [ ] Test GET /api/tracking/assets/live endpoint
  - [ ] Test heatmap endpoints with various grid sizes (10m, 25m, 50m, 100m)
  - [ ] Test tenant filtering (VIDP, LIRN, YBBN)
  - [ ] Test date range validation (reject ranges > 30 days)
  - [ ] Test WebSocket broadcasts with @SpringBootTest
  - [ ] Use @DirtiesContext to reset state between tests
- [ ] T062 [TEST] Frontend component tests (React Testing Library)
  - [ ] Test AssetMarker renders correct color for each category
  - [ ] Test AssetFilterPanel applies filters and updates count badge
  - [ ] Test HeatmapControls mode switching updates state
  - [ ] Test HotspotDetailModal displays correct data based on mode
  - [ ] Mock API calls with MSW (Mock Service Worker)
- [ ] T063 [TEST] Frontend integration tests
  - [ ] Test AirsideMapPage loads and displays markers
  - [ ] Test real-time marker updates (mock WebSocket events)
  - [ ] Test HeatmapView renders heatmap layer correctly
  - [ ] Test filter application updates visible markers
  - [ ] Test search functionality zooms to asset
- [ ] T064 [TEST] E2E tests (Playwright or Cypress)
  - [ ] E2E: View live asset map, click asset, view details
  - [ ] E2E: Apply category filter, verify marker count updates
  - [ ] E2E: Search for asset by ID, verify zoom to location
  - [ ] E2E: Switch to heatmap, change mode to Violations, click hotspot
  - [ ] E2E: Export heatmap as PNG, verify download
  - [ ] E2E: Navigate from hotspot to violation report
- [ ] T065 [TEST] Performance testing
  - [ ] Test map with 500+ assets (should render <3 seconds)
  - [ ] Test heatmap with 10,000+ data points (render <5 seconds)
  - [ ] Test WebSocket with rapid updates (100 updates/sec, no lag)
  - [ ] Monitor memory usage during 30-minute session
  - [ ] Test marker clustering performance at various zoom levels
  - [ ] Use Chrome DevTools Performance profiler

### Documentation

- [ ] T066 [DOC] Update spec.md with US5 and US6
- [ ] T067 [DOC] Update plan.md with Phase 2A details
- [ ] T068 [DOC] Update tasks.md with new Phase 2A tasks
- [ ] T069 [DOC] Create heatmap user guide
  - [ ] How to interpret heatmap colors (gradient explanation)
  - [ ] When to use each mode (Activity for congestion, Violation for security, Dwell for bottlenecks)
  - [ ] How to identify problematic areas (look for red hotspots)
  - [ ] How to drill down into hotspot details
  - [ ] Example use cases with screenshots
- [ ] T070 [DOC] Create universal map user guide
  - [ ] How to filter assets (category, status, zone)
  - [ ] How to search for specific asset
  - [ ] Understanding marker colors and states
  - [ ] How to track asset in real-time
  - [ ] How to view movement history from popup
- [ ] T071 [DOC] Add screenshots to README
  - [ ] Universal asset map screenshot (with markers and zones)
  - [ ] Heatmap analysis screenshot (activity mode)
  - [ ] Hotspot detail modal screenshot
  - [ ] Filter panel screenshot

**Acceptance Criteria:**
- ✅ Universal asset map displays all assets in real-time with <3 sec load time
- ✅ Asset markers update smoothly via WebSocket (<1 sec latency)
- ✅ Filters work correctly and update marker count
- ✅ Marker clustering prevents overlap at low zoom levels
- ✅ Heatmap displays correctly for all 3 modes (Activity, Violation, Dwell)
- ✅ Hotspot click shows detailed breakdown with time distribution chart
- ✅ Export functionality works for PNG/CSV/PDF formats
- ✅ Performance acceptable with 500+ assets and 10,000+ heatmap points
- ✅ Demo Flow requirements 1 (Universal Airside Visibility) and 3 (Hotspot Identification) fully satisfied (100% coverage)
- ✅ No regressions in existing features
- ✅ All tests pass (unit, integration, E2E)

**Estimated Duration**: 3-4 days

---

## Phase 2: Backend Domain Models

- [ ] T024 [BE] Create `backend/src/main/java/com/utam/tracking/domain/` package
- [ ] T025 [BE] Create `RestrictedZone.java` entity
  - [ ] Map to `restricted_zones` table
  - [ ] Add `@Entity` and `@Table` annotations
  - [ ] Add Polygon geometry field with Hibernate Spatial
  - [ ] Add authorizedAssetCategories array mapping
  - [ ] Add all getters/setters
  - [ ] Add Lombok annotations (@Data, @Entity)
- [ ] T026 [BE] Create `AssetMovementTrail.java` entity
  - [ ] Map to `asset_movement_trail` table
  - [ ] Add Point geometry field
  - [ ] Add metadata JSONB field
  - [ ] Add all fields from schema
- [ ] T027 [BE] Create `ZoneViolation.java` entity
  - [ ] Map to `zone_violations` table
  - [ ] Add Point geometry for entry location
  - [ ] Add acknowledged fields
  - [ ] Add severity enum
- [ ] T028 [BE] Create `MovementDiscrepancy.java` entity
  - [ ] Map to `movement_discrepancies` table
  - [ ] Add expected/actual location fields
  - [ ] Add deviation calculation fields
- [ ] T029 [BE] Create `AssetLocationRegister.java` entity
  - [ ] Map to `asset_location_register` table
  - [ ] Add current location Point field
  - [ ] Add zone status fields
- [ ] T030 [BE] Configure Hibernate Spatial in `application.properties`
- [ ] T031 [BE] Add hibernate-spatial dependency to `pom.xml`
- [ ] T032 [BE] Test entity persistence with sample data

**Acceptance Criteria:**
- All entities compile without errors
- JPA mappings validated
- Spatial types (Point, Polygon) working
- Sample entities can be saved/retrieved

---

## Phase 3: Backend Repositories

- [ ] T033 [BE] Create `backend/src/main/java/com/utam/tracking/repository/` package
- [ ] T034 [BE] Create `RestrictedZoneRepository` extends JpaRepository
  - [ ] Add `findByTenantCode(String tenantCode)`
  - [ ] Add `findByTenantCodeAndIsActive(String tenantCode, Boolean isActive)`
  - [ ] Add custom query `findZonesContainingPoint(Point location, String tenantCode)`
- [ ] T035 [BE] Create `AssetMovementTrailRepository`
  - [ ] Add `findByAssetIdentifierAndTimestampBetween(String assetId, ZonedDateTime start, ZonedDateTime end)`
  - [ ] Add `findByTenantCodeAndTimestampAfter(String tenantCode, ZonedDateTime after)`
  - [ ] Add pagination support
- [ ] T036 [BE] Create `ZoneViolationRepository`
  - [ ] Add `findByTenantCodeAndTimestampBetween()` with filters
  - [ ] Add `findByAcknowledged(Boolean acknowledged, Pageable pageable)`
  - [ ] Add `countByTenantCodeAndSeverity(String tenantCode, String severity)`
- [ ] T037 [BE] Create `MovementDiscrepancyRepository`
  - [ ] Add `findByTenantCodeAndTimestampBetween()` with filters
  - [ ] Add `findByDiscrepancyType(String type, Pageable pageable)`
- [ ] T038 [BE] Create `AssetLocationRegisterRepository`
  - [ ] Add `findByAssetId(UUID assetId)`
  - [ ] Add `findByTenantCodeAndIsInRestrictedZone(String tenantCode, Boolean inZone)`
- [ ] T039 [BE] Write repository unit tests
- [ ] T040 [BE] Test pagination and sorting

**Acceptance Criteria:**
- All repositories tested with sample data
- Custom queries return expected results
- Pagination working correctly

---

## Phase 4: Movement Trail Ingestion Service

- [ ] T041 [BE] Create `backend/src/main/java/com/utam/tracking/service/` package
- [ ] T042 [BE] Create `MovementTrailIngestionService.java`
  - [ ] Add `@Service` annotation
  - [ ] Add `@Scheduled(fixedRate = 5000)` method
  - [ ] Inject VehicleRepository, AssetRepository
  - [ ] Inject all tracking repositories
- [ ] T043 [BE] Implement `ingestVehiclePositions()` method
  - [ ] Query vehicles table for recent positions (last 10 seconds)
  - [ ] Loop through each vehicle position
  - [ ] Match vehicle_id to asset using qr_id
  - [ ] Create Point from lat/long
  - [ ] Call zone detection logic
  - [ ] Create AssetMovementTrail record
  - [ ] Update AssetLocationRegister
- [ ] T044 [BE] Implement zone detection logic
  - [ ] Query restricted_zones using ST_Contains
  - [ ] Check if asset is authorized for zone
  - [ ] Return zone info + authorization status
- [ ] T045 [BE] Implement violation detection logic
  - [ ] If in restricted zone AND unauthorized
  - [ ] Check if violation already exists (within last 5 min)
  - [ ] If new violation: Create ZoneViolation record
  - [ ] Calculate severity based on zone type
  - [ ] Broadcast WebSocket event
- [ ] T046 [BE] Implement discrepancy detection logic
  - [ ] Check UNEXPECTED_MOVEMENT (status vs position change)
  - [ ] Check LOCATION_MISMATCH (register vs GPS)
  - [ ] Check SPEED_ANOMALY (speed vs category max)
  - [ ] Check MISSING_TRACKING (last update >10 min)
  - [ ] Check DUPLICATE_SIGNAL (same asset, different locations)
  - [ ] Create MovementDiscrepancy if detected
  - [ ] Broadcast WebSocket event
- [ ] T047 [BE] Add error handling and logging
  - [ ] Try-catch around scheduled task
  - [ ] Log each ingestion cycle (INFO level)
  - [ ] Log violations (WARN level)
  - [ ] Log errors (ERROR level with stack trace)
- [ ] T048 [BE] Add monitoring metrics
  - [ ] Counter: positions ingested
  - [ ] Counter: violations detected
  - [ ] Counter: discrepancies detected
  - [ ] Gauge: ingestion latency
- [ ] T049 [BE] Test with simulated vehicle data
- [ ] T050 [BE] Test performance with 500+ assets

**Acceptance Criteria:**
- Scheduled task runs every 5 seconds
- Trail data captured successfully
- Violations detected within 5 seconds of zone entry
- Discrepancies detected correctly
- CPU usage <10%
- No memory leaks

---

## Phase 5: Backend Business Services

- [ ] T051 [BE] Create `ZoneViolationService.java`
  - [ ] Implement `getViolations(filters, pageable)` method
  - [ ] Implement `acknowledgeViolation(violationId, userId, notes)` method
  - [ ] Implement `getViolationStatistics(tenantCode, dateRange)` method
  - [ ] Add tenant isolation checks
  - [ ] Add authorization checks
- [ ] T052 [BE] Create `MovementDiscrepancyService.java`
  - [ ] Implement `getDiscrepancies(filters, pageable)` method
  - [ ] Implement `acknowledgeDiscrepancy(discrepancyId, userId, notes)` method
  - [ ] Implement `getDiscrepancyStatistics(tenantCode, dateRange)` method
- [ ] T053 [BE] Create `MovementTrailService.java`
  - [ ] Implement `getTrail(assetId, startDate, endDate)` method
  - [ ] Implement `calculateZoneEntries(trailPoints, zones)` method
  - [ ] Implement `calculateTrailSummary(trailPoints)` method
  - [ ] Implement `exportTrail(assetId, dateRange, format)` method
- [ ] T054 [BE] Create `RestrictedZoneService.java`
  - [ ] Implement `getZones(tenantCode)` method
  - [ ] Implement `createZone(zoneDTO)` method (ADMIN only)
  - [ ] Implement `updateZone(zoneId, zoneDTO)` method
  - [ ] Implement `deleteZone(zoneId)` method
  - [ ] Add geometry validation
- [ ] T055 [BE] Create DTOs package `backend/src/main/java/com/utam/tracking/dto/`
  - [ ] Create `ZoneViolationDTO.java`
  - [ ] Create `MovementDiscrepancyDTO.java`
  - [ ] Create `MovementTrailDTO.java`
  - [ ] Create `MovementTrailPointDTO.java`
  - [ ] Create `ZoneEntryDTO.java`
  - [ ] Create `TrailSummaryDTO.java`
  - [ ] Create `RestrictedZoneDTO.java`
  - [ ] Create `AcknowledgeRequestDTO.java`
- [ ] T056 [BE] Create MapStruct mappers
  - [ ] Create `ZoneViolationMapper.java`
  - [ ] Create `DiscrepancyMapper.java`
  - [ ] Create `TrailMapper.java`
  - [ ] Create `ZoneMapper.java`
- [ ] T057 [BE] Write service unit tests (>80% coverage)

**Acceptance Criteria:**
- All services tested
- DTOs properly mapped
- Business logic validated
- Unit test coverage >80%

---

## Phase 6: Backend REST Controllers

- [ ] T058 [BE] Create `backend/src/main/java/com/utam/tracking/controller/` package
- [ ] T059 [BE] Create `ZoneViolationController.java`
  - [ ] Add `@RestController` and `@RequestMapping("/api/tracking/zones/violations")`
  - [ ] Implement `GET /` endpoint with filters, pagination, sorting
  - [ ] Implement `POST /{id}/acknowledge` endpoint
  - [ ] Add `@PreAuthorize` for role checks (GH, ADMIN)
  - [ ] Add tenant isolation from JWT token
- [ ] T060 [BE] Create `MovementDiscrepancyController.java`
  - [ ] Implement `GET /api/tracking/discrepancies`
  - [ ] Implement `POST /api/tracking/discrepancies/{id}/acknowledge`
  - [ ] Add authorization checks
- [ ] T061 [BE] Create `MovementTrailController.java`
  - [ ] Implement `GET /api/tracking/trail/{assetId}`
  - [ ] Implement `GET /api/tracking/trail/{assetId}/export`
  - [ ] Add date range validation (max 30 days)
  - [ ] Support CSV/JSON export formats
- [ ] T062 [BE] Create `RestrictedZoneController.java`
  - [ ] Implement `GET /api/tracking/zones`
  - [ ] Implement `POST /api/tracking/zones` (ADMIN only)
  - [ ] Implement `PUT /api/tracking/zones/{id}` (ADMIN only)
  - [ ] Implement `DELETE /api/tracking/zones/{id}` (ADMIN only)
- [ ] T063 [BE] Add global exception handler for tracking exceptions
- [ ] T064 [BE] Add Swagger/OpenAPI documentation annotations
- [ ] T065 [BE] Test all endpoints with Postman/REST client
- [ ] T066 [BE] Test authorization and tenant isolation

**Acceptance Criteria:**
- All endpoints return correct data
- Authorization enforced (403 for unauthorized)
- Tenant isolation working (users see only own data)
- API documentation generated
- Postman collection created

---

## Phase 7: WebSocket Integration

- [ ] T067 [BE] Configure WebSocket in `WebSocketConfig.java`
  - [ ] Add tracking topics: `/topic/violations/{tenantCode}`
  - [ ] Add tracking topics: `/topic/discrepancies/{tenantCode}`
- [ ] T068 [BE] Create `TrackingWebSocketService.java`
  - [ ] Method `broadcastViolation(ZoneViolation violation)`
  - [ ] Method `broadcastDiscrepancy(MovementDiscrepancy discrepancy)`
  - [ ] Filter by tenant before broadcasting
- [ ] T069 [BE] Integrate WebSocket broadcasts in ingestion service
  - [ ] Call after creating violation
  - [ ] Call after creating discrepancy
- [ ] T070 [BE] Test WebSocket events with WebSocket client

**Acceptance Criteria:**
- WebSocket events broadcast correctly
- Tenant filtering working
- Events received by frontend clients

---

## Phase 8: Frontend Services

- [ ] T071 [FE] Create `frontend/src/services/trackingService.ts`
- [ ] T072 [FE] Add TypeScript interfaces in `frontend/src/types/tracking.ts`
  - [ ] `ZoneViolation` interface
  - [ ] `MovementDiscrepancy` interface
  - [ ] `MovementTrail` interface
  - [ ] `MovementTrailPoint` interface
  - [ ] `ZoneEntry` interface
  - [ ] `RestrictedZone` interface
  - [ ] `ViolationFilters` interface
  - [ ] `DiscrepancyFilters` interface
- [ ] T073 [FE] Implement `fetchZoneViolations(filters, page, size)` method
- [ ] T074 [FE] Implement `acknowledgeViolation(id, notes)` method
- [ ] T075 [FE] Implement `fetchMovementDiscrepancies(filters, page, size)` method
- [ ] T076 [FE] Implement `acknowledgeDiscrepancy(id, notes)` method
- [ ] T077 [FE] Implement `fetchMovementTrail(assetId, startDate, endDate)` method
- [ ] T078 [FE] Implement `fetchRestrictedZones(tenantCode)` method
- [ ] T079 [FE] Implement `exportViolations(filters, format)` method
- [ ] T080 [FE] Implement `exportDiscrepancies(filters, format)` method
- [ ] T081 [FE] Implement `exportTrail(assetId, dateRange)` method
- [ ] T082 [FE] Update `WebSocketService.ts` for tracking events
  - [ ] Add `subscribeToViolations(tenantCode, callback)` method
  - [ ] Add `subscribeToDiscrepancies(tenantCode, callback)` method
- [ ] T083 [FE] Test all service methods with backend API

**Acceptance Criteria:**
- All API calls working
- TypeScript types defined
- Error handling implemented
- WebSocket subscriptions working

---

## Phase 9: Frontend - Restricted Zone Violations Report

- [ ] T084 [FE] Create `frontend/src/pages/RestrictedZoneReportPage.tsx`
- [ ] T085 [FE] Create `frontend/src/components/Tracking/ViolationTable.tsx`
  - [ ] Columns: Asset, Zone, Entry Time, Duration, Severity, Status, Actions
  - [ ] Severity color coding (CRITICAL=red, HIGH=orange, etc.)
  - [ ] Sortable columns
  - [ ] Acknowledge button in actions
- [ ] T086 [FE] Create `frontend/src/components/Tracking/ViolationFilters.tsx`
  - [ ] Date range picker
  - [ ] Zone type dropdown
  - [ ] Asset category dropdown
  - [ ] Severity dropdown
  - [ ] Acknowledged toggle
  - [ ] Apply/Reset buttons
- [ ] T087 [FE] Create `frontend/src/components/Tracking/AcknowledgeViolationModal.tsx`
  - [ ] Form with resolution notes textarea
  - [ ] Submit/Cancel buttons
  - [ ] Success/Error toast notifications
- [ ] T088 [FE] Implement pagination controls
- [ ] T089 [FE] Implement real-time updates using WebSocket
  - [ ] Subscribe to violations topic on mount
  - [ ] Add new violations to table
  - [ ] Show toast notification for CRITICAL violations
- [ ] T090 [FE] Add export button (PDF/Excel)
- [ ] T091 [FE] Add loading states (skeleton/spinner)
- [ ] T092 [FE] Add empty state when no violations
- [ ] T093 [FE] Add error boundary
- [ ] T094 [FE] Style with Tailwind CSS
- [ ] T095 [FE] Test responsive design (mobile, tablet, desktop)

**Acceptance Criteria:**
- Table displays violations correctly
- Filters update results
- Sorting works on all columns
- Pagination working
- Acknowledge modal submits successfully
- Real-time updates appear in table
- Export downloads file
- Responsive on all screen sizes

---

## Phase 10: Frontend - Movement Discrepancy Report

- [ ] T096 [FE] Create `frontend/src/pages/MovementDiscrepancyReportPage.tsx`
- [ ] T097 [FE] Create `frontend/src/components/Tracking/DiscrepancyTable.tsx`
  - [ ] Columns: Asset, Type, Expected, Actual, Deviation (m), Severity, Status, Actions
  - [ ] Color-coded severity
  - [ ] Sortable columns
  - [ ] Acknowledge button
- [ ] T098 [FE] Create `frontend/src/components/Tracking/DiscrepancyFilters.tsx`
  - [ ] Similar to ViolationFilters
  - [ ] Add discrepancy type dropdown
- [ ] T099 [FE] Create `frontend/src/components/Tracking/DiscrepancyMapView.tsx`
  - [ ] Use Leaflet for map
  - [ ] Show expected location (green marker A)
  - [ ] Show actual location (red marker B)
  - [ ] Draw line between A and B with distance label
  - [ ] Display discrepancy details in popup
- [ ] T100 [FE] Create `frontend/src/components/Tracking/AcknowledgeDiscrepancyModal.tsx`
- [ ] T101 [FE] Implement table/map view toggle
- [ ] T102 [FE] Add real-time updates
- [ ] T103 [FE] Add export button
- [ ] T104 [FE] Add loading/empty/error states
- [ ] T105 [FE] Style and test responsiveness

**Acceptance Criteria:**
- Table view displays discrepancies
- Map view shows expected vs actual locations
- Toggle between table/map works
- Real-time updates working
- Export functional

---

## Phase 11: Frontend - Movement Trail Visualization

- [ ] T106 [FE] Create `frontend/src/pages/MovementTrailPage.tsx`
- [ ] T107 [FE] Create `frontend/src/components/Tracking/AssetSelector.tsx`
  - [ ] Autocomplete dropdown
  - [ ] Search by asset ID or name
  - [ ] Show asset category
- [ ] T108 [FE] Create `frontend/src/components/Tracking/TrailMap.tsx`
  - [ ] Leaflet map component
  - [ ] Draw polyline for trail path
  - [ ] Color segments: green (normal), orange (controlled), red (restricted)
  - [ ] Add markers for zone entry/exit points
  - [ ] Add current position marker
  - [ ] Show zone boundaries as polygons
  - [ ] Add info popups on markers
- [ ] T109 [FE] Create `frontend/src/components/Tracking/TrailTimeline.tsx`
  - [ ] Timeline slider showing time range
  - [ ] Drag to scrub through trail
  - [ ] Highlight current position on timeline
  - [ ] Show zone entries as markers on timeline
- [ ] T110 [FE] Create `frontend/src/components/Tracking/TrailPlaybackControls.tsx`
  - [ ] Play/Pause button
  - [ ] Speed selector (1x, 5x, 10x, 30x, 60x)
  - [ ] Current time display
  - [ ] Progress indicator
- [ ] T111 [FE] Create `frontend/src/components/Tracking/TrailInfoPanel.tsx`
  - [ ] Display current position details
  - [ ] Show: timestamp, lat/long, zone, speed, status
  - [ ] Show trail summary statistics
  - [ ] Show zone dwell times
- [ ] T112 [FE] Implement playback animation
  - [ ] Use requestAnimationFrame for smooth animation
  - [ ] Move marker along path
  - [ ] Update info panel in real-time
  - [ ] Pause at zone entry points (optional)
- [ ] T113 [FE] Create `frontend/src/components/Tracking/TrailDateRangePicker.tsx`
  - [ ] Date range selector (max 30 days)
  - [ ] Quick options: Last 24h, Last 7 days, etc.
- [ ] T114 [FE] Add export trail data button (CSV)
- [ ] T115 [FE] Add loading states while fetching trail
- [ ] T116 [FE] Handle large trails (>1000 points) with optimization
  - [ ] Simplify polyline for rendering
  - [ ] Lazy load timeline
- [ ] T117 [FE] Style and test responsiveness

**Acceptance Criteria:**
- Asset search returns results
- Map displays trail correctly
- Timeline scrubber works smoothly
- Playback animation is smooth (60fps)
- Color coding reflects zone types
- Zone markers clickable with info
- Export downloads CSV file
- Performs well with 1000+ points

---

## Phase 12: Navigation & Routes

- [ ] T118 [FE] Update `frontend/src/components/Layout/MainLayout.tsx`
  - [ ] Add "Security Reports" section in sidebar
  - [ ] Add "Zone Violations" menu item with icon
  - [ ] Add "Movement Discrepancies" menu item with icon
  - [ ] Add "Movement Trail" menu item with icon
  - [ ] Show only for roles: GH, ADMIN
- [ ] T119 [FE] Update `frontend/src/App.tsx`
  - [ ] Add route: `/tracking/violations`
  - [ ] Add route: `/tracking/discrepancies`
  - [ ] Add route: `/tracking/trail`
  - [ ] Wrap routes in ProtectedRoute with role check
- [ ] T120 [FE] Add icons to navigation (use lucide-react)
  - [ ] AlertOctagon for Violations
  - [ ] AlertTriangle for Discrepancies
  - [ ] Route for Movement Trail
- [ ] T121 [FE] Test navigation for all roles
  - [ ] ADMIN: All 3 pages accessible
  - [ ] GH: All 3 pages accessible
  - [ ] AIRPORT_USER: 403 Forbidden

**Acceptance Criteria:**
- Menu items visible for authorized roles
- Routes navigate correctly
- Unauthorized users see 403 error
- Icons display correctly

---

## Phase 13: Backend Testing

- [ ] T122 [TEST] Create unit tests for domain models
  - [ ] Test entity constructors
  - [ ] Test getters/setters
  - [ ] Test JPA annotations
- [ ] T123 [TEST] Create unit tests for repositories
  - [ ] Test custom queries
  - [ ] Test spatial queries
  - [ ] Test pagination
- [ ] T124 [TEST] Create unit tests for services
  - [ ] Test ZoneViolationService methods
  - [ ] Test MovementDiscrepancyService methods
  - [ ] Test MovementTrailService methods
  - [ ] Test detection algorithms
  - [ ] Mock dependencies
- [ ] T125 [TEST] Create integration tests for controllers
  - [ ] Test GET endpoints with filters
  - [ ] Test POST acknowledge endpoints
  - [ ] Test authorization
  - [ ] Test tenant isolation
  - [ ] Use @SpringBootTest and TestRestTemplate
- [ ] T126 [TEST] Create integration test for ingestion service
  - [ ] Mock vehicle data
  - [ ] Verify trail records created
  - [ ] Verify violations detected
  - [ ] Verify discrepancies detected
- [ ] T127 [TEST] Create WebSocket integration tests
  - [ ] Test event broadcasting
  - [ ] Test tenant filtering
- [ ] T128 [TEST] Run all tests and verify >80% coverage

**Acceptance Criteria:**
- Unit test coverage >80%
- Integration tests pass
- All test scenarios covered

---

## Phase 14: Frontend Testing

- [ ] T129 [TEST] Create component tests (React Testing Library)
  - [ ] Test ViolationTable rendering
  - [ ] Test ViolationFilters state changes
  - [ ] Test AcknowledgeModal submission
  - [ ] Test DiscrepancyTable rendering
  - [ ] Test DiscrepancyMapView markers
  - [ ] Test TrailMap polyline rendering
  - [ ] Test TrailTimeline scrubbing
  - [ ] Test PlaybackControls interactions
- [ ] T130 [TEST] Create service tests
  - [ ] Mock API responses
  - [ ] Test error handling
  - [ ] Test data transformation
- [ ] T131 [TEST] Create E2E tests (Playwright or Cypress)
  - [ ] E2E: View zone violations report
  - [ ] E2E: Filter violations by severity
  - [ ] E2E: Acknowledge violation
  - [ ] E2E: View movement trail
  - [ ] E2E: Playback trail animation
  - [ ] E2E: Export reports
- [ ] T132 [TEST] Run all frontend tests

**Acceptance Criteria:**
- Component tests pass
- E2E tests cover main workflows
- Coverage >70%

---

## Phase 15: Performance & Load Testing

- [ ] T133 [TEST] Load test ingestion service
  - [ ] Simulate 500 assets sending positions every 5 sec
  - [ ] Monitor CPU/memory usage
  - [ ] Verify no data loss
  - [ ] Verify <10 second latency
- [ ] T134 [TEST] Load test API endpoints
  - [ ] 100 concurrent users
  - [ ] Query violations endpoint
  - [ ] Verify response time <3 seconds
  - [ ] Verify no errors
- [ ] T135 [TEST] Test map rendering performance
  - [ ] Load trail with 1000+ points
  - [ ] Verify renders in <2 seconds
  - [ ] Verify smooth panning/zooming
- [ ] T136 [TEST] Test database query performance
  - [ ] Insert 100K trail records
  - [ ] Run spatial queries
  - [ ] Verify <100ms response time
  - [ ] Check execution plans
- [ ] T137 [TEST] Optimize slow queries
  - [ ] Add missing indexes
  - [ ] Refactor complex queries
  - [ ] Use EXPLAIN ANALYZE

**Acceptance Criteria:**
- All performance SLAs met
- No bottlenecks identified
- System stable under load

---

## Phase 16: Documentation

- [ ] T138 [DOC] Create user guide
  - [ ] How to view zone violations
  - [ ] How to acknowledge violations
  - [ ] How to view movement trail
  - [ ] How to interpret discrepancies
- [ ] T139 [DOC] Create admin configuration guide
  - [ ] How to create restricted zones
  - [ ] How to authorize assets for zones
  - [ ] How to configure detection thresholds
- [ ] T140 [DOC] Update API documentation
  - [ ] Generate Swagger UI
  - [ ] Add example requests/responses
  - [ ] Add authentication notes
- [ ] T141 [DOC] Create data model documentation (data-model.md)
  - [ ] ERD diagram
  - [ ] Table descriptions
  - [ ] Relationship explanations
- [ ] T142 [DOC] Update main README.md
  - [ ] Add feature description
  - [ ] Add screenshots
  - [ ] Add setup instructions
- [ ] T143 [DOC] Create demo data script
  - [ ] Script to generate sample violations
  - [ ] Script to generate sample discrepancies
  - [ ] Script to generate sample trail data
- [ ] T144 [DOC] Record demo video (5-10 minutes)
  - [ ] Show violations report
  - [ ] Show acknowledging a violation
  - [ ] Show movement trail playback
  - [ ] Show real-time updates

**Acceptance Criteria:**
- All documentation complete
- Screenshots added
- Demo video recorded

---

## Phase 17: Deployment & Verification

- [ ] T145 [P] Apply database migration to dev environment
  - [ ] Run 06-asset-tracking-security.sql
  - [ ] Verify all tables created
  - [ ] Seed restricted zones
- [ ] T146 [P] Deploy backend to dev
  - [ ] Build JAR: `mvn clean package`
  - [ ] Deploy to server
  - [ ] Verify ingestion service started
  - [ ] Check logs for errors
- [ ] T147 [P] Deploy frontend to dev
  - [ ] Build: `npm run build`
  - [ ] Deploy to nginx
  - [ ] Verify all routes accessible
- [ ] T148 [P] End-to-end smoke test
  - [ ] Verify trail data being captured
  - [ ] Trigger a test violation
  - [ ] View violation in report
  - [ ] Acknowledge violation
  - [ ] View movement trail
  - [ ] Test playback
- [ ] T149 [P] Monitor for 24 hours
  - [ ] Check CPU/memory usage
  - [ ] Check error logs
  - [ ] Verify no crashes
  - [ ] Verify data accuracy
- [ ] T150 [P] Fix any deployment issues
- [ ] T151 [P] Get stakeholder approval for production

**Acceptance Criteria:**
- Dev environment fully functional
- No critical errors
- Performance acceptable
- Stakeholders approve for production

---

## Phase 18: Production Deployment

- [ ] T152 [P] Create production deployment plan
  - [ ] Backup strategy
  - [ ] Rollback plan
  - [ ] Maintenance window schedule
- [ ] T153 [P] Apply database migration to production
  - [ ] Backup database first
  - [ ] Run migration during maintenance window
  - [ ] Verify success
- [ ] T154 [P] Deploy backend to production
  - [ ] Blue-green deployment
  - [ ] Verify health check
  - [ ] Monitor logs
- [ ] T155 [P] Deploy frontend to production
  - [ ] Update API endpoints
  - [ ] Deploy to CDN
  - [ ] Verify static assets
- [ ] T156 [P] Production smoke test
  - [ ] Test all 3 report pages
  - [ ] Test real-time updates
  - [ ] Test exports
- [ ] T157 [P] Enable monitoring and alerts
  - [ ] Configure CPU/memory alerts
  - [ ] Configure error rate alerts
  - [ ] Configure ingestion alerts
- [ ] T158 [P] Announce feature to users
  - [ ] Send email notification
  - [ ] Conduct training session
  - [ ] Share user guide

**Acceptance Criteria:**
- Production deployment successful
- All features working
- Monitoring enabled
- Users notified

---

## Post-Launch Tasks

- [ ] T159 [P] Monitor usage metrics
  - [ ] Track active users
  - [ ] Track reports viewed
  - [ ] Track violations detected
- [ ] T160 [P] Gather user feedback
  - [ ] Survey GH managers
  - [ ] Collect feature requests
  - [ ] Identify pain points
- [ ] T161 [P] Optimize based on feedback
  - [ ] Performance improvements
  - [ ] UX enhancements
  - [ ] Additional filters
- [ ] T162 [P] Plan Phase 2 enhancements
  - [ ] ML-based anomaly detection
  - [ ] Mobile app
  - [ ] Advanced analytics

---

## Summary

**Total Tasks**: 162  
**Completed**: 3  
**Remaining**: 159  
**Estimated Completion**: ~20 days

**Current Phase**: Phase 0 - Foundation  
**Next Phase**: Phase 1 - Database Layer
