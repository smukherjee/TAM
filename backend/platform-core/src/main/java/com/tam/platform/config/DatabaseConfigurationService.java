package com.tam.platform.config;

import com.tam.platform.context.TenantContext;
import com.tam.platform.context.TenantContextService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RequiredArgsConstructor
public class DatabaseConfigurationService implements ConfigurationService {

    private final TenantConfigurationRepository repository;

    // Cache: TenantId:DomainId:Key -> Value
    private final Map<String, String> configCache = new ConcurrentHashMap<>();

    private Instant lastPollTime = Instant.EPOCH;

    @PostConstruct
    public void init() {
        refreshCache();
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        String value = resolveValue(key);
        return value != null ? value : defaultValue;
    }

    @Override
    public int getIntProperty(String key, int defaultValue) {
        String value = resolveValue(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                log.warn("Invalid integer format for key {}: {}", key, value);
            }
        }
        return defaultValue;
    }

    @Override
    public boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = resolveValue(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }

    @Override
    public <T> T getProperty(String key, Class<T> type, T defaultValue) {
        String value = resolveValue(key);
        if (value == null) return defaultValue;
        
        // Simple conversion support
        if (type == String.class) return type.cast(value);
        if (type == Integer.class) return type.cast(Integer.parseInt(value));
        if (type == Boolean.class) return type.cast(Boolean.parseBoolean(value));
        if (type == Long.class) return type.cast(Long.parseLong(value));
        
        // For complex types, we might need Jackson
        return defaultValue;
    }

    private String resolveValue(String key) {
        TenantContext context = TenantContextService.get();
        String tenantId = context != null && context.tenantId() != null ? context.tenantId() : "SYSTEM";
        String domainId = context != null && context.domainId() != null ? context.domainId() : "GLOBAL";

        // 1. Specific Tenant + Domain
        String value = configCache.get(makeKey(tenantId, domainId, key));
        if (value != null) return value;

        // 2. Specific Tenant + Global Domain
        value = configCache.get(makeKey(tenantId, "GLOBAL", key));
        if (value != null) return value;

        // 3. System Tenant + Global Domain
        value = configCache.get(makeKey("SYSTEM", "GLOBAL", key));
        return value;
    }

    @Scheduled(fixedDelayString = "${platform.config.poll-interval:60000}")
    public void pollForChanges() {
        List<TenantConfiguration> changes = repository.findByUpdatedAtAfter(lastPollTime);
        if (!changes.isEmpty()) {
            updateCache(changes);
            log.info("Refreshed {} configuration entries", changes.size());
        }
    }

    private void refreshCache() {
        List<TenantConfiguration> all = repository.findAll();
        updateCache(all);
    }

    private void updateCache(List<TenantConfiguration> configs) {
        for (TenantConfiguration config : configs) {
            configCache.put(makeKey(config.getTenantId(), config.getDomainId(), config.getConfigKey()), config.getConfigValue());
            if (config.getUpdatedAt() != null && config.getUpdatedAt().isAfter(lastPollTime)) {
                lastPollTime = config.getUpdatedAt();
            }
        }
    }

    private String makeKey(String tenantId, String domainId, String key) {
        return tenantId + ":" + domainId + ":" + key;
    }
}
