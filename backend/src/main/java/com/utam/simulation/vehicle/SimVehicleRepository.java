package com.utam.simulation.vehicle;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for simulation Vehicle entity.
 */
@Repository
public interface SimVehicleRepository extends JpaRepository<Vehicle, UUID> {

    List<Vehicle> findByTenantCode(String tenantCode);

    List<Vehicle> findByTenantCodeAndStatus(String tenantCode, String status);

    List<Vehicle> findByTenantCodeAndVehicleTypeId(String tenantCode, UUID vehicleTypeId);

    @Query("SELECT v FROM SimulationVehicle v WHERE v.tenantCode = :tenantCode AND v.lastUpdated > :since")
    List<Vehicle> findActiveVehicles(String tenantCode, Instant since);

    @Query("SELECT COUNT(v) FROM SimulationVehicle v WHERE v.tenantCode = :tenantCode")
    long countByTenantCode(String tenantCode);

    @Query("SELECT COUNT(v) FROM SimulationVehicle v WHERE v.tenantCode = :tenantCode AND v.status = :status")
    long countByTenantCodeAndStatus(String tenantCode, String status);

    void deleteByTenantCode(String tenantCode);
}
