package com.utam.tracking.repository;

import com.utam.tracking.domain.AssetLocationRegister;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for AssetLocationRegister entities.
 * Provides current state queries for asset locations.
 * 
 * Feature: 005-asset-tracking-security
 */
@Repository
public interface AssetLocationRegisterRepository extends JpaRepository<AssetLocationRegister, UUID> {

    /**
     * Find location by asset ID
     */
    Optional<AssetLocationRegister> findByAssetId(UUID assetId);

    /**
     * Find location by asset identifier
     */
    Optional<AssetLocationRegister> findByAssetIdentifier(String assetIdentifier);

    /**
     * Find all assets in restricted zones for a tenant
     */
    List<AssetLocationRegister> findByTenantCodeAndIsInRestrictedZone(
            String tenantCode,
            Boolean isInRestrictedZone
    );

    /**
     * Find all unauthorized assets in restricted zones
     */
    @Query("SELECT alr FROM AssetLocationRegister alr " +
           "WHERE alr.tenantCode = :tenantCode " +
           "AND alr.isInRestrictedZone = true " +
           "AND alr.isAuthorized = false")
    List<AssetLocationRegister> findUnauthorizedAssetsInZones(@Param("tenantCode") String tenantCode);

    /**
     * Find all assets in a specific zone
     */
    List<AssetLocationRegister> findByRestrictedZoneId(UUID zoneId);

    /**
     * Find all assets for a tenant
     */
    List<AssetLocationRegister> findByTenantCode(String tenantCode);

    /**
     * Count assets in restricted zones for a tenant
     */
    long countByTenantCodeAndIsInRestrictedZone(String tenantCode, Boolean isInRestrictedZone);
}
