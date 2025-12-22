# Phase 0: Research & Architecture Decisions

**Feature**: Core Platform Services Layer (`004-platform-services-layer`)
**Date**: 2025-12-22

## 1. Tenant Context Propagation (Java 21)

### Decision
Use **Micrometer Context Propagation** library to bridge `ThreadLocal` to `CompletableFuture` and Virtual Threads, while designing the `TenantContextService` interface to be compatible with `ScopedValue` in the future.

### Rationale
*   **ScopedValue Limitations**: While `ScopedValue` is efficient, it requires the scope to be strictly lexical (try-with-resources style). Standard Spring `@Async` and `CompletableFuture` do not strictly adhere to this structure without manual wrapping.
*   **Compatibility**: Micrometer Context Propagation is the standard way in Spring Boot 3 to propagate context across thread boundaries, including Virtual Threads.
*   **Future Proofing**: The `TenantContextService` interface will hide the implementation, allowing a switch to pure `ScopedValue` once structured concurrency is ubiquitous in the codebase.

### Alternatives Considered
*   **Pure ScopedValue**: Rejected because it requires significant changes to how async tasks are spawned (must use `StructuredTaskScope`), which might be too invasive for a Phase 1 refactor.
*   **InheritableThreadLocal**: Rejected due to potential memory leaks and uncontrolled propagation in thread pools.

## 2. Platform Module Structure (Spring Boot Starter)

### Decision
Create a multi-module Maven project where `platform-core` is a library (jar) containing the contracts and default implementations. It will use Spring Boot's Auto-Configuration mechanism.

### Rationale
*   **Zero Config**: Developers should just add the dependency, and it works (InMemory defaults).
*   **Standardization**: Using `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` is the Spring Boot 3 standard.
*   **Conditional Loading**: `@ConditionalOnProperty` will allow switching between `InMemoryEventBus` (Test/Dev) and `KafkaEventBus` (Prod) via `application.yml`.

## 3. Resilience & Fault Tolerance

### Decision
Use **Resilience4j** specifically for the `CacheService` circuit breaker.

### Rationale
*   **Granularity**: We need to protect the DB from cache failure storms.
*   **Integration**: Resilience4j integrates well with Spring Boot metrics.
*   **Configuration**: We will define default `CircuitBreakerConfig` in code but allow overrides via `application.yml`.

## 4. Kafka Topic Naming & Multi-Tenancy

### Decision
Enforce a strict topic naming convention: `{domain}.{tenant}.{eventType}`.

### Rationale
*   **Isolation**: Allows ACLs to be applied per tenant or per domain in the future.
*   **Filtering**: Consumers can subscribe to `tam.vidp.*` to get all events for a tenant, or `tam.*.flight-updated` for analytics across tenants.
*   **Simplicity**: Avoids complex header-based routing logic in the `EventBus` implementation.

## 5. Database Routing

### Decision
Use Spring's `AbstractRoutingDataSource` for domain-scoped database routing.

### Rationale
*   **Dynamic Routing**: Allows switching datasources based on the `domainId` in `TenantContext`.
*   **Spring Native**: Standard Spring JDBC approach, works with JPA/Hibernate.
*   **Configurable**: Datasources can be defined in `application.yml` and mapped to domains.

## 6. Object Storage Client

### Decision
Use **MinIO Java SDK** for S3-compatible storage.

### Rationale
*   **Lightweight**: Much smaller footprint than the full AWS SDK v2.
*   **Compatibility**: Native support for MinIO features while maintaining S3 API compatibility.
*   **Local Dev**: Works perfectly with the local MinIO container without complex IAM mocking.

## 7. Configuration Service Implementation

### Decision
Implement **Database-backed Configuration** with a polling mechanism.

### Rationale
*   **Simplicity**: Avoids the need for a separate Spring Cloud Config Server infrastructure.
*   **Multi-tenancy**: Configuration can be easily scoped by `(tenant_id, domain, key)` in a relational table.
*   **Hot Reload**: A simple poller (running every 30s) or an event-based refresh (via EventBus) can trigger `@ConfigurationProperties` rebinding.

## 8. Transactional Event Publishing

### Decision
Use **Spring Transaction Synchronization** (`TransactionSynchronizationManager`).

### Rationale
*   **Reliability**: Ensures events are only published to Kafka *after* the database transaction commits.
*   **Simplicity**: No external components required (unlike Outbox pattern which needs a CDC connector).
*   **Trade-off**: There is a small risk of "dual write" failure (DB commits, Kafka fails). This is acceptable for MVP given the "Edge Case" requirement to buffer and retry failed events in-memory.

