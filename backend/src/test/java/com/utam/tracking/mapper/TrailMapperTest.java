package com.utam.tracking.mapper;

import com.utam.tracking.domain.AssetMovementTrail;
import com.utam.tracking.dto.MovementTrailDTO;
import com.utam.tracking.dto.MovementTrailPointDTO;
import com.utam.tracking.dto.TrailSummaryDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TrailMapper.
 * Feature: 005-asset-tracking-security
 * Task: T122
 */
@DisplayName("TrailMapper Tests")
class TrailMapperTest {

    private TrailMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new TrailMapper();
    }

    @Test
    @DisplayName("Should map trail point entity to DTO correctly")
    void toPointDTO_shouldMapAllFields() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        UUID restrictedZoneId = UUID.randomUUID();
        ZonedDateTime timestamp = ZonedDateTime.now();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "GPS");
        metadata.put("accuracy", 5.0);

        AssetMovementTrail entity = AssetMovementTrail.builder()
                .id(id)
                .assetId(assetId)
                .assetIdentifier("GSE-001")
                .latitude(28.5574)
                .longitude(77.0889)
                .speed(15.5)
                .heading(90.0)
                .altitude(200.0)
                .zone("Apron A")
                .restrictedZoneId(restrictedZoneId)
                .status("MOVING")
                .timestamp(timestamp)
                .metadata(metadata)
                .tenantCode("VIDP")
                .build();

        // Act
        MovementTrailPointDTO dto = mapper.toPointDTO(entity);

        // Assert
        assertNotNull(dto);
        assertEquals(id, dto.getId());
        assertEquals(28.5574, dto.getLatitude());
        assertEquals(77.0889, dto.getLongitude());
        assertEquals(15.5, dto.getSpeed());
        assertEquals(90.0, dto.getHeading());
        assertEquals("Apron A", dto.getZoneName());
        assertEquals("MOVING", dto.getStatus());
        assertEquals(timestamp, dto.getTimestamp());
        assertTrue(dto.getInRestrictedZone());
        assertNotNull(dto.getMetadata());
        assertEquals("GPS", dto.getMetadata().get("source"));
    }

    @Test
    @DisplayName("Should return null when entity is null")
    void toPointDTO_shouldReturnNull_whenEntityIsNull() {
        assertNull(mapper.toPointDTO(null));
    }

    @Test
    @DisplayName("Should handle null restricted zone ID")
    void toPointDTO_shouldHandleNullRestrictedZoneId() {
        AssetMovementTrail entity = AssetMovementTrail.builder()
                .id(UUID.randomUUID())
                .assetIdentifier("GSE-001")
                .latitude(28.5574)
                .longitude(77.0889)
                .restrictedZoneId(null)
                .timestamp(ZonedDateTime.now())
                .tenantCode("VIDP")
                .build();

        MovementTrailPointDTO dto = mapper.toPointDTO(entity);

        assertNotNull(dto);
        assertFalse(dto.getInRestrictedZone());
    }

    @Test
    @DisplayName("Should convert list of trail points to DTOs")
    void toPointDTOList_shouldConvertList() {
        List<AssetMovementTrail> entities = Arrays.asList(
                createTrailPoint(28.5574, 77.0889, ZonedDateTime.now().minusMinutes(10)),
                createTrailPoint(28.5575, 77.0890, ZonedDateTime.now().minusMinutes(5)),
                createTrailPoint(28.5576, 77.0891, ZonedDateTime.now())
        );

        List<MovementTrailPointDTO> dtos = mapper.toPointDTOList(entities);

        assertEquals(3, dtos.size());
        assertEquals(28.5574, dtos.get(0).getLatitude());
        assertEquals(28.5575, dtos.get(1).getLatitude());
        assertEquals(28.5576, dtos.get(2).getLatitude());
    }

    @Test
    @DisplayName("Should return empty list when input is null")
    void toPointDTOList_shouldReturnEmptyList_whenInputIsNull() {
        List<MovementTrailPointDTO> dtos = mapper.toPointDTOList(null);
        assertNotNull(dtos);
        assertTrue(dtos.isEmpty());
    }

    @Test
    @DisplayName("Should return empty list when input is empty")
    void toPointDTOList_shouldReturnEmptyList_whenInputIsEmpty() {
        List<MovementTrailPointDTO> dtos = mapper.toPointDTOList(Collections.emptyList());
        assertNotNull(dtos);
        assertTrue(dtos.isEmpty());
    }

    @Test
    @DisplayName("Should create complete trail DTO from trail points")
    void toTrailDTO_shouldCreateCompleteTrailDTO() {
        // Create 5 trail points over 10 minutes
        ZonedDateTime baseTime = ZonedDateTime.now().minusMinutes(10);
        UUID assetId = UUID.randomUUID();
        
        List<AssetMovementTrail> trailPoints = Arrays.asList(
                createTrailPointWithDetails(assetId, "GSE-001", 28.5574, 77.0889, 10.0, baseTime),
                createTrailPointWithDetails(assetId, "GSE-001", 28.5575, 77.0890, 15.0, baseTime.plusMinutes(2)),
                createTrailPointWithDetails(assetId, "GSE-001", 28.5576, 77.0891, 20.0, baseTime.plusMinutes(4)),
                createTrailPointWithDetails(assetId, "GSE-001", 28.5577, 77.0892, 12.0, baseTime.plusMinutes(6)),
                createTrailPointWithDetails(assetId, "GSE-001", 28.5578, 77.0893, 8.0, baseTime.plusMinutes(10))
        );

        MovementTrailDTO dto = mapper.toTrailDTO(trailPoints, "Test Asset", "GSE");

        assertNotNull(dto);
        assertEquals(assetId, dto.getAssetId());
        assertEquals("GSE-001", dto.getAssetIdentifier());
        assertEquals("Test Asset", dto.getAssetName());
        assertEquals("GSE", dto.getAssetCategory());
        assertEquals(5, dto.getTotalPoints());
        assertEquals(baseTime, dto.getStartTime());
        assertEquals(baseTime.plusMinutes(10), dto.getEndTime());
        assertNotNull(dto.getPoints());
        assertEquals(5, dto.getPoints().size());
        assertTrue(dto.getTotalDistanceMeters() > 0); // Should have traveled some distance
        assertTrue(dto.getAvgSpeed() > 0);
        assertTrue(dto.getMaxSpeed() > 0);
    }

    @Test
    @DisplayName("Should return null for empty trail points")
    void toTrailDTO_shouldReturnNull_whenTrailPointsEmpty() {
        assertNull(mapper.toTrailDTO(null, "Test", "GSE"));
        assertNull(mapper.toTrailDTO(Collections.emptyList(), "Test", "GSE"));
    }

    @Test
    @DisplayName("Should create trail summary from trail points")
    void toSummaryDTO_shouldCreateTrailSummary() {
        ZonedDateTime baseTime = ZonedDateTime.now().minusMinutes(30);
        UUID assetId = UUID.randomUUID();
        
        // Create trail points with various zones and speeds
        List<AssetMovementTrail> trailPoints = new ArrayList<>();
        trailPoints.add(createTrailPointWithZone(assetId, 28.5574, 77.0889, 10.0, "Apron A", null, baseTime));
        trailPoints.add(createTrailPointWithZone(assetId, 28.5575, 77.0890, 15.0, "Apron A", null, baseTime.plusMinutes(5)));
        trailPoints.add(createTrailPointWithZone(assetId, 28.5576, 77.0891, 20.0, "Taxiway B", null, baseTime.plusMinutes(10)));
        trailPoints.add(createTrailPointWithZone(assetId, 28.5577, 77.0892, 25.0, "Taxiway B", UUID.randomUUID(), baseTime.plusMinutes(15)));
        trailPoints.add(createTrailPointWithZone(assetId, 28.5578, 77.0893, 30.0, "Runway 09", UUID.randomUUID(), baseTime.plusMinutes(20)));
        trailPoints.add(createTrailPointWithZone(assetId, 28.5579, 77.0894, 5.0, "Runway 09", null, baseTime.plusMinutes(30)));

        TrailSummaryDTO summary = mapper.toSummaryDTO(trailPoints);

        assertNotNull(summary);
        assertEquals(6, summary.getTotalPoints());
        assertEquals(1800L, summary.getTotalDurationSeconds()); // 30 minutes
        assertTrue(summary.getTotalDistanceMeters() > 0);
        assertTrue(summary.getAverageSpeedKmh() > 0);
        assertEquals(30.0, summary.getMaxSpeedKmh());
        assertEquals(3, summary.getZonesEntered()); // Apron A, Taxiway B, Runway 09
        assertEquals(2, summary.getRestrictedZonesEntered()); // Two unique restricted zone IDs
    }

    @Test
    @DisplayName("Should return null for empty summary input")
    void toSummaryDTO_shouldReturnNull_whenTrailPointsEmpty() {
        assertNull(mapper.toSummaryDTO(null));
        assertNull(mapper.toSummaryDTO(Collections.emptyList()));
    }

    @Test
    @DisplayName("Should calculate distance correctly using Haversine formula")
    void toTrailDTO_shouldCalculateDistanceCorrectly() {
        // Two points approximately 100m apart
        UUID assetId = UUID.randomUUID();
        ZonedDateTime now = ZonedDateTime.now();
        
        List<AssetMovementTrail> points = Arrays.asList(
                createTrailPointWithDetails(assetId, "GSE", 28.5574, 77.0889, 0.0, now),
                createTrailPointWithDetails(assetId, "GSE", 28.5583, 77.0889, 0.0, now.plusMinutes(1))
        );

        MovementTrailDTO dto = mapper.toTrailDTO(points, "Test", "GSE");

        // Distance should be approximately 100m (0.0009° latitude ≈ 100m)
        assertTrue(dto.getTotalDistanceMeters() > 90 && dto.getTotalDistanceMeters() < 110,
                "Distance should be approximately 100m, got: " + dto.getTotalDistanceMeters());
    }

    @Test
    @DisplayName("Should handle single point trail")
    void toTrailDTO_shouldHandleSinglePoint() {
        List<AssetMovementTrail> points = Arrays.asList(
                createTrailPoint(28.5574, 77.0889, ZonedDateTime.now())
        );

        MovementTrailDTO dto = mapper.toTrailDTO(points, "Test", "GSE");

        assertNotNull(dto);
        assertEquals(1, dto.getTotalPoints());
        assertEquals(0.0, dto.getTotalDistanceMeters());
    }

    private AssetMovementTrail createTrailPoint(double lat, double lng, ZonedDateTime timestamp) {
        return AssetMovementTrail.builder()
                .id(UUID.randomUUID())
                .assetId(UUID.randomUUID())
                .assetIdentifier("GSE-001")
                .latitude(lat)
                .longitude(lng)
                .speed(10.0)
                .timestamp(timestamp)
                .tenantCode("VIDP")
                .build();
    }

    private AssetMovementTrail createTrailPointWithDetails(UUID assetId, String identifier, 
            double lat, double lng, double speed, ZonedDateTime timestamp) {
        return AssetMovementTrail.builder()
                .id(UUID.randomUUID())
                .assetId(assetId)
                .assetIdentifier(identifier)
                .latitude(lat)
                .longitude(lng)
                .speed(speed)
                .heading(90.0)
                .timestamp(timestamp)
                .tenantCode("VIDP")
                .build();
    }

    private AssetMovementTrail createTrailPointWithZone(UUID assetId, double lat, double lng, 
            double speed, String zone, UUID restrictedZoneId, ZonedDateTime timestamp) {
        return AssetMovementTrail.builder()
                .id(UUID.randomUUID())
                .assetId(assetId)
                .assetIdentifier("GSE-001")
                .latitude(lat)
                .longitude(lng)
                .speed(speed)
                .zone(zone)
                .restrictedZoneId(restrictedZoneId)
                .status("MOVING")
                .timestamp(timestamp)
                .tenantCode("VIDP")
                .build();
    }
}
