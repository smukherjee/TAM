package com.utam.tracking.repository;

import com.utam.tracking.domain.ZoneViolation;
import com.utam.tracking.domain.ZoneViolation.ViolationSeverity;
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
 * Unit tests for ZoneViolationRepository.
 * Uses mocking to test repository method contracts without requiring a database.
 * 
 * Feature: 005-asset-tracking-security
 * Task: T123 - Repository unit tests
 */
@ExtendWith(MockitoExtension.class)
class ZoneViolationRepositoryTest {

    @Mock
    private ZoneViolationRepository repository;

    private ZoneViolation violation1;
    private ZoneViolation violation2;
    private ZoneViolation violation3;
    private final String tenantCode = "VIDP";
    private final Pageable pageable = PageRequest.of(0, 10);

    @BeforeEach
    void setUp() {
        ZonedDateTime now = ZonedDateTime.now();
        
        violation1 = ZoneViolation.builder()
                .id(UUID.randomUUID())
                .violationId("VIO-001")
                .assetId(UUID.randomUUID())
                .assetIdentifier("VEH-001")
                .assetName("Baggage Tractor 1")
                .assetCategory("GSE")
                .restrictedZoneId(UUID.randomUUID())
                .zoneName("Runway 09R/27L")
                .zoneType("RUNWAY")
                .violationType("UNAUTHORIZED_ENTRY")
                .severity(ViolationSeverity.CRITICAL)
                .acknowledged(false)
                .tenantCode(tenantCode)
                .timestamp(now.minusHours(1))
                .build();
        
        violation2 = ZoneViolation.builder()
                .id(UUID.randomUUID())
                .violationId("VIO-002")
                .assetId(UUID.randomUUID())
                .assetIdentifier("VEH-002")
                .assetName("Fuel Truck 3")
                .assetCategory("GSE")
                .restrictedZoneId(UUID.randomUUID())
                .zoneName("Taxiway Alpha")
                .zoneType("TAXIWAY")
                .violationType("OVERSTAY")
                .severity(ViolationSeverity.HIGH)
                .acknowledged(true)
                .acknowledgedBy("admin@vidp.airport")
                .acknowledgedAt(now.minusMinutes(30))
                .tenantCode(tenantCode)
                .timestamp(now.minusHours(2))
                .build();
        
        violation3 = ZoneViolation.builder()
                .id(UUID.randomUUID())
                .violationId("VIO-003")
                .assetId(UUID.randomUUID())
                .assetIdentifier("VEH-003")
                .assetName("Catering Vehicle 5")
                .assetCategory("GSE")
                .restrictedZoneId(UUID.randomUUID())
                .zoneName("Gate Security Zone")
                .zoneType("GATE")
                .violationType("UNAUTHORIZED_ENTRY")
                .severity(ViolationSeverity.CRITICAL)
                .acknowledged(false)
                .tenantCode(tenantCode)
                .timestamp(now)
                .build();
    }

    @Test
    @DisplayName("findByTenantCode returns paginated violations for tenant")
    void findByTenantCode_ReturnsPaginatedResults() {
        Page<ZoneViolation> expectedPage = new PageImpl<>(
                Arrays.asList(violation1, violation2, violation3),
                pageable, 3);
        
        when(repository.findByTenantCode(eq(tenantCode), any(Pageable.class)))
                .thenReturn(expectedPage);
        
        Page<ZoneViolation> result = repository.findByTenantCode(tenantCode, pageable);
        
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent()).allMatch(v -> v.getTenantCode().equals(tenantCode));
    }

    @Test
    @DisplayName("findByTenantCodeAndTimestampBetween filters by time range")
    void findByTenantCodeAndTimestampBetween_FiltersTimeRange() {
        ZonedDateTime startTime = ZonedDateTime.now().minusHours(3);
        ZonedDateTime endTime = ZonedDateTime.now();
        
        Page<ZoneViolation> expectedPage = new PageImpl<>(
                Arrays.asList(violation1, violation2, violation3),
                pageable, 3);
        
        when(repository.findByTenantCodeAndTimestampBetween(
                eq(tenantCode), any(ZonedDateTime.class), any(ZonedDateTime.class), any(Pageable.class)))
                .thenReturn(expectedPage);
        
        Page<ZoneViolation> result = repository.findByTenantCodeAndTimestampBetween(
                tenantCode, startTime, endTime, pageable);
        
        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("findByAcknowledged returns only unacknowledged violations")
    void findByAcknowledged_ReturnsUnacknowledgedOnly() {
        Page<ZoneViolation> expectedPage = new PageImpl<>(
                Arrays.asList(violation1, violation3),
                pageable, 2);
        
        when(repository.findByAcknowledged(eq(false), any(Pageable.class)))
                .thenReturn(expectedPage);
        
        Page<ZoneViolation> result = repository.findByAcknowledged(false, pageable);
        
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).allMatch(v -> !v.getAcknowledged());
    }

    @Test
    @DisplayName("findByAcknowledged returns only acknowledged violations")
    void findByAcknowledged_ReturnsAcknowledgedOnly() {
        Page<ZoneViolation> expectedPage = new PageImpl<>(
                Collections.singletonList(violation2),
                pageable, 1);
        
        when(repository.findByAcknowledged(eq(true), any(Pageable.class)))
                .thenReturn(expectedPage);
        
        Page<ZoneViolation> result = repository.findByAcknowledged(true, pageable);
        
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getAcknowledgedBy()).isNotNull();
    }

    @Test
    @DisplayName("findByTenantCodeAndSeverity filters by severity level")
    void findByTenantCodeAndSeverity_FiltersBySeverity() {
        Page<ZoneViolation> expectedPage = new PageImpl<>(
                Arrays.asList(violation1, violation3),
                pageable, 2);
        
        when(repository.findByTenantCodeAndSeverity(
                eq(tenantCode), eq(ViolationSeverity.CRITICAL), any(Pageable.class)))
                .thenReturn(expectedPage);
        
        Page<ZoneViolation> result = repository.findByTenantCodeAndSeverity(
                tenantCode, ViolationSeverity.CRITICAL, pageable);
        
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).allMatch(v -> v.getSeverity() == ViolationSeverity.CRITICAL);
    }

    @Test
    @DisplayName("findByTenantCodeAndSeverityOrderByTimestampDesc returns ordered list")
    void findByTenantCodeAndSeverityOrdered_ReturnsOrderedList() {
        List<ZoneViolation> violations = Arrays.asList(violation3, violation1);
        
        when(repository.findByTenantCodeAndSeverityOrderByTimestampDesc(
                eq(tenantCode), eq(ViolationSeverity.CRITICAL)))
                .thenReturn(violations);
        
        List<ZoneViolation> result = repository.findByTenantCodeAndSeverityOrderByTimestampDesc(
                tenantCode, ViolationSeverity.CRITICAL);
        
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTimestamp()).isAfter(result.get(1).getTimestamp());
    }

    @Test
    @DisplayName("Empty result for non-existent tenant")
    void findByTenantCode_ReturnsEmptyForNonExistentTenant() {
        when(repository.findByTenantCode(eq("UNKNOWN"), any(Pageable.class)))
                .thenReturn(Page.empty());
        
        Page<ZoneViolation> result = repository.findByTenantCode("UNKNOWN", pageable);
        
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("Pagination returns correct page size")
    void findByTenantCode_PaginationWorksCorrectly() {
        Pageable smallPage = PageRequest.of(0, 2);
        Page<ZoneViolation> expectedPage = new PageImpl<>(
                Arrays.asList(violation1, violation2),
                smallPage, 3);
        
        when(repository.findByTenantCode(eq(tenantCode), eq(smallPage)))
                .thenReturn(expectedPage);
        
        Page<ZoneViolation> result = repository.findByTenantCode(tenantCode, smallPage);
        
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    @DisplayName("Second page returns remaining violations")
    void findByTenantCode_SecondPageReturnsRemaining() {
        Pageable secondPage = PageRequest.of(1, 2);
        Page<ZoneViolation> expectedPage = new PageImpl<>(
                Collections.singletonList(violation3),
                secondPage, 3);
        
        when(repository.findByTenantCode(eq(tenantCode), eq(secondPage)))
                .thenReturn(expectedPage);
        
        Page<ZoneViolation> result = repository.findByTenantCode(tenantCode, secondPage);
        
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getNumber()).isEqualTo(1);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isTrue();
    }
}
