package com.utam.tracking.repository;

import com.utam.tracking.domain.AssetMovementTrail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for AssetMovementTrail entities.
 * Provides time-series queries for movement history.
 * 
 * Feature: 005-asset-tracking-security
 */
@Repository
public interface AssetMovementTrailRepository extends JpaRepository<AssetMovementTrail, Long> {

    /**
     * Find movement trail for a specific asset within a time range
     */
    List<AssetMovementTrail> findByAssetIdAndTimestampBetweenOrderByTimestampAsc(
            UUID assetId,
            ZonedDateTime startTime,
            ZonedDateTime endTime
    );

    /**
     * Find movement trail by asset identifier within a time range
     */
    List<AssetMovementTrail> findByAssetIdentifierAndTimestampBetweenOrderByTimestampAsc(
            String assetIdentifier,
            ZonedDateTime startTime,
            ZonedDateTime endTime
    );

    /**
     * Find recent movements for a tenant after a specific time
     */
    Page<AssetMovementTrail> findByTenantCodeAndTimestampAfterOrderByTimestampDesc(
            String tenantCode,
            ZonedDateTime after,
            Pageable pageable
    );

    /**
     * Find movements in a specific zone
     */
    @Query("SELECT amt FROM AssetMovementTrail amt " +
           "WHERE amt.restrictedZoneId = :zoneId " +
           "AND amt.timestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY amt.timestamp DESC")
    List<AssetMovementTrail> findByZoneAndTimeRange(
            @Param("zoneId") UUID zoneId,
            @Param("startTime") ZonedDateTime startTime,
            @Param("endTime") ZonedDateTime endTime
    );

    /**
     * Count movements for an asset in a time range
     */
    long countByAssetIdAndTimestampBetween(UUID assetId, ZonedDateTime startTime, ZonedDateTime endTime);
}
