package com.utam.tracking.repository;

import com.utam.tracking.domain.RestrictedZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for RestrictedZoneRepository.
 * Tests spatial query contracts through mocking.
 * 
 * Feature: 005-asset-tracking-security
 * Task: T123 - Repository unit tests
 */
@ExtendWith(MockitoExtension.class)
class RestrictedZoneRepositoryTest {

    @Mock
    private RestrictedZoneRepository repository;

    private RestrictedZone runwayZone;
    private RestrictedZone taxiwayZone;
    private RestrictedZone fuelDepotZone;
    private final String tenantCode = "VIDP";
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @BeforeEach
    void setUp() {
        runwayZone = RestrictedZone.builder()
                .id(UUID.randomUUID())
                .zoneId("RZ-VIDP-RWY09R27L")
                .zoneName("Runway 09R/27L")
                .zoneType("RUNWAY")
                .tenantCode(tenantCode)
                .description("Main runway restricted zone")
                .isActive(true)
                .build();
        
        taxiwayZone = RestrictedZone.builder()
                .id(UUID.randomUUID())
                .zoneId("RZ-VIDP-TWY-A")
                .zoneName("Taxiway Alpha")
                .zoneType("TAXIWAY")
                .tenantCode(tenantCode)
                .description("Taxiway Alpha crossing zone")
                .isActive(true)
                .build();
        
        fuelDepotZone = RestrictedZone.builder()
                .id(UUID.randomUUID())
                .zoneId("RZ-VIDP-FUEL")
                .zoneName("Fuel Depot Area")
                .zoneType("FUEL_STORAGE")
                .tenantCode(tenantCode)
                .description("Fuel storage facility")
                .isActive(false) // Inactive zone
                .build();
    }

    @Test
    @DisplayName("findByTenantCode returns all zones for tenant")
    void findByTenantCode_ReturnsAllZones() {
        when(repository.findByTenantCode(tenantCode))
                .thenReturn(Arrays.asList(runwayZone, taxiwayZone, fuelDepotZone));
        
        List<RestrictedZone> result = repository.findByTenantCode(tenantCode);
        
        assertThat(result).hasSize(3);
        assertThat(result).allMatch(z -> z.getTenantCode().equals(tenantCode));
    }

    @Test
    @DisplayName("findByTenantCodeAndIsActive returns only active zones")
    void findByTenantCodeAndIsActive_ReturnsActiveOnly() {
        when(repository.findByTenantCodeAndIsActive(tenantCode, true))
                .thenReturn(Arrays.asList(runwayZone, taxiwayZone));
        
        List<RestrictedZone> result = repository.findByTenantCodeAndIsActive(tenantCode, true);
        
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(z -> z.getIsActive());
        assertThat(result).noneMatch(z -> z.getZoneId().equals("RZ-VIDP-FUEL"));
    }

    @Test
    @DisplayName("findByTenantCodeAndIsActive returns only inactive zones")
    void findByTenantCodeAndIsActive_ReturnsInactiveOnly() {
        when(repository.findByTenantCodeAndIsActive(tenantCode, false))
                .thenReturn(Collections.singletonList(fuelDepotZone));
        
        List<RestrictedZone> result = repository.findByTenantCodeAndIsActive(tenantCode, false);
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIsActive()).isFalse();
        assertThat(result.get(0).getZoneId()).isEqualTo("RZ-VIDP-FUEL");
    }

    @Test
    @DisplayName("findByZoneId returns zone by ID")
    void findByZoneId_ReturnsZone() {
        when(repository.findByZoneId("RZ-VIDP-RWY09R27L"))
                .thenReturn(Optional.of(runwayZone));
        
        Optional<RestrictedZone> result = repository.findByZoneId("RZ-VIDP-RWY09R27L");
        
        assertThat(result).isPresent();
        assertThat(result.get().getZoneName()).isEqualTo("Runway 09R/27L");
        assertThat(result.get().getZoneType()).isEqualTo("RUNWAY");
    }

    @Test
    @DisplayName("findByZoneId returns empty for non-existent zone")
    void findByZoneId_ReturnsEmptyForNonExistent() {
        when(repository.findByZoneId("UNKNOWN-ZONE"))
                .thenReturn(Optional.empty());
        
        Optional<RestrictedZone> result = repository.findByZoneId("UNKNOWN-ZONE");
        
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findZonesContainingPoint returns zones containing a point")
    void findZonesContainingPoint_ReturnsContainingZones() {
        Point assetLocation = geometryFactory.createPoint(
                new Coordinate(77.1025, 28.5523)); // Near Delhi airport
        
        when(repository.findZonesContainingPoint(any(Point.class), eq(tenantCode)))
                .thenReturn(Collections.singletonList(runwayZone));
        
        List<RestrictedZone> result = repository.findZonesContainingPoint(assetLocation, tenantCode);
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getZoneId()).isEqualTo("RZ-VIDP-RWY09R27L");
    }

    @Test
    @DisplayName("findZonesContainingPoint returns empty when point is outside all zones")
    void findZonesContainingPoint_ReturnsEmptyWhenOutside() {
        Point outsideLocation = geometryFactory.createPoint(
                new Coordinate(0.0, 0.0)); // Middle of Atlantic
        
        when(repository.findZonesContainingPoint(any(Point.class), eq(tenantCode)))
                .thenReturn(Collections.emptyList());
        
        List<RestrictedZone> result = repository.findZonesContainingPoint(outsideLocation, tenantCode);
        
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findZonesContainingPoint can return multiple overlapping zones")
    void findZonesContainingPoint_ReturnsMultipleOverlappingZones() {
        Point intersectionPoint = geometryFactory.createPoint(
                new Coordinate(77.1030, 28.5530));
        
        when(repository.findZonesContainingPoint(any(Point.class), eq(tenantCode)))
                .thenReturn(Arrays.asList(runwayZone, taxiwayZone));
        
        List<RestrictedZone> result = repository.findZonesContainingPoint(intersectionPoint, tenantCode);
        
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("findZonesWithinDistance returns zones within specified distance")
    void findZonesWithinDistance_ReturnsNearbyZones() {
        Point nearbyLocation = geometryFactory.createPoint(
                new Coordinate(77.1020, 28.5520));
        double distanceMeters = 500.0;
        
        when(repository.findZonesWithinDistance(any(Point.class), eq(distanceMeters), eq(tenantCode)))
                .thenReturn(Arrays.asList(runwayZone, taxiwayZone));
        
        List<RestrictedZone> result = repository.findZonesWithinDistance(
                nearbyLocation, distanceMeters, tenantCode);
        
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("findZonesWithinDistance returns empty when no zones nearby")
    void findZonesWithinDistance_ReturnsEmptyWhenNoNearbyZones() {
        Point farLocation = geometryFactory.createPoint(
                new Coordinate(0.0, 0.0));
        double distanceMeters = 100.0;
        
        when(repository.findZonesWithinDistance(any(Point.class), eq(distanceMeters), eq(tenantCode)))
                .thenReturn(Collections.emptyList());
        
        List<RestrictedZone> result = repository.findZonesWithinDistance(
                farLocation, distanceMeters, tenantCode);
        
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Empty result for non-existent tenant")
    void findByTenantCode_ReturnsEmptyForNonExistentTenant() {
        when(repository.findByTenantCode("UNKNOWN"))
                .thenReturn(Collections.emptyList());
        
        List<RestrictedZone> result = repository.findByTenantCode("UNKNOWN");
        
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Zone has correct description")
    void zone_HasCorrectDescription() {
        when(repository.findByZoneId("RZ-VIDP-TWY-A"))
                .thenReturn(Optional.of(taxiwayZone));
        
        Optional<RestrictedZone> result = repository.findByZoneId("RZ-VIDP-TWY-A");
        
        assertThat(result).isPresent();
        assertThat(result.get().getDescription()).isEqualTo("Taxiway Alpha crossing zone");
    }

    @Test
    @DisplayName("Different zone types are categorized correctly")
    void zones_HaveCorrectZoneTypes() {
        when(repository.findByTenantCodeAndIsActive(tenantCode, true))
                .thenReturn(Arrays.asList(runwayZone, taxiwayZone));
        
        List<RestrictedZone> result = repository.findByTenantCodeAndIsActive(tenantCode, true);
        
        assertThat(result).extracting("zoneType")
                .containsExactlyInAnyOrder("RUNWAY", "TAXIWAY");
    }
}
