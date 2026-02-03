package com.utam.tracking.mapper;

import com.utam.tracking.domain.RestrictedZone;
import com.utam.tracking.dto.RestrictedZoneDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Polygon;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ZoneMapper.
 * Feature: 005-asset-tracking-security
 * Task: T122
 */
@DisplayName("ZoneMapper Tests")
class ZoneMapperTest {

    private ZoneMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ZoneMapper();
    }

    @Test
    @DisplayName("Should map entity to DTO correctly")
    void toDTO_shouldMapAllFields() {
        // Arrange
        UUID id = UUID.randomUUID();
        ZonedDateTime createdAt = ZonedDateTime.now().minusDays(7);
        ZonedDateTime updatedAt = ZonedDateTime.now();
        ZonedDateTime effectiveFrom = ZonedDateTime.now().minusDays(30);
        ZonedDateTime effectiveTo = ZonedDateTime.now().plusDays(365);
        
        // Create a simple square polygon
        List<List<Double>> coords = Arrays.asList(
                Arrays.asList(77.0889, 28.5574),
                Arrays.asList(77.0900, 28.5574),
                Arrays.asList(77.0900, 28.5585),
                Arrays.asList(77.0889, 28.5585),
                Arrays.asList(77.0889, 28.5574)
        );
        Polygon polygon = mapper.coordinatesToPolygon(coords);

        RestrictedZone entity = RestrictedZone.builder()
                .id(id)
                .zoneId("RZ-VIDP-001")
                .zoneName("Fuel Storage Area")
                .zoneType("PROHIBITED")
                .description("Highly restricted fuel storage facility")
                .boundary(polygon)
                .authorizedAssetCategories(new String[]{"FUEL_TRUCK", "MAINTENANCE"})
                .isActive(true)
                .effectiveFrom(effectiveFrom)
                .effectiveTo(effectiveTo)
                .tenantCode("VIDP")
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        // Act
        RestrictedZoneDTO dto = mapper.toDTO(entity);

        // Assert
        assertNotNull(dto);
        assertEquals(id, dto.getId());
        assertEquals("RZ-VIDP-001", dto.getZoneId());
        assertEquals("Fuel Storage Area", dto.getZoneName());
        assertEquals("PROHIBITED", dto.getZoneType());
        assertEquals("Highly restricted fuel storage facility", dto.getDescription());
        assertNotNull(dto.getBoundaryCoordinates());
        assertEquals(5, dto.getBoundaryCoordinates().size()); // Closed polygon
        assertArrayEquals(new String[]{"FUEL_TRUCK", "MAINTENANCE"}, dto.getAuthorizedAssetCategories());
        assertTrue(dto.getIsActive());
        assertEquals(effectiveFrom, dto.getEffectiveFrom());
        assertEquals(effectiveTo, dto.getEffectiveTo());
        assertEquals("VIDP", dto.getTenantCode());
        assertEquals(createdAt, dto.getCreatedAt());
        assertEquals(updatedAt, dto.getUpdatedAt());
    }

    @Test
    @DisplayName("Should return null when entity is null")
    void toDTO_shouldReturnNull_whenEntityIsNull() {
        assertNull(mapper.toDTO(null));
    }

    @Test
    @DisplayName("Should map DTO to entity correctly")
    void toEntity_shouldMapAllFields() {
        // Arrange
        UUID id = UUID.randomUUID();
        List<List<Double>> coords = Arrays.asList(
                Arrays.asList(77.0889, 28.5574),
                Arrays.asList(77.0900, 28.5574),
                Arrays.asList(77.0900, 28.5585),
                Arrays.asList(77.0889, 28.5585),
                Arrays.asList(77.0889, 28.5574)
        );

        RestrictedZoneDTO dto = RestrictedZoneDTO.builder()
                .id(id)
                .zoneId("RZ-LIRN-001")
                .zoneName("Runway 16R")
                .zoneType("RESTRICTED")
                .description("Active runway area")
                .boundaryCoordinates(coords)
                .authorizedAssetCategories(new String[]{"AIRCRAFT"})
                .isActive(true)
                .tenantCode("LIRN")
                .build();

        // Act
        RestrictedZone entity = mapper.toEntity(dto);

        // Assert
        assertNotNull(entity);
        assertEquals(id, entity.getId());
        assertEquals("RZ-LIRN-001", entity.getZoneId());
        assertEquals("Runway 16R", entity.getZoneName());
        assertEquals("RESTRICTED", entity.getZoneType());
        assertEquals("Active runway area", entity.getDescription());
        assertNotNull(entity.getBoundary());
        assertEquals(4326, entity.getBoundary().getSRID());
        assertArrayEquals(new String[]{"AIRCRAFT"}, entity.getAuthorizedAssetCategories());
        assertTrue(entity.getIsActive());
        assertEquals("LIRN", entity.getTenantCode());
    }

    @Test
    @DisplayName("Should return null when DTO is null")
    void toEntity_shouldReturnNull_whenDTOIsNull() {
        assertNull(mapper.toEntity(null));
    }

    @Test
    @DisplayName("Should convert polygon to coordinates correctly")
    void polygonToCoordinates_shouldReturnCoordinateList() {
        // Create polygon via mapper
        List<List<Double>> inputCoords = Arrays.asList(
                Arrays.asList(77.0889, 28.5574),
                Arrays.asList(77.0900, 28.5574),
                Arrays.asList(77.0900, 28.5585),
                Arrays.asList(77.0889, 28.5585),
                Arrays.asList(77.0889, 28.5574)
        );
        Polygon polygon = mapper.coordinatesToPolygon(inputCoords);

        // Convert back
        List<List<Double>> outputCoords = mapper.polygonToCoordinates(polygon);

        assertEquals(5, outputCoords.size());
        assertEquals(77.0889, outputCoords.get(0).get(0), 0.0001);
        assertEquals(28.5574, outputCoords.get(0).get(1), 0.0001);
    }

    @Test
    @DisplayName("Should return empty list for null polygon")
    void polygonToCoordinates_shouldReturnEmptyList_whenPolygonIsNull() {
        List<List<Double>> coords = mapper.polygonToCoordinates(null);
        assertNotNull(coords);
        assertTrue(coords.isEmpty());
    }

    @Test
    @DisplayName("Should create valid polygon from coordinates")
    void coordinatesToPolygon_shouldCreateValidPolygon() {
        List<List<Double>> coords = Arrays.asList(
                Arrays.asList(77.0889, 28.5574),
                Arrays.asList(77.0900, 28.5574),
                Arrays.asList(77.0900, 28.5585),
                Arrays.asList(77.0889, 28.5585),
                Arrays.asList(77.0889, 28.5574)
        );

        Polygon polygon = mapper.coordinatesToPolygon(coords);

        assertNotNull(polygon);
        assertTrue(polygon.isValid());
        assertEquals(4326, polygon.getSRID());
        assertFalse(polygon.isEmpty());
    }

    @Test
    @DisplayName("Should auto-close polygon if not closed")
    void coordinatesToPolygon_shouldAutoClosePolygon() {
        // Polygon without closing point
        List<List<Double>> coords = Arrays.asList(
                Arrays.asList(77.0889, 28.5574),
                Arrays.asList(77.0900, 28.5574),
                Arrays.asList(77.0900, 28.5585),
                Arrays.asList(77.0889, 28.5585)
        );

        Polygon polygon = mapper.coordinatesToPolygon(coords);

        assertNotNull(polygon);
        assertTrue(polygon.isValid());
        // Verify polygon is closed
        assertEquals(polygon.getExteriorRing().getCoordinateN(0),
                     polygon.getExteriorRing().getCoordinateN(polygon.getExteriorRing().getNumPoints() - 1));
    }

    @Test
    @DisplayName("Should throw exception for invalid polygon coordinates")
    void coordinatesToPolygon_shouldThrowException_whenInsufficientCoords() {
        List<List<Double>> coords = Arrays.asList(
                Arrays.asList(77.0889, 28.5574),
                Arrays.asList(77.0900, 28.5574)
        );

        assertThrows(IllegalArgumentException.class, () -> mapper.coordinatesToPolygon(coords));
    }

    @Test
    @DisplayName("Should validate polygon coordinates correctly")
    void isValidPolygon_shouldValidateCorrectly() {
        // Valid polygon
        List<List<Double>> validCoords = Arrays.asList(
                Arrays.asList(77.0889, 28.5574),
                Arrays.asList(77.0900, 28.5574),
                Arrays.asList(77.0900, 28.5585),
                Arrays.asList(77.0889, 28.5585)
        );
        assertTrue(mapper.isValidPolygon(validCoords));

        // Invalid - too few points
        List<List<Double>> tooFewCoords = Arrays.asList(
                Arrays.asList(77.0889, 28.5574),
                Arrays.asList(77.0900, 28.5574)
        );
        assertFalse(mapper.isValidPolygon(tooFewCoords));

        // Invalid - null
        assertFalse(mapper.isValidPolygon(null));

        // Invalid - out of range longitude
        List<List<Double>> invalidLng = Arrays.asList(
                Arrays.asList(200.0, 28.5574),
                Arrays.asList(77.0900, 28.5574),
                Arrays.asList(77.0900, 28.5585),
                Arrays.asList(77.0889, 28.5585)
        );
        assertFalse(mapper.isValidPolygon(invalidLng));

        // Invalid - out of range latitude
        List<List<Double>> invalidLat = Arrays.asList(
                Arrays.asList(77.0889, 100.0),
                Arrays.asList(77.0900, 28.5574),
                Arrays.asList(77.0900, 28.5585),
                Arrays.asList(77.0889, 28.5585)
        );
        assertFalse(mapper.isValidPolygon(invalidLat));
    }

    @Test
    @DisplayName("Should convert list of entities to DTOs")
    void toDTOList_shouldConvertList() {
        List<RestrictedZone> entities = Arrays.asList(
                createMinimalZone("RZ-001", "Zone A"),
                createMinimalZone("RZ-002", "Zone B"),
                createMinimalZone("RZ-003", "Zone C")
        );

        List<RestrictedZoneDTO> dtos = mapper.toDTOList(entities);

        assertEquals(3, dtos.size());
        assertEquals("RZ-001", dtos.get(0).getZoneId());
        assertEquals("RZ-002", dtos.get(1).getZoneId());
        assertEquals("RZ-003", dtos.get(2).getZoneId());
    }

    @Test
    @DisplayName("Should return empty list when input is null")
    void toDTOList_shouldReturnEmptyList_whenInputIsNull() {
        List<RestrictedZoneDTO> dtos = mapper.toDTOList(null);
        assertNotNull(dtos);
        assertTrue(dtos.isEmpty());
    }

    @Test
    @DisplayName("Should update entity from DTO")
    void updateEntityFromDTO_shouldUpdateFields() {
        // Arrange
        RestrictedZone entity = createMinimalZone("RZ-UPDATE", "Old Name");

        List<List<Double>> newCoords = Arrays.asList(
                Arrays.asList(77.1000, 28.5600),
                Arrays.asList(77.1050, 28.5600),
                Arrays.asList(77.1050, 28.5650),
                Arrays.asList(77.1000, 28.5650),
                Arrays.asList(77.1000, 28.5600)
        );

        RestrictedZoneDTO dto = RestrictedZoneDTO.builder()
                .zoneName("New Name")
                .zoneType("PROHIBITED")
                .description("Updated description")
                .boundaryCoordinates(newCoords)
                .authorizedAssetCategories(new String[]{"MAINTENANCE"})
                .isActive(false)
                .build();

        // Act
        mapper.updateEntityFromDTO(entity, dto);

        // Assert
        assertEquals("New Name", entity.getZoneName());
        assertEquals("PROHIBITED", entity.getZoneType());
        assertEquals("Updated description", entity.getDescription());
        assertNotNull(entity.getBoundary());
        assertArrayEquals(new String[]{"MAINTENANCE"}, entity.getAuthorizedAssetCategories());
        assertFalse(entity.getIsActive());
    }

    private RestrictedZone createMinimalZone(String zoneId, String zoneName) {
        List<List<Double>> coords = Arrays.asList(
                Arrays.asList(77.0889, 28.5574),
                Arrays.asList(77.0900, 28.5574),
                Arrays.asList(77.0900, 28.5585),
                Arrays.asList(77.0889, 28.5585),
                Arrays.asList(77.0889, 28.5574)
        );

        return RestrictedZone.builder()
                .id(UUID.randomUUID())
                .zoneId(zoneId)
                .zoneName(zoneName)
                .zoneType("RESTRICTED")
                .boundary(mapper.coordinatesToPolygon(coords))
                .isActive(true)
                .tenantCode("VIDP")
                .createdAt(ZonedDateTime.now())
                .build();
    }
}
