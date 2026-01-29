package com.utam.asset.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HeatmapDataDTO
 */
class HeatmapDataDTOTest {
    
    @Test
    void canCreateHeatmapDataDTO() {
        HeatmapDataDTO dto = new HeatmapDataDTO();
        assertNotNull(dto);
    }
    
    @Test
    void canSetAndGetCoordinates() {
        HeatmapDataDTO dto = new HeatmapDataDTO();
        dto.setLatitude(-27.3817);
        dto.setLongitude(153.1150);
        
        assertEquals(-27.3817, dto.getLatitude());
        assertEquals(153.1150, dto.getLongitude());
    }
    
    @Test
    void canSetAndGetIntensity() {
        HeatmapDataDTO dto = new HeatmapDataDTO();
        dto.setIntensity(0.75);
        assertEquals(0.75, dto.getIntensity());
    }
    
    @Test
    void canBuildWithBuilder() {
        HeatmapDataDTO dto = HeatmapDataDTO.builder()
            .latitude(-27.3817)
            .longitude(153.1150)
            .intensity(0.5)
            .build();
        
        assertNotNull(dto);
        assertEquals(-27.3817, dto.getLatitude());
    }
}
