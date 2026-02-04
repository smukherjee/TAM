package com.utam.simulation.dispatch;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Dispatch entity.
 */
@Repository
public interface DispatchRepository extends JpaRepository<Dispatch, UUID> {

    List<Dispatch> findByTenantCode(String tenantCode);

    List<Dispatch> findByTenantCodeAndStatus(String tenantCode, String status);

    List<Dispatch> findByTenantCodeAndVehicleId(String tenantCode, UUID vehicleId);

    List<Dispatch> findByTenantCodeAndStandCode(String tenantCode, String standCode);

    @Query("SELECT d FROM Dispatch d WHERE d.tenantCode = :tenantCode AND d.status NOT IN ('COMPLETED', 'CANCELLED')")
    List<Dispatch> findActiveDispatches(String tenantCode);

    @Query("SELECT d FROM Dispatch d WHERE d.tenantCode = :tenantCode AND d.delaySeconds > 300")
    List<Dispatch> findDelayedDispatches(String tenantCode);

    @Query("SELECT d FROM Dispatch d WHERE d.tenantCode = :tenantCode AND d.createdAt > :since ORDER BY d.createdAt DESC")
    List<Dispatch> findRecentDispatches(String tenantCode, Instant since);

    @Query("SELECT COUNT(d) FROM Dispatch d WHERE d.tenantCode = :tenantCode")
    long countByTenantCode(String tenantCode);

    @Query("SELECT AVG(d.actualDurationSeconds) FROM Dispatch d WHERE d.tenantCode = :tenantCode AND d.status = 'COMPLETED'")
    Double getAverageDispatchTime(String tenantCode);

    void deleteByTenantCode(String tenantCode);
}
