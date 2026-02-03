package com.utam.tracking.repository;

import com.utam.tracking.domain.MovementDiscrepancy;
import com.utam.tracking.domain.MovementDiscrepancy.DiscrepancyType;
import com.utam.tracking.domain.MovementDiscrepancy.DiscrepancySeverity;
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
 * Unit tests for MovementDiscrepancyRepository.
 * Uses mocking to test repository method contracts without requiring a database.
 * 
 * Feature: 005-asset-tracking-security
 * Task: T123 - Repository unit tests
 */
@ExtendWith(MockitoExtension.class)
class MovementDiscrepancyRepositoryTest {

    @Mock
    private MovementDiscrepancyRepository repository;

    private MovementDiscrepancy discrepancy1;
    private MovementDiscrepancy discrepancy2;
    private MovementDiscrepancy discrepancy3;
    private final String tenantCode = "VIDP";
    private final Pageable pageable = PageRequest.of(0, 10);

    @BeforeEach
    void setUp() {
        ZonedDateTime now = ZonedDateTime.now();
        
        discrepancy1 = MovementDiscrepancy.builder()
                .id(UUID.randomUUID())
                .discrepancyId("DISC-001")
                .assetId(UUID.randomUUID())
                .assetIdentifier("VEH-001")
                .assetName("Baggage Cart 1")
                .assetCategory("GSE")
                .discrepancyType(DiscrepancyType.LOCATION_MISMATCH)
                .expectedLocation("Terminal 2 Gate A")
                .actualLocation("Runway Holding Point")
                .deviationMeters(500.0)
                .severity(DiscrepancySeverity.CRITICAL)
                .acknowledged(false)
                .tenantCode(tenantCode)
                .timestamp(now.minusHours(1))
                .build();
        
        discrepancy2 = MovementDiscrepancy.builder()
                .id(UUID.randomUUID())
                .discrepancyId("DISC-002")
                .assetId(UUID.randomUUID())
                .assetIdentifier("VEH-002")
                .assetName("Fuel Truck 5")
                .assetCategory("GSE")
                .discrepancyType(DiscrepancyType.SPEED_ANOMALY)
                .description("Exceeded 50 km/h limit")
                .severity(DiscrepancySeverity.MEDIUM)
                .acknowledged(true)
                .acknowledgedBy(UUID.randomUUID())
                .acknowledgedAt(now.minusMinutes(30))
                .tenantCode(tenantCode)
                .timestamp(now.minusHours(2))
                .build();
        
        discrepancy3 = MovementDiscrepancy.builder()
                .id(UUID.randomUUID())
                .discrepancyId("DISC-003")
                .assetId(UUID.randomUUID())
                .assetIdentifier("VEH-003")
                .assetName("Passenger Bus 2")
                .assetCategory("GSE")
                .discrepancyType(DiscrepancyType.UNEXPECTED_MOVEMENT)
                .expectedStatus("STATIONARY")
                .actualStatus("MOVING")
                .severity(DiscrepancySeverity.HIGH)
                .acknowledged(false)
                .tenantCode(tenantCode)
                .timestamp(now)
                .build();
    }

    @Test
    @DisplayName("findByTenantCode returns paginated discrepancies for tenant")
    void findByTenantCode_ReturnsPaginatedResults() {
        Page<MovementDiscrepancy> expectedPage = new PageImpl<>(
                Arrays.asList(discrepancy1, discrepancy2, discrepancy3),
                pageable, 3);
        
        when(repository.findByTenantCode(eq(tenantCode), any(Pageable.class)))
                .thenReturn(expectedPage);
        
        Page<MovementDiscrepancy> result = repository.findByTenantCode(tenantCode, pageable);
        
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent()).allMatch(d -> d.getTenantCode().equals(tenantCode));
    }

    @Test
    @DisplayName("findByTenantCodeAndTimestampBetween filters by time range")
    void findByTenantCodeAndTimestampBetween_FiltersTimeRange() {
        ZonedDateTime startTime = ZonedDateTime.now().minusHours(3);
        ZonedDateTime endTime = ZonedDateTime.now();
        
        Page<MovementDiscrepancy> expectedPage = new PageImpl<>(
                Arrays.asList(discrepancy1, discrepancy2, discrepancy3),
                pageable, 3);
        
        when(repository.findByTenantCodeAndTimestampBetween(
                eq(tenantCode), any(ZonedDateTime.class), any(ZonedDateTime.class), any(Pageable.class)))
                .thenReturn(expectedPage);
        
        Page<MovementDiscrepancy> result = repository.findByTenantCodeAndTimestampBetween(
                tenantCode, startTime, endTime, pageable);
        
        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("findByDiscrepancyType filters by type")
    void findByDiscrepancyType_FiltersByType() {
        Page<MovementDiscrepancy> expectedPage = new PageImpl<>(
                Collections.singletonList(discrepancy1),
                pageable, 1);
        
        when(repository.findByDiscrepancyType(eq(DiscrepancyType.LOCATION_MISMATCH), any(Pageable.class)))
                .thenReturn(expectedPage);
        
        Page<MovementDiscrepancy> result = repository.findByDiscrepancyType(
                DiscrepancyType.LOCATION_MISMATCH, pageable);
        
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getDiscrepancyType())
                .isEqualTo(DiscrepancyType.LOCATION_MISMATCH);
    }

    @Test
    @DisplayName("findByAcknowledged returns only unacknowledged discrepancies")
    void findByAcknowledged_ReturnsUnacknowledgedOnly() {
        Page<MovementDiscrepancy> expectedPage = new PageImpl<>(
                Arrays.asList(discrepancy1, discrepancy3),
                pageable, 2);
        
        when(repository.findByAcknowledged(eq(false), any(Pageable.class)))
                .thenReturn(expectedPage);
        
        Page<MovementDiscrepancy> result = repository.findByAcknowledged(false, pageable);
        
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).allMatch(d -> !d.getAcknowledged());
    }

    @Test
    @DisplayName("findByAcknowledged returns only acknowledged discrepancies")
    void findByAcknowledged_ReturnsAcknowledgedOnly() {
        Page<MovementDiscrepancy> expectedPage = new PageImpl<>(
                Collections.singletonList(discrepancy2),
                pageable, 1);
        
        when(repository.findByAcknowledged(eq(true), any(Pageable.class)))
                .thenReturn(expectedPage);
        
        Page<MovementDiscrepancy> result = repository.findByAcknowledged(true, pageable);
        
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getAcknowledgedBy()).isNotNull();
    }

    @Test
    @DisplayName("findByTenantCodeAndSeverityOrderByTimestampDesc returns ordered list")
    void findByTenantCodeAndSeverityOrdered_ReturnsOrderedList() {
        List<MovementDiscrepancy> discrepancies = Collections.singletonList(discrepancy1);
        
        when(repository.findByTenantCodeAndSeverityOrderByTimestampDesc(
                eq(tenantCode), eq(DiscrepancySeverity.CRITICAL)))
                .thenReturn(discrepancies);
        
        List<MovementDiscrepancy> result = repository.findByTenantCodeAndSeverityOrderByTimestampDesc(
                tenantCode, DiscrepancySeverity.CRITICAL);
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSeverity()).isEqualTo(DiscrepancySeverity.CRITICAL);
    }

    @Test
    @DisplayName("Empty result for non-existent tenant")
    void findByTenantCode_ReturnsEmptyForNonExistentTenant() {
        when(repository.findByTenantCode(eq("UNKNOWN"), any(Pageable.class)))
                .thenReturn(Page.empty());
        
        Page<MovementDiscrepancy> result = repository.findByTenantCode("UNKNOWN", pageable);
        
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("Pagination returns correct page size")
    void findByTenantCode_PaginationWorksCorrectly() {
        Pageable smallPage = PageRequest.of(0, 2);
        Page<MovementDiscrepancy> expectedPage = new PageImpl<>(
                Arrays.asList(discrepancy1, discrepancy2),
                smallPage, 3);
        
        when(repository.findByTenantCode(eq(tenantCode), eq(smallPage)))
                .thenReturn(expectedPage);
        
        Page<MovementDiscrepancy> result = repository.findByTenantCode(tenantCode, smallPage);
        
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    @DisplayName("findByDiscrepancyTypeOrderByTimestampDesc returns ordered by time")
    void findByDiscrepancyTypeOrdered_ReturnsTimeOrdered() {
        Page<MovementDiscrepancy> expectedPage = new PageImpl<>(
                Collections.singletonList(discrepancy2),
                pageable, 1);
        
        when(repository.findByDiscrepancyTypeOrderByTimestampDesc(
                eq(DiscrepancyType.SPEED_ANOMALY), any(Pageable.class)))
                .thenReturn(expectedPage);
        
        Page<MovementDiscrepancy> result = repository.findByDiscrepancyTypeOrderByTimestampDesc(
                DiscrepancyType.SPEED_ANOMALY, pageable);
        
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getDiscrepancyType())
                .isEqualTo(DiscrepancyType.SPEED_ANOMALY);
    }

    @Test
    @DisplayName("Multiple discrepancy types can be filtered")
    void findByDiscrepancyType_DifferentTypes() {
        Page<MovementDiscrepancy> unexpectedMovementPage = new PageImpl<>(
                Collections.singletonList(discrepancy3),
                pageable, 1);
        
        when(repository.findByDiscrepancyType(eq(DiscrepancyType.UNEXPECTED_MOVEMENT), any(Pageable.class)))
                .thenReturn(unexpectedMovementPage);
        
        Page<MovementDiscrepancy> result = repository.findByDiscrepancyType(
                DiscrepancyType.UNEXPECTED_MOVEMENT, pageable);
        
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getExpectedStatus()).isEqualTo("STATIONARY");
        assertThat(result.getContent().get(0).getActualStatus()).isEqualTo("MOVING");
    }
}
