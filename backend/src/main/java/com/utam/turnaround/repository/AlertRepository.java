package com.utam.turnaround.repository;

import com.utam.turnaround.domain.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.List;

@Repository("TurnaroundAlertRepository")
public interface AlertRepository extends JpaRepository<Alert, UUID> {
    List<Alert> findByIcaoCode(String icaoCode);
    List<Alert> findBySessionId(UUID sessionId);
    List<Alert> findByIcaoCodeAndIsActiveTrue(String icaoCode);
}
