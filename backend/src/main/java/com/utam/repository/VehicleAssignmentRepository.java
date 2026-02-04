package com.utam.repository;

import com.utam.entity.VehicleAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for VehicleAssignment entities.
 */
@Repository
public interface VehicleAssignmentRepository extends JpaRepository<VehicleAssignment, UUID> {

    List<VehicleAssignment> findByTurnaroundId(UUID turnaroundId);

    List<VehicleAssignment> findByVehicleId(UUID vehicleId);

    List<VehicleAssignment> findByVehicleIdAndStatus(UUID vehicleId, String status);

    List<VehicleAssignment> findByTenantCode(String tenantCode);

    List<VehicleAssignment> findByTenantCodeAndStatus(String tenantCode, String status);

    @Query("SELECT va FROM VehicleAssignment va WHERE va.tenantCode = :tenantCode " +
           "AND va.status NOT IN ('COMPLETE', 'CANCELLED')")
    List<VehicleAssignment> findActiveAssignments(String tenantCode);

    @Query("SELECT va FROM VehicleAssignment va WHERE va.vehicleId = :vehicleId " +
           "AND va.status NOT IN ('COMPLETE', 'CANCELLED')")
    List<VehicleAssignment> findActiveAssignmentsForVehicle(UUID vehicleId);

    @Query("SELECT COUNT(va) FROM VehicleAssignment va WHERE va.turnaroundId = :turnaroundId " +
           "AND va.status = 'COMPLETE'")
    long countCompletedAssignments(UUID turnaroundId);

    @Query("SELECT va FROM VehicleAssignment va WHERE va.tenantCode = :tenantCode " +
           "AND va.dispatchTime BETWEEN :start AND :end")
    List<VehicleAssignment> findByTenantCodeAndDispatchTimeBetween(
            String tenantCode, Instant start, Instant end);

    void deleteByTenantCodeAndCreatedAtBefore(String tenantCode, Instant before);
}
