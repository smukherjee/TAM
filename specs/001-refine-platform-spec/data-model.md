# Data Model: Platform Spec Refinement

## 1. Parquet Schema for DomainEvent Archival

Using Avro union schema for polymorphic payloads with tenant isolation.

```avro
{
  "type": "record",
  "name": "DomainEvent",
  "fields": [
    {"name": "eventId", "type": "string"},
    {"name": "timestamp", "type": "long", "logicalType": "timestamp-millis"},
    {"name": "eventType", "type": "string"},
    {"name": "tenantId", "type": "string"},
    {"name": "domainId", "type": "string"},
    {"name": "correlationId", "type": "string"},
    {"name": "payload", "type": [
      "OrderCreatedPayload",
      "UserUpdatedPayload",
      "FlightUpdatedPayload",
      "null"
    ]}
  ]
}

{
  "type": "record",
  "name": "OrderCreatedPayload",
  "fields": [
    {"name": "orderId", "type": "string"},
    {"name": "amount", "type": "double"},
    {"name": "customerId", "type": "string"}
  ]
}

{
  "type": "record",
  "name": "UserUpdatedPayload",
  "fields": [
    {"name": "userId", "type": "string"},
    {"name": "newEmail", "type": ["null", "string"], "default": null},
    {"name": "newRole", "type": ["null", "string"], "default": null}
  ]
}

{
  "type": "record",
  "name": "FlightUpdatedPayload",
  "fields": [
    {"name": "flightId", "type": "string"},
    {"name": "status", "type": "string"},
    {"name": "altitude", "type": ["null", "double"], "default": null}
  ]
}
```

## 2. Tenant-Isolated Bucket Model

```java
public record TenantBucket(
    String tenantId,
    String bucketName,  // tenant-{tenantId}-archive
    String region,
    EncryptionConfig encryption,  // SSE-KMS
    LifecycleConfig lifecycle  // 6-month retention
) {}

public record EncryptionConfig(
    String algorithm,  // aws:kms
    String keyId
) {}

public record LifecycleConfig(
    int transitionDays,  // 30
    String storageClass,  // STANDARD_IA or GLACIER
    int expirationDays  // 180
) {}
```

## 3. RawDataArchiver Configuration

```java
public record ArchivalConfig(
    String bucketPrefix,  // tenant-
    String bucketSuffix,  // -archive
    CompressionCodec compression,  // SNAPPY
    int batchSize,  // 1000
    Duration retentionPeriod  // 6 months
) {}
```

## 4. Schema Evolution Rules

- Add new payload types to union (backward compatible)
- Add optional fields to existing payloads
- Version schemas semantically (major.minor.patch)
- Store schemas in registry for validation