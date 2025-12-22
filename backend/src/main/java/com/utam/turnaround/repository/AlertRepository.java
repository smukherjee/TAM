package com.utam.turnaround.repository;

import com.utam.turnaround.domain.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.List;

@Repository("TurnaroundAlertRepository")
public interface AlertRepository extends JpaRepository<Alert, UUID> {
    List<Alert> findByTenantCode(String tenantCode);
    List<Alert> findBySessionId(UUID sessionId);
    List<Alert> findByTenantCodeAndIsActiveTrue(String tenantCode);
}
