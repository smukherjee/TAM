package com.utam.asset.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for HeatmapService
 * Task: T061 - Backend integration tests
 */
@SpringBootTest
@ActiveProfiles("test")
class HeatmapServiceTest {

    @Autowired
    private HeatmapService service;

    @Test
    void serviceIsInjected() {
        assertNotNull(service, "HeatmapService should be autowired");
    }
}
