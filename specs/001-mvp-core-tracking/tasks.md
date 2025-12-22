---
description: "Task list for Core MVP Tracking & Alerting"
---

# Tasks: Core MVP Tracking & Alerting

**Input**: Design documents from `/specs/001-mvp-core-tracking/`
**Prerequisites**: plan.md, spec.md, data-model.md, contracts/

**Tests**: Tests are OPTIONAL - only included where critical for verification.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [x] T001 Create project structure (backend, frontend folders)
- [x] T002 Initialize Spring Boot application in `backend/` with Web, Kafka, JPA, PostgreSQL dependencies
- [x] T003 Initialize React application in `frontend/` with TypeScript and Leaflet
- [x] T004 Create `docker-compose.yml` with NiFi, Kafka, Zookeeper, TimescaleDB services

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T005 Configure PostgreSQL/TimescaleDB connection in `backend/src/main/resources/application.properties`
- [x] T006 Create database schema initialization script (Hypertables) in `backend/src/main/resources/schema.sql`
- [x] T007 Configure Kafka Consumer/Producer factories in `backend/src/main/java/com/utam/config/KafkaConfig.java`
- [x] T008 Create base API response wrapper and error handling in `backend/src/main/java/com/utam/common/ApiResponse.java`
- [x] T009 Setup React Leaflet and basic Map component in `frontend/src/components/Map/Map.tsx` (Default center: IGIA)
- [x] T010 Configure CORS and Security (Basic Auth) in `backend/src/main/java/com/utam/config/SecurityConfig.java`

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Live Flight Tracking (Priority: P1) 🎯 MVP

**Goal**: Ingest ADSB data via NiFi, process in Spring Boot, and display on React Map.

**Independent Test**: Run Mock ADSB Generator -> Verify icons on Map.

### Implementation for User Story 1

- [x] T011 [P] [US1] Create Flight entity in `backend/src/main/java/com/utam/model/Flight.java`
- [x] T012 [P] [US1] Create FlightRepository in `backend/src/main/java/com/utam/repository/FlightRepository.java`
- [x] T013 [US1] Implement FlightConsumer to ingest from 'flight-raw-json' in `backend/src/main/java/com/utam/service/FlightConsumer.java`
- [x] T014 [US1] Implement FlightService to save data in `backend/src/main/java/com/utam/service/FlightService.java`
- [x] T015 [US1] Create FlightController with GET /api/flights in `backend/src/main/java/com/utam/controller/FlightController.java`
- [x] T016 [US1] Create Mock ADSB Generator in `backend/src/main/java/com/utam/simulation/MockAdsbGenerator.java`
- [x] T017 [US1] Create NiFi flow configuration for ADSB (ListenHTTP -> PublishKafka) in `infrastructure/nifi/flow_adsb.json`
- [x] T018 [P] [US1] Implement FlightService client in `frontend/src/services/flightService.ts`
- [x] T019 [US1] Update Map component to render Flight icons in `frontend/src/components/Map/FlightLayer.tsx`

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently

---

## Phase 4: User Story 2 - Live Vehicle Tracking (Priority: P1)

**Goal**: Ingest TelIT data via NiFi, process in Spring Boot, and display on React Map.

**Independent Test**: Run Mock TelIT Generator -> Verify icons on Map.

### Implementation for User Story 2

- [x] T020 [P] [US2] Create Vehicle entity in `backend/src/main/java/com/utam/model/Vehicle.java`
- [x] T021 [P] [US2] Create VehicleRepository in `backend/src/main/java/com/utam/repository/VehicleRepository.java`
- [x] T022 [US2] Implement VehicleConsumer to ingest from 'vehicle-raw-json' in `backend/src/main/java/com/utam/service/VehicleConsumer.java`
- [x] T023 [US2] Implement VehicleService to save data in `backend/src/main/java/com/utam/service/VehicleService.java`
- [x] T024 [US2] Create VehicleController with GET /api/vehicles in `backend/src/main/java/com/utam/controller/VehicleController.java`
- [x] T025 [US2] Create Mock TelIT Generator in `backend/src/main/java/com/utam/simulation/MockTelitGenerator.java`
- [ ] T026 [US2] Create NiFi flow configuration for TelIT in `infrastructure/nifi/flow_telit.json`
- [x] T027 [P] [US2] Implement VehicleService client in `frontend/src/services/vehicleService.ts`
- [x] T028 [US2] Update Map component to render Vehicle icons in `frontend/src/components/Map/VehicleLayer.tsx`

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently

---

## Phase 5: User Story 3 - Speed Violation Alerting (Priority: P2)

**Goal**: Detect speed violations from vehicle stream and display alerts.

**Independent Test**: Send high speed vehicle data -> Verify alert in list.

### Implementation for User Story 3

- [ ] T029 [P] [US3] Create Alert entity in `backend/src/main/java/com/utam/model/Alert.java`
- [ ] T030 [P] [US3] Create AlertRepository in `backend/src/main/java/com/utam/repository/AlertRepository.java`
- [ ] T031 [US3] Implement AlertService with speed check logic in `backend/src/main/java/com/utam/service/AlertService.java`
- [ ] T032 [US3] Integrate AlertService into VehicleConsumer in `backend/src/main/java/com/utam/service/VehicleConsumer.java`
- [ ] T033 [US3] Create AlertController with GET /api/alerts in `backend/src/main/java/com/utam/controller/AlertController.java`
- [ ] T034 [P] [US3] Implement AlertService client in `frontend/src/services/alertService.ts`
- [ ] T035 [US3] Create AlertList component in `frontend/src/components/Alerts/AlertList.tsx`
- [ ] T036 [US3] Add alert visualization/focus to Map in `frontend/src/components/Map/Map.tsx`

**Checkpoint**: All user stories should now be independently functional

---

## Phase 6: User Story 4 - Turnaround Management (Priority: P2)

**Goal**: Ingest CV events via NiFi and visualize on Gantt chart.

**Independent Test**: Run Mock CV Generator -> Verify bars on Gantt Chart.

### Implementation for User Story 4

- [ ] T037 [P] [US4] Create TurnaroundEvent entity in `backend/src/main/java/com/utam/model/TurnaroundEvent.java`
- [ ] T038 [P] [US4] Create TurnaroundRepository in `backend/src/main/java/com/utam/repository/TurnaroundRepository.java`
- [ ] T039 [US4] Implement TurnaroundConsumer to ingest from 'turnaround-raw-json' in `backend/src/main/java/com/utam/service/TurnaroundConsumer.java`
- [ ] T040 [US4] Implement TurnaroundService in `backend/src/main/java/com/utam/service/TurnaroundService.java`
- [ ] T041 [US4] Create TurnaroundController with GET /api/turnaround in `backend/src/main/java/com/utam/controller/TurnaroundController.java`
- [ ] T042 [US4] Create Mock CV Generator script in `backend/src/main/resources/simulation/cv-generator.py`
- [ ] T043 [US4] Create NiFi flow configuration for CV events in `infrastructure/nifi/flow_cv.json`
- [ ] T044 [P] [US4] Implement TurnaroundService client in `frontend/src/services/turnaroundService.ts`
- [ ] T045 [US4] Create Gantt Chart component in `frontend/src/components/Turnaround/TurnaroundGantt.tsx`

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] T046 Update README.md with setup instructions
- [ ] T047 Verify end-to-end latency meets < 5s requirement
- [ ] T048 Ensure all mock generators handle connection errors gracefully
- [ ] T049 Perform load test with 10 concurrent flights and vehicles (SC-002)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2)
- **User Story 2 (P1)**: Can start after Foundational (Phase 2)
- **User Story 3 (P2)**: Depends on User Story 2 (Vehicle Tracking) for data source
- **User Story 4 (P2)**: Can start after Foundational (Phase 2)

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- Once Foundational phase completes, US1, US2, and US4 can start in parallel
- US3 must wait for US2 core implementation

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Test User Story 1 independently
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational
2. Add User Story 1 (Flights) -> Demo
3. Add User Story 2 (Vehicles) -> Demo
4. Add User Story 3 (Alerts) -> Demo
5. Add User Story 4 (Turnaround) -> Demo
