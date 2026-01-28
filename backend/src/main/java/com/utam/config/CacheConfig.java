package com.utam.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Enable Spring Cache with Caffeine.
 * Configured cache names: liveAssets, liveAsset
 * TTL: 5 seconds (matches data ingestion interval)
 * <p>
 * Feature: 005-asset-tracking-security (US5)
 * Task: T028
 */
@Configuration
@EnableCaching
public class CacheConfig {
    // Cache configuration is in application.yml
    // This class just enables the @Cacheable annotations
}
