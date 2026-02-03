package com.utam.tracking.repository;

import com.utam.tracking.domain.ZoneViolation;
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
 * Repository for ZoneViolation entities.
 * Provides filtering and aggregation queries for violation tracking.
 * 
 * Feature: 005-asset-tracking-security
 */
@Repository
public interface ZoneViolationRepository extends JpaRepository<ZoneViolation, UUID> {

    /**
     * Find violations for a tenant with pagination
     */
    Page<ZoneViolation> findByTenantCode(String tenantCode, Pageable pageable);

    /**
     * Find violations within a time range for a tenant
     */
    Page<ZoneViolation> findByTenantCodeAndTimestampBetween(
            String tenantCode,
            ZonedDateTime startTime,
            ZonedDateTime endTime,
            Pageable pageable
    );

    /**
     * Find violations within a time range for a tenant (ordered)
     */
    Page<ZoneViolation> findByTenantCodeAndTimestampBetweenOrderByTimestampDesc(
            String tenantCode,
            ZonedDateTime startTime,
            ZonedDateTime endTime,
            Pageable pageable
    );

    /**
     * Find unacknowledged violations
     */
    Page<ZoneViolation> findByAcknowledged(Boolean acknowledged, Pageable pageable);

    /**
     * Find unacknowledged violations (ordered)
     */
    Page<ZoneViolation> findByAcknowledgedOrderByTimestampDesc(Boolean acknowledged, Pageable pageable);

    /**
     * Find violations by severity for a tenant
     */
    Page<ZoneViolation> findByTenantCodeAndSeverity(
            String tenantCode,
            ZoneViolation.ViolationSeverity severity,
            Pageable pageable
    );

    /**
     * Find violations by severity for a tenant (list)
     */
    List<ZoneViolation> findByTenantCodeAndSeverityOrderByTimestampDesc(
            String tenantCode,
            ZoneViolation.ViolationSeverity severity
    );

    /**
     * Find violations by tenant and acknowledged status
     */
    List<ZoneViolation> findByTenantCodeAndAcknowledgedOrderByTimestampDesc(
            String tenantCode,
            Boolean acknowledged
    );

    /**
     * Count violations by tenant and severity
     */
    long countByTenantCodeAndSeverity(String tenantCode, ZoneViolation.ViolationSeverity severity);

    /**
     * Count unacknowledged violations for a tenant
     */
    long countByTenantCodeAndAcknowledged(String tenantCode, Boolean acknowledged);

    /**
     * Find violations for a specific asset
     */
    Page<ZoneViolation> findByAssetIdAndTimestampBetweenOrderByTimestampDesc(
            UUID assetId,
            ZonedDateTime startTime,
            ZonedDateTime endTime,
            Pageable pageable
    );

    /**
     * Find violations in a specific zone
     */
    Page<ZoneViolation> findByRestrictedZoneIdAndTimestampBetweenOrderByTimestampDesc(
            UUID zoneId,
            ZonedDateTime startTime,
            ZonedDateTime endTime,
            Pageable pageable
    );

    /**
     * Get violation statistics by severity
     */
    @Query("SELECT v.severity as severity, COUNT(v) as count " +
           "FROM ZoneViolation v " +
           "WHERE v.tenantCode = :tenantCode " +
           "AND v.timestamp BETWEEN :startTime AND :endTime " +
           "GROUP BY v.severity")
    List<Object[]> getViolationStatsBySeverity(
            @Param("tenantCode") String tenantCode,
            @Param("startTime") ZonedDateTime startTime,
            @Param("endTime") ZonedDateTime endTime
    );

    // ============================================================
    // Export Support Methods (T055a, T055b)
    // ============================================================

    /**
     * Find all violations for a tenant (for export - no pagination)
     */
    List<ZoneViolation> findByTenantCodeOrderByTimestampDesc(String tenantCode);

    /**
     * Find violations within a time range for export (no pagination)
     */
    List<ZoneViolation> findByTenantCodeAndTimestampBetweenOrderByTimestampDesc(
            String tenantCode,
            ZonedDateTime startTime,
            ZonedDateTime endTime
    );
}
