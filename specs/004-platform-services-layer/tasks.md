---
description: "Task list for Platform Services Layer"
---

# Tasks: Platform Services Layer

**Input**: Design documents from `/specs/004-platform-services-layer/`
**Prerequisites**: plan.md, spec.md, data-model.md, contracts/

**Tests**: Tests are REQUIRED for platform core components to ensure stability.

## Format: `[ID] [P?] [Component] Description`

- **[P]**: Can run in parallel
- **[Component]**: Logical component (Context, EventBus, Cache, Config, Storage)

## Phase 1: Module Setup

**Purpose**: Initialize the reusable library module.

- [x] T001 [Setup] Create `backend/platform-core` Maven module structure and `pom.xml`
- [x] T002 [Setup] Update root `backend/pom.xml` to include `platform-core` as a module
- [x] T003 [Setup] Add dependencies: Spring Boot Starter, Kafka, Redis, Resilience4j, Micrometer (Core + Tracing + Prometheus), MinIO

## Phase 2: Core Services Implementation

**Purpose**: Implement the core building blocks.

### Tenant Context
- [x] T004 [Context] Define `TenantContext` record (tenantId, domainId, roles, correlationId). *Note: Roles are opaque strings; do not implement RBAC logic.*
- [x] T005 [Context] Implement `TenantContextService` using `ThreadLocal` (with `ScopedValue` abstraction prep)
- [x] T006 [Context] Implement `TenantContextFilter` to extract headers (`X-Tenant-ID`, `X-Domain-ID`, `X-Roles`) and initialize context
- [x] T007 [Context] Configure Micrometer Context Propagation for `ExecutorService` wrapping

### Event Bus
- [x] T008 [EventBus] Define `EventBus` interface and `DomainEvent` record
- [x] T009 [EventBus] Implement `KafkaEventBus` producer with `KafkaTemplate` (Must inject `TenantContext.correlationId` into Kafka Headers)
- [x] T010 [EventBus] Implement `InMemoryEventBus` fallback for when Kafka is unavailable
- [x] T011 [EventBus] Create `EventBusAutoConfiguration` to conditionally load Kafka/InMemory
- [x] T012 [EventBus] Implement `@DomainSubscriber` annotation and aspect/listener adapter (Must extract `correlationId` from Headers -> `TenantContext`)
- [x] T029 [EventBus] Implement Transaction Synchronization for event publishing (publish only after DB commit) using `TransactionSynchronizationManager` (Note: Provides at-most-once reliability relative to DB commit)
- [x] T031 [EventBus] Configure Dead Letter Queue (DLQ) for failed event consumers with retry policy (FR-014)

### Cache Service
- [x] T013 [Cache] Define `CacheService` interface (include bulk `getAll`/`putAll` per FR-007 and pattern-based `evict` methods)
- [x] T014 [Cache] Implement `RedisCacheService` with `RedisTemplate`
- [x] T015 [Cache] Add Resilience4j Circuit Breaker to `RedisCacheService` (fallback to DB/empty)
- [x] T016 [Cache] Implement L1 Caffeine local cache (optional, for high-frequency keys)

### Configuration Service
- [x] T017 [Config] Define `ConfigurationService` interface
- [x] T018 [Config] Create `TenantConfiguration` entity and Repository
- [x] T019 [Config] Implement `DatabaseConfigurationService` with caching and hot-reload (use scheduled polling to detect changes and trigger cache invalidation)
- [x] T020 [Config] Implement hierarchical config resolution (Tenant > Domain > Global)
- [x] T032 [Config] Configure single shared DataSource for all domains (deferring FR-021 multi-DB routing for MVP)

### Object Storage
- [x] T021 [Storage] Define `ObjectStorageService` interface
- [x] T022 [Storage] Implement `MinioStorageService` using MinIO Java SDK
- [x] T023 [Storage] Implement presigned URL generation
- [x] T033 [Storage] Implement RawDataArchiver to consume events and write to Object Storage (FR-024)

## Phase 3: Integration & Verification

**Purpose**: Ensure components work together and are testable.

- [x] T024 [AutoConfig] Create `PlatformAutoConfiguration` to bootstrap all services
- [x] T025 [Test] Create `TestContainerSupport` for integration testing (Kafka, Redis, Postgres)
- [x] T026 [Test] Verify Context Propagation in async `@Async` methods
- [x] T027 [Test] Verify Circuit Breaker behavior (simulate Redis down)
- [x] T028 [Test] Verify Event Bus fallback behavior
- [x] T030 [Health] Implement `PlatformHealthIndicator` exposing status of Kafka, Redis, and MinIO connections (FR-012)
- [x] T034 [Observability] Implement custom metrics (Cache Hit/Miss, Tenant Request Count) and verify Trace propagation (FR-016)
- [x] T035 [Observability] Update Grafana dashboards to support multi-tenancy (add `$tenant` variable, filter SQL/Prometheus queries) and align with new Platform metrics
- [x] T036 [Data] Update ksqlDB scripts (`ksql-init.sql`) to handle domain-prefixed topics and extract tenant context from headers (FR-022)
- [x] T037 [Data] Update NiFi setup scripts (`setup-flows.sh`) to inject `X-Tenant-ID` header and use domain-prefixed topics (FR-022)
