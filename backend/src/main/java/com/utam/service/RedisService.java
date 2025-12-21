package com.utam.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class RedisService {

    private static final Logger logger = LoggerFactory.getLogger(RedisService.class);
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisService(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Save an object as a JSON string in Redis with a TTL.
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        try {
            String jsonValue = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, jsonValue, timeout, unit);
        } catch (JsonProcessingException e) {
            logger.error("Error serializing object to JSON for key: {}", key, e);
        }
    }

    /**
     * Retrieve an object from Redis and deserialize it.
     */
    public <T> Optional<T> get(String key, Class<T> clazz) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value != null) {
            try {
                return Optional.of(objectMapper.readValue(value.toString(), clazz));
            } catch (JsonProcessingException e) {
                logger.error("Error deserializing JSON for key: {}", key, e);
            }
        }
        return Optional.empty();
    }

    /**
     * Add a value to a Redis Set.
     */
    public void addToSet(String key, String value) {
        redisTemplate.opsForSet().add(key, value);
    }

    /**
     * Remove a value from a Redis Set.
     */
    public void removeFromSet(String key, String value) {
        redisTemplate.opsForSet().remove(key, value);
    }

    /**
     * Get all members of a Redis Set.
     */
    public Set<Object> getSetMembers(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * Set expiration for a key.
     */
    public void expire(String key, long timeout, TimeUnit unit) {
        redisTemplate.expire(key, timeout, unit);
    }

    /**
     * Delete a key.
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }
}
