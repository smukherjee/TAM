package com.utam.repository;

import com.utam.entity.TurnaroundMilestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for TurnaroundMilestone entities.
 */
@Repository
public interface TurnaroundMilestoneRepository extends JpaRepository<TurnaroundMilestone, UUID> {

    List<TurnaroundMilestone> findByTurnaroundId(UUID turnaroundId);

    List<TurnaroundMilestone> findByTurnaroundIdOrderBySequenceOrder(UUID turnaroundId);

    List<TurnaroundMilestone> findByTenantCode(String tenantCode);

    List<TurnaroundMilestone> findByTenantCodeAndPlannedTimeBetween(
            String tenantCode, Instant start, Instant end);

    @Query("SELECT m FROM TurnaroundMilestone m WHERE m.tenantCode = :tenantCode " +
           "AND m.actualTime IS NULL AND m.plannedTime < :now")
    List<TurnaroundMilestone> findOverdueMilestones(String tenantCode, Instant now);

    @Query("SELECT m FROM TurnaroundMilestone m WHERE m.turnaroundId = :turnaroundId " +
           "AND m.actualTime IS NULL ORDER BY m.sequenceOrder")
    List<TurnaroundMilestone> findPendingMilestones(UUID turnaroundId);

    @Query("SELECT COUNT(m) FROM TurnaroundMilestone m WHERE m.turnaroundId = :turnaroundId " +
           "AND m.varianceMinutes > 0")
    long countDelayedMilestones(UUID turnaroundId);

    void deleteByTenantCodeAndCreatedAtBefore(String tenantCode, Instant before);
}
