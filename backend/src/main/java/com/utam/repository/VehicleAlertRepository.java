package com.utam.repository;

import com.utam.model.VehicleAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VehicleAlertRepository extends JpaRepository<VehicleAlert, UUID> {
    List<VehicleAlert> findTop10ByTenantCodeOrderByTimestampDesc(String tenantCode);

    List<VehicleAlert> findTop10ByOrderByTimestampDesc();
}
