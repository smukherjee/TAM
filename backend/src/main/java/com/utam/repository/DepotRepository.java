package com.utam.repository;

import com.utam.entity.Depot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepotRepository extends JpaRepository<Depot, Long> {

    List<Depot> findByTenantCode(String tenantCode);

    List<Depot> findByTenantCodeAndActive(String tenantCode, Boolean active);

    List<Depot> findByTenantCodeAndDepotType(String tenantCode, String depotType);

    Optional<Depot> findFirstByTenantCodeAndDepotType(String tenantCode, String depotType);

    long countByTenantCode(String tenantCode);

    boolean existsByTenantCodeAndName(String tenantCode, String name);
}
