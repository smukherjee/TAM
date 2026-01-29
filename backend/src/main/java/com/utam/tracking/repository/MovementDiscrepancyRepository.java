package com.utam.tracking.repository;

import com.utam.tracking.domain.MovementDiscrepancy;
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
 * Repository for MovementDiscrepancy entities.
 * Provides filtering by discrepancy type and severity.
 * 
 * Feature: 005-asset-tracking-security
 */
@Repository
public interface MovementDiscrepancyRepository extends JpaRepository<MovementDiscrepancy, UUID> {

    /**
     * Find discrepancies for a tenant with pagination
     */
    Page<MovementDiscrepancy> findByTenantCode(String tenantCode, Pageable pageable);

    /**
     * Find discrepancies within a time range for a tenant
     */
    Page<MovementDiscrepancy> findByTenantCodeAndTimestampBetween(
            String tenantCode,
            ZonedDateTime startTime,
            ZonedDateTime endTime,
            Pageable pageable
    );

    /**
     * Find discrepancies within a time range for a tenant (ordered)
     */
    Page<MovementDiscrepancy> findByTenantCodeAndTimestampBetweenOrderByTimestampDesc(
            String tenantCode,
            ZonedDateTime startTime,
            ZonedDateTime endTime,
            Pageable pageable
    );

    /**
     * Find discrepancies by type
     */
    Page<MovementDiscrepancy> findByDiscrepancyType(
            MovementDiscrepancy.DiscrepancyType type,
            Pageable pageable
    );

    /**
     * Find discrepancies by type (ordered)
     */
    Page<MovementDiscrepancy> findByDiscrepancyTypeOrderByTimestampDesc(
            MovementDiscrepancy.DiscrepancyType type,
            Pageable pageable
    );

    /**
     * Find unacknowledged discrepancies
     */
    Page<MovementDiscrepancy> findByAcknowledged(Boolean acknowledged, Pageable pageable);

    /**
     * Find unacknowledged discrepancies (ordered)
     */
    Page<MovementDiscrepancy> findByAcknowledgedOrderByTimestampDesc(Boolean acknowledged, Pageable pageable);

    /**
     * Find discrepancies by severity for a tenant
     */
    List<MovementDiscrepancy> findByTenantCodeAndSeverityOrderByTimestampDesc(
            String tenantCode,
            MovementDiscrepancy.DiscrepancySeverity severity
    );

    /**
     * Count discrepancies by tenant and type
     */
    long countByTenantCodeAndDiscrepancyType(
            String tenantCode,
            MovementDiscrepancy.DiscrepancyType type
    );

    /**
     * Count unacknowledged discrepancies for a tenant
     */
    long countByTenantCodeAndAcknowledged(String tenantCode, Boolean acknowledged);

    /**
     * Find discrepancies for a specific asset
     */
    Page<MovementDiscrepancy> findByAssetIdAndTimestampBetweenOrderByTimestampDesc(
            UUID assetId,
            ZonedDateTime startTime,
            ZonedDateTime endTime,
            Pageable pageable
    );

    /**
     * Get discrepancy statistics by type
     */
    @Query("SELECT d.discrepancyType as type, COUNT(d) as count " +
           "FROM MovementDiscrepancy d " +
           "WHERE d.tenantCode = :tenantCode " +
           "AND d.timestamp BETWEEN :startTime AND :endTime " +
           "GROUP BY d.discrepancyType")
    List<Object[]> getDiscrepancyStatsByType(
            @Param("tenantCode") String tenantCode,
            @Param("startTime") ZonedDateTime startTime,
            @Param("endTime") ZonedDateTime endTime
    );
}
