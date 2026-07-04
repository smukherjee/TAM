package com.utam.asset.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AssetLocationService
 * Task: T061 - Backend integration tests
 */
@DisplayName("AssetLocationService Tests")
class AssetLocationServiceTest {

    private AssetLocationService service;

    @BeforeEach
    void setUp() {
        service = new AssetLocationService(new StubJdbcTemplate());
    }

    @Test
    void serviceIsInjected() {
        assertNotNull(service, "AssetLocationService should be initialized");
    }

    @Test
    void getAllLiveAssetsReturnsNonNull() {
        List<?> result = service.getAllLiveAssets("VIDP", null, null, null, null, 10, 0);
        assertNotNull(result, "getAllLiveAssets should never return null");
        assertTrue(result.isEmpty(), "stub jdbc template should return empty list");
    }

    private static class StubJdbcTemplate extends JdbcTemplate {
        @Override
        public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
            return Collections.emptyList();
        }
    }
}
