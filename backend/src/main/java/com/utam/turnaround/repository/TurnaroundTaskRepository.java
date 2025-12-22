package com.utam.turnaround.repository;

import com.utam.turnaround.domain.TurnaroundTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.List;

@Repository
public interface TurnaroundTaskRepository extends JpaRepository<TurnaroundTask, UUID> {
    List<TurnaroundTask> findByIcaoCode(String icaoCode);
    List<TurnaroundTask> findBySessionId(UUID sessionId);
}
