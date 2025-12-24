# Platform Spec Refinement Quickstart

## 1. Updated Dependencies

Add to `backend/platform-core/pom.xml`:

```xml
<!-- Parquet Support -->
<dependency>
    <groupId>org.apache.parquet</groupId>
    <artifactId>parquet-avro</artifactId>
    <version>1.13.1</version>
</dependency>
<dependency>
    <groupId>org.apache.hadoop</groupId>
    <artifactId>hadoop-common</artifactId>
    <version>3.3.6</version>
    <exclusions>
        <exclusion>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-log4j12</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>org.apache.avro</groupId>
    <artifactId>avro</artifactId>
    <version>1.11.3</version>
</dependency>
```

## 2. Configuration

Update `application.yml`:

```yaml
platform:
  storage:
    archival:
      bucket-prefix: tenant-
      bucket-suffix: -archive
      compression: SNAPPY
      batch-size: 1000
      retention-days: 180
  minio:
    endpoint: http://minio:9000
    access-key: ${MINIO_ACCESS_KEY}
    secret-key: ${MINIO_SECRET_KEY}
    region: us-east-1
```

## 3. Usage Example: Archival Integration

```java
@Service
public class DomainEventHandler {

    private final RawDataArchiver archiver;

    public DomainEventHandler(RawDataArchiver archiver) {
        this.archiver = archiver;
    }

    @DomainSubscriber(topics = "#")
    public void handleEvent(DomainEvent<?> event) {
        // Process event
        process(event);

        // Archive to Parquet
        try {
            archiver.archive(event);
        } catch (ArchivalException e) {
            log.error("Failed to archive event {}", event.eventId(), e);
        }
    }
}
```

## 4. Schema Evolution

When adding new event types:

1. Add new payload record to Avro schema
2. Add to union in DomainEvent schema
3. Update schema version
4. Deploy schema to registry
5. Update Java code with new payload class

## 5. Monitoring

Monitor archival metrics:

- `archival.events.total` (counter)
- `archival.size.bytes` (gauge)
- `archival.errors.total` (counter)

Use Grafana dashboards to track per-tenant archival stats.