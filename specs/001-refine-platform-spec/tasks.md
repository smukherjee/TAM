# Tasks: Refine Platform Spec

**Input**: Design documents from `/specs/001-refine-platform-spec/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Tests are OPTIONAL - not requested in specification.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- Backend: `backend/` at repository root
- Platform core: `backend/platform-core/`

## Dependencies

User Story 1 (US1): Update Java version to 21  
User Story 2 (US2): Standardize tenant identifier to tenantId  
User Story 3 (US3): Implement Parquet archival with tenant isolation  

Dependency Graph: US1 → US2 → US3 (Java 21 first, then standardize tenantId, then implement archival)

Parallel Opportunities: Within each story, parallel tasks for different files.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and dependency updates

- [ ] T001 Update pom.xml with Parquet and Hadoop dependencies in backend/platform-core/pom.xml
- [ ] T002 Update Java version to 21 in backend/pom.xml and backend/platform-core/pom.xml
- [ ] T003 Configure MinIO client for tenant-isolated buckets in backend/platform-core/src/main/resources/application.yml

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core updates that MUST be complete before user stories

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T004 Update TenantContextFilter to use tenantId instead of icaoCode in backend/platform-core/src/main/java/com/tam/platform/context/TenantContextFilter.java
- [ ] T005 Update RedisCacheService metrics for tenantId in backend/platform-core/src/main/java/com/tam/platform/cache/RedisCacheService.java
- [ ] T006 Update Grafana dashboard for tenantId variables in infrastructure/grafana/dashboards/06-infrastructure-metrics.json

**Checkpoint**: Foundation ready - user story implementation can now begin

---

## Phase 3: User Story 1 - Update Java Version to 21 (Priority: P1) 🎯 MVP

**Goal**: Ensure platform uses Java 21+ for virtual threads and modern JVM features

**Independent Test**: Verify application starts with Java 21 and passes existing tests

### Implementation for User Story 1

- [ ] T007 [US1] Update Maven compiler plugin to target Java 21 in backend/pom.xml
- [ ] T008 [US1] Update Spring Boot version to 3.x for Java 21 compatibility in backend/pom.xml
- [ ] T009 [US1] Test application startup with Java 21 runtime

**Checkpoint**: Java 21 update complete, application runs on Java 21

---

## Phase 4: User Story 2 - Standardize Tenant Identifier (Priority: P1)

**Goal**: Replace inconsistent tenant naming (icaoCode) with standardized tenantId

**Independent Test**: All tenant references use tenantId consistently

### Implementation for User Story 2

- [ ] T010 [P] [US2] Update DomainEvent class to use tenantId field in backend/platform-core/src/main/java/com/tam/platform/event/DomainEvent.java
- [ ] T011 [P] [US2] Update TenantContext to use tenantId in backend/platform-core/src/main/java/com/tam/platform/context/TenantContext.java
- [ ] T012 [P] [US2] Update Kafka headers to use X-Tenant-ID in infrastructure/nifi/setup-nifi.sh
- [ ] T013 [P] [US2] Update ksqlDB scripts for tenantId extraction in infrastructure/ksqldb/ksql-init.sql

**Checkpoint**: Tenant identifier standardized to tenantId across codebase

---

## Phase 5: User Story 3 - Implement Parquet Archival (Priority: P1)

**Goal**: Archive domain events to Parquet format in tenant-isolated MinIO buckets with 6-month retention

**Independent Test**: Events are archived to tenant-specific Parquet files and lifecycle rules apply

### Implementation for User Story 3

- [ ] T014 [P] [US3] Create Parquet schema classes for DomainEvent in backend/platform-core/src/main/java/com/tam/platform/storage/schema/
- [ ] T015 [P] [US3] Update ObjectStorageService for tenant bucket operations in backend/platform-core/src/main/java/com/tam/platform/storage/ObjectStorageService.java
- [ ] T016 [US3] Implement Parquet writer in RawDataArchiver in backend/platform-core/src/main/java/com/tam/platform/storage/RawDataArchiver.java
- [ ] T017 [US3] Add tenant bucket creation and lifecycle configuration in backend/platform-core/src/main/java/com/tam/platform/storage/
- [ ] T018 [US3] Update ArchivalConfig for Parquet settings in backend/platform-core/src/main/java/com/tam/platform/config/ArchivalConfig.java
- [ ] T019 [US3] Add schema registry integration for Avro schemas in backend/platform-core/src/main/java/com/tam/platform/storage/

**Checkpoint**: Parquet archival with tenant isolation implemented

---

## Final Phase: Polish & Cross-Cutting Concerns

**Purpose**: Final cleanup, documentation, and integration testing

- [ ] T020 Update README with Java 21 and Parquet archival details
- [ ] T021 Run integration tests for tenant isolation
- [ ] T022 Validate Parquet files can be queried
- [ ] T023 Update agent context with final technologies

**Final Checkpoint**: Feature complete and ready for deployment</content>
<parameter name="filePath">/Users/sujoymukherjee/code/TAM/specs/001-refine-platform-spec/tasks.md