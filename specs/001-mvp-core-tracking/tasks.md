---
description: "Task list for Core MVP Tracking & Alerting"
---

# Tasks: Core MVP Tracking & Alerting

**Input**: Design documents from `/specs/001-mvp-core-tracking/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Tests are OPTIONAL - only include them if explicitly requested in the feature specification.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Web app**: `backend/src/`, `frontend/src/`

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [x] T001 Create monorepo structure with backend and frontend directories
- [x] T002 Initialize Spring Boot project in `backend/` with Web, Kafka, JPA, Timescale dependencies
- [x] T003 [P] Initialize React project in `frontend/` with TypeScript, Leaflet, Axios
- [x] T004 Create `docker-compose.yml` with PostgreSQL, TimescaleDB, and Kafka services
- [x] T005 [P] Configure `backend/pom.xml` dependencies
- [x] T006 [P] Configure `frontend/package.json` dependencies

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T007 Configure Spring Boot application properties in `backend/src/main/resources/application.yml` (DB, Kafka)
- [x] T008 [P] Create database initialization script in `backend/src/main/resources/schema.sql` (Hypertable setup)
- [x] T009 [P] Implement CORS configuration in `backend/src/main/java/com/utam/config/WebConfig.java`
- [x] T010 [P] Implement Kafka configuration in `backend/src/main/java/com/utam/config/KafkaConfig.java`
- [x] T011 [P] Setup frontend API client base in `frontend/src/services/api.ts`
- [x] T012 [P] Create main Dashboard layout in `frontend/src/components/Dashboard/DashboardLayout.tsx`
- [x] T013 [P] Implement Global Error Handling in `backend/src/main/java/com/utam/exception/GlobalExceptionHandler.java`

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Live Flight Tracking (Priority: P1) 🎯 MVP

**Goal**: View real-time positions of aircraft on a map.

**Independent Test**: Run Mock ADSB Generator -> Verify aircraft icons on Dashboard Map.

### Implementation for User Story 1

- [x] T014 [P] [US1] Create Flight entity in `backend/src/main/java/com/utam/model/Flight.java`
- [x] T015 [P] [US1] Create Flight repository in `backend/src/main/java/com/utam/repository/FlightRepository.java`
- [x] T016 [US1] Implement Flight Service (Ingestion & Query) in `backend/src/main/java/com/utam/service/FlightService.java`
- [x] T017 [US1] Implement Flight Controller (API endpoints) in `backend/src/main/java/com/utam/controller/FlightController.java`
- [x] T018 [P] [US1] Implement Mock ADSB Generator in `backend/src/main/java/com/utam/simulation/MockAdsbGenerator.java`
- [x] T019 [P] [US1] Create Map Component in `frontend/src/components/Map/MapComponent.tsx`
- [x] T020 [P] [US1] Implement Flight Layer in `frontend/src/components/Map/FlightLayer.tsx`
- [x] T021 [US1] Integrate Flight API polling in `frontend/src/components/Dashboard/Dashboard.tsx`

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently

---

## Phase 4: User Story 2 - Live Vehicle Tracking (Priority: P1)

**Goal**: View real-time positions of ground vehicles on the map.

**Independent Test**: Run Mock TelIT Generator -> Verify vehicle icons on Dashboard Map.

### Implementation for User Story 2

- [x] T022 [P] [US2] Create Vehicle entity in `backend/src/main/java/com/utam/model/Vehicle.java`
- [x] T023 [P] [US2] Create Vehicle repository in `backend/src/main/java/com/utam/repository/VehicleRepository.java`
- [x] T024 [US2] Implement Vehicle Service (Ingestion & Query) in `backend/src/main/java/com/utam/service/VehicleService.java`
- [x] T025 [US2] Implement Vehicle Controller (API endpoints) in `backend/src/main/java/com/utam/controller/VehicleController.java`
- [x] T026 [P] [US2] Implement Mock TelIT Generator in `backend/src/main/java/com/utam/simulation/MockTelitGenerator.java`
- [x] T027 [P] [US2] Implement Vehicle Layer in `frontend/src/components/Map/VehicleLayer.tsx`
- [x] T028 [US2] Integrate Vehicle API polling in `frontend/src/components/Dashboard/Dashboard.tsx`

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently

---

## Phase 5: User Story 3 - Speed Violation Alerting (Priority: P2)

**Goal**: Receive immediate alerts when a vehicle exceeds the speed limit.

**Independent Test**: Configure Mock Generator for high speed -> Verify Alert in List & Map Focus.

### Implementation for User Story 3

- [x] T029 [P] [US3] Create Alert entity in `backend/src/main/java/com/utam/model/Alert.java`
- [x] T030 [P] [US3] Create Alert repository in `backend/src/main/java/com/utam/repository/AlertRepository.java`
- [x] T031 [US3] Implement Alert Service (Detection Logic) in `backend/src/main/java/com/utam/service/AlertService.java`
- [x] T032 [US3] Implement Alert Controller (API endpoints) in `backend/src/main/java/com/utam/controller/AlertController.java`
- [x] T033 [P] [US3] Create Alert List Component in `frontend/src/components/Alerts/AlertList.tsx`
- [x] T034 [US3] Integrate Alert API polling in `frontend/src/components/Dashboard/Dashboard.tsx`

**Checkpoint**: All user stories should now be independently functional

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [x] T036 [P] Update README.md with setup instructions
- [ ] T037 Verify end-to-end latency (< 5s)
- [ ] T038 Verify load handling (10 flights, 10 vehicles)
- [ ] T039 Ensure Basic Auth is applied to all endpoints

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Polish (Final Phase)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P1)**: Can start after Foundational (Phase 2) - Independent of US1
- **User Story 3 (P2)**: Can start after Foundational (Phase 2) - Depends on Vehicle Service (US2) for trigger logic

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (within Phase 2)
- Once Foundational phase completes, US1 and US2 can start in parallel
- US3 requires Vehicle Service (US2) to be partially complete (for ingestion hook), but Alert Entity/Repo can be done in parallel

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Test User Story 1 independently
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP!)
3. Add User Story 2 → Test independently → Deploy/Demo
4. Add User Story 3 → Test independently → Deploy/Demo
5. Each story adds value without breaking previous stories
