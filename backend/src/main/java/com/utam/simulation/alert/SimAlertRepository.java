package com.utam.simulation.alert;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for simulation Alert entity.
 */
@Repository
public interface SimAlertRepository extends JpaRepository<Alert, UUID> {

    List<Alert> findByTenantCode(String tenantCode);

    List<Alert> findByTenantCodeAndStatus(String tenantCode, String status);

    List<Alert> findByTenantCodeAndSeverity(String tenantCode, String severity);

    List<Alert> findByTenantCodeAndAlertType(String tenantCode, String alertType);

    @Query("SELECT a FROM SimulationAlert a WHERE a.tenantCode = :tenantCode AND a.status NOT IN ('RESOLVED', 'DISMISSED') ORDER BY a.createdAt DESC")
    List<Alert> findActiveAlerts(String tenantCode);

    @Query("SELECT a FROM SimulationAlert a WHERE a.tenantCode = :tenantCode AND a.severity IN ('HIGH', 'CRITICAL') AND a.status = 'NEW'")
    List<Alert> findCriticalAlerts(String tenantCode);

    @Query("SELECT a FROM SimulationAlert a WHERE a.tenantCode = :tenantCode AND a.createdAt > :since ORDER BY a.createdAt DESC")
    List<Alert> findRecentAlerts(String tenantCode, Instant since);

    @Query("SELECT a FROM SimulationAlert a WHERE a.entityType = :entityType AND a.entityId = :entityId")
    List<Alert> findByEntity(String entityType, UUID entityId);

    @Query("SELECT COUNT(a) FROM SimulationAlert a WHERE a.tenantCode = :tenantCode AND a.status = 'NEW'")
    long countNewAlerts(String tenantCode);

    @Query("SELECT COUNT(a) FROM SimulationAlert a WHERE a.tenantCode = :tenantCode")
    long countByTenantCode(String tenantCode);

    void deleteByTenantCode(String tenantCode);
}
