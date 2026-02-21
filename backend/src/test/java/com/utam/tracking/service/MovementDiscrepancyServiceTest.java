package com.utam.tracking.service;

import com.utam.tracking.domain.MovementDiscrepancy;
import com.utam.tracking.domain.MovementDiscrepancy.DiscrepancyType;
import com.utam.tracking.domain.MovementDiscrepancy.DiscrepancySeverity;
import com.utam.tracking.dto.MovementDiscrepancyDTO;
import com.utam.tracking.repository.MovementDiscrepancyRepository;
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
 * Unit tests for MovementDiscrepancyService.
 * Tests business logic with mocked repository.
 * 
 * Feature: 005-asset-tracking-security
 * Task: T124 - Service unit tests
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MovementDiscrepancyService Tests")
class MovementDiscrepancyServiceTest {

    @Mock
    private MovementDiscrepancyRepository discrepancyRepository;

    @InjectMocks
    private MovementDiscrepancyService service;

    private MovementDiscrepancy discrepancy1;
    private MovementDiscrepancy discrepancy2;
    private final String tenantCode = "VIDP";
    private final UUID discrepancyId = UUID.randomUUID();
    private final Pageable pageable = PageRequest.of(0, 10);

    @BeforeEach
    void setUp() {
        ZonedDateTime now = ZonedDateTime.now();
        
        discrepancy1 = MovementDiscrepancy.builder()
                .id(discrepancyId)
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
    }

    @Nested
    @DisplayName("getDiscrepancies")
    class GetDiscrepancies {

        @Test
        @DisplayName("returns discrepancies for tenant with pagination")
        void returnsDiscrepanciesForTenant() {
            Page<MovementDiscrepancy> page = new PageImpl<>(
                    Arrays.asList(discrepancy1, discrepancy2), pageable, 2);
            when(discrepancyRepository.findByTenantCode(eq(tenantCode), any(Pageable.class)))
                    .thenReturn(page);

            Page<MovementDiscrepancyDTO> result = service.getDiscrepancies(
                    tenantCode, null, null, null, null, pageable);

            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).hasSize(2);
            verify(discrepancyRepository).findByTenantCode(tenantCode, pageable);
        }

        @Test
        @DisplayName("filters by discrepancy type when provided")
        void filtersByType() {
            Page<MovementDiscrepancy> page = new PageImpl<>(
                    Collections.singletonList(discrepancy1), pageable, 1);
            when(discrepancyRepository.findByTenantCodeAndDiscrepancyType(
                    eq(tenantCode), eq(DiscrepancyType.LOCATION_MISMATCH), any(Pageable.class)))
                    .thenReturn(page);

            Page<MovementDiscrepancyDTO> result = service.getDiscrepancies(
                    tenantCode, null, null, "LOCATION_MISMATCH", null, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getDiscrepancyType())
                    .isEqualTo("LOCATION_MISMATCH");
            verify(discrepancyRepository).findByTenantCodeAndDiscrepancyType(
                    tenantCode, DiscrepancyType.LOCATION_MISMATCH, pageable);
        }

        @Test
        @DisplayName("filters by acknowledged status when provided")
        void filtersByAcknowledged() {
            Page<MovementDiscrepancy> page = new PageImpl<>(
                    Collections.singletonList(discrepancy1), pageable, 1);
            when(discrepancyRepository.findByTenantCodeAndAcknowledged(eq(tenantCode), eq(false), any(Pageable.class)))
                    .thenReturn(page);

            Page<MovementDiscrepancyDTO> result = service.getDiscrepancies(
                    tenantCode, null, null, null, false, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getAcknowledged()).isFalse();
        }

        @Test
        @DisplayName("filters by date range when provided")
        void filtersByDateRange() {
            ZonedDateTime startDate = ZonedDateTime.now().minusDays(1);
            ZonedDateTime endDate = ZonedDateTime.now();
            
            Page<MovementDiscrepancy> page = new PageImpl<>(
                    Arrays.asList(discrepancy1, discrepancy2), pageable, 2);
            when(discrepancyRepository.findByTenantCodeAndTimestampBetween(
                    eq(tenantCode), any(ZonedDateTime.class), any(ZonedDateTime.class), any(Pageable.class)))
                    .thenReturn(page);

            Page<MovementDiscrepancyDTO> result = service.getDiscrepancies(
                    tenantCode, startDate, endDate, null, null, pageable);

            assertThat(result.getTotalElements()).isEqualTo(2);
            verify(discrepancyRepository).findByTenantCodeAndTimestampBetween(
                    tenantCode, startDate, endDate, pageable);
        }

        @Test
        @DisplayName("returns empty page for tenant with no discrepancies")
        void returnsEmptyForNoDiscrepancies() {
            when(discrepancyRepository.findByTenantCode(eq("LIRN"), any(Pageable.class)))
                    .thenReturn(Page.empty());

            Page<MovementDiscrepancyDTO> result = service.getDiscrepancies(
                    "LIRN", null, null, null, null, pageable);

            assertThat(result.getTotalElements()).isZero();
            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getDiscrepancyById")
    class GetDiscrepancyById {

        @Test
        @DisplayName("returns discrepancy when found")
        void returnsDiscrepancyWhenFound() {
            when(discrepancyRepository.findByIdAndTenantCode(discrepancyId, tenantCode))
                    .thenReturn(Optional.of(discrepancy1));

            MovementDiscrepancyDTO result = service.getDiscrepancyById(discrepancyId, tenantCode);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(discrepancyId);
            assertThat(result.getDiscrepancyId()).isEqualTo("DISC-001");
            assertThat(result.getAssetName()).isEqualTo("Baggage Cart 1");
        }

        @Test
        @DisplayName("throws exception when not found")
        void throwsExceptionWhenNotFound() {
            UUID unknownId = UUID.randomUUID();
            when(discrepancyRepository.findByIdAndTenantCode(unknownId, tenantCode))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getDiscrepancyById(unknownId, tenantCode))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Discrepancy not found");
        }
    }

    @Nested
    @DisplayName("acknowledgeDiscrepancy")
    class AcknowledgeDiscrepancy {

        @Test
        @DisplayName("acknowledges discrepancy successfully with UUID userId")
        void acknowledgesDiscrepancySuccessfully() {
            UUID userId = UUID.randomUUID();
            when(discrepancyRepository.findByIdAndTenantCode(discrepancyId, tenantCode))
                    .thenReturn(Optional.of(discrepancy1));
            when(discrepancyRepository.save(any(MovementDiscrepancy.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            MovementDiscrepancyDTO result = service.acknowledgeDiscrepancy(
                    discrepancyId, tenantCode, userId.toString(), "Investigated and cleared");

            assertThat(result.getAcknowledged()).isTrue();
            assertThat(result.getResolutionNotes()).isEqualTo("Investigated and cleared");
            assertThat(result.getAcknowledgedAt()).isNotNull();
            
            verify(discrepancyRepository).save(any(MovementDiscrepancy.class));
        }

        @Test
        @DisplayName("handles non-UUID userId gracefully")
        void handlesNonUuidUserId() {
            when(discrepancyRepository.findByIdAndTenantCode(discrepancyId, tenantCode))
                    .thenReturn(Optional.of(discrepancy1));
            when(discrepancyRepository.save(any(MovementDiscrepancy.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Should not throw even with invalid UUID
            MovementDiscrepancyDTO result = service.acknowledgeDiscrepancy(
                    discrepancyId, tenantCode, "invalid-uuid-format", "notes");

            assertThat(result.getAcknowledged()).isTrue();
            verify(discrepancyRepository).save(any(MovementDiscrepancy.class));
        }

        @Test
        @DisplayName("sets acknowledgedAt timestamp")
        void setsAcknowledgedAtTimestamp() {
            UUID userId = UUID.randomUUID();
            when(discrepancyRepository.findByIdAndTenantCode(discrepancyId, tenantCode))
                    .thenReturn(Optional.of(discrepancy1));
            when(discrepancyRepository.save(any(MovementDiscrepancy.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            ZonedDateTime beforeAck = ZonedDateTime.now();
            service.acknowledgeDiscrepancy(discrepancyId, tenantCode, userId.toString(), "notes");
            ZonedDateTime afterAck = ZonedDateTime.now();

            ArgumentCaptor<MovementDiscrepancy> captor = ArgumentCaptor.forClass(MovementDiscrepancy.class);
            verify(discrepancyRepository).save(captor.capture());
            
            MovementDiscrepancy saved = captor.getValue();
            assertThat(saved.getAcknowledgedAt())
                    .isAfterOrEqualTo(beforeAck)
                    .isBeforeOrEqualTo(afterAck);
        }

        @Test
        @DisplayName("throws exception when discrepancy not found")
        void throwsExceptionWhenNotFound() {
            UUID unknownId = UUID.randomUUID();
            when(discrepancyRepository.findByIdAndTenantCode(unknownId, tenantCode))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> 
                    service.acknowledgeDiscrepancy(unknownId, tenantCode, "user", "notes"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Discrepancy not found");
        }
    }

    @Nested
    @DisplayName("getAllDiscrepanciesForExport")
    class GetAllDiscrepanciesForExport {

        @Test
        @DisplayName("returns all discrepancies for export without date filter")
        void returnsAllWithoutDateFilter() {
            when(discrepancyRepository.findByTenantCodeOrderByTimestampDesc(tenantCode))
                    .thenReturn(Arrays.asList(discrepancy1, discrepancy2));

            List<MovementDiscrepancyDTO> result = service.getAllDiscrepanciesForExport(
                    tenantCode, null, null, null, null);

            assertThat(result).hasSize(2);
            verify(discrepancyRepository).findByTenantCodeOrderByTimestampDesc(tenantCode);
        }

        @Test
        @DisplayName("filters by date range for export")
        void filtersByDateRangeForExport() {
            ZonedDateTime startDate = ZonedDateTime.now().minusDays(1);
            ZonedDateTime endDate = ZonedDateTime.now();
            
            when(discrepancyRepository.findByTenantCodeAndTimestampBetweenOrderByTimestampDesc(
                    eq(tenantCode), any(ZonedDateTime.class), any(ZonedDateTime.class)))
                    .thenReturn(Arrays.asList(discrepancy1, discrepancy2));

            List<MovementDiscrepancyDTO> result = service.getAllDiscrepanciesForExport(
                    tenantCode, startDate, endDate, null, null);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("filters by discrepancy type for export")
        void filtersByTypeForExport() {
            when(discrepancyRepository.findByTenantCodeOrderByTimestampDesc(tenantCode))
                    .thenReturn(Arrays.asList(discrepancy1, discrepancy2));

            List<MovementDiscrepancyDTO> result = service.getAllDiscrepanciesForExport(
                    tenantCode, null, null, "LOCATION_MISMATCH", null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDiscrepancyType()).isEqualTo("LOCATION_MISMATCH");
        }

        @Test
        @DisplayName("filters by acknowledged status for export")
        void filtersByAcknowledgedForExport() {
            when(discrepancyRepository.findByTenantCodeOrderByTimestampDesc(tenantCode))
                    .thenReturn(Arrays.asList(discrepancy1, discrepancy2));

            List<MovementDiscrepancyDTO> result = service.getAllDiscrepanciesForExport(
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
            when(discrepancyRepository.findByIdAndTenantCode(discrepancyId, tenantCode))
                    .thenReturn(Optional.of(discrepancy1));

            MovementDiscrepancyDTO dto = service.getDiscrepancyById(discrepancyId, tenantCode);

            assertThat(dto.getId()).isEqualTo(discrepancy1.getId());
            assertThat(dto.getDiscrepancyId()).isEqualTo(discrepancy1.getDiscrepancyId());
            assertThat(dto.getAssetId()).isEqualTo(discrepancy1.getAssetId());
            assertThat(dto.getAssetIdentifier()).isEqualTo(discrepancy1.getAssetIdentifier());
            assertThat(dto.getAssetName()).isEqualTo(discrepancy1.getAssetName());
            assertThat(dto.getAssetCategory()).isEqualTo(discrepancy1.getAssetCategory());
            assertThat(dto.getDiscrepancyType()).isEqualTo(discrepancy1.getDiscrepancyType().name());
            assertThat(dto.getSeverity()).isEqualTo(discrepancy1.getSeverity().name());
            assertThat(dto.getAcknowledged()).isEqualTo(discrepancy1.getAcknowledged());
            assertThat(dto.getTenantCode()).isEqualTo(discrepancy1.getTenantCode());
        }

        @Test
        @DisplayName("maps location deviation data")
        void mapsLocationDeviationData() {
            when(discrepancyRepository.findByIdAndTenantCode(discrepancyId, tenantCode))
                    .thenReturn(Optional.of(discrepancy1));

            MovementDiscrepancyDTO dto = service.getDiscrepancyById(discrepancyId, tenantCode);

            assertThat(dto.getExpectedLocation()).isEqualTo("Terminal 2 Gate A");
            assertThat(dto.getActualLocation()).isEqualTo("Runway Holding Point");
            assertThat(dto.getDeviationMeters()).isEqualTo(500.0);
        }
    }
}
