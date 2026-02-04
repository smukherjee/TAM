package com.utam.repository;

import com.utam.entity.Stand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StandRepository extends JpaRepository<Stand, Long> {

    List<Stand> findByTenantCode(String tenantCode);

    List<Stand> findByTenantCodeAndActive(String tenantCode, Boolean active);

    List<Stand> findByTenantCodeAndTerminalId(String tenantCode, String terminalId);

    Optional<Stand> findByTenantCodeAndStandId(String tenantCode, String standId);

    boolean existsByTenantCodeAndStandId(String tenantCode, String standId);

    long countByTenantCode(String tenantCode);
}
