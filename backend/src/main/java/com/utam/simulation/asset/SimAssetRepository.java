package com.utam.simulation.asset;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for simulation Asset entity.
 */
@Repository
public interface SimAssetRepository extends JpaRepository<Asset, UUID> {

    List<Asset> findByTenantCode(String tenantCode);

    List<Asset> findByTenantCodeAndStatus(String tenantCode, String status);

    List<Asset> findByTenantCodeAndAssetType(String tenantCode, String assetType);

    Optional<Asset> findByAssetId(String assetId);

    @Query("SELECT a FROM SimulationAsset a WHERE a.tenantCode = :tenantCode AND a.lastSeen > :since")
    List<Asset> findActiveAssets(String tenantCode, Instant since);

    @Query("SELECT COUNT(a) FROM SimulationAsset a WHERE a.tenantCode = :tenantCode")
    long countByTenantCode(String tenantCode);

    void deleteByTenantCode(String tenantCode);
}
