package com.utam.asset.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;

/**
 * Unit tests for AssetLocationDTO
 */
class AssetLocationDTOTest {
    
    @Test
    void canCreateAssetLocationDTO() {
        AssetLocationDTO dto = new AssetLocationDTO();
        assertNotNull(dto);
    }
    
    @Test
    void canSetAndGetAssetId() {
        AssetLocationDTO dto = new AssetLocationDTO();
        UUID id = UUID.randomUUID();
        dto.setAssetId(id);
        assertEquals(id, dto.getAssetId());
    }
    
    @Test
    void canSetAndGetAssetIdentifier() {
        AssetLocationDTO dto = new AssetLocationDTO();
        dto.setAssetIdentifier("A-VIDP-001");
        assertEquals("A-VIDP-001", dto.getAssetIdentifier());
    }
    
    @Test
    void canSetAndGetName() {
        AssetLocationDTO dto = new AssetLocationDTO();
        dto.setName("Fire Truck 1");
        assertEquals("Fire Truck 1", dto.getName());
    }
    
    @Test
    void canSetAndGetCoordinates() {
        AssetLocationDTO dto = new AssetLocationDTO();
        dto.setLatitude(-27.3817);
        dto.setLongitude(153.1150);
        
        assertEquals(-27.3817, dto.getLatitude());
        assertEquals(153.1150, dto.getLongitude());
    }
}
