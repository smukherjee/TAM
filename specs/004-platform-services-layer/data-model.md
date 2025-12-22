# Data Model: Core Platform Services

## 1. Tenant Context

Represents the execution context for a request or background job.

```java
public record TenantContext(
    String tenantId,        // e.g., "VIDP", "LIRN", "SYSTEM"
    String domainId,        // e.g., "TAM", "RESOURCE_MGMT"
    TenantStatus status,    // ENABLED, DISABLED
    ZoneId timezone,        // e.g., "Asia/Kolkata"
    Locale locale,          // e.g., "en_IN"
    List<String> roles,     // Opaque roles (e.g. "MANAGER") propagated for domain RBAC
    Map<String, String> metadata, // Custom attributes
    String userId,          // Optional: ID of the user initiating the request
    String correlationId    // Trace ID for observability
) {}

public enum TenantStatus {
    ENABLED,
    DISABLED // Soft-deleted, read-only access to historical data
}
```

## 2. Domain Event

Standard envelope for all business events published via `EventBus`.

```java
public record DomainEvent<T>(
    String eventId,         // UUID
    String eventType,       // e.g., "FlightUpdated", "TurnaroundCreated"
    Instant timestamp,      // UTC timestamp
    TenantContext context,  // The context in which the event occurred
    T payload               // The actual business data (must be Serializable)
) {}
```

## 3. Cache Entry

Internal wrapper for cached data to support tenant scoping and metadata.

```java
public record CacheEntry<T>(
    String key,             // Original key (e.g., "flight:123")
    T value,                // Cached value
    String tenantId,        // Scope
    Instant createdAt,      // For TTL calculation if needed
    long accessCount        // For LRU/metrics
) {}
```

## 4. Platform Configuration

Represents tenant-specific configuration loaded from external source.

```java
public record PlatformConfiguration(
    String namespace,       // e.g., "tam.vidp"
    Map<String, Object> properties, // Key-value pairs
    String version          // Configuration version for audit
) {}
```

## 5. Service Health

Standard health status for platform services.

```java
public record ServiceHealth(
    String serviceName,     // e.g., "EventBus", "CacheService"
    Status status,          // UP, DOWN, DEGRADED
    Instant lastCheck,
    String details,         // Error message or stats
    Map<String, Status> dependencies // Health of underlying infra (Kafka, Redis)
) {
    public enum Status { UP, DOWN, DEGRADED }
}

## 6. Database Schema (Configuration)

Since we decided on a database-backed configuration service in Phase 0, here is the schema:

```sql
CREATE TABLE platform_config (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    domain VARCHAR(50) NOT NULL,
    config_key VARCHAR(100) NOT NULL,
    config_value TEXT,
    version INTEGER DEFAULT 1,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE (tenant_id, domain, config_key)
);

CREATE INDEX idx_platform_config_lookup ON platform_config(tenant_id, domain);
``````
