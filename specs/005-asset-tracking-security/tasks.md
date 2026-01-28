# Implementation Tasks: Asset Tracking & Security Module

**Feature ID**: 005  
**Created**: 2026-01-28  
**Status**: In Progress  
**Estimated Total Effort**: 18-24 days

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
- [ ] T004 [DOC] Create tasks.md (this file)
- [ ] T005 [DOC] Create data-model.md with ERD and schema documentation

---

## Phase 1: Database Layer

- [ ] T006 [DB] Create 06-asset-tracking-security.sql migration file
- [ ] T007 [DB] Create `restricted_zones` table with PostGIS POLYGON
- [ ] T008 [DB] Create `asset_movement_trail` hypertable
- [ ] T009 [DB] Create `zone_violations` hypertable
- [ ] T010 [DB] Create `movement_discrepancies` hypertable
- [ ] T011 [DB] Create `asset_location_register` table
- [ ] T012 [DB] Add spatial indexes (GIST) for all geometry columns
- [ ] T013 [DB] Add time-series indexes for hypertables
- [ ] T014 [DB] Create `zone_violations_hourly` continuous aggregate
- [ ] T015 [DB] Create `movement_discrepancies_daily` continuous aggregate
- [ ] T016 [DB] Seed restricted zones for VIDP (4 zones)
- [ ] T017 [DB] Seed restricted zones for LIRN (2 zones)
- [ ] T018 [DB] Seed restricted zones for YBBN (2 zones)
- [ ] T019 [DB] Create helper function `get_asset_id_from_vehicle()`
- [ ] T020 [DB] Test spatial queries (ST_Contains, ST_Distance)
- [ ] T021 [DB] Verify TimescaleDB compression policies
- [ ] T022 [DB] Apply migration to local database
- [ ] T023 [DB] Verify all tables created successfully

**Acceptance Criteria:**
- All tables exist with correct schema
- Spatial queries execute in <100ms
- Sample restricted zones inserted
- Continuous aggregates configured

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
