package com.utam.simulation.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * T076: Repository for MovementDiscrepancy entities.
 */
@Repository
public interface DiscrepancyRepository extends JpaRepository<MovementDiscrepancy, UUID> {

    /**
     * Find all discrepancies for a tenant, ordered by detection time.
     */
    List<MovementDiscrepancy> findByTenantCodeOrderByDetectedAtDesc(String tenantCode);

    /**
     * Find discrepancies by tenant and status.
     */
    List<MovementDiscrepancy> findByTenantCodeAndStatus(String tenantCode, String status);

    /**
     * Find discrepancies by tenant and type.
     */
    List<MovementDiscrepancy> findByTenantCodeAndDiscrepancyType(String tenantCode, String discrepancyType);

    /**
     * Find discrepancies for a specific entity.
     */
    List<MovementDiscrepancy> findByEntityIdOrderByDetectedAtDesc(String entityId);

    /**
     * Find discrepancies in a time range.
     */
    List<MovementDiscrepancy> findByTenantCodeAndDetectedAtBetween(
        String tenantCode, Instant start, Instant end);

    /**
     * Count discrepancies by tenant and status.
     */
    long countByTenantCodeAndStatus(String tenantCode, String status);

    /**
     * Count discrepancies by type.
     */
    long countByTenantCodeAndDiscrepancyType(String tenantCode, String discrepancyType);

    /**
     * Delete old resolved discrepancies (for cleanup).
     */
    void deleteByStatusAndResolvedAtBefore(String status, Instant before);
}
