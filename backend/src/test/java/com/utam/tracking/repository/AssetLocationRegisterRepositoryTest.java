package com.utam.tracking.repository;

import com.utam.tracking.domain.AssetLocationRegister;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AssetLocationRegisterRepository.
 * Tests current state query contracts through mocking.
 * 
 * Feature: 005-asset-tracking-security
 * Task: T123 - Repository unit tests
 */
@ExtendWith(MockitoExtension.class)
class AssetLocationRegisterRepositoryTest {

    @Mock
    private AssetLocationRegisterRepository repository;

    private AssetLocationRegister authorizedInZone;
    private AssetLocationRegister unauthorizedInZone;
    private AssetLocationRegister outsideZone;
    private final String tenantCode = "VIDP";
    private final UUID restrictedZoneId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ZonedDateTime now = ZonedDateTime.now();
        
        authorizedInZone = AssetLocationRegister.builder()
                .assetId(UUID.randomUUID())
                .assetIdentifier("VEH-001")
                .tenantCode(tenantCode)
                .isInRestrictedZone(true)
                .isAuthorized(true)
                .restrictedZoneId(restrictedZoneId)
                .currentZoneName("Runway 09R/27L")
                .speed(10.0)
                .heading(90.0)
                .status("MOVING")
                .lastUpdated(now)
                .build();
        
        unauthorizedInZone = AssetLocationRegister.builder()
                .assetId(UUID.randomUUID())
                .assetIdentifier("VEH-002")
                .tenantCode(tenantCode)
                .isInRestrictedZone(true)
                .isAuthorized(false) // Unauthorized!
                .restrictedZoneId(restrictedZoneId)
                .currentZoneName("Runway 09R/27L")
                .speed(15.0)
                .heading(92.0)
                .status("MOVING")
                .lastUpdated(now.minusMinutes(2))
                .build();
        
        outsideZone = AssetLocationRegister.builder()
                .assetId(UUID.randomUUID())
                .assetIdentifier("VEH-003")
                .tenantCode(tenantCode)
                .isInRestrictedZone(false)
                .isAuthorized(true)
                .restrictedZoneId(null)
                .currentZoneName(null)
                .speed(5.0)
                .heading(180.0)
                .status("STATIONARY")
                .lastUpdated(now.minusMinutes(5))
                .build();
    }

    @Test
    @DisplayName("findByAssetId returns location for asset")
    void findByAssetId_ReturnsLocation() {
        UUID assetId = authorizedInZone.getAssetId();
        when(repository.findByAssetId(assetId))
                .thenReturn(Optional.of(authorizedInZone));
        
        Optional<AssetLocationRegister> result = repository.findByAssetId(assetId);
        
        assertThat(result).isPresent();
        assertThat(result.get().getAssetIdentifier()).isEqualTo("VEH-001");
    }

    @Test
    @DisplayName("findByAssetIdentifier returns location by identifier")
    void findByAssetIdentifier_ReturnsLocation() {
        when(repository.findByAssetIdentifier("VEH-002"))
                .thenReturn(Optional.of(unauthorizedInZone));
        
        Optional<AssetLocationRegister> result = repository.findByAssetIdentifier("VEH-002");
        
        assertThat(result).isPresent();
        assertThat(result.get().getAssetIdentifier()).isEqualTo("VEH-002");
        assertThat(result.get().getIsAuthorized()).isFalse();
    }

    @Test
    @DisplayName("findByAssetIdentifier returns empty for non-existent asset")
    void findByAssetIdentifier_ReturnsEmptyForNonExistent() {
        when(repository.findByAssetIdentifier("UNKNOWN"))
                .thenReturn(Optional.empty());
        
        Optional<AssetLocationRegister> result = repository.findByAssetIdentifier("UNKNOWN");
        
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByTenantCodeAndIsInRestrictedZone returns assets in restricted zones")
    void findByTenantCodeAndIsInRestrictedZone_ReturnsAssetsInZones() {
        when(repository.findByTenantCodeAndIsInRestrictedZone(tenantCode, true))
                .thenReturn(Arrays.asList(authorizedInZone, unauthorizedInZone));
        
        List<AssetLocationRegister> result = repository.findByTenantCodeAndIsInRestrictedZone(
                tenantCode, true);
        
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(a -> a.getIsInRestrictedZone());
    }

    @Test
    @DisplayName("findByTenantCodeAndIsInRestrictedZone returns assets outside zones")
    void findByTenantCodeAndIsInRestrictedZone_ReturnsAssetsOutsideZones() {
        when(repository.findByTenantCodeAndIsInRestrictedZone(tenantCode, false))
                .thenReturn(Collections.singletonList(outsideZone));
        
        List<AssetLocationRegister> result = repository.findByTenantCodeAndIsInRestrictedZone(
                tenantCode, false);
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIsInRestrictedZone()).isFalse();
        assertThat(result.get(0).getRestrictedZoneId()).isNull();
    }

    @Test
    @DisplayName("findUnauthorizedAssetsInZones returns only unauthorized assets")
    void findUnauthorizedAssetsInZones_ReturnsUnauthorizedOnly() {
        when(repository.findUnauthorizedAssetsInZones(tenantCode))
                .thenReturn(Collections.singletonList(unauthorizedInZone));
        
        List<AssetLocationRegister> result = repository.findUnauthorizedAssetsInZones(tenantCode);
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIsInRestrictedZone()).isTrue();
        assertThat(result.get(0).getIsAuthorized()).isFalse();
        assertThat(result.get(0).getAssetIdentifier()).isEqualTo("VEH-002");
    }

    @Test
    @DisplayName("findUnauthorizedAssetsInZones returns empty when all authorized")
    void findUnauthorizedAssetsInZones_ReturnsEmptyWhenAllAuthorized() {
        when(repository.findUnauthorizedAssetsInZones("LIRN"))
                .thenReturn(Collections.emptyList());
        
        List<AssetLocationRegister> result = repository.findUnauthorizedAssetsInZones("LIRN");
        
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByRestrictedZoneId returns all assets in specific zone")
    void findByRestrictedZoneId_ReturnsAssetsInZone() {
        when(repository.findByRestrictedZoneId(restrictedZoneId))
                .thenReturn(Arrays.asList(authorizedInZone, unauthorizedInZone));
        
        List<AssetLocationRegister> result = repository.findByRestrictedZoneId(restrictedZoneId);
        
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(a -> restrictedZoneId.equals(a.getRestrictedZoneId()));
    }

    @Test
    @DisplayName("findByRestrictedZoneId returns empty for zone with no assets")
    void findByRestrictedZoneId_ReturnsEmptyForEmptyZone() {
        UUID emptyZoneId = UUID.randomUUID();
        when(repository.findByRestrictedZoneId(emptyZoneId))
                .thenReturn(Collections.emptyList());
        
        List<AssetLocationRegister> result = repository.findByRestrictedZoneId(emptyZoneId);
        
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByTenantCode returns all assets for tenant")
    void findByTenantCode_ReturnsAllAssets() {
        when(repository.findByTenantCode(tenantCode))
                .thenReturn(Arrays.asList(authorizedInZone, unauthorizedInZone, outsideZone));
        
        List<AssetLocationRegister> result = repository.findByTenantCode(tenantCode);
        
        assertThat(result).hasSize(3);
        assertThat(result).allMatch(a -> a.getTenantCode().equals(tenantCode));
    }

    @Test
    @DisplayName("countByTenantCodeAndIsInRestrictedZone returns correct count")
    void countByTenantCodeAndIsInRestrictedZone_ReturnsCount() {
        when(repository.countByTenantCodeAndIsInRestrictedZone(tenantCode, true))
                .thenReturn(2L);
        
        long count = repository.countByTenantCodeAndIsInRestrictedZone(tenantCode, true);
        
        assertThat(count).isEqualTo(2L);
    }

    @Test
    @DisplayName("countByTenantCodeAndIsInRestrictedZone returns zero for no assets")
    void countByTenantCodeAndIsInRestrictedZone_ReturnsZeroForNoAssets() {
        when(repository.countByTenantCodeAndIsInRestrictedZone("UNKNOWN", true))
                .thenReturn(0L);
        
        long count = repository.countByTenantCodeAndIsInRestrictedZone("UNKNOWN", true);
        
        assertThat(count).isZero();
    }

    @Test
    @DisplayName("Asset location includes speed and heading data")
    void assetLocation_IncludesSpeedAndHeading() {
        when(repository.findByAssetIdentifier("VEH-001"))
                .thenReturn(Optional.of(authorizedInZone));
        
        Optional<AssetLocationRegister> result = repository.findByAssetIdentifier("VEH-001");
        
        assertThat(result).isPresent();
        assertThat(result.get().getSpeed()).isNotNull();
        assertThat(result.get().getHeading()).isNotNull();
        assertThat(result.get().getSpeed()).isGreaterThanOrEqualTo(0.0);
        assertThat(result.get().getHeading()).isBetween(0.0, 360.0);
    }

    @Test
    @DisplayName("Asset location tracks last update time")
    void assetLocation_TracksLastUpdate() {
        when(repository.findByTenantCode(tenantCode))
                .thenReturn(Arrays.asList(authorizedInZone, unauthorizedInZone, outsideZone));
        
        List<AssetLocationRegister> result = repository.findByTenantCode(tenantCode);
        
        assertThat(result).allMatch(a -> a.getLastUpdated() != null);
    }
}
