package com.utam.simulation.turnaround;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Turnaround entity.
 */
@Repository
public interface TurnaroundRepository extends JpaRepository<Turnaround, UUID> {

    List<Turnaround> findByTenantCode(String tenantCode);

    List<Turnaround> findByTenantCodeAndStatus(String tenantCode, String status);

    List<Turnaround> findByTenantCodeAndStandCode(String tenantCode, String standCode);

    @Query("SELECT t FROM Turnaround t WHERE t.tenantCode = :tenantCode AND t.status = 'IN_PROGRESS'")
    List<Turnaround> findActiveTurnarounds(String tenantCode);

    @Query("SELECT t FROM Turnaround t WHERE t.tenantCode = :tenantCode AND t.delayMinutes > 5")
    List<Turnaround> findDelayedTurnarounds(String tenantCode);

    @Query("SELECT t FROM Turnaround t WHERE t.tenantCode = :tenantCode AND t.startTime > :since ORDER BY t.startTime DESC")
    List<Turnaround> findRecentTurnarounds(String tenantCode, Instant since);

    @Query("SELECT COUNT(t) FROM Turnaround t WHERE t.tenantCode = :tenantCode")
    long countByTenantCode(String tenantCode);

    @Query("SELECT AVG(t.actualDurationMinutes) FROM Turnaround t WHERE t.tenantCode = :tenantCode AND t.status = 'COMPLETED'")
    Double getAverageTurnaroundTime(String tenantCode);

    void deleteByTenantCode(String tenantCode);
}
