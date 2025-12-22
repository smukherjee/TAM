package com.tam.platform.config;

/**
 * Interface for retrieving configuration properties with tenant/domain resolution.
 */
public interface ConfigurationService {

    String getProperty(String key, String defaultValue);

    int getIntProperty(String key, int defaultValue);

    boolean getBooleanProperty(String key, boolean defaultValue);

    <T> T getProperty(String key, Class<T> type, T defaultValue);
}
