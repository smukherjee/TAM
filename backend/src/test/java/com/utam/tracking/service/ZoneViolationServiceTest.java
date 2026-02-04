package com.utam.tracking.service;

import com.utam.tracking.domain.ZoneViolation;
import com.utam.tracking.domain.ZoneViolation.ViolationSeverity;
import com.utam.tracking.dto.ZoneViolationDTO;
import com.utam.tracking.repository.ZoneViolationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ZoneViolationService.
 * Tests business logic with mocked repository.
 * 
 * Feature: 005-asset-tracking-security
 * Task: T124 - Service unit tests
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ZoneViolationService Tests")
class ZoneViolationServiceTest {

    @Mock
    private ZoneViolationRepository violationRepository;

    @InjectMocks
    private ZoneViolationService service;

    private ZoneViolation violation1;
    private ZoneViolation violation2;
    private final String tenantCode = "VIDP";
    private final UUID violationId = UUID.randomUUID();
    private final Pageable pageable = PageRequest.of(0, 10);

    @BeforeEach
    void setUp() {
        ZonedDateTime now = ZonedDateTime.now();
        
        violation1 = ZoneViolation.builder()
                .id(violationId)
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
                .acknowledgedBy(UUID.randomUUID())
                .acknowledgedAt(now.minusMinutes(30))
                .tenantCode(tenantCode)
                .timestamp(now.minusHours(2))
                .build();
    }

    @Nested
    @DisplayName("getViolations")
    class GetViolations {

        @Test
        @DisplayName("returns violations for tenant with pagination")
        void returnsViolationsForTenant() {
            Page<ZoneViolation> page = new PageImpl<>(
                    Arrays.asList(violation1, violation2), pageable, 2);
            when(violationRepository.findByTenantCode(eq(tenantCode), any(Pageable.class)))
                    .thenReturn(page);

            Page<ZoneViolationDTO> result = service.getViolations(
                    tenantCode, null, null, null, null, pageable);

            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).hasSize(2);
            verify(violationRepository).findByTenantCode(tenantCode, pageable);
        }

        @Test
        @DisplayName("filters by severity when provided")
        void filtersBySeverity() {
            Page<ZoneViolation> page = new PageImpl<>(
                    Collections.singletonList(violation1), pageable, 1);
            when(violationRepository.findByTenantCodeAndSeverity(
                    eq(tenantCode), eq(ViolationSeverity.CRITICAL), any(Pageable.class)))
                    .thenReturn(page);

            Page<ZoneViolationDTO> result = service.getViolations(
                    tenantCode, null, null, "CRITICAL", null, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getSeverity()).isEqualTo("CRITICAL");
            verify(violationRepository).findByTenantCodeAndSeverity(
                    tenantCode, ViolationSeverity.CRITICAL, pageable);
        }

        @Test
        @DisplayName("filters by acknowledged status when provided")
        void filtersByAcknowledged() {
            Page<ZoneViolation> page = new PageImpl<>(
                    Collections.singletonList(violation1), pageable, 1);
            when(violationRepository.findByAcknowledged(eq(false), any(Pageable.class)))
                    .thenReturn(page);

            Page<ZoneViolationDTO> result = service.getViolations(
                    tenantCode, null, null, null, false, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getAcknowledged()).isFalse();
        }

        @Test
        @DisplayName("filters by date range when provided")
        void filtersByDateRange() {
            ZonedDateTime startDate = ZonedDateTime.now().minusDays(1);
            ZonedDateTime endDate = ZonedDateTime.now();
            
            Page<ZoneViolation> page = new PageImpl<>(
                    Arrays.asList(violation1, violation2), pageable, 2);
            when(violationRepository.findByTenantCodeAndTimestampBetween(
                    eq(tenantCode), any(ZonedDateTime.class), any(ZonedDateTime.class), any(Pageable.class)))
                    .thenReturn(page);

            Page<ZoneViolationDTO> result = service.getViolations(
                    tenantCode, startDate, endDate, null, null, pageable);

            assertThat(result.getTotalElements()).isEqualTo(2);
            verify(violationRepository).findByTenantCodeAndTimestampBetween(
                    tenantCode, startDate, endDate, pageable);
        }

        @Test
        @DisplayName("returns empty page for tenant with no violations")
        void returnsEmptyForNoViolations() {
            when(violationRepository.findByTenantCode(eq("LIRN"), any(Pageable.class)))
                    .thenReturn(Page.empty());

            Page<ZoneViolationDTO> result = service.getViolations(
                    "LIRN", null, null, null, null, pageable);

            assertThat(result.getTotalElements()).isZero();
            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getViolationById")
    class GetViolationById {

        @Test
        @DisplayName("returns violation when found")
        void returnsViolationWhenFound() {
            when(violationRepository.findById(violationId))
                    .thenReturn(Optional.of(violation1));

            ZoneViolationDTO result = service.getViolationById(violationId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(violationId);
            assertThat(result.getViolationId()).isEqualTo("VIO-001");
            assertThat(result.getAssetName()).isEqualTo("Baggage Tractor 1");
        }

        @Test
        @DisplayName("throws exception when not found")
        void throwsExceptionWhenNotFound() {
            UUID unknownId = UUID.randomUUID();
            when(violationRepository.findById(unknownId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getViolationById(unknownId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Violation not found");
        }
    }

    @Nested
    @DisplayName("acknowledgeViolation")
    class AcknowledgeViolation {

        @Test
        @DisplayName("acknowledges violation successfully")
        void acknowledgesViolationSuccessfully() {
            when(violationRepository.findById(violationId))
                    .thenReturn(Optional.of(violation1));
            when(violationRepository.save(any(ZoneViolation.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            ZoneViolationDTO result = service.acknowledgeViolation(
                    violationId, "supervisor@vidp.airport", "Investigated and cleared");

            assertThat(result.getAcknowledged()).isTrue();
            assertThat(result.getAcknowledgedBy()).isEqualTo("supervisor@vidp.airport");
            assertThat(result.getResolutionNotes()).isEqualTo("Investigated and cleared");
            assertThat(result.getAcknowledgedAt()).isNotNull();
            
            verify(violationRepository).save(any(ZoneViolation.class));
        }

        @Test
        @DisplayName("sets acknowledgedAt timestamp")
        void setsAcknowledgedAtTimestamp() {
            when(violationRepository.findById(violationId))
                    .thenReturn(Optional.of(violation1));
            when(violationRepository.save(any(ZoneViolation.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            ZonedDateTime beforeAck = ZonedDateTime.now();
            service.acknowledgeViolation(violationId, "user", "notes");
            ZonedDateTime afterAck = ZonedDateTime.now();

            ArgumentCaptor<ZoneViolation> captor = ArgumentCaptor.forClass(ZoneViolation.class);
            verify(violationRepository).save(captor.capture());
            
            ZoneViolation saved = captor.getValue();
            assertThat(saved.getAcknowledgedAt())
                    .isAfterOrEqualTo(beforeAck)
                    .isBeforeOrEqualTo(afterAck);
        }

        @Test
        @DisplayName("throws exception when violation not found")
        void throwsExceptionWhenNotFound() {
            UUID unknownId = UUID.randomUUID();
            when(violationRepository.findById(unknownId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> 
                    service.acknowledgeViolation(unknownId, "user", "notes"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Violation not found");
        }
    }

    @Nested
    @DisplayName("getRecentUnacknowledgedViolations")
    class GetRecentUnacknowledgedViolations {

        @Test
        @DisplayName("returns limited unacknowledged violations")
        void returnsLimitedUnacknowledgedViolations() {
            when(violationRepository.findByTenantCodeAndAcknowledgedOrderByTimestampDesc(
                    tenantCode, false))
                    .thenReturn(Arrays.asList(violation1));

            List<ZoneViolationDTO> result = service.getRecentUnacknowledgedViolations(
                    tenantCode, 5);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAcknowledged()).isFalse();
        }

        @Test
        @DisplayName("respects limit parameter")
        void respectsLimitParameter() {
            // Create 10 violations
            List<ZoneViolation> manyViolations = Arrays.asList(
                    violation1, violation1, violation1, violation1, violation1,
                    violation1, violation1, violation1, violation1, violation1
            );
            when(violationRepository.findByTenantCodeAndAcknowledgedOrderByTimestampDesc(
                    tenantCode, false))
                    .thenReturn(manyViolations);

            List<ZoneViolationDTO> result = service.getRecentUnacknowledgedViolations(
                    tenantCode, 3);

            assertThat(result).hasSize(3);
        }

        @Test
        @DisplayName("returns empty list when no unacknowledged violations")
        void returnsEmptyWhenNoUnacknowledged() {
            when(violationRepository.findByTenantCodeAndAcknowledgedOrderByTimestampDesc(
                    tenantCode, false))
                    .thenReturn(Collections.emptyList());

            List<ZoneViolationDTO> result = service.getRecentUnacknowledgedViolations(
                    tenantCode, 5);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getAllViolationsForExport")
    class GetAllViolationsForExport {

        @Test
        @DisplayName("returns all violations for export without date filter")
        void returnsAllWithoutDateFilter() {
            when(violationRepository.findByTenantCodeOrderByTimestampDesc(tenantCode))
                    .thenReturn(Arrays.asList(violation1, violation2));

            List<ZoneViolationDTO> result = service.getAllViolationsForExport(
                    tenantCode, null, null, null, null);

            assertThat(result).hasSize(2);
            verify(violationRepository).findByTenantCodeOrderByTimestampDesc(tenantCode);
        }

        @Test
        @DisplayName("filters by date range for export")
        void filtersByDateRangeForExport() {
            ZonedDateTime startDate = ZonedDateTime.now().minusDays(1);
            ZonedDateTime endDate = ZonedDateTime.now();
            
            when(violationRepository.findByTenantCodeAndTimestampBetweenOrderByTimestampDesc(
                    eq(tenantCode), any(ZonedDateTime.class), any(ZonedDateTime.class)))
                    .thenReturn(Arrays.asList(violation1, violation2));

            List<ZoneViolationDTO> result = service.getAllViolationsForExport(
                    tenantCode, startDate, endDate, null, null);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("filters by severity for export")
        void filtersBySeverityForExport() {
            when(violationRepository.findByTenantCodeOrderByTimestampDesc(tenantCode))
                    .thenReturn(Arrays.asList(violation1, violation2));

            List<ZoneViolationDTO> result = service.getAllViolationsForExport(
                    tenantCode, null, null, "CRITICAL", null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSeverity()).isEqualTo("CRITICAL");
        }

        @Test
        @DisplayName("filters by acknowledged status for export")
        void filtersByAcknowledgedForExport() {
            when(violationRepository.findByTenantCodeOrderByTimestampDesc(tenantCode))
                    .thenReturn(Arrays.asList(violation1, violation2));

            List<ZoneViolationDTO> result = service.getAllViolationsForExport(
                    tenantCode, null, null, null, false);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAcknowledged()).isFalse();
        }
    }

    @Nested
    @DisplayName("DTO Mapping")
    class DtoMapping {

        @Test
        @DisplayName("maps all fields from entity to DTO")
        void mapsAllFields() {
            when(violationRepository.findById(violationId))
                    .thenReturn(Optional.of(violation1));

            ZoneViolationDTO dto = service.getViolationById(violationId);

            assertThat(dto.getId()).isEqualTo(violation1.getId());
            assertThat(dto.getViolationId()).isEqualTo(violation1.getViolationId());
            assertThat(dto.getAssetId()).isEqualTo(violation1.getAssetId());
            assertThat(dto.getAssetIdentifier()).isEqualTo(violation1.getAssetIdentifier());
            assertThat(dto.getAssetName()).isEqualTo(violation1.getAssetName());
            assertThat(dto.getAssetCategory()).isEqualTo(violation1.getAssetCategory());
            assertThat(dto.getZoneName()).isEqualTo(violation1.getZoneName());
            assertThat(dto.getZoneType()).isEqualTo(violation1.getZoneType());
            assertThat(dto.getViolationType()).isEqualTo(violation1.getViolationType());
            assertThat(dto.getSeverity()).isEqualTo(violation1.getSeverity().name());
            assertThat(dto.getAcknowledged()).isEqualTo(violation1.getAcknowledged());
            assertThat(dto.getTenantCode()).isEqualTo(violation1.getTenantCode());
        }

        @Test
        @DisplayName("handles null entry location gracefully")
        void handlesNullEntryLocation() {
            violation1.setEntryLocation(null);
            when(violationRepository.findById(violationId))
                    .thenReturn(Optional.of(violation1));

            ZoneViolationDTO dto = service.getViolationById(violationId);

            assertThat(dto.getEntryLatitude()).isNull();
            assertThat(dto.getEntryLongitude()).isNull();
        }
    }
}
