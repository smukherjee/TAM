package com.utam.simulation.asset;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for AssetPosition entity (time-series data).
 */
@Repository
public interface AssetPositionRepository extends JpaRepository<AssetPosition, UUID> {

    List<AssetPosition> findByAssetIdOrderByTimestampDesc(UUID assetId);

    @Query("SELECT p FROM AssetPosition p WHERE p.assetId = :assetId AND p.timestamp > :since ORDER BY p.timestamp DESC")
    List<AssetPosition> findRecentPositions(UUID assetId, Instant since);

    @Query("SELECT p FROM AssetPosition p WHERE p.tenantCode = :tenantCode AND p.timestamp > :since ORDER BY p.timestamp DESC")
    List<AssetPosition> findByTenantCodeAndTimestampAfter(String tenantCode, Instant since);

    @Query("SELECT COUNT(p) FROM AssetPosition p WHERE p.tenantCode = :tenantCode")
    long countByTenantCode(String tenantCode);

    @Modifying
    @Query("DELETE FROM AssetPosition p WHERE p.timestamp < :before")
    int deleteOlderThan(Instant before);

    @Modifying
    @Query("DELETE FROM AssetPosition p WHERE p.tenantCode = :tenantCode")
    void deleteByTenantCode(String tenantCode);
}
