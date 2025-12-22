# Tasks: Turnaround UI Overhaul

## Phase 1: Setup & Configuration
- [x] T001 Install frontend dependencies (`gantt-task-react`, `react-zoom-pan-pinch`, `lucide-react`) in `frontend/package.json`
- [x] T002 Create feature directory structure in `frontend/src/components/Turnaround` (Grid, Map, Detail, Gantt)
- [x] T003 Copy `documentations/Airside_AI_144P.mp4` to `frontend/public/assets/video/mock_feed.mp4` for video simulation
- [x] T004 Copy reference images (`assaiastands.jpg`, `assaiturnaroundallcard.jpg`, `assaiturnaroundtimeline.jpg`, `1_NTSiNq6S2ARV0A9IyPEGAQ.png`) from `documentations/` to `frontend/public/assets/images/` for card layout backgrounds

## Phase 2: Foundational (Backend & Data)
- [x] T005 Create SQL migration `infrastructure/db/init/03-turnaround-schema.sql` implementing `TurnaroundSession`, `TurnaroundTask`, `Alert` tables with `icao_code` column
- [x] T006 [P] Create JPA Entity `TurnaroundSession` in `backend/src/main/java/com/tam/turnaround/domain/TurnaroundSession.java` including `icaoCode`
- [x] T007 [P] Create JPA Entity `TurnaroundTask` in `backend/src/main/java/com/tam/turnaround/domain/TurnaroundTask.java` including `icaoCode`
- [x] T008 [P] Create JPA Entity `Alert` in `backend/src/main/java/com/tam/turnaround/domain/Alert.java` including `icaoCode`
- [x] T009 Create Repositories for Session, Task, and Alert in `backend/src/main/java/com/tam/turnaround/repository/`
- [x] T010 Implement `MockDataGeneratorService` in `backend/src/main/java/com/tam/turnaround/service/MockDataGeneratorService.java` to send simulated events to NiFi (HTTP POST at `http://nifi:8091/contentListener`) -> Kafka topic `turnaround-events`, adhering to ingestion architecture
- [x] T011 Implement `TurnaroundEventConsumer` in `backend/src/main/java/com/tam/turnaround/service/TurnaroundEventConsumer.java` to consume Kafka messages from topic `turnaround-events` and persist/update `TurnaroundSession` entities
- [x] T012 Implement `TurnaroundRuleEngine` in `backend/src/main/java/com/tam/turnaround/service/TurnaroundRuleEngine.java` to evaluate events against SOPs and generate `Alert` entities

## Phase 3: User Story 1 - Dashboard Grid View
**Goal**: View active turnaround sessions in a tabular format.
- [x] T013 [US1] Create DTOs (`TurnaroundSessionSummaryDTO`) in `backend/src/main/java/com/tam/turnaround/dto/`
- [x] T014 [US1] Implement `TurnaroundService.getAllSessions()` in `backend/src/main/java/com/tam/turnaround/service/TurnaroundService.java`
- [x] T015 [US1] Implement REST Endpoint `GET /api/turnaround/sessions` in `backend/src/main/java/com/tam/turnaround/controller/TurnaroundController.java`
- [x] T016 [US1] Create frontend service methods in `frontend/src/services/turnaroundService.ts`
- [x] T017 [US1] Create `TurnaroundGrid` component in `frontend/src/components/Turnaround/TurnaroundGrid.tsx` utilizing `assets/images` for card backgrounds
- [x] T018 [US1] Create main `TurnaroundPage` in `frontend/src/pages/TurnaroundPage.tsx` with View Switcher (Grid/Map)

## Phase 4: User Story 2 - Map View
**Goal**: Visual representation of stands and aircraft status.
- [x] T019 [P] [US2] Create `AirportMap` component using SVG/Canvas in `frontend/src/components/Turnaround/Map/AirportMap.tsx`
- [x] T020 [US2] Implement `StandPin` component to show status colors in `frontend/src/components/Turnaround/Map/StandPin.tsx`
- [x] T021 [US2] Integrate Map View into `TurnaroundPage.tsx` to toggle with Grid View

## Phase 5: User Story 3 - Detailed View & Gantt
**Goal**: Deep dive into a specific flight's turnaround progress.
- [x] T022 [US3] Create DTOs (`TurnaroundSessionDetailDTO`, `TaskDetailDTO`) in `backend/src/main/java/com/tam/turnaround/dto/`
- [x] T023 [US3] Implement `TurnaroundService.getSessionDetails(id)` in `backend/src/main/java/com/tam/turnaround/service/TurnaroundService.java`
- [x] T024 [US3] Implement REST Endpoint `GET /api/turnaround/sessions/{id}` in `backend/src/main/java/com/tam/turnaround/controller/TurnaroundController.java`
- [x] T025 [US3] Create `GanttChart` wrapper component for `gantt-task-react` in `frontend/src/components/Turnaround/Detail/GanttChart.tsx`
- [x] T026 [US3] Create `VideoTimeline` component in `frontend/src/components/Turnaround/Detail/VideoTimeline.tsx` using `assets/video/mock_feed.mp4`
- [x] T027 [US3] Create `TurnaroundDetailPage` in `frontend/src/pages/TurnaroundDetailPage.tsx` combining Gantt, Video, and Task List

## Phase 6: User Story 4 - Alerts & Notifications
**Goal**: Real-time awareness of issues.
- [x] T028 [US4] Implement `AlertService.getActiveAlerts()` in `backend/src/main/java/com/tam/turnaround/service/AlertService.java`
- [x] T029 [US4] Implement REST Endpoint `GET /api/alerts` in `backend/src/main/java/com/tam/turnaround/controller/AlertController.java`
- [x] T030 [US4] Create `AlertSidebar` component in `frontend/src/components/Turnaround/Alerts/AlertSidebar.tsx`
- [x] T031 [US4] Integrate Alert polling/updates in `TurnaroundPage.tsx`

## Phase 7: Polish & Integration
- [x] T032 Add loading skeletons for Grid and Detail views
- [x] T033 Implement error handling for API failures
- [x] T034 Verify `icao_code` filtering is applied in all Service queries (Multi-tenancy check)

## Dependencies
1. **Setup** -> **Foundational**
2. **Foundational** -> **US1** (Grid requires DB & API)
3. **US1** -> **US2** (Map uses same data source)
4. **US1** -> **US3** (Detail view accessed from Grid/Map)
5. **Foundational** -> **US4** (Alerts require Alert entity)

## Parallel Execution Opportunities
- **Backend vs Frontend**: Once API contracts (DTOs) are defined, Backend (T012, T013) and Frontend (T015, T016) can proceed in parallel.
- **Components**: `AirportMap` (T017) and `GanttChart` (T023) are independent UI components.
- **Entities**: T006, T007, T008 can be created simultaneously.
