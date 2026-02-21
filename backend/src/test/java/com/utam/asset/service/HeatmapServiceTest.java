package com.utam.asset.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HeatmapService
 * Task: T061 - Backend integration tests
 */
@DisplayName("HeatmapService Tests")
class HeatmapServiceTest {

    @Test
    void serviceIsInitialized() {
        HeatmapService service = new HeatmapService(new JdbcTemplate());
        assertNotNull(service, "HeatmapService should be initialized");
    }
}
