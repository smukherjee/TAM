package com.utam.repository;

import com.utam.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Zone entities.
 * Provides queries for admin zone management.
 */
@Repository
public interface ZoneRepository extends JpaRepository<Zone, Long> {

    /**
     * Find all zones for a specific tenant.
     */
    List<Zone> findByTenantCode(String tenantCode);

    /**
     * Find active zones for a specific tenant.
     */
    List<Zone> findByTenantCodeAndActiveTrue(String tenantCode);

    /**
     * Find zones by tenant and type.
     */
    List<Zone> findByTenantCodeAndType(String tenantCode, String type);

    /**
     * Find zone by tenant and code.
     */
    Optional<Zone> findByTenantCodeAndCode(String tenantCode, String code);

    /**
     * Check if zone code exists for tenant.
     */
    boolean existsByTenantCodeAndCode(String tenantCode, String code);
}
