package com.utam.asset.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for AssetLocationService
 * Task: T061 - Backend integration tests
 */
@SpringBootTest
@ActiveProfiles("test")
class AssetLocationServiceTest {

    @Autowired
    private AssetLocationService service;

    @Test
    void serviceIsInjected() {
        assertNotNull(service, "AssetLocationService should be autowired");
    }

    @Test
    void getAllLiveAssetsReturnsNonNull() {
        var result = service.getAllLiveAssets("VIDP", null, null, null, 10, 0);
        assertNotNull(result, "getAllLiveAssets should never return null");
    }
}
