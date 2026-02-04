# Feature Specification: Core Platform Services Layer

**Feature Branch**: `004-platform-services-layer`  
**Created**: 2025-12-22  
**Status**: Draft  
**Input**: Design core platform services layer to decouple business domains from infrastructure concerns, implementing TenantContextService, EventBus, CacheService, and foundational platform abstractions for multi-tenant scalability. Target Java 21+ for virtual threads and ScopedValue support.

## User Scenarios & Testing

### User Story 1 - Platform Developer Implements New Business Domain (Priority: P1)

A platform developer needs to add a new business domain (e.g., "Resource Management" for ground equipment) without modifying existing infrastructure code or duplicating cross-cutting concerns like caching, messaging, or tenant isolation.

**Why this priority**: This is the core value proposition - enabling rapid domain addition without platform changes. Current architecture requires developers to understand and modify infrastructure code for every new domain.

**Independent Test**: Create a new domain service that uses TenantContextService for multi-tenancy, EventBus for domain events, and CacheService for data caching - all without touching infrastructure layer. Verify the service can be deployed alongside existing domains without conflicts.

**Acceptance Scenarios**:

1. **Given** a new domain service is being developed, **When** the developer needs tenant isolation, **Then** they inject TenantContextService and access current tenant context without implementing tenant filtering logic
2. **Given** a domain service needs to publish events, **When** they use EventBus.publish(), **Then** events are routed to all registered handlers without the service knowing about Kafka
3. **Given** a domain service needs caching, **When** they inject CacheService, **Then** data is cached with automatic tenant scoping without Redis-specific code
4. **Given** multiple domains are deployed, **When** platform services are updated, **Then** all domains benefit from improvements without code changes

---

### User Story 2 - Platform Abstractions Enable Future Cloud Portability (Priority: P3)

Platform architects design infrastructure abstractions (EventBus, CacheService, ObjectStorageService) that can support multiple implementations without requiring business code changes, enabling future cloud migration.

**Why this priority**: Cloud portability is a strategic architectural requirement but actual cloud deployment is deferred. Focus is on correct abstraction design and local verification, not production cloud testing.

**Independent Test**: Implement platform service interfaces with in-memory test doubles and verify business logic works identically with mock implementations. Document configuration contracts for future Kafka/EventHub swap.

**Acceptance Scenarios**:

1. **Given** platform service interfaces are defined, **When** business code uses EventBus, **Then** it has no Kafka-specific imports or dependencies
2. **Given** multiple CacheService implementations exist (InMemory, Redis), **When** configuration selects implementation, **Then** business logic functions identically
3. **Given** platform abstractions are documented, **When** reviewing architecture, **Then** substitution points are clear for cloud equivalents (EventHub, Azure Redis, S3)
4. **Given** local development environment, **When** switching from Redpanda to Kafka via config, **Then** system works without code changes (validates portability design)

---

### User Story 3 - Development Team Adds Observability (Priority: P2)

Development team needs to add distributed tracing and audit logging across all domains to troubleshoot production issues without modifying each service individually.

**Why this priority**: Centralized observability is critical for production support but shouldn't require changes to every service. Platform layer should provide this transparently.

**Independent Test**: Enable distributed tracing in platform configuration. Verify trace IDs are automatically propagated across EventBus events, cache operations, and external API calls without domain services implementing tracing logic.

**Acceptance Scenarios**:

1. **Given** distributed tracing is enabled, **When** a flight update triggers turnaround events, **Then** a single trace spans FlightConsumer → EventBus → TurnaroundService → Database
2. **Given** audit requirements exist, **When** any domain modifies data, **Then** AuditLogService automatically captures who, what, when without explicit audit calls in business logic
3. **Given** performance issues occur, **When** operations reviews traces, **Then** they identify bottlenecks across platform boundaries (cache miss, slow database, Kafka lag)

---

### User Story 4 - Multi-Tenant Onboarding (Priority: P2)

System administrator onboards a new airport (tenant) with custom configuration, data isolation, and specific business rules without code deployment.

**Why this priority**: Multi-tenancy is a core architectural requirement. Current scattered `tenantId` handling makes tenant isolation fragile and configuration difficult.

**Independent Test**: Onboard new tenant "LIRN" with custom speed limits, cache TTL, and feature flags. Verify complete data isolation from existing "VIDP" tenant and custom behavior without code changes.

**Acceptance Scenarios**:

1. **Given** new tenant "LIRN" is registered, **When** flight data arrives for LIRN, **Then** it's stored separately from VIDP data enforced by TenantContextService
2. **Given** tenant-specific configuration, **When** LIRN services execute, **Then** they use LIRN speed limits (30 km/h) vs VIDP limits (25 km/h) from ConfigurationService
3. **Given** tenant isolation, **When** LIRN user queries data, **Then** they only see LIRN flights/vehicles enforced at platform layer
4. **Given** tenant customization, **When** feature flags are enabled for LIRN only, **Then** new features are available to LIRN while VIDP continues with stable version

---

### User Story 5 - Developer Tests Business Logic in Isolation (Priority: P3)

Developer writes unit tests for domain logic without starting Kafka, Redis, or database infrastructure.

**Why this priority**: Fast feedback loops require testable code. Current infrastructure coupling makes unit testing difficult, forcing reliance on slow integration tests.

**Independent Test**: Write unit test for TurnaroundService using mock implementations of EventBus and CacheService. Verify business logic executes correctly in milliseconds without Docker containers.

**Acceptance Scenarios**:

1. **Given** a unit test for TurnaroundService, **When** test uses MockEventBus, **Then** domain logic is verified without Kafka running
2. **Given** a unit test needs cached data, **When** test uses InMemoryCacheService, **Then** cache behavior is simulated without Redis
3. **Given** platform interfaces are defined, **When** tests inject mocks, **Then** 100% code coverage is achievable for business logic
4. **Given** CI pipeline runs, **When** unit tests execute, **Then** they complete in under 10 seconds without infrastructure startup

---

### Edge Cases

- When tenant context is not set (e.g., background job, system event), TenantContextService automatically uses "SYSTEM" tenant for data isolation
- When event bus infrastructure fails (Kafka down), events are queued in bounded in-memory buffer (10,000 events), retried with exponential backoff, oldest dropped if buffer full with metric alert
- When cache service is unavailable (Redis down), operations automatically fallback to database queries with circuit breaker stopping cache attempts after repeated failures, resuming when Redis recovers
- Circular event dependencies are detected at publish time by tracking chain depth in correlation metadata; if depth exceeds 5, publish is rejected with warning log and metric emitted
- When a tenant is offboarded ("deleted"), the platform performs a soft delete: tenant is marked DISABLED, all new writes and logins are rejected, historical data remains readable for audit/reporting, and tenant-specific cache entries are cleared

## Architectural Principles

### Lean Core & Authorization

- **Identity Propagation Only**: The Platform Core is responsible ONLY for propagating the `TenantContext` (Identity, Tenant ID, Roles) to downstream services.
- **Decentralized Enforcement**: The Platform Core MUST NOT enforce business-level permissions (RBAC). Domain services (e.g., Turnaround, Flight) are solely responsible for checking if the propagated roles allow the requested operation.
- **Agnostic Roles**: The Platform Core treats roles as opaque strings (e.g., `["ROLE_A", "ROLE_B"]`) and does not validate their semantic meaning.
- How does system handle cache eviction during high-load tenant-specific operations?
- What happens when configuration changes while services are running?
- Tenant-specific rate limiting is deferred to a future phase; Phase 1 relies on infrastructure-level limits (Kafka quotas, Redis maxmemory)

## Clarifications

### Session 2025-12-22

- Q: When TenantContextService propagates tenant context across async operations (FR-006), how should it handle Java 21 virtual threads and structured concurrency patterns? → A: Use thread-local storage with automatic propagation to virtual threads via ScopedValue (Java 21 feature), accepting that some manual context passing may be needed for structured concurrency
- Q: When multiple events are published for the same tenant (FR-018 mentions tenant-based filtering), what ordering guarantees does EventBus provide? → A: Strict FIFO ordering per tenant per event type (FlightUpdated events for VIDP arrive in order, but TurnaroundCreated may interleave)
- Q: When tenant context is not set (background jobs, system events), what should TenantContextService do? → A: Use a default "SYSTEM" tenant for all non-user operations, treating system operations as a special tenant with its own data isolation
- Q: When EventBus encounters infrastructure failure (Kafka down per edge case), what should the failure handling strategy be? → A: Queue events locally in bounded in-memory buffer (e.g., 10,000 events), retry with exponential backoff, drop oldest if buffer full with metric alert
- Q: When CacheService is unavailable (Redis down per edge case), what should the fallback behavior be? → A: Automatically fallback to database queries, use circuit breaker to stop cache attempts after repeated failures, resume when Redis recovers
- Q: When a tenant is deleted or offboarded, what should happen to its existing data and operations? → A: Treat deletion as a soft delete / disable: mark tenant as DISABLED, reject new writes and logins, keep historical data read-only for audit/reporting, and clear tenant cache entries
- Q: How should circular event dependencies (Event A → Event B → Event A) be detected and prevented? → A: Detect at publish time by tracking event chain depth via correlation metadata; reject publish if depth exceeds threshold (default 5), log warning and emit metric
- Q: How should tenant-specific rate limits be enforced when multiple tenants share infrastructure? → A: Defer to future phase; rate limiting is out of scope for Phase 1 MVP, rely on infrastructure-level limits (Kafka quotas, Redis maxmemory) for now
- Q: When multiple multi-tenant applications (different business domains) are onboarded, how should database isolation work? → A: Domain-scoped database routing via ConfigurationService: each domain maps to its own database connection (TAM→DB1, ResourceMgmt→DB2); TenantContextService includes domainId alongside tenantId; platform routes queries to correct datasource
- Q: When tenants within the same domain have different business rule thresholds (e.g., LIRN alerts at 30 km/h, VIDP at 25 km/h), how should the platform support this? → A: ConfigurationService with tenant-scoped parameters: store thresholds as tenant config (e.g., `tam.vidp.alert.speed_threshold=25`); business logic reads from ConfigurationService using current TenantContext; hot-reload supported
- Q: How should Kafka topics, NiFi flows, and ksqlDB streams be organized when a domain has multiple CEP pipelines? → A: Domain-prefixed topics with shared infrastructure: topics named `{domain}.{tenant}.{eventType}` (e.g., `tam.vidp.flight-updated`), NiFi processor groups per domain, ksqlDB streams per domain; clear separation with shared Kafka cluster

## Requirements

### Functional Requirements

- **FR-001**: System MUST provide TenantContextService that manages current tenant scope using thread-local storage for request-scoped tenant isolation
- **FR-002**: System MUST provide EventBus that allows publish/subscribe to domain events with reliable delivery (best-effort after DB commit), async processing support, and FIFO ordering per tenant per event type
- **FR-003**: System MUST provide CacheService with automatic tenant scoping that supports get, put, delete operations with configurable TTL
- **FR-004**: System MUST define platform service interfaces that can support multiple infrastructure implementations (Kafka/EventHubs, Redis/Hazelcast, MinIO/S3) via configuration without code changes (initial delivery includes local implementations only)
- **FR-005**: EventBus MUST support event handlers to be registered dynamically and execute in order of registration priority
- **FR-006**: TenantContextService MUST propagate tenant context across async operations (CompletableFuture, @Async methods) using ThreadLocal (with planned migration to Java 21 ScopedValue), ensuring context is available in worker threads
- **FR-007**: CacheService MUST support bulk operations (multi-get, multi-put) for performance optimization
- **FR-008**: System MUST provide ObjectStorageService abstraction supporting upload, download, delete, and list operations
- **FR-009**: Platform services MUST integrate with Spring Boot auto-configuration for zero-configuration developer experience
- **FR-010**: EventBus MUST support transactional event publishing (events only published if database transaction commits) using Spring Transaction Synchronization. Note: This provides "at-most-once" delivery relative to the DB commit in crash scenarios, which is acceptable for MVP.
- **FR-011**: TenantContextService MUST support tenant metadata storage (timezone, locale, custom attributes) accessible to all services
- **FR-012**: Platform services MUST expose health check endpoints for monitoring service availability
- **FR-013**: CacheService MUST support pattern-based invalidation (delete all keys matching tenant:flight:*)
- **FR-014**: EventBus MUST support dead letter queue for events that fail processing after retry attempts
- **FR-015**: System MUST provide ConfigurationService for tenant-specific configuration with hot-reload support
- **FR-016**: Platform services MUST emit metrics (cache hit rate, event processing latency, tenant request count) and propagate distributed tracing context (traceId, spanId) via Micrometer to the Observability Infrastructure
- **FR-017**: TenantContextService MUST enforce tenant context is set for all operations, using a special "SYSTEM" tenant identifier for background jobs and system operations to maintain data isolation
- **FR-018**: EventBus MUST support event filtering based on tenant context (LIRN handlers don't receive VIDP events)
- **FR-019**: ObjectStorageService MUST support presigned URLs for direct client upload/download without proxying through backend
- **FR-020**: Platform services MUST support graceful degradation - cache operations fallback to database with circuit breaker pattern (open circuit after 5 consecutive failures, half-open retry after 30s), event publish failures queue locally in bounded 10,000-event buffer with exponential backoff retry, dropping oldest events and emitting metrics when buffer is full
- **FR-021**: System MUST support domain-scoped database routing where each business domain (e.g., TAM, ResourceMgmt) maps to its own database via ConfigurationService; TenantContext includes domainId which platform uses to route queries to correct datasource
- **FR-022**: EventBus MUST use domain-prefixed topic naming convention `{domain}.{tenant}.{eventType}` for Kafka topics to enable clear separation across domains while sharing infrastructure
- **FR-023**: ConfigurationService MUST support hierarchical tenant-scoped parameters in format `{domain}.{tenant}.{key}` enabling per-tenant business rule thresholds (e.g., alert speed limits) with hot-reload support
- **FR-024**: System MUST provide a RawDataArchiver service that consumes domain events and archives them to ObjectStorageService for audit and replay purposes. Archive format MUST be Parquet files with tenant-isolated buckets (e.g., `tenant-{tenantId}-archive`). Data MUST be retained for 6 months with automatic cleanup. No failure recovery implemented for MVP.

### Key Entities

- **TenantContext**: Represents the current execution scope containing domain ID (e.g., "tam", "resource-mgmt"), tenant ID (tenantId), tenant status (ENABLED/DISABLED), timezone, locale, custom metadata, and user information. Provides thread-local access pattern for request-scoped isolation using Java 21 ScopedValue for virtual thread compatibility. Special "SYSTEM" tenant ID is used for background jobs and non-user operations. Tenant deletion is modeled as DISABLED status with read-only access to historical data and no new writes. Domain ID is used for database routing and Kafka topic prefixing.

- **DomainEvent**: Base abstraction for all business events containing event ID, type, timestamp, tenant context, correlation ID for tracing, and serializable payload. Supports both synchronous and asynchronous processing.

- **CacheEntry**: Represents cached data with key, value, TTL, tenant scope, creation timestamp, and access count. Supports automatic serialization/deserialization.

- **EventHandler**: Registered callback for domain events with priority, filter criteria, execution mode (sync/async), retry policy, and dead letter handling.

- **PlatformConfiguration**: Tenant-specific configuration with namespace, key-value pairs, change listeners, and version tracking for audit.

- **StorageObject**: Represents object in storage with path, tenant scope, content type, size, metadata, and checksum for integrity verification.

- **ServiceHealth**: Health status of platform services with status (UP/DOWN/DEGRADED), last check timestamp, error details, and dependency health.

## Success Criteria

### Measurable Outcomes (Phase 1)

- **SC-001**: Developers can add a new business domain service in under 4 hours using platform services without modifying infrastructure code
- **SC-002**: Platform service interfaces are verified to have zero cloud-specific dependencies, enabling future deployment to Azure/AWS with configuration-only changes
- **SC-003**: Unit test execution time for business logic is under 5 seconds without requiring Docker containers
- **SC-006**: Multi-tenant data isolation verified with 100% data separation in tests simulating concurrent tenant operations
- **SC-009**: Configuration changes propagate to all running services within 30 seconds without requiring restart
- **SC-010**: Distributed traces show complete request flow across platform boundaries with zero manual instrumentation in business code

### Deferred Performance Targets (Future Phase)

The following performance targets are intentionally deferred and will be validated in a later scaling phase. They do not gate completion of this feature.

- **SC-004 (Deferred)**: Platform layer handles 10,000 events per second with less than 50ms median latency from publish to handler execution
- **SC-005 (Deferred)**: Cache hit rate exceeds 80% for tenant-scoped queries reducing database load by 60%
- **SC-007 (Deferred)**: Platform service health checks respond within 100ms and correctly report degraded state when dependencies fail under peak load
- **SC-008 (Deferred)**: Event processing failures are automatically retried with exponential backoff, achieving 99.9% eventual delivery success rate under peak load

## Assumptions

- Spring Boot framework continues to be the application runtime
- Java 21+ is the target JVM version providing virtual threads for async processing
- Existing database schema remains PostgreSQL/TimescaleDB (no migration to NoSQL)
- Current Kafka topic structure is compatible with EventBus abstraction
- Tenant identifier (tenantId) uniquely identifies each airport deployment
- Network latency between services is under 10ms for synchronous operations
- Cache eviction policies (LRU) are acceptable for all use cases
- Event ordering is FIFO per tenant per event type (e.g., FlightUpdated events for VIDP are ordered, but different event types may interleave)
- Redis cluster supports tenant-based partitioning for horizontal scaling
- Object storage supports S3-compatible API across all cloud providers

## Out of Scope

- Production deployment to cloud environments (Azure/AWS/GCP) - architecture supports it but actual deployment and testing is deferred to future phase
- Azure Event Hubs, Azure Redis, AWS MSK implementations - interfaces designed for these but local/Docker implementations only for this phase
- Complete migration of existing code to use platform services (gradual adoption planned)
- Workflow orchestration service (Camunda/Temporal integration) - future feature
- Saga coordinator for distributed transactions - future feature
- Full-text search service integration - future feature
- Advanced geo-spatial queries beyond basic distance calculations
- Real-time notification service (email/SMS) - future feature
- API Gateway with rate limiting and authentication - future feature
- Tenant-specific rate limiting at platform layer - future feature; Phase 1 relies on infrastructure-level limits
- Schema registry and Avro serialization - existing NiFi handles this
- Multi-region active-active deployment - future architecture evolution
- Machine learning model serving platform - separate initiative
- GraphQL API layer - REST is sufficient for current needs
- Service mesh (Istio) integration - Kubernetes deployment is future phase
- Custom authentication/authorization service - Spring Security is sufficient
- Data lake integration and analytics pipelines - existing Superset/Grafana handles this
- Video stream processing for CV events - existing NiFi handles ingestion

## Dependencies

- Existing database schema must support tenant isolation via `icao_code` column
- Current Kafka topics must include tenant identifier in message headers or payload
- Redis instance must support keyspace partitioning for tenant isolation
- MinIO/S3 bucket structure must support tenant-based folder hierarchy
- Spring Boot autoconfiguration must be compatible with platform service initialization order
- Existing domain services (FlightService, TurnaroundService) must be refactorable to use platform interfaces
- Monitoring stack (Prometheus/Grafana) must support platform metrics
- CI/CD pipeline must support modular builds for platform-core library

## Constraints

- Platform services must be backward compatible during gradual migration period
- Cannot break existing REST APIs during platform layer introduction
- Must support both synchronous and asynchronous event processing patterns
- Platform abstractions must not introduce more than 10ms overhead compared to direct implementation
- Cache service must support at least 1 million keys per tenant
- Event bus must handle at least 10,000 messages per second per tenant
- Platform configuration must be externalized (not hardcoded in services)
- Platform services must work with existing Docker Compose development environment
- All platform services must be testable without external infrastructure
- Platform layer must support Java records for event payloads (immutability)
- Memory footprint of platform services must be under 256MB at idle
- Circuit breaker thresholds must be configurable per environment (dev may tolerate more failures than prod)

## Risks & Mitigations

**Risk**: Existing code tightly coupled to infrastructure makes extraction difficult  
**Mitigation**: Implement Strangler Fig pattern - new code uses platform, old code gradually migrated. Both patterns supported during transition.

**Risk**: Performance overhead of abstraction layer impacts latency SLAs  
**Mitigation**: Benchmark platform services against direct implementations. Use virtual threads for async operations. Monitor 95th percentile latency.

**Risk**: EventBus semantic differences between Kafka and Azure Event Hubs cause issues  
**Mitigation**: Define strict EventBus contract based on common capabilities. Integration tests verify behavior across implementations.

**Risk**: Tenant context not propagated correctly in async operations causing data leakage  
**Mitigation**: Implement context propagation for CompletableFuture, @Async, and virtual threads. Add automated tests verifying tenant isolation.

**Risk**: Cache invalidation bugs cause stale data across tenants  
**Mitigation**: Implement cache versioning and automatic invalidation on entity updates. Circuit breaker pattern for cache failures.

**Risk**: Learning curve for developers unfamiliar with event-driven architecture  
**Mitigation**: Provide comprehensive documentation, code examples, and pair programming sessions. Start with simple use cases.

**Risk**: Platform services become another "framework" that constrains flexibility  
**Mitigation**: Keep abstractions minimal and focused. Support escape hatches for advanced use cases. Regular architecture reviews.

## Related Features

- **001-mvp-core-tracking**: Current implementation that will be refactored to use platform services
- **002-architecture-docs**: Documents platform architecture patterns this feature implements
- **003-nifi-ksqldb-integration**: External integrations that will consume platform EventBus

## Notes

This platform layer addresses the critical architectural debt identified in the senior architect review. The design follows Hexagonal Architecture (Ports & Adapters) principles to ensure business logic independence from infrastructure.

**Cloud Portability Strategy**: While cloud portability is a core architectural requirement, actual deployment to Azure/AWS is deferred to a future phase. This feature focuses on:

- Defining clean abstraction interfaces that eliminate cloud-specific dependencies
- Implementing and validating with local/Docker infrastructure (Kafka, Redis, MinIO)
- Documenting configuration contracts for future cloud substitution
- Ensuring business logic has zero coupling to infrastructure implementation

The design is deliberately cloud-ready (interfaces can accommodate EventHubs, Azure Redis, S3) but implementation and testing is limited to local environments. This allows architecture validation without cloud costs and complexity.

Key design principles:

- **Contract-first**: Define interfaces before implementations
- **Dependency inversion**: Business domains depend on platform abstractions, not implementations
- **Single responsibility**: Each platform service has one clear purpose
- **Open/closed**: Easy to extend with new implementations without modifying existing code
- **Testability**: All platform services have in-memory test doubles

The phased rollout strategy allows gradual adoption without big-bang rewrite:

1. Phase 1: Implement core platform services (TenantContext, EventBus, CacheService)
2. Phase 2: Refactor one domain (Turnaround) to use platform services
3. Phase 3: Migrate remaining domains (Flight tracking, Alerting)
4. Phase 4: Remove direct infrastructure dependencies from business code

Success will be measured by developer velocity improvements and deployment flexibility gains.
