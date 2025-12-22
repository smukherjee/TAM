package com.tam.platform.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tam.platform.context.TenantContext;
import com.tam.platform.context.TenantContextService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class RedisCacheService implements CacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final Cache<String, Object> localCache;
    private static final String CB_NAME = "cacheService";

    public RedisCacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.localCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(5))
                .maximumSize(10_000)
                .build();
    }

    @Override
    @CircuitBreaker(name = CB_NAME, fallbackMethod = "getFallback")
    public <T> Optional<T> get(String key, Class<T> type) {
        String scopedKey = getScopedKey(key);
        
        // L1 Cache (Caffeine)
        Object localValue = localCache.getIfPresent(scopedKey);
        if (localValue != null && type.isInstance(localValue)) {
            log.trace("L1 Cache hit for key: {}", scopedKey);
            return Optional.of(type.cast(localValue));
        }

        // L2 Cache (Redis)
        Object value = redisTemplate.opsForValue().get(scopedKey);
        if (value != null && type.isInstance(value)) {
            log.trace("L2 Cache hit for key: {}", scopedKey);
            localCache.put(scopedKey, value);
            return Optional.of(type.cast(value));
        }
        return Optional.empty();
    }

    public <T> Optional<T> getFallback(String key, Class<T> type, Throwable t) {
        log.warn("Cache get failed for key {}: {}", key, t.getMessage());
        return Optional.empty();
    }

    @Override
    @CircuitBreaker(name = CB_NAME, fallbackMethod = "putFallback")
    public void put(String key, Object value, Duration ttl) {
        String scopedKey = getScopedKey(key);
        localCache.put(scopedKey, value);
        redisTemplate.opsForValue().set(scopedKey, value, ttl);
    }

    public void putFallback(String key, Object value, Duration ttl, Throwable t) {
        log.warn("Cache put failed for key {}: {}", key, t.getMessage());
    }

    @Override
    @CircuitBreaker(name = CB_NAME, fallbackMethod = "evictFallback")
    public void evict(String key) {
        String scopedKey = getScopedKey(key);
        localCache.invalidate(scopedKey);
        redisTemplate.delete(scopedKey);
    }

    public void evictFallback(String key, Throwable t) {
        log.warn("Cache evict failed for key {}: {}", key, t.getMessage());
    }

    @Override
    @CircuitBreaker(name = CB_NAME, fallbackMethod = "getAllFallback")
    public <T> Map<String, T> getAll(Set<String> keys, Class<T> type) {
        if (keys.isEmpty()) return Collections.emptyMap();

        Map<String, T> result = new HashMap<>();
        Set<String> missingKeys = new java.util.HashSet<>();

        for (String key : keys) {
            String scopedKey = getScopedKey(key);
            Object localValue = localCache.getIfPresent(scopedKey);
            if (localValue != null && type.isInstance(localValue)) {
                result.put(key, type.cast(localValue));
            } else {
                missingKeys.add(key);
            }
        }

        if (missingKeys.isEmpty()) {
            return result;
        }

        List<String> keyList = new ArrayList<>(missingKeys);
        List<String> scopedKeys = keyList.stream().map(this::getScopedKey).toList();

        List<Object> values = redisTemplate.opsForValue().multiGet(scopedKeys);

        if (values != null) {
            for (int i = 0; i < values.size(); i++) {
                Object value = values.get(i);
                if (value != null && type.isInstance(value)) {
                    String originalKey = keyList.get(i);
                    result.put(originalKey, type.cast(value));
                    localCache.put(getScopedKey(originalKey), value);
                }
            }
        }
        return result;
    }

    public <T> Map<String, T> getAllFallback(Set<String> keys, Class<T> type, Throwable t) {
        log.warn("Cache getAll failed: {}", t.getMessage());
        return Collections.emptyMap();
    }

    @Override
    @CircuitBreaker(name = CB_NAME, fallbackMethod = "putAllFallback")
    public void putAll(Map<String, Object> entries, Duration ttl) {
        if (entries.isEmpty()) return;

        Map<String, Object> scopedEntries = new HashMap<>();
        entries.forEach((k, v) -> {
            String scopedKey = getScopedKey(k);
            scopedEntries.put(scopedKey, v);
            localCache.put(scopedKey, v);
        });

        redisTemplate.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
            scopedEntries.forEach((k, v) -> {
                byte[] keyBytes = k.getBytes();
                byte[] valueBytes = ((org.springframework.data.redis.serializer.RedisSerializer<Object>) redisTemplate.getValueSerializer()).serialize(v);
                connection.setEx(keyBytes, ttl.getSeconds(), valueBytes);
            });
            return null;
        });
    }

    public void putAllFallback(Map<String, Object> entries, Duration ttl, Throwable t) {
        log.warn("Cache putAll failed: {}", t.getMessage());
    }

    @Override
    @CircuitBreaker(name = CB_NAME, fallbackMethod = "evictPatternFallback")
    public void evictPattern(String pattern) {
        String scopedPattern = getScopedKey(pattern);
        
        // Evict from local cache
        String regex = scopedPattern.replace("*", ".*");
        localCache.asMap().keySet().removeIf(k -> k.matches(regex));

        ScanOptions options = ScanOptions.scanOptions().match(scopedPattern).count(100).build();
        
        // Use scan to find keys and delete them
        // This can be slow for large datasets, but safer than KEYS
        Set<String> keysToDelete = new java.util.HashSet<>();
        try (Cursor<byte[]> cursor = redisTemplate.getConnectionFactory().getConnection().scan(options)) {
            while (cursor.hasNext()) {
                keysToDelete.add(new String(cursor.next()));
            }
        }
        
        if (!keysToDelete.isEmpty()) {
            redisTemplate.delete(keysToDelete);
        }
    }

    public void evictPatternFallback(String pattern, Throwable t) {
        log.warn("Cache evictPattern failed for pattern {}: {}", pattern, t.getMessage());
    }

    private String getScopedKey(String key) {
        TenantContext context = TenantContextService.get();
        String tenantId = (context != null && context.tenantId() != null) ? context.tenantId() : "SYSTEM";
        return tenantId + ":" + key;
    }
}
