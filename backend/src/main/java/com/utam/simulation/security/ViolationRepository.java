package com.utam.simulation.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * T075: Repository for Violation entities.
 */
@Repository
public interface ViolationRepository extends JpaRepository<Violation, UUID> {

    /**
     * Find all violations for a tenant, ordered by detection time.
     */
    List<Violation> findByTenantCodeOrderByDetectedAtDesc(String tenantCode);

    /**
     * Find violations by tenant and status.
     */
    List<Violation> findByTenantCodeAndStatus(String tenantCode, String status);

    /**
     * Find violations by tenant and type.
     */
    List<Violation> findByTenantCodeAndViolationType(String tenantCode, String violationType);

    /**
     * Find violations for a specific entity.
     */
    List<Violation> findByEntityIdOrderByDetectedAtDesc(String entityId);

    /**
     * Find violations for a specific zone.
     */
    List<Violation> findByZoneIdOrderByDetectedAtDesc(UUID zoneId);

    /**
     * Find violations in a time range.
     */
    List<Violation> findByTenantCodeAndDetectedAtBetween(
        String tenantCode, Instant start, Instant end);

    /**
     * Count violations by tenant and status.
     */
    long countByTenantCodeAndStatus(String tenantCode, String status);

    /**
     * Count violations by severity.
     */
    long countByTenantCodeAndSeverity(String tenantCode, String severity);

    /**
     * Delete old resolved violations (for cleanup).
     */
    void deleteByStatusAndResolvedAtBefore(String status, Instant before);
}
