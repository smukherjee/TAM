# Platform Service Interfaces

## TenantContextService

```java
package com.tam.platform.context;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

public interface TenantContextService {
    /**
     * Get the current tenant context.
     * @throws IllegalStateException if context is not set
     */
    TenantContext getContext();

    /**
     * Execute a runnable within a specific tenant context.
     * Uses ScopedValue/ThreadLocal for propagation.
     */
    void runWithContext(TenantContext context, Runnable task);

    /**
     * Execute a supplier within a specific tenant context.
     */
    <T> T callWithContext(TenantContext context, Callable<T> task) throws Exception;

    /**
     * Set the context for the current thread (use with caution, prefer runWithContext).
     */
    void setContext(TenantContext context);

    /**
     * Clear the context for the current thread.
     */
    void clear();
}
```

## EventBus

```java
package com.tam.platform.event;

public interface EventBus {
    /**
     * Publish an event to the bus.
     * If a transaction is active, the event is buffered and sent after commit.
     */
    <T> void publish(DomainEvent<T> event);

    /**
     * Register a handler for a specific event type.
     */
    <T> void subscribe(Class<T> eventType, EventHandler<T> handler);
}
```

## CacheService

```java
package com.tam.platform.cache;

import java.time.Duration;
import java.util.Optional;

public interface CacheService {
    /**
     * Get a value from cache.
     * Automatically scopes key to current tenant.
     */
    <T> Optional<T> get(String key, Class<T> type);

    /**
     * Put a value into cache.
     * Automatically scopes key to current tenant.
     */
    void put(String key, Object value, Duration ttl);

    /**
     * Delete a value from cache.
     */
    void evict(String key);
    
    /**
     * Delete all keys matching a pattern for the current tenant.
     * e.g. "flight:*" -> deletes "tenant:flight:*"
     */
    void evictPattern(String pattern);
}
```

## ConfigurationService

```java
package com.tam.platform.config;

import java.util.Optional;

public interface ConfigurationService {
    /**
     * Get a configuration value for the current tenant and domain.
     * e.g. key="alert.speed_threshold" -> looks up "tam.vidp.alert.speed_threshold"
     */
    <T> Optional<T> getProperty(String key, Class<T> targetType);

    /**
     * Get a property with a default value.
     */
    <T> T getProperty(String key, Class<T> targetType, T defaultValue);
}
```
