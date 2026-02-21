package com.utam.tracking.mapper;

import com.utam.tracking.domain.ZoneViolation;
import com.utam.tracking.dto.ZoneViolationDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ZoneViolationMapper.
 * Feature: 005-asset-tracking-security
 * Task: T122
 */
@DisplayName("ZoneViolationMapper Tests")
class ZoneViolationMapperTest {

    private ZoneViolationMapper mapper;
    private GeometryFactory geometryFactory;

    @BeforeEach
    void setUp() {
        mapper = new ZoneViolationMapper();
        geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    }

    @Test
    @DisplayName("Should map entity to DTO correctly")
    void toDTO_shouldMapAllFields() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        UUID zoneId = UUID.randomUUID();
        UUID acknowledgedBy = UUID.randomUUID();
        ZonedDateTime timestamp = ZonedDateTime.now();
        ZonedDateTime acknowledgedAt = ZonedDateTime.now().minusHours(1);
        
        Point entryLocation = geometryFactory.createPoint(new Coordinate(77.0889, 28.5574));
        
        ZoneViolation entity = ZoneViolation.builder()
                .id(id)
                .violationId("VIOL-001")
                .assetId(assetId)
                .assetIdentifier("GSE-001")
                .assetName("Test Asset")
                .assetCategory("GSE")
                .restrictedZoneId(zoneId)
                .zoneName("Restricted Area A")
                .zoneType("PROHIBITED")
                .violationType("UNAUTHORIZED_ENTRY")
                .entryLocation(entryLocation)
                .severity(ZoneViolation.ViolationSeverity.CRITICAL)
                .durationSeconds(300)
                .timestamp(timestamp)
                .acknowledged(true)
                .acknowledgedBy(acknowledgedBy)
                .acknowledgedAt(acknowledgedAt)
                .resolutionNotes("Authorized after verification")
                .tenantCode("VIDP")
                .build();

        // Act
        ZoneViolationDTO dto = mapper.toDTO(entity);

        // Assert
        assertNotNull(dto);
        assertEquals(id, dto.getId());
        assertEquals("VIOL-001", dto.getViolationId());
        assertEquals(assetId, dto.getAssetId());
        assertEquals("GSE-001", dto.getAssetIdentifier());
        assertEquals("Test Asset", dto.getAssetName());
        assertEquals("GSE", dto.getAssetCategory());
        assertEquals(zoneId, dto.getRestrictedZoneId());
        assertEquals("Restricted Area A", dto.getZoneName());
        assertEquals("PROHIBITED", dto.getZoneType());
        assertEquals("UNAUTHORIZED_ENTRY", dto.getViolationType());
        assertEquals("CRITICAL", dto.getSeverity());
        assertEquals(300L, dto.getDurationSeconds());
        assertEquals(timestamp, dto.getTimestamp());
        assertTrue(dto.getAcknowledged());
        assertEquals(acknowledgedBy.toString(), dto.getAcknowledgedBy());
        assertEquals(acknowledgedAt, dto.getAcknowledgedAt());
        assertEquals("Authorized after verification", dto.getResolutionNotes());
        assertEquals("VIDP", dto.getTenantCode());
        
        // Check coordinates from Point
        assertEquals(28.5574, dto.getEntryLatitude(), 0.0001);
        assertEquals(77.0889, dto.getEntryLongitude(), 0.0001);
    }

    @Test
    @DisplayName("Should return null when entity is null")
    void toDTO_shouldReturnNull_whenEntityIsNull() {
        assertNull(mapper.toDTO(null));
    }

    @Test
    @DisplayName("Should handle null entry location")
    void toDTO_shouldHandleNullEntryLocation() {
        ZoneViolation entity = ZoneViolation.builder()
                .id(UUID.randomUUID())
                .violationId("VIOL-002")
                .assetId(UUID.randomUUID())
                .assetIdentifier("GSE-002")
                .restrictedZoneId(UUID.randomUUID())
                .zoneName("Zone B")
                .zoneType("RESTRICTED")
                .violationType("UNAUTHORIZED_ENTRY")
                .entryLocation(null)
                .severity(ZoneViolation.ViolationSeverity.HIGH)
                .timestamp(ZonedDateTime.now())
                .acknowledged(false)
                .tenantCode("VIDP")
                .build();

        ZoneViolationDTO dto = mapper.toDTO(entity);

        assertNotNull(dto);
        assertNull(dto.getEntryLatitude());
        assertNull(dto.getEntryLongitude());
    }

    @Test
    @DisplayName("Should map DTO to entity correctly")
    void toEntity_shouldMapAllFields() {
        // Arrange
        UUID id = UUID.randomUUID();
        ZonedDateTime timestamp = ZonedDateTime.now();
        
        ZoneViolationDTO dto = ZoneViolationDTO.builder()
                .id(id)
                .violationId("VIOL-003")
                .assetId(UUID.randomUUID())
                .assetIdentifier("GSE-003")
                .assetName("Asset 3")
                .assetCategory("VEHICLE")
                .restrictedZoneId(UUID.randomUUID())
                .zoneName("Zone C")
                .zoneType("CONTROLLED")
                .violationType("OVERSTAY")
                .severity("HIGH")
                .durationSeconds(600L)
                .timestamp(timestamp)
                .acknowledged(false)
                .tenantCode("LIRN")
                .build();

        // Act
        ZoneViolation entity = mapper.toEntity(dto);

        // Assert
        assertNotNull(entity);
        assertEquals(id, entity.getId());
        assertEquals("VIOL-003", entity.getViolationId());
        assertEquals("GSE-003", entity.getAssetIdentifier());
        assertEquals("Zone C", entity.getZoneName());
        assertEquals(ZoneViolation.ViolationSeverity.HIGH, entity.getSeverity());
        assertEquals(600, entity.getDurationSeconds());
        assertEquals(timestamp, entity.getTimestamp());
        assertFalse(entity.getAcknowledged());
        assertEquals("LIRN", entity.getTenantCode());
    }

    @Test
    @DisplayName("Should return null when DTO is null")
    void toEntity_shouldReturnNull_whenDTOIsNull() {
        assertNull(mapper.toEntity(null));
    }

    @Test
    @DisplayName("Should convert list of entities to DTOs")
    void toDTOList_shouldConvertList() {
        List<ZoneViolation> entities = Arrays.asList(
                createMinimalViolation("VIOL-A"),
                createMinimalViolation("VIOL-B"),
                createMinimalViolation("VIOL-C")
        );

        List<ZoneViolationDTO> dtos = mapper.toDTOList(entities);

        assertEquals(3, dtos.size());
        assertEquals("VIOL-A", dtos.get(0).getViolationId());
        assertEquals("VIOL-B", dtos.get(1).getViolationId());
        assertEquals("VIOL-C", dtos.get(2).getViolationId());
    }

    @Test
    @DisplayName("Should return empty list when input is null")
    void toDTOList_shouldReturnEmptyList_whenInputIsNull() {
        List<ZoneViolationDTO> dtos = mapper.toDTOList(null);
        assertNotNull(dtos);
        assertTrue(dtos.isEmpty());
    }

    @Test
    @DisplayName("Should update entity from DTO for acknowledgment")
    void updateEntityFromDTO_shouldUpdateAcknowledgmentFields() {
        // Arrange
        ZoneViolation entity = createMinimalViolation("VIOL-UPDATE");
        entity.setAcknowledged(false);
        entity.setAcknowledgedBy(null);
        entity.setResolutionNotes(null);

        ZonedDateTime acknowledgedAt = ZonedDateTime.now();
        ZoneViolationDTO dto = ZoneViolationDTO.builder()
                .acknowledged(true)
                .acknowledgedBy(UUID.randomUUID().toString())
                .acknowledgedAt(acknowledgedAt)
                .resolutionNotes("Resolved by admin")
                .build();

        // Act
        mapper.updateEntityFromDTO(entity, dto);

        // Assert
        assertTrue(entity.getAcknowledged());
        assertNotNull(entity.getAcknowledgedBy());
        assertEquals(acknowledgedAt, entity.getAcknowledgedAt());
        assertEquals("Resolved by admin", entity.getResolutionNotes());
    }

    private ZoneViolation createMinimalViolation(String violationId) {
        return ZoneViolation.builder()
                .id(UUID.randomUUID())
                .violationId(violationId)
                .assetId(UUID.randomUUID())
                .assetIdentifier("GSE-001")
                .restrictedZoneId(UUID.randomUUID())
                .zoneName("Zone A")
                .zoneType("RESTRICTED")
                .violationType("UNAUTHORIZED_ENTRY")
                .severity(ZoneViolation.ViolationSeverity.MEDIUM)
                .timestamp(ZonedDateTime.now())
                .acknowledged(false)
                .tenantCode("VIDP")
                .build();
    }
}
