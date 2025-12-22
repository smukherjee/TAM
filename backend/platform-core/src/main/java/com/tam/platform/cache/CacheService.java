package com.tam.platform.cache;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Interface for caching operations.
 * Implementations should handle tenant isolation.
 */
public interface CacheService {

    <T> Optional<T> get(String key, Class<T> type);

    void put(String key, Object value, Duration ttl);

    void evict(String key);

    <T> Map<String, T> getAll(Set<String> keys, Class<T> type);

    void putAll(Map<String, Object> entries, Duration ttl);

    void evictPattern(String pattern);
}
