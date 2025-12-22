# Platform Services Quickstart

## 1. Add Dependency

Add the `platform-core-starter` to your domain module's `pom.xml`:

```xml
<dependency>
    <groupId>com.tam.platform</groupId>
    <artifactId>platform-core-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## 2. Configuration

Configure the platform in `application.yml`:

```yaml
platform:
  eventbus:
    type: kafka # or 'memory' for tests
    kafka:
      bootstrap-servers: localhost:9092
      topic-prefix: tam
  cache:
    type: redis # or 'memory' for tests
    redis:
      host: localhost
      port: 6379
  tenant:
    default-zone: Asia/Kolkata
```

## 3. Usage Example: Turnaround Service

### Injecting Services

```java
@Service
public class TurnaroundService {
    private final TenantContextService tenantContext;
    private final EventBus eventBus;
    private final CacheService cache;

    public TurnaroundService(TenantContextService tenantContext, EventBus eventBus, CacheService cache) {
        this.tenantContext = tenantContext;
        this.eventBus = eventBus;
        this.cache = cache;
    }

    public void createTurnaround(String flightId) {
        // 1. Access Tenant Context
        TenantContext context = tenantContext.getContext();
        System.out.println("Processing for tenant: " + context.tenantId());

        // 2. Use Cache
        String cacheKey = "flight:" + flightId;
        Flight flight = cache.get(cacheKey, Flight.class)
            .orElseThrow(() -> new RuntimeException("Flight not found"));

        // 3. Publish Event
        TurnaroundCreated eventPayload = new TurnaroundCreated(flightId, Instant.now());
        DomainEvent<TurnaroundCreated> event = new DomainEvent<>(
            UUID.randomUUID().toString(),
            "TurnaroundCreated",
            Instant.now(),
            context,
            eventPayload
        );
        
        eventBus.publish(event);
    }
}
```

## 4. Testing

Use the test slice annotation to load in-memory implementations:

```java
@SpringBootTest
@ActiveProfiles("test") // Sets platform.eventbus.type=memory
class TurnaroundServiceTest {
    @Autowired
    private TurnaroundService service;
    
    @Autowired
    private EventBus eventBus; // In-memory instance

    @Test
    void testCreateTurnaround() {
        // ...
    }
}
```
