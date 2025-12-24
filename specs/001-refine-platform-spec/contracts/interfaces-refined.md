# Updated ObjectStorageService Interface

```java
package com.tam.platform.storage;

import java.io.InputStream;
import java.time.Duration;

/**
 * Interface for tenant-aware object storage operations.
 */
public interface ObjectStorageService {

    // Existing methods with tenant context
    void upload(String bucket, String key, InputStream data, long size, String contentType);

    InputStream download(String bucket, String key);

    String getPresignedUrl(String bucket, String key, Duration expiry);

    void delete(String bucket, String key);

    // New tenant-aware methods
    default String getTenantBucket(String tenantId) {
        return "tenant-" + tenantId + "-archive";
    }

    boolean bucketExists(String bucket);

    void createBucket(String bucket);
}
```

## RawDataArchiver Contract

```java
package com.tam.platform.storage;

import com.tam.platform.event.DomainEvent;

/**
 * Contract for archiving domain events to Parquet format in tenant-isolated buckets.
 */
public interface RawDataArchiver {

    /**
     * Archive a domain event to Parquet storage.
     * @param event The domain event to archive
     * @throws ArchivalException if archiving fails
     */
    void archive(DomainEvent<?> event) throws ArchivalException;

    /**
     * Get archival statistics for a tenant.
     * @param tenantId The tenant ID
     * @return Archival statistics
     */
    ArchivalStats getStats(String tenantId);

    class ArchivalException extends Exception {
        public ArchivalException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    record ArchivalStats(
        long totalEvents,
        long totalSizeBytes,
        Instant lastArchived
    ) {}
}
```

## Parquet Schema Contract

```avro
// Schema registry contract for DomainEvent Parquet files
// Version: 1.0.0
// Compatible with Avro union evolution
{
  "type": "record",
  "name": "DomainEvent",
  "namespace": "com.tam.platform.event",
  "fields": [
    {"name": "eventId", "type": "string", "doc": "Unique event identifier"},
    {"name": "timestamp", "type": "long", "logicalType": "timestamp-millis", "doc": "Event timestamp"},
    {"name": "eventType", "type": "string", "doc": "Type of event (e.g., OrderCreated)"},
    {"name": "tenantId", "type": "string", "doc": "Tenant identifier"},
    {"name": "domainId", "type": "string", "doc": "Domain identifier"},
    {"name": "correlationId", "type": "string", "doc": "Trace correlation ID"},
    {"name": "payload", "type": ["OrderCreatedPayload", "UserUpdatedPayload", "FlightUpdatedPayload", "null"], "doc": "Polymorphic event payload"}
  ]
}
```