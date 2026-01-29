package com.utam.asset.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HotspotDetailDTO
 */
class HotspotDetailDTOTest {
    
    @Test
    void canCreateHotspotDetailDTO() {
        HotspotDetailDTO dto = new HotspotDetailDTO();
        assertNotNull(dto);
    }
    
    @Test
    void canSetAndGetMode() {
        HotspotDetailDTO dto = new HotspotDetailDTO();
        dto.setMode("activity");
        assertEquals("activity", dto.getMode());
    }
    
    @Test
    void canSetAndGetActivityCount() {
        HotspotDetailDTO dto = new HotspotDetailDTO();
        dto.setActivityCount(42L);
        assertEquals(42L, dto.getActivityCount());
    }
    
    @Test
    void canSetAndGetUniqueAssets() {
        HotspotDetailDTO dto = new HotspotDetailDTO();
        dto.setUniqueAssets(10);
        assertEquals(10, dto.getUniqueAssets());
    }
    
    @Test
    void canBuildWithBuilder() {
        HotspotDetailDTO dto = HotspotDetailDTO.builder()
            .mode("violations")
            .activityCount(100L)
            .uniqueAssets(5)
            .build();
        
        assertNotNull(dto);
        assertEquals("violations", dto.getMode());
    }
}
