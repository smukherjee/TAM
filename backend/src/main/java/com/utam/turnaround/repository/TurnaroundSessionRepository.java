package com.utam.turnaround.repository;

import com.utam.turnaround.domain.TurnaroundSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.List;
import java.util.Optional;

@Repository
public interface TurnaroundSessionRepository extends JpaRepository<TurnaroundSession, UUID> {
    List<TurnaroundSession> findByTenantCode(String tenantCode);
    List<TurnaroundSession> findByTenantCodeAndStatus(String tenantCode, String status);
    Optional<TurnaroundSession> findByIdAndTenantCode(UUID id, String tenantCode);
    Optional<TurnaroundSession> findByFlightIdAndTenantCode(String flightId, String tenantCode);
}
