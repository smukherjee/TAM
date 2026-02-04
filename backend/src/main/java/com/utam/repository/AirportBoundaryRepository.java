package com.utam.repository;

import com.utam.entity.AirportBoundary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AirportBoundaryRepository extends JpaRepository<AirportBoundary, Long> {

    Optional<AirportBoundary> findByTenantCode(String tenantCode);

    boolean existsByTenantCode(String tenantCode);
}
