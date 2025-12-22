package com.utam.turnaround.repository;

import com.utam.turnaround.domain.TurnaroundSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.List;
import java.util.Optional;

@Repository
public interface TurnaroundSessionRepository extends JpaRepository<TurnaroundSession, UUID> {
    List<TurnaroundSession> findByIcaoCode(String icaoCode);
    List<TurnaroundSession> findByIcaoCodeAndStatus(String icaoCode, String status);
    Optional<TurnaroundSession> findByIdAndIcaoCode(UUID id, String icaoCode);
    Optional<TurnaroundSession> findByFlightIdAndIcaoCode(String flightId, String icaoCode);
}
