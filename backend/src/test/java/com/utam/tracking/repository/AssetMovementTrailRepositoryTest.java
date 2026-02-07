package com.utam.tracking.repository;

import com.utam.tracking.domain.AssetMovementTrail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AssetMovementTrailRepository.
 * Tests time-series query contracts through mocking.
 * 
 * Feature: 005-asset-tracking-security
 * Task: T123 - Repository unit tests
 */
@ExtendWith(MockitoExtension.class)
class AssetMovementTrailRepositoryTest {

    @Mock
    private AssetMovementTrailRepository repository;

    private AssetMovementTrail trail1;
    private AssetMovementTrail trail2;
    private AssetMovementTrail trail3;
    private final String tenantCode = "VIDP";
    private final UUID assetId = UUID.randomUUID();
    private final String assetIdentifier = "VEH-001";
    private final Pageable pageable = PageRequest.of(0, 10);

    @BeforeEach
    void setUp() {
        ZonedDateTime now = ZonedDateTime.now();
        UUID zoneId = UUID.randomUUID();
        
        trail1 = AssetMovementTrail.builder()
                .id(UUID.randomUUID())
                .assetId(assetId)
                .assetIdentifier(assetIdentifier)
                .latitude(28.5562)
                .longitude(77.1000)
                .heading(90.0)
                .speed(15.5)
                .tenantCode(tenantCode)
                .restrictedZoneId(zoneId)
                .zone("Runway 09R/27L")
                .timestamp(now.minusMinutes(30))
                .build();
        
        trail2 = AssetMovementTrail.builder()
                .id(UUID.randomUUID())
                .assetId(assetId)
                .assetIdentifier(assetIdentifier)
                .latitude(28.5565)
                .longitude(77.1005)
                .heading(92.0)
                .speed(18.2)
                .tenantCode(tenantCode)
                .restrictedZoneId(zoneId)
                .zone("Runway 09R/27L")
                .timestamp(now.minusMinutes(20))
                .build();
        
        trail3 = AssetMovementTrail.builder()
                .id(UUID.randomUUID())
                .assetId(assetId)
                .assetIdentifier(assetIdentifier)
                .latitude(28.5570)
                .longitude(77.1010)
                .heading(95.0)
                .speed(20.0)
                .tenantCode(tenantCode)
                .restrictedZoneId(null) // Outside restricted zone
                .zone(null)
                .timestamp(now.minusMinutes(10))
                .build();
    }

    @Test
    @DisplayName("findByAssetIdAndTenantCodeAndTimestampBetweenOrderByTimestampAsc returns ordered trail")
    void findByAssetIdAndTenantCodeAndTimestampBetween_ReturnsOrderedTrail() {
        ZonedDateTime startTime = ZonedDateTime.now().minusHours(1);
        ZonedDateTime endTime = ZonedDateTime.now();
        
        when(repository.findByAssetIdAndTenantCodeAndTimestampBetweenOrderByTimestampAsc(
                eq(assetId), eq(tenantCode), any(ZonedDateTime.class), any(ZonedDateTime.class)))
                .thenReturn(Arrays.asList(trail1, trail2, trail3));
        
        List<AssetMovementTrail> result = repository.findByAssetIdAndTenantCodeAndTimestampBetweenOrderByTimestampAsc(
                assetId, tenantCode, startTime, endTime);
        
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getTimestamp()).isBefore(result.get(1).getTimestamp());
        assertThat(result.get(1).getTimestamp()).isBefore(result.get(2).getTimestamp());
    }

    @Test
    @DisplayName("findByAssetIdentifierAndTenantCodeAndTimestampBetweenOrderByTimestampAsc returns trail by identifier")
    void findByAssetIdentifierAndTenantCodeAndTimestampBetween_ReturnsTrailByIdentifier() {
        ZonedDateTime startTime = ZonedDateTime.now().minusHours(1);
        ZonedDateTime endTime = ZonedDateTime.now();
        
        when(repository.findByAssetIdentifierAndTenantCodeAndTimestampBetweenOrderByTimestampAsc(
                eq(assetIdentifier), eq(tenantCode), any(ZonedDateTime.class), any(ZonedDateTime.class)))
                .thenReturn(Arrays.asList(trail1, trail2, trail3));
        
        List<AssetMovementTrail> result = repository.findByAssetIdentifierAndTenantCodeAndTimestampBetweenOrderByTimestampAsc(
                assetIdentifier, tenantCode, startTime, endTime);
        
        assertThat(result).hasSize(3);
        assertThat(result).allMatch(t -> t.getAssetIdentifier().equals(assetIdentifier));
    }

    @Test
    @DisplayName("findByTenantCodeAndTimestampAfterOrderByTimestampDesc returns recent movements")
    void findByTenantCodeAndTimestampAfter_ReturnsRecentMovements() {
        ZonedDateTime after = ZonedDateTime.now().minusHours(1);
        Page<AssetMovementTrail> expectedPage = new PageImpl<>(
                Arrays.asList(trail3, trail2, trail1), // Descending order
                pageable, 3);
        
        when(repository.findByTenantCodeAndTimestampAfterOrderByTimestampDesc(
                eq(tenantCode), any(ZonedDateTime.class), any(Pageable.class)))
                .thenReturn(expectedPage);
        
        Page<AssetMovementTrail> result = repository.findByTenantCodeAndTimestampAfterOrderByTimestampDesc(
                tenantCode, after, pageable);
        
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent().get(0).getTimestamp())
                .isAfter(result.getContent().get(1).getTimestamp());
    }

    @Test
    @DisplayName("findByZoneAndTimeRange returns movements in specific zone")
    void findByZoneAndTimeRange_ReturnsZoneMovements() {
        UUID zoneId = trail1.getRestrictedZoneId();
        ZonedDateTime startTime = ZonedDateTime.now().minusHours(1);
        ZonedDateTime endTime = ZonedDateTime.now();
        
        when(repository.findByZoneAndTimeRange(eq(zoneId), eq(tenantCode), any(ZonedDateTime.class), any(ZonedDateTime.class)))
                .thenReturn(Arrays.asList(trail1, trail2));
        
        List<AssetMovementTrail> result = repository.findByZoneAndTimeRange(
                zoneId, tenantCode, startTime, endTime);
        
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(t -> t.getRestrictedZoneId() != null);
        assertThat(result).allMatch(t -> t.getZone() != null);
    }

    @Test
    @DisplayName("countByAssetIdAndTimestampBetween returns correct count")
    void countByAssetIdAndTimestampBetween_ReturnsCount() {
        ZonedDateTime startTime = ZonedDateTime.now().minusHours(1);
        ZonedDateTime endTime = ZonedDateTime.now();
        
        when(repository.countByAssetIdAndTimestampBetween(
                eq(assetId), any(ZonedDateTime.class), any(ZonedDateTime.class)))
                .thenReturn(3L);
        
        long count = repository.countByAssetIdAndTimestampBetween(assetId, startTime, endTime);
        
        assertThat(count).isEqualTo(3L);
    }

    @Test
    @DisplayName("Empty result for non-existent asset")
    void findByAssetIdAndTenantCodeAndTimestampBetween_ReturnsEmptyForNonExistentAsset() {
        UUID unknownAssetId = UUID.randomUUID();
        ZonedDateTime startTime = ZonedDateTime.now().minusHours(1);
        ZonedDateTime endTime = ZonedDateTime.now();
        
        when(repository.findByAssetIdAndTenantCodeAndTimestampBetweenOrderByTimestampAsc(
                eq(unknownAssetId), eq(tenantCode), any(ZonedDateTime.class), any(ZonedDateTime.class)))
                .thenReturn(Collections.emptyList());
        
        List<AssetMovementTrail> result = repository.findByAssetIdAndTenantCodeAndTimestampBetweenOrderByTimestampAsc(
                unknownAssetId, tenantCode, startTime, endTime);
        
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Empty result for time range with no data")
    void findByAssetIdAndTenantCodeAndTimestampBetween_ReturnsEmptyForEmptyTimeRange() {
        ZonedDateTime startTime = ZonedDateTime.now().minusYears(10);
        ZonedDateTime endTime = ZonedDateTime.now().minusYears(9);
        
        when(repository.findByAssetIdAndTenantCodeAndTimestampBetweenOrderByTimestampAsc(
                eq(assetId), eq(tenantCode), any(ZonedDateTime.class), any(ZonedDateTime.class)))
                .thenReturn(Collections.emptyList());
        
        List<AssetMovementTrail> result = repository.findByAssetIdAndTenantCodeAndTimestampBetweenOrderByTimestampAsc(
                assetId, tenantCode, startTime, endTime);
        
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Trail contains speed and heading data")
    void trail_ContainsSpeedAndHeading() {
        ZonedDateTime startTime = ZonedDateTime.now().minusHours(1);
        ZonedDateTime endTime = ZonedDateTime.now();
        
        when(repository.findByAssetIdAndTenantCodeAndTimestampBetweenOrderByTimestampAsc(
                eq(assetId), eq(tenantCode), any(ZonedDateTime.class), any(ZonedDateTime.class)))
                .thenReturn(Arrays.asList(trail1, trail2, trail3));
        
        List<AssetMovementTrail> result = repository.findByAssetIdAndTenantCodeAndTimestampBetweenOrderByTimestampAsc(
                assetId, tenantCode, startTime, endTime);
        
        assertThat(result).allMatch(t -> t.getSpeed() != null && t.getSpeed() >= 0);
        assertThat(result).allMatch(t -> t.getHeading() != null && t.getHeading() >= 0);
    }

    @Test
    @DisplayName("Pagination returns correct page for large result sets")
    void findByTenantCodeAndTimestampAfter_PaginationWorks() {
        ZonedDateTime after = ZonedDateTime.now().minusHours(1);
        Pageable smallPage = PageRequest.of(0, 2);
        Page<AssetMovementTrail> expectedPage = new PageImpl<>(
                Arrays.asList(trail3, trail2),
                smallPage, 3);
        
        when(repository.findByTenantCodeAndTimestampAfterOrderByTimestampDesc(
                eq(tenantCode), any(ZonedDateTime.class), eq(smallPage)))
                .thenReturn(expectedPage);
        
        Page<AssetMovementTrail> result = repository.findByTenantCodeAndTimestampAfterOrderByTimestampDesc(
                tenantCode, after, smallPage);
        
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    @DisplayName("Trail points show zone entry and exit")
    void trail_ShowsZoneTransitions() {
        ZonedDateTime startTime = ZonedDateTime.now().minusHours(1);
        ZonedDateTime endTime = ZonedDateTime.now();
        
        when(repository.findByAssetIdAndTenantCodeAndTimestampBetweenOrderByTimestampAsc(
                eq(assetId), eq(tenantCode), any(ZonedDateTime.class), any(ZonedDateTime.class)))
                .thenReturn(Arrays.asList(trail1, trail2, trail3));
        
        List<AssetMovementTrail> result = repository.findByAssetIdAndTenantCodeAndTimestampBetweenOrderByTimestampAsc(
                assetId, tenantCode, startTime, endTime);
        
        // First two points in restricted zone
        assertThat(result.get(0).getRestrictedZoneId()).isNotNull();
        assertThat(result.get(1).getRestrictedZoneId()).isNotNull();
        // Third point outside restricted zone
        assertThat(result.get(2).getRestrictedZoneId()).isNull();
    }
}
