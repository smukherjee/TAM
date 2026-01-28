# Task Breakdown Summary: Feature 005

**Generated**: 2026-01-28  
**Source Documents**: spec.md, plan.md, research.md, data-model.md, contracts/, quickstart.md

---

## Overview

The implementation plan for **Feature 005 - Asset Tracking & Security Module** has been broken down into **233 granular tasks** organized across **18 phases**. Tasks are structured to enable independent implementation and testing of each user story.

---

## Task Organization

### By Phase

| Phase | Task Range | Count | Status | Effort (days) |
|-------|-----------|-------|--------|---------------|
| **Phase 0: Foundation** | T001-T005 | 5 | ✅ Complete | 0.5 |
| **Phase 1: Database Layer** | T006-T023c | 21 | ✅ 18 complete, 3 pending | 3-4 |
| **Phase 2: Backend Models** | T024-T032 | 13 | ⏳ Pending | 2-3 |
| **Phase 2A: US5 & US6** | T033-T071 | 48 | ⏳ Pending | 3-4 |
| **Phase 3: US1 Backend** | T072-T083 | 12 | ⏳ Pending | 2-3 |
| **Phase 4: US2 Backend** | T084-T094 | 11 | ⏳ Pending | 2 |
| **Phase 5: US3 Backend** | T095-T105 | 11 | ⏳ Pending | 2 |
| **Phase 6: US4 Backend** | T106-T115 | 10 | ⏳ Pending | 2 |
| **Phase 7: US1 Frontend** | T116-T126 | 11 | ⏳ Pending | 2-3 |
| **Phase 8: US2 Frontend** | T127-T136 | 10 | ⏳ Pending | 2 |
| **Phase 9: US3 Frontend** | T137-T147 | 11 | ⏳ Pending | 2-3 |
| **Phase 10: US4 Frontend** | T148-T157 | 10 | ⏳ Pending | 2 |
| **Phase 11-16: Frontend Components** | T158-T210 | 53 | ⏳ Pending | 4-5 |
| **Phase 17: Testing** | T211-T225 | 15 | ⏳ Pending | 2-3 |
| **Phase 18: Deployment** | T226-T233 | 8 | ⏳ Pending | 1 |
| **TOTAL** | T001-T233 | **233** | **23/233** | **21-28** |

### By User Story

| User Story | Tasks | Description | Priority |
|------------|-------|-------------|----------|
| **Setup/Infrastructure** | 39 | Foundation, database, NiFi, Kafka | - |
| **US1: Zone Violations Report** | 23 | Backend (12) + Frontend (11) | P0 |
| **US2: Discrepancy Report** | 21 | Backend (11) + Frontend (10) | P0 |
| **US3: Movement Trail** | 22 | Backend (11) + Frontend (11) | P0 |
| **US4: Configure Zones** | 20 | Backend (10) + Frontend (10) | P1 |
| **US5: Universal Airside Map** | 31 | Backend (9) + Frontend (22) | P0 |
| **US6: Hotspot Analysis** | 24 | Backend (8) + Frontend (16) | P0 |
| **Shared Components** | 38 | Common services, utilities, tests | - |
| **Testing & Deployment** | 23 | Integration tests, deployment | - |

### By Technology Layer

| Layer | Task Count | Key Components |
|-------|-----------|----------------|
| **Database (PostgreSQL/TimescaleDB/PostGIS)** | 21 | 5 tables, 18 indexes, 4 aggregates, 2 views |
| **Backend (Spring Boot/Java 21)** | 61 | 15 entities, 12 repositories, 10 services, 13 controllers, 11 DTOs |
| **Ingestion (Apache NiFi + Kafka)** | 6 | NiFi flow, Kafka topic, consumer processor |
| **Frontend (React/TypeScript)** | 91 | 6 pages, 30+ components, services, hooks |
| **Testing** | 15 | Unit, integration, E2E tests |
| **Documentation & Deployment** | 39 | API docs, deployment scripts, monitoring |

---

## Critical Path Analysis

### Prerequisites (Must Complete First)

**Phase 1: Database Layer** (T006-T023c) - 3-4 days
- ✅ T006-T023: Schema, tables, indexes, aggregates, zones - **COMPLETE**
- ⏳ T023a-T023c: NiFi flow + Kafka setup - **PENDING** (0.5-1 day)

**Phase 2: Backend Domain Models** (T024-T032) - 2-3 days
- T026a-T026c: Kafka consumer (MovementTrailProcessor) - **CRITICAL**
- T027-T032: Entity models, repositories

**Blocking**: No user story implementation can begin until Phase 1 and Phase 2 complete

### Independent User Story Tracks (After Prerequisites)

Once prerequisites complete, these can proceed in parallel:

**Track 1: Zone Violations (US1)** - 4-5 days
- T072-T083: Backend API (violations controller, service, DTOs)
- T116-T126: Frontend (ViolationReportPage, table, filters, detail modal)

**Track 2: Discrepancy Report (US2)** - 4 days
- T084-T094: Backend API (discrepancies controller, service, DTOs)
- T127-T136: Frontend (DiscrepancyReportPage, table, filters)

**Track 3: Movement Trail (US3)** - 4-5 days
- T095-T105: Backend API (trail controller, service, playback)
- T137-T147: Frontend (MovementTrailPage, map, timeline, playback)

**Track 4: Universal Airside Map (US5)** - 3-4 days
- T033-T041: Backend enhancements (live positions API, WebSocket)
- T042-T063: Frontend (AirsideMapPage, LiveAssetMap, filters, legend)

**Track 5: Hotspot Analysis (US6)** - 3-4 days
- T064-T071: Backend (heatmap APIs, hotspot detail)
- T158-T173: Frontend (HotspotAnalysisPage, heatmap layer, modes)

**Track 6: Zone Configuration (US4)** - 4 days
- T106-T115: Backend (zone CRUD APIs, validation)
- T148-T157: Frontend (ZoneConfigPage, map editor, form)

---

## Execution Strategies

### Strategy 1: Sequential MVP (Minimum Risk)

**Week 1: Infrastructure**
- Complete Phase 1 (NiFi flow) + Phase 2 (Kafka consumer, domain models)
- **Deliverable**: Asset positions flowing from vehicles → NiFi → Kafka → Database

**Week 2-3: Core Security (US1 + US2)**
- Implement Zone Violations Report (US1)
- Implement Discrepancy Report (US2)
- **Deliverable**: Security monitoring operational

**Week 3-4: Investigation & Configuration (US3 + US4)**
- Implement Movement Trail (US3)
- Implement Zone Configuration (US4)
- **Deliverable**: Full security module operational

**Week 4-5: Demo Flow Completion (US5 + US6)**
- Implement Universal Airside Map (US5)
- Implement Hotspot Analysis (US6)
- **Deliverable**: 100% Demo Flow coverage

**Week 5-6: Polish & Testing**
- Integration testing, bug fixes, performance tuning
- Documentation updates
- **Deliverable**: Production-ready feature

**Total Duration**: 5-6 weeks (21-28 days)

### Strategy 2: Parallel Development (Fastest)

Requires 2-3 developers working simultaneously:

**Team Member 1: Backend Focus**
- Week 1: Infrastructure (NiFi, Kafka, domain models)
- Week 2: US1 + US2 backend APIs
- Week 3: US3 + US5 backend APIs
- Week 4: US4 + US6 backend APIs

**Team Member 2: Frontend Focus**
- Week 1: Setup, shared components
- Week 2: US1 + US2 frontend pages
- Week 3: US3 + US5 frontend pages
- Week 4: US4 + US6 frontend pages

**Team Member 3 (Optional): QA/DevOps**
- Continuous testing
- Deployment preparation
- Performance monitoring setup

**Total Duration**: 4 weeks (16-20 days with 3 people)

### Strategy 3: Story-by-Story (Best for Learning)

Complete one user story end-to-end before starting next:

1. **Infrastructure** (3-4 days): Phase 1 + Phase 2
2. **US1 Complete** (4-5 days): Backend + Frontend + Testing
3. **US2 Complete** (4 days): Backend + Frontend + Testing
4. **US3 Complete** (4-5 days): Backend + Frontend + Testing
5. **US5 Complete** (3-4 days): Backend + Frontend + Testing
6. **US6 Complete** (3-4 days): Backend + Frontend + Testing
7. **US4 Complete** (4 days): Backend + Frontend + Testing
8. **Polish** (2 days): Integration, deployment

**Total Duration**: 6-7 weeks (27-34 days)

---

## Parallel Execution Opportunities

### Database Phase (Can run in parallel)
- T007, T008, T009, T010, T011 - Create tables (5 parallel)
- T012, T013 - Create indexes (2 parallel)
- T014, T015 - Create aggregates (2 parallel)
- T016, T017, T018 - Seed zones (3 parallel)

### Backend Models (Can run in parallel after dependencies)
- T027, T028, T029, T030, T031 - Create entity models (5 parallel)

### Frontend Components (Can run in parallel within each story)
- US1: T117-T121 - Components (5 parallel)
- US2: T128-T132 - Components (5 parallel)
- US3: T138-T142 - Components (5 parallel)
- US5: T043-T058 - Components (16 parallel - largest opportunity!)
- US6: T159-T168 - Components (10 parallel)

---

## Testing Strategy

### Phase 17: Testing Tasks (T211-T225)

**Unit Tests** (5 tasks)
- T211: Backend service layer tests
- T212: Backend repository tests
- T213: Frontend component tests
- T214: Frontend service tests
- T215: Utility function tests

**Integration Tests** (5 tasks)
- T216: Kafka consumer integration tests
- T217: Zone detection integration tests
- T218: API endpoint integration tests
- T219: WebSocket integration tests
- T220: Database query performance tests

**E2E Tests** (3 tasks)
- T221: User Story 1 end-to-end test
- T222: User Story 3 end-to-end test
- T223: User Story 5 end-to-end test

**Performance Tests** (2 tasks)
- T224: Load testing (500 assets, 100 concurrent users)
- T225: Spatial query benchmarking

---

## Dependencies

### Research → Design → Implementation Mapping

| Research Decision | Data Model | API Contract | Tasks |
|------------------|------------|--------------|-------|
| **NiFi Ingestion** (research.md Q1) | - | - | T023a-T023c, T026a-T026c |
| **ST_DWithin 50m Buffer** (research.md Q2) | restricted_zones.geometry | - | T020, violation detection logic |
| **90-day Retention** (research.md Q3) | asset_movement_trail hypertable | - | T008, T021 |
| **8 Asset Categories** (research.md Q4) | Asset Category Reference | LiveAssetPositionResponse.markerColor | T043-T048 |
| **5 Discrepancy Types** (research.md Q5) | movement_discrepancies.discrepancy_type | DiscrepancyTypeEnum | T084-T094 |
| **Heatmap Grid Resolution** (research.md Q6) | activity_heatmap, violation_heatmap | HeatmapResponse | T024-T025, T064-T071 |
| **WebSocket (SockJS+STOMP)** (research.md Q7) | - | /topic/assets/live/{tenantCode} | T039-T041, T057-T058 |
| **TanStack Query + Zustand** (research.md Q8) | - | - | T044, T050 |

### External Dependencies

| Dependency | Version | Required For | Tasks Affected |
|------------|---------|--------------|----------------|
| PostgreSQL | 16+ | All database operations | T006-T025 |
| TimescaleDB | 2.13+ | Hypertables, compression | T008-T010, T021 |
| PostGIS | 3.4+ | Spatial queries | T007, T012, T020 |
| Apache NiFi | Latest | Ingestion pipeline | T023a-T023c |
| Apache Kafka | Latest | Message broker | T023b, T026a-T026c |
| Spring Boot | 3.2+ | Backend framework | T027-T115 |
| Java | 21+ | Backend runtime | T027-T115 |
| React | 18+ | Frontend framework | T116-T210 |
| TypeScript | 5+ | Frontend type safety | T116-T210 |
| Leaflet | Latest | Map visualization | T137-T147, T042-T063, T158-T173 |

---

## Risk Mitigation

### High-Risk Tasks (Require Extra Attention)

**T023a-T023c: NiFi Flow Configuration** (Risk: Integration complexity)
- Mitigation: Use quickstart.md instructions, test with mock data first
- Validation: Monitor flow performance, verify Kafka message format

**T026b: MovementTrailProcessor (Kafka Consumer)** (Risk: Real-time processing bugs)
- Mitigation: Comprehensive unit tests, integration tests with Testcontainers
- Validation: Verify violation detection accuracy >95%, latency <10 sec

**T020: Spatial Query Optimization** (Risk: Performance bottlenecks)
- Mitigation: GIST indexes, query plan analysis, benchmarking
- Validation: <100ms query time for 8 zones × 500 assets

**T138-T142: Movement Trail Playback** (Risk: UX complexity)
- Mitigation: Reference existing patterns, user testing, animation library
- Validation: Smooth playback at 1-60x speed, responsive controls

**T158-T168: Heatmap Rendering** (Risk: Large dataset visualization)
- Mitigation: Materialized views, grid resolution selector, lazy loading
- Validation: <5 sec aggregation for 30-day data, <3 sec render time

---

## Quick Reference

### Next 5 Tasks to Complete

1. **T023a** [P]: Create NiFi Asset Position Polling flow
2. **T023b** [P]: Create Kafka topic asset-positions-json
3. **T023c** [P]: Test NiFi flow with mock data
4. **T026a** [P]: Create AssetPositionEvent.java message model
5. **T026b**: Implement MovementTrailProcessor Kafka consumer

### MVP Scope (First Release)

Minimum tasks for production deployment:

- ✅ Phase 0: Foundation (5 tasks) - **COMPLETE**
- ✅ Phase 1: Database (18 tasks) - **COMPLETE**
- ⏳ Phase 1+: NiFi Setup (3 tasks) - **PENDING**
- ⏳ Phase 2: Backend Models (13 tasks)
- ⏳ Phase 3-6: US1-US4 Backend (44 tasks)
- ⏳ Phase 7-10: US1-US4 Frontend (42 tasks)
- ⏳ Phase 17-18: Testing + Deployment (23 tasks)

**MVP Total**: 148 tasks (excludes US5 & US6 for initial release)

### Full Feature Scope (Demo Flow Complete)

All 233 tasks including US5 (Universal Airside Map) and US6 (Hotspot Analysis)

---

## Resource Links

- **Specification**: [spec.md](spec.md) - 557 lines, 6 user stories, 18 FRs
- **Implementation Plan**: [plan.md](plan.md) - 1,141 lines, architecture, data flow, API design
- **Detailed Tasks**: [tasks.md](tasks.md) - 1,135 lines, 233 tasks with file paths
- **Research**: [research.md](research.md) - 1,047 lines, 8 technical decisions
- **Data Model**: [data-model.md](data-model.md) - 657 lines, 5 tables, ERD
- **API Contracts**: [contracts/asset-tracking-api.yaml](contracts/asset-tracking-api.yaml) - 1,271 lines, OpenAPI 3.0.3
- **Quickstart**: [quickstart.md](quickstart.md) - 386 lines, developer onboarding
- **Planning Summary**: [PLANNING_COMPLETE.md](PLANNING_COMPLETE.md) - 621 lines, executive summary

---

**Status**: ✅ Task breakdown complete  
**Total Tasks**: 233  
**Progress**: 23/233 (9.9%)  
**Estimated Remaining Effort**: 21-28 days  
**Recommended Next Step**: Complete T023a-T023c (NiFi configuration)
