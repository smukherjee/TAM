package com.utam.tracking.mapper;

import com.utam.tracking.domain.MovementDiscrepancy;
import com.utam.tracking.dto.MovementDiscrepancyDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DiscrepancyMapper.
 * Feature: 005-asset-tracking-security
 * Task: T122
 */
@DisplayName("DiscrepancyMapper Tests")
class DiscrepancyMapperTest {

    private DiscrepancyMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new DiscrepancyMapper();
    }

    @Test
    @DisplayName("Should map entity to DTO correctly")
    void toDTO_shouldMapAllFields() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        UUID acknowledgedBy = UUID.randomUUID();
        ZonedDateTime timestamp = ZonedDateTime.now();
        ZonedDateTime acknowledgedAt = ZonedDateTime.now().minusHours(1);

        MovementDiscrepancy entity = MovementDiscrepancy.builder()
                .id(id)
                .discrepancyId("DISC-001")
                .assetId(assetId)
                .assetIdentifier("GSE-001")
                .assetName("Test Asset")
                .assetCategory("GSE")
                .discrepancyType(MovementDiscrepancy.DiscrepancyType.LOCATION_MISMATCH)
                .expectedLocation("Bay 42")
                .actualLocation("Bay 15")
                .expectedLatitude(28.5574)
                .expectedLongitude(77.0889)
                .actualLatitude(28.5600)
                .actualLongitude(77.0920)
                .deviationMeters(350.5)
                .expectedStatus("PARKED")
                .actualStatus("MOVING")
                .description("Asset found in wrong location")
                .severity(MovementDiscrepancy.DiscrepancySeverity.HIGH)
                .timestamp(timestamp)
                .acknowledged(true)
                .acknowledgedBy(acknowledgedBy)
                .acknowledgedAt(acknowledgedAt)
                .resolutionNotes("Confirmed movement authorized")
                .tenantCode("VIDP")
                .build();

        // Act
        MovementDiscrepancyDTO dto = mapper.toDTO(entity);

        // Assert
        assertNotNull(dto);
        assertEquals(id, dto.getId());
        assertEquals("DISC-001", dto.getDiscrepancyId());
        assertEquals(assetId, dto.getAssetId());
        assertEquals("GSE-001", dto.getAssetIdentifier());
        assertEquals("Test Asset", dto.getAssetName());
        assertEquals("GSE", dto.getAssetCategory());
        assertEquals("LOCATION_MISMATCH", dto.getDiscrepancyType());
        assertEquals("Bay 42", dto.getExpectedLocation());
        assertEquals("Bay 15", dto.getActualLocation());
        assertEquals(28.5574, dto.getExpectedLatitude());
        assertEquals(77.0889, dto.getExpectedLongitude());
        assertEquals(28.5600, dto.getActualLatitude());
        assertEquals(77.0920, dto.getActualLongitude());
        assertEquals(350.5, dto.getDeviationMeters());
        assertEquals("PARKED", dto.getExpectedStatus());
        assertEquals("MOVING", dto.getActualStatus());
        assertEquals("Asset found in wrong location", dto.getDescription());
        assertEquals("HIGH", dto.getSeverity());
        assertEquals(timestamp, dto.getTimestamp());
        assertTrue(dto.getAcknowledged());
        assertEquals(acknowledgedBy, dto.getAcknowledgedBy());
        assertEquals(acknowledgedAt, dto.getAcknowledgedAt());
        assertEquals("Confirmed movement authorized", dto.getResolutionNotes());
        assertEquals("VIDP", dto.getTenantCode());
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
        ZonedDateTime timestamp = ZonedDateTime.now();

        MovementDiscrepancyDTO dto = MovementDiscrepancyDTO.builder()
                .id(id)
                .discrepancyId("DISC-002")
                .assetId(UUID.randomUUID())
                .assetIdentifier("VEH-001")
                .assetName("Vehicle 1")
                .assetCategory("VEHICLE")
                .discrepancyType("LOCATION_MISMATCH")
                .expectedStatus("PARKED")
                .actualStatus("IN_USE")
                .description("Status conflict")
                .severity("MEDIUM")
                .timestamp(timestamp)
                .acknowledged(false)
                .tenantCode("LIRN")
                .build();

        // Act
        MovementDiscrepancy entity = mapper.toEntity(dto);

        // Assert
        assertNotNull(entity);
        assertEquals(id, entity.getId());
        assertEquals("DISC-002", entity.getDiscrepancyId());
        assertEquals("VEH-001", entity.getAssetIdentifier());
        assertEquals(MovementDiscrepancy.DiscrepancyType.LOCATION_MISMATCH, entity.getDiscrepancyType());
        assertEquals("PARKED", entity.getExpectedStatus());
        assertEquals("IN_USE", entity.getActualStatus());
        assertEquals(MovementDiscrepancy.DiscrepancySeverity.MEDIUM, entity.getSeverity());
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
        List<MovementDiscrepancy> entities = Arrays.asList(
                createMinimalDiscrepancy("DISC-A"),
                createMinimalDiscrepancy("DISC-B"),
                createMinimalDiscrepancy("DISC-C")
        );

        List<MovementDiscrepancyDTO> dtos = mapper.toDTOList(entities);

        assertEquals(3, dtos.size());
        assertEquals("DISC-A", dtos.get(0).getDiscrepancyId());
        assertEquals("DISC-B", dtos.get(1).getDiscrepancyId());
        assertEquals("DISC-C", dtos.get(2).getDiscrepancyId());
    }

    @Test
    @DisplayName("Should return empty list when input is null")
    void toDTOList_shouldReturnEmptyList_whenInputIsNull() {
        List<MovementDiscrepancyDTO> dtos = mapper.toDTOList(null);
        assertNotNull(dtos);
        assertTrue(dtos.isEmpty());
    }

    @Test
    @DisplayName("Should update entity from DTO for acknowledgment")
    void updateEntityFromDTO_shouldUpdateAcknowledgmentFields() {
        // Arrange
        MovementDiscrepancy entity = createMinimalDiscrepancy("DISC-UPDATE");
        entity.setAcknowledged(false);
        entity.setAcknowledgedBy(null);
        entity.setResolutionNotes(null);

        UUID acknowledgedBy = UUID.randomUUID();
        ZonedDateTime acknowledgedAt = ZonedDateTime.now();
        MovementDiscrepancyDTO dto = MovementDiscrepancyDTO.builder()
                .acknowledged(true)
                .acknowledgedBy(acknowledgedBy)
                .acknowledgedAt(acknowledgedAt)
                .resolutionNotes("Resolved by admin")
                .build();

        // Act
        mapper.updateEntityFromDTO(entity, dto);

        // Assert
        assertTrue(entity.getAcknowledged());
        assertEquals(acknowledgedBy, entity.getAcknowledgedBy());
        assertEquals(acknowledgedAt, entity.getAcknowledgedAt());
        assertEquals("Resolved by admin", entity.getResolutionNotes());
    }

    @Test
    @DisplayName("Should handle all discrepancy types")
    void toDTO_shouldHandleAllDiscrepancyTypes() {
        for (MovementDiscrepancy.DiscrepancyType type : MovementDiscrepancy.DiscrepancyType.values()) {
            MovementDiscrepancy entity = MovementDiscrepancy.builder()
                    .id(UUID.randomUUID())
                    .discrepancyId("DISC-TYPE-" + type.name())
                    .assetIdentifier("GSE-001")
                    .discrepancyType(type)
                    .timestamp(ZonedDateTime.now())
                    .tenantCode("VIDP")
                    .build();

            MovementDiscrepancyDTO dto = mapper.toDTO(entity);
            assertEquals(type.name(), dto.getDiscrepancyType());
        }
    }

    private MovementDiscrepancy createMinimalDiscrepancy(String discrepancyId) {
        return MovementDiscrepancy.builder()
                .id(UUID.randomUUID())
                .discrepancyId(discrepancyId)
                .assetIdentifier("GSE-001")
                .discrepancyType(MovementDiscrepancy.DiscrepancyType.LOCATION_MISMATCH)
                .timestamp(ZonedDateTime.now())
                .acknowledged(false)
                .tenantCode("VIDP")
                .build();
    }
}
