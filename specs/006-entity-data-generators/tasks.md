# Tasks: Entity Data Generators

**Input**: Design documents from `/specs/006-entity-data-generators/`  
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/ ✅

## Format: `[ID] [P?] [Story?] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2)
- Include exact file paths in descriptions

## Path Conventions

- **Backend**: `backend/src/main/java/com/utam/`
- **Frontend**: `frontend/src/`
- **Tests**: `backend/src/test/java/com/utam/`, `frontend/tests/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization, configuration, and base classes

- [X] T001 Create SimulationConfig with retention, rates, and tenant settings in `backend/src/main/java/com/utam/simulation/config/SimulationConfig.java`
- [X] T002 Create BaseDataGenerator abstract class with common logic in `backend/src/main/java/com/utam/simulation/core/BaseDataGenerator.java`
- [X] T003 [P] Create GeneratorOrchestrator to coordinate all generators in `backend/src/main/java/com/utam/simulation/core/GeneratorOrchestrator.java`
- [X] T004 [P] Create SimulationHealthIndicator for actuator health endpoint in `backend/src/main/java/com/utam/monitoring/SimulationHealthIndicator.java`
- [X] T005 [P] Add Micrometer metrics registry for generator metrics in `backend/src/main/java/com/utam/simulation/config/SimulationMetrics.java`
- [X] T006 [P] Create DataRetentionService for 90-day cleanup in `backend/src/main/java/com/utam/simulation/retention/DataRetentionService.java`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Reference data entities and generators that ALL user stories depend on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### Reference Data Entities

- [X] T007 Create Stand entity and repository in `backend/src/main/java/com/utam/entity/Stand.java` and `backend/src/main/java/com/utam/repository/StandRepository.java`
- [X] T008 [P] Create Depot entity and repository in `backend/src/main/java/com/utam/entity/Depot.java` and `backend/src/main/java/com/utam/repository/DepotRepository.java`
- [X] T009 [P] Create VehicleType entity and repository in `backend/src/main/java/com/utam/entity/VehicleType.java` and `backend/src/main/java/com/utam/repository/VehicleTypeRepository.java`
- [X] T010 [P] Create AirportBoundary entity and repository in `backend/src/main/java/com/utam/entity/AirportBoundary.java` and `backend/src/main/java/com/utam/repository/AirportBoundaryRepository.java`

### Reference Data Seed Files

- [X] T011 [P] Create VIDP reference data seed file in `backend/src/main/resources/data/vidp.json` (stands, depots, boundary)
- [X] T012 [P] Create YBBN reference data seed file in `backend/src/main/resources/data/ybbn.json` (stands, depots, boundary)

### Reference Data Generators

- [X] T013 Create StandDataGenerator in `backend/src/main/java/com/utam/simulation/reference/StandDataGenerator.java`
- [X] T014 [P] Create DepotDataGenerator in `backend/src/main/java/com/utam/simulation/reference/DepotDataGenerator.java`
- [X] T015 [P] Create VehicleTypeDataGenerator in `backend/src/main/java/com/utam/simulation/reference/VehicleTypeDataGenerator.java`
- [X] T016 [P] Create BoundaryDataGenerator in `backend/src/main/java/com/utam/simulation/reference/BoundaryDataGenerator.java`
- [X] T017 Create BoundaryValidator service in `backend/src/main/java/com/utam/simulation/security/BoundaryValidator.java`

**Checkpoint**: Foundation ready - all reference data in place, user story implementation can now begin

---

## Phase 3: User Story 1 - Generate Comprehensive Demo Data (Priority: P1) 🎯 MVP

**Goal**: Generate realistic sample data for all entities so all screens display meaningful content

**Independent Test**: Run data generators and verify Map, Turnaround, Asset Management pages show populated data

### Implementation for User Story 1

- [X] T018 [P] [US1] Enhance FlightDataGenerator with arrivals/departures in `backend/src/main/java/com/utam/simulation/flight/FlightDataGenerator.java`
- [X] T019 [P] [US1] Create FlightTrajectoryCalculator for realistic movement in `backend/src/main/java/com/utam/simulation/flight/FlightTrajectoryCalculator.java`
- [X] T020 [P] [US1] Enhance VehicleDataGenerator with 15 GSE types in `backend/src/main/java/com/utam/simulation/vehicle/VehicleDataGenerator.java`
- [X] T021 [P] [US1] Create AssetDataGenerator for equipment/tools in `backend/src/main/java/com/utam/simulation/asset/AssetDataGenerator.java`
- [X] T022 [US1] Create batch population endpoint in `backend/src/main/java/com/utam/simulation/api/GeneratorController.java`
- [X] T023 [US1] Wire all generators into GeneratorOrchestrator in `backend/src/main/java/com/utam/simulation/core/GeneratorOrchestrator.java`

**Checkpoint**: US1 complete - All entity tables populated, all screens show data

---

## Phase 4: User Story 2 - Multi-Tenant Data Generation (Priority: P1)

**Goal**: Generate data for multiple tenants (VIDP, YBBN) with tenant isolation

**Independent Test**: Run generators for each tenant and verify data is correctly isolated

### Implementation for User Story 2

- [X] T024 [US2] Add tenant-aware generation to all generators (pass tenantCode parameter) in `backend/src/main/java/com/utam/simulation/core/BaseDataGenerator.java`
- [X] T025 [P] [US2] Add timezone handling per tenant in SimulationConfig in `backend/src/main/java/com/utam/simulation/config/SimulationConfig.java`
- [X] T026 [US2] Add tenant validation before generation in GeneratorOrchestrator in `backend/src/main/java/com/utam/simulation/core/GeneratorOrchestrator.java`

**Checkpoint**: US2 complete - Each tenant has isolated, realistic data

---

## Phase 5: User Story 5 - Generate Turnaround Operations Data (Priority: P1)

**Goal**: Generate complete turnaround sessions with 30+ sub-processes, milestones, and vehicle correlations

**Independent Test**: View Turnaround page and verify all sub-processes, vehicle correlations, timing milestones visible

### Turnaround Entities

- [X] T027 [P] [US5] Create TurnaroundMilestone entity in `backend/src/main/java/com/utam/entity/TurnaroundMilestone.java`
- [X] T028 [P] [US5] Create VehicleAssignment entity in `backend/src/main/java/com/utam/entity/VehicleAssignment.java`
- [X] T029 [P] [US5] Create TurnaroundTaskType enum with 34 sub-processes in `backend/src/main/java/com/utam/entity/TurnaroundTaskType.java`

### Turnaround Generators

- [X] T030 [US5] Create TurnaroundDataGenerator in `backend/src/main/java/com/utam/simulation/turnaround/TurnaroundDataGenerator.java`
- [X] T031 [US5] Create TurnaroundCorrelator (flight → session) in `backend/src/main/java/com/utam/simulation/turnaround/TurnaroundCorrelator.java`
- [X] T032 [US5] Create TurnaroundMilestoneGenerator in `backend/src/main/java/com/utam/simulation/turnaround/TurnaroundMilestoneGenerator.java`

**Checkpoint**: US5 complete - Turnaround sessions with all sub-processes and milestones

---

## Phase 6: User Story 5a - Vehicle-to-Flight Correlation (Priority: P1)

**Goal**: Show which GSE vehicles are assigned to which flights with real-time position tracking

**Independent Test**: Select turnaround session and verify all assigned vehicles listed with positions and status

### Implementation for User Story 5a

- [X] T033 [US5a] Create ProximityAssignmentService (nearest available vehicle) in `backend/src/main/java/com/utam/simulation/vehicle/ProximityAssignmentService.java`
- [X] T034 [US5a] Create VehicleAssignmentGenerator in `backend/src/main/java/com/utam/simulation/vehicle/VehicleAssignmentGenerator.java`
- [X] T035 [US5a] Integrate vehicle assignments into TurnaroundDataGenerator in `backend/src/main/java/com/utam/simulation/turnaround/TurnaroundDataGenerator.java`
- [X] T036 [US5a] Create VehicleRouteCalculator (depot → stand → next) in `backend/src/main/java/com/utam/simulation/vehicle/VehicleRouteCalculator.java`

**Checkpoint**: US5a complete - Vehicle-flight correlation working

---

## Phase 7: User Story 5b - Comprehensive GSE Fleet (Priority: P1)

**Goal**: Simulate all 15+ GSE vehicle types with realistic fleet sizes

**Independent Test**: View vehicle fleet and verify all GSE categories with appropriate quantities

### Implementation for User Story 5b

- [X] T037 [US5b] Extend VehicleDataGenerator with all 15 GSE types in `backend/src/main/java/com/utam/simulation/vehicle/VehicleDataGenerator.java`
- [X] T038 [P] [US5b] Create VehiclePathPlayer for path following in `backend/src/main/java/com/utam/simulation/vehicle/VehiclePathPlayer.java`
- [X] T039 [US5b] Add fleet size configuration per airport in SimulationConfig in `backend/src/main/java/com/utam/simulation/config/SimulationConfig.java`

**Checkpoint**: US5b complete - Full GSE fleet operational

---

## Phase 8: User Story 5c - Extensive Turnaround Alerts (Priority: P1)

**Goal**: Generate predictive alerts 30+ minutes in advance with financial impact

**Independent Test**: View alerts dashboard and verify alerts appear with financial impact estimates

### Alert Entities

- [X] T040 [P] [US5c] Create FinancialMetric entity in `backend/src/main/java/com/utam/simulation/financial/FinancialMetric.java`

### Alert Generators

- [X] T041 [US5c] Create AlertDataGenerator with 12 alert types in `backend/src/main/java/com/utam/simulation/alert/AlertDataGenerator.java`
- [X] T042 [US5c] Create FinancialImpactCalculator ($100-150/min) in `backend/src/main/java/com/utam/simulation/alert/FinancialImpactCalculator.java`
- [X] T043 [P] [US5c] Create AlertEnricher (severity, recommendations) in `backend/src/main/java/com/utam/simulation/alert/AlertEnricher.java`
- [X] T044 [P] [US5c] Create CascadeAnalyzer for network impact in `backend/src/main/java/com/utam/simulation/alert/CascadeAnalyzer.java`

**Checkpoint**: US5c complete - Predictive alerts with financial impact

---

## Phase 9: User Story 5d - Financial Impact Tracking (Priority: P1)

**Goal**: Calculate and display delay costs and prevented delay value

**Independent Test**: View financial dashboard and verify delay costs and saved costs displayed

### Implementation for User Story 5d

- [X] T045 [US5d] Create FinancialMetricGenerator in `backend/src/main/java/com/utam/simulation/alert/FinancialMetricGenerator.java`
- [X] T046 [US5d] Add delay cost calculation to TurnaroundDataGenerator in `backend/src/main/java/com/utam/simulation/turnaround/TurnaroundDataGenerator.java`
- [X] T047 [US5d] Add prevented delay tracking in AlertDataGenerator in `backend/src/main/java/com/utam/simulation/alert/AlertDataGenerator.java`

**Checkpoint**: US5d complete - Financial ROI tracking operational

---

## Phase 10: User Story 7 - Realistic Flight Data (Priority: P1)

**Goal**: Generate realistic arrival/departure patterns matching actual airport traffic

**Independent Test**: Compare generated flight data against known patterns for each airport

### Implementation for User Story 7

- [X] T048 [US7] Add realistic airline codes per airport to FlightDataGenerator in `backend/src/main/java/com/utam/simulation/flight/FlightDataGenerator.java`
- [X] T049 [US7] Add approach/departure trajectory patterns in FlightTrajectoryCalculator in `backend/src/main/java/com/utam/simulation/flight/FlightTrajectoryCalculator.java`
- [X] T050 [P] [US7] Add peak hour traffic patterns in SimulationConfig in `backend/src/main/java/com/utam/simulation/config/SimulationConfig.java`

**Checkpoint**: US7 complete - Realistic flight patterns for VIDP/YBBN

---

## Phase 11: User Story 8 - Admin Path Drawing (Priority: P1)

**Goal**: Enable admins to draw vehicle movement paths on a map

**Independent Test**: Draw a path on admin screen and verify generated vehicles follow it

### Backend for Path Editor

- [X] T051 [P] [US8] Create VehiclePath entity in `backend/src/main/java/com/utam/entity/VehiclePath.java`
- [X] T052 [P] [US8] Create VehiclePathRepository in `backend/src/main/java/com/utam/repository/VehiclePathRepository.java`
- [X] T053 [US8] Create VehiclePathService in `backend/src/main/java/com/utam/admin/service/VehiclePathService.java`
- [X] T054 [US8] Create PathEditorController with CRUD endpoints in `backend/src/main/java/com/utam/admin/controller/PathEditorController.java`

### Frontend for Path Editor

- [X] T055 [P] [US8] Create pathApi service in `frontend/src/services/pathApi.ts`
- [X] T056 [US8] Create PathDrawingMap component with leaflet-geoman in `frontend/src/components/admin/PathDrawingMap.tsx`
- [X] T057 [US8] Create PathPreview component with animation in `frontend/src/components/admin/PathPreview.tsx`
- [X] T058 [US8] Create PathEditorPage in `frontend/src/pages/admin/PathEditorPage.tsx`
- [X] T059 [P] [US8] Create UndoRedoProvider for Ctrl+Z/Y support in `frontend/src/components/shared/UndoRedoProvider.tsx`

**Checkpoint**: US8 complete - Admin can draw and save vehicle paths

---

## Phase 12: User Story 9 - GeoJSON Zone Editor (Priority: P1)

**Goal**: Enable admins to draw and edit restricted zones visually like geojson.io

**Independent Test**: Draw polygon zone on map and verify saved with all properties

### Backend for Zone Editor

- [X] T060 [P] [US9] Create ZoneDTO for API in `backend/src/main/java/com/utam/admin/dto/ZoneDTO.java`
- [X] T061 [US9] Create ZoneService with GeoJSON import/export in `backend/src/main/java/com/utam/admin/service/ZoneService.java`
- [X] T062 [US9] Create ZoneEditorController with CRUD + import/export in `backend/src/main/java/com/utam/admin/controller/ZoneEditorController.java`

### Frontend for Zone Editor

- [X] T063 [P] [US9] Create zoneApi service with import/export in `frontend/src/services/zoneApi.ts`
- [X] T064 [US9] Create ZoneEditorMap component with polygon tools in `frontend/src/components/admin/ZoneEditorMap.tsx`
- [X] T065 [US9] Create ZonePropertiesPanel for editing zone metadata in `frontend/src/components/admin/ZonePropertiesPanel.tsx`
- [X] T066 [US9] Create GeoJsonImportExport component in `frontend/src/components/admin/GeoJsonImportExport.tsx`
- [X] T067 [US9] Create GeometryValidator for polygon validation in `frontend/src/utils/GeometryValidator.ts`
- [X] T068 [US9] Create ZoneEditorPage in `frontend/src/pages/admin/ZoneEditorPage.tsx`

**Checkpoint**: US9 complete - Admin can draw, edit, import/export zones

---

## Phase 13: User Story 3 - Continuous Real-Time Simulation (Priority: P2)

**Goal**: Continuous position updates for real-time tracking demonstration

**Independent Test**: Enable continuous generation and observe real-time movement on map

### Implementation for User Story 3

- [X] T069 [US3] Add continuous generation mode to GeneratorOrchestrator in `backend/src/main/java/com/utam/simulation/core/GeneratorOrchestrator.java`
- [X] T070 [US3] Add scheduled position updates to VehicleDataGenerator in `backend/src/main/java/com/utam/simulation/vehicle/VehicleDataGenerator.java`
- [X] T071 [US3] Add scheduled position updates to FlightDataGenerator in `backend/src/main/java/com/utam/simulation/flight/FlightDataGenerator.java`
- [X] T072 [P] [US3] Create MovementTrailGenerator in `backend/src/main/java/com/utam/simulation/tracking/MovementTrailGenerator.java`
- [X] T073 [P] [US3] Create LocationRegisterUpdater in `backend/src/main/java/com/utam/simulation/tracking/LocationRegisterUpdater.java`

**Checkpoint**: US3 complete - Real-time movement simulation working

---

## Phase 14: User Story 4 - Security and Compliance Data (Priority: P2)

**Goal**: Generate restricted zones, violations, and movement discrepancies

**Independent Test**: Run security generators and verify violation reports show data

### Implementation for User Story 4

- [X] T074 [US4] Enhance ZoneDataGenerator with realistic zones in `backend/src/main/java/com/utam/simulation/security/ZoneDataGenerator.java`
- [X] T075 [US4] Create ViolationDataGenerator in `backend/src/main/java/com/utam/simulation/security/ViolationDataGenerator.java`
- [X] T076 [US4] Create MovementDiscrepancyGenerator in `backend/src/main/java/com/utam/simulation/security/MovementDiscrepancyGenerator.java`

**Checkpoint**: US4 complete - Security monitoring data available

---

## Phase 15: User Story 10 - Airport Boundary Constraint (Priority: P2)

**Goal**: Ensure all ground movements stay within airport boundaries

**Independent Test**: Run generators and verify no positions fall outside boundaries

### Implementation for User Story 10

- [X] T077 [US10] Add boundary validation to VehicleDataGenerator in `backend/src/main/java/com/utam/simulation/vehicle/VehicleDataGenerator.java`
- [X] T078 [US10] Add boundary validation to AssetDataGenerator in `backend/src/main/java/com/utam/simulation/tracking/AssetDataGenerator.java`
- [X] T079 [US10] Add path boundary check in VehiclePathService in `backend/src/main/java/com/utam/admin/service/VehiclePathService.java`

**Checkpoint**: US10 complete - All ground data within airport perimeters

---

## Phase 16: User Story 6 - Analytics and Hotspot Data (Priority: P3)

**Goal**: Generate historical data for analytics dashboards and hotspot analysis

**Independent Test**: View Analytics and Hotspot Analysis pages with meaningful patterns

### Implementation for User Story 6

- [X] T080 [US6] Add historical data generation mode (backfill) to GeneratorOrchestrator in `backend/src/main/java/com/utam/simulation/core/GeneratorOrchestrator.java`
- [X] T081 [US6] Generate multi-day movement trail data in MovementTrailGenerator in `backend/src/main/java/com/utam/simulation/tracking/MovementTrailGenerator.java`
- [X] T082 [US6] Generate historical turnaround data in TurnaroundDataGenerator in `backend/src/main/java/com/utam/simulation/turnaround/TurnaroundDataGenerator.java`

**Checkpoint**: US6 complete - Analytics and hotspot data available

---

## Phase 17: Admin Dashboard & Security

**Purpose**: Admin UI dashboard and ADMIN role security

- [X] T083 [P] Create GeneratorControls component in `frontend/src/components/admin/GeneratorControls.tsx`
- [X] T084 [P] Create GeneratorStatus display component in `frontend/src/components/admin/GeneratorStatus.tsx`
- [X] T085 Create DataGeneratorAdminPage in `frontend/src/pages/admin/DataGeneratorAdminPage.tsx`
- [X] T086 Create AdminRoute with ADMIN guard in `frontend/src/components/admin/AdminRoute.tsx`
- [X] T087 Add admin routes to router in `frontend/src/App.tsx`
- [X] T088 Add ADMIN role security to admin endpoints in `backend/src/main/java/com/utam/config/SecurityConfig.java`

---

## Phase 18: Polish & Cross-Cutting Concerns

**Purpose**: Final integration, cleanup, and validation

- [X] T089 [P] Run quickstart.md validation (all steps work)
- [X] T090 [P] Verify all 24 entity types have generators
- [X] T091 [P] Verify Prometheus metrics exposed correctly
- [X] T092 Code cleanup and consistent error handling
- [X] T093 Update README.md with data generator documentation
- [X] T094 Final integration test of full generation flow

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies - start immediately
- **Phase 2 (Foundational)**: Depends on Phase 1 - BLOCKS all user stories
- **Phases 3-16 (User Stories)**: All depend on Phase 2 completion
- **Phase 17 (Admin Dashboard)**: Depends on US8, US9 completion
- **Phase 18 (Polish)**: Depends on all desired stories being complete

### User Story Dependencies

| Story | Can Start After | Integrates With |
|-------|-----------------|-----------------|
| US1 | Phase 2 | - |
| US2 | Phase 2 | US1 |
| US5 | Phase 2 | US1, US2 |
| US5a | US5 | US5 |
| US5b | US5 | US5a |
| US5c | US5, US5a | US5d |
| US5d | US5c | US5c |
| US7 | Phase 2 | US1 |
| US8 | Phase 2 | - |
| US9 | Phase 2 | - |
| US3 | US1 | US5 |
| US4 | Phase 2 | US3 |
| US10 | Phase 2 | US1, US8 |
| US6 | US3 | US5 |

### Parallel Opportunities

```text
After Phase 2 completes, these can run in parallel:
- US1 (Comprehensive Demo Data)
- US7 (Realistic Flights)
- US8 (Path Drawing)
- US9 (Zone Editor)

After US5 completes:
- US5a, US5b, US5c, US5d can proceed in sequence
- US3 can start in parallel

After US1 + US5:
- US4 (Security Data)
- US6 (Analytics Data)
```

---

## Implementation Strategy

### MVP First (User Stories 1, 2, 5 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL)
3. Complete Phase 3: US1 - Comprehensive Demo Data
4. Complete Phase 4: US2 - Multi-Tenant
5. Complete Phase 5-9: US5, 5a, 5b, 5c, 5d - Turnaround Operations
6. **STOP and VALIDATE**: All core screens populated with turnaround data
7. Deploy/demo if ready

### Incremental Delivery

1. MVP: Setup + Foundational + US1 + US2 + US5 → Core demo working
2. Add US7 + US8 + US9 → Realistic flights + admin tools
3. Add US3 + US4 + US10 → Real-time + security
4. Add US6 → Analytics historical data
5. Polish phase → Production ready

---

## Summary

| Phase | Stories | Tasks | Description |
|-------|---------|-------|-------------|
| 1 | - | T001-T006 | Setup infrastructure |
| 2 | - | T007-T017 | Foundational reference data |
| 3 | US1 | T018-T023 | Comprehensive demo data |
| 4 | US2 | T024-T026 | Multi-tenant support |
| 5 | US5 | T027-T032 | Turnaround operations |
| 6 | US5a | T033-T036 | Vehicle-flight correlation |
| 7 | US5b | T037-T039 | GSE fleet |
| 8 | US5c | T040-T044 | Turnaround alerts |
| 9 | US5d | T045-T047 | Financial tracking |
| 10 | US7 | T048-T050 | Realistic flights |
| 11 | US8 | T051-T059 | Path drawing admin |
| 12 | US9 | T060-T068 | Zone editor admin |
| 13 | US3 | T069-T073 | Real-time simulation |
| 14 | US4 | T074-T076 | Security data |
| 15 | US10 | T077-T079 | Boundary constraints |
| 16 | US6 | T080-T082 | Analytics data |
| 17 | - | T083-T088 | Admin dashboard |
| 18 | - | T089-T094 | Polish |

**Total Tasks**: 94  
**P1 Stories**: US1, US2, US5, US5a, US5b, US5c, US5d, US7, US8, US9  
**P2 Stories**: US3, US4, US10  
**P3 Stories**: US6
