# Tasks: Platform Phase 1 (Infrastructure & Real-time)

**Feature**: `003-nifi-ksqldb-integration` | **Date**: 2025-12-21
**Spec**: [specs/003-nifi-ksqldb-integration/spec.md](specs/003-nifi-ksqldb-integration/spec.md)

## Phase 1: Setup & Database Fixes
*Goal: Ensure database schema is consistent and supports multi-tenancy before adding new features.*

- [x] T001 Apply DB schema fixes (Turnaround, Alerts, Views) using `infrastructure/db/init/03-fix-schema-mismatches.sql`
- [x] T002 Verify `TurnaroundEvent` entity matches new DB schema in `backend/src/main/java/com/utam/model/TurnaroundEvent.java`
- [x] T003 Verify `Alert` entity matches new DB schema in `backend/src/main/java/com/utam/model/Alert.java`
- [x] T004 [P] Verify `flights_hourly` view supports multi-tenancy (manual check via SQL)

## Phase 2: Foundational Infrastructure
*Goal: Enable Redis and WebSocket capabilities in the backend.*

- [x] T005 Configure Redis connection in `backend/src/main/resources/application.yml` (ensure host/port match docker-compose)
- [x] T006 [P] Create `RedisService` wrapper for common operations (set, get, expire) in `backend/src/main/java/com/utam/service/RedisService.java`
- [x] T007 Configure WebSocket message broker (STOMP) in `backend/src/main/java/com/utam/config/WebSocketConfig.java` to support `/topic/flights/{icao}`
- [x] T008 [P] Create `MinioService` for object storage operations in `backend/src/main/java/com/utam/service/MinioService.java`

## Phase 3: User Story 1 - Real-time Flight Updates (WebSockets)
*Goal: Push flight updates to frontend instantly.*

- [x] T009 [US1] Update `FlightConsumer.java` to publish updates to `SimpMessagingTemplate` topic `/topic/flights/{icao}`
- [x] T010 [US1] Update `FlightController.java` to remove polling-only logic (optional cleanup)
- [x] T011 [US1] Create `WebSocketService.ts` in frontend to handle STOMP connection and subscriptions
- [x] T012 [US1] Update `MapPage.tsx` to subscribe to `/topic/flights/{userIcao}` instead of polling
- [x] T013 [US1] [P] Update `FlightLayer.tsx` to render real-time updates smoothly

## Phase 4: User Story 2 - High Performance Data Access (Redis)
*Goal: Cache active entities to reduce DB load.*

- [x] T014 [US2] Update `FlightConsumer.java` to save flight state to Redis (`flight:{icao}:{callsign}`)
- [x] T015 [US2] Update `VehicleService.java` (consumer) to save vehicle state to Redis (`vehicle:{icao}:{id}`)
- [x] T016 [US2] Update `FlightService.java` to fetch "Active Flights" from Redis instead of DB
- [x] T017 [US2] Update `VehicleService.java` to fetch "Active Vehicles" from Redis instead of DB

## Phase 5: User Story 3 - Multi-Tenant Analytics
*Goal: Ensure data isolation in analytics.*

- [ ] T018 [US3] Verify `AnalyticsPage.tsx` passes `icaoCode` to all API calls
- [ ] T019 [US3] Update `MonitoringController.java` to filter metrics by ICAO code where applicable
- [ ] T020 [US3] [P] Update `AlertService.java` to filter alerts by ICAO code in `getAllAlerts`

## Phase 6: User Story 4 - Data Lake Storage (MinIO)
*Goal: Archive raw data.*

- [x] T021 [US4] Update `FlightConsumer.java` to asynchronously archive raw JSON to MinIO bucket `tam-datalake`
- [x] T022 [US4] [P] Update `VehicleService.java` to asynchronously archive raw JSON to MinIO

## Final Phase: Polish & Verification
*Goal: Ensure system stability and performance.*

- [x] T023 Verify WebSocket reconnection logic in frontend
- [x] T024 Verify Redis keys expire correctly (TTL check)
- [ ] T025 Run full end-to-end test with Mock Generators (LIRN and VIDP)

## Dependencies
- Phase 1 must be complete before Phase 4 (DB schema affects entities).
- Phase 2 must be complete before Phase 3 and 4.
- Phase 3 and 4 can be executed in parallel.

## Implementation Strategy
1. **Fix DB First**: Ensure the foundation is solid.
2. **Enable Infra**: Turn on Redis/WS.
3. **Real-time**: Make the map come alive (highest user value).
4. **Performance**: Optimize with Redis.
5. **Archive**: Add MinIO logging last.
