package com.utam.repository;

import com.utam.entity.VehiclePath;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for VehiclePath entities.
 */
@Repository
public interface VehiclePathRepository extends JpaRepository<VehiclePath, Long> {

    List<VehiclePath> findByTenantCode(String tenantCode);

    List<VehiclePath> findByTenantCodeAndActive(String tenantCode, Boolean active);

    List<VehiclePath> findByTenantCodeAndVehicleTypeCode(String tenantCode, String vehicleTypeCode);

    List<VehiclePath> findByTenantCodeAndVehicleTypeCodeAndActive(
            String tenantCode, String vehicleTypeCode, Boolean active);

    List<VehiclePath> findByCreatedBy(String createdBy);

    long countByTenantCodeAndActive(String tenantCode, Boolean active);
}
