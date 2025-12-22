# Platform Service Contracts (Java Interfaces)

## 1. TenantContextService

Manages the current tenant context for the executing thread.

```java
public interface TenantContextService {
    // Set context for current thread
    void setContext(TenantContext context);
    
    // Get current context (throws exception if not set, unless strictly system)
    TenantContext getContext();
    
    // Clear context
    void clear();
    
    // Execute a runnable with a specific context (handles cleanup)
    void runWithContext(TenantContext context, Runnable task);
    
    // Execute a callable with a specific context
    <T> T callWithContext(TenantContext context, Callable<T> task) throws Exception;
    
    // Wrap a Runnable for async execution (propagates current context)
    Runnable wrap(Runnable task);
    
    // Wrap a Callable for async execution
    <T> Callable<T> wrap(Callable<T> task);
}
```

## 2. EventBus

Publishes domain events to registered handlers.

```java
public interface EventBus {
    // Publish an event asynchronously
    <T> void publish(DomainEvent<T> event);
    
    // Register a handler dynamically
    <T> void subscribe(String eventType, EventHandler<T> handler);
    
    // Unsubscribe a handler
    void unsubscribe(String eventType, EventHandler<?> handler);
}

@FunctionalInterface
public interface EventHandler<T> {
    void handle(DomainEvent<T> event);
}
```

## 3. CacheService

Provides tenant-scoped caching operations.

```java
public interface CacheService {
    // Get value (returns Optional.empty() on miss or error)
    <T> Optional<T> get(String key, Class<T> type);
    
    // Put value with default TTL
    <T> void put(String key, T value);
    
    // Put value with specific TTL
    <T> void put(String key, T value, Duration ttl);
    
    // Delete value
    void evict(String key);
    
    // Pattern-based eviction (e.g., "flight:*")
    void evictPattern(String pattern);
}
```

## 4. ConfigurationService

Access to tenant-specific configuration.

```java
public interface ConfigurationService {
    // Get typed property
    <T> T getProperty(String key, Class<T> type);
    
    // Get property with default
    <T> T getProperty(String key, Class<T> type, T defaultValue);
    
    // Register a listener for configuration changes
    void addChangeListener(String keyPrefix, ConfigurationChangeListener listener);
}

@FunctionalInterface
public interface ConfigurationChangeListener {
    void onChange(String key, Object newValue);
}
```

## 5. ObjectStorageService

Abstraction for file storage (S3/MinIO).

```java
public interface ObjectStorageService {
    // Upload file
    void upload(String path, InputStream content, long size, String contentType);
    
    // Download file
    InputStream download(String path);
    
    // Generate presigned URL for direct client access
    URL generatePresignedUrl(String path, Duration expiration, HttpMethod method);
    
    // Delete file
    void delete(String path);
}
```
