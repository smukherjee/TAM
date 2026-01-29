package com.utam.tracking.repository;

import com.utam.tracking.domain.RestrictedZone;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for RestrictedZone entities.
 * Provides spatial queries for zone containment checks.
 * 
 * Feature: 005-asset-tracking-security
 */
@Repository
public interface RestrictedZoneRepository extends JpaRepository<RestrictedZone, UUID> {

    /**
     * Find all zones for a specific tenant
     */
    List<RestrictedZone> findByTenantCode(String tenantCode);

    /**
     * Find active zones for a specific tenant
     */
    List<RestrictedZone> findByTenantCodeAndIsActive(String tenantCode, Boolean isActive);

    /**
     * Find zone by zone_id
     */
    Optional<RestrictedZone> findByZoneId(String zoneId);

    /**
     * Find all zones that contain a given point using PostGIS ST_Contains
     * 
     * @param location Point to check
     * @param tenantCode Tenant filter
     * @return List of zones containing the point
     */
    @Query(value = "SELECT z.* FROM restricted_zones z " +
            "WHERE z.tenant_code = :tenantCode " +
            "AND z.is_active = true " +
            "AND ST_Contains(z.boundary, :location)",
            nativeQuery = true)
    List<RestrictedZone> findZonesContainingPoint(@Param("location") Point location,
                                                   @Param("tenantCode") String tenantCode);

    /**
     * Find zones within a certain distance from a point
     * 
     * @param location Point to check
     * @param distanceMeters Distance in meters
     * @param tenantCode Tenant filter
     * @return List of nearby zones
     */
    @Query(value = "SELECT z.* FROM restricted_zones z " +
            "WHERE z.tenant_code = :tenantCode " +
            "AND z.is_active = true " +
            "AND ST_DWithin(z.boundary::geography, :location::geography, :distanceMeters)",
            nativeQuery = true)
    List<RestrictedZone> findZonesWithinDistance(@Param("location") Point location,
                                                   @Param("distanceMeters") double distanceMeters,
                                                   @Param("tenantCode") String tenantCode);
}
