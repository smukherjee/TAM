package com.utam.repository;

import com.utam.model.TurnaroundEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;

@Repository
public interface TurnaroundEventRepository extends JpaRepository<TurnaroundEvent, String> {
    java.util.List<TurnaroundEvent> findByIcaoCodeOrderByEventTimeStampDesc(String icaoCode);

    java.util.List<TurnaroundEvent> findAllByOrderByEventTimeStampDesc();

    @Query("SELECT e FROM TurnaroundEvent e WHERE e.icaoCode = :icaoCode AND e.eventTimeStamp >= :since ORDER BY e.eventTimeStamp DESC")
    java.util.List<TurnaroundEvent> findRecentByIcaoCode(@Param("icaoCode") String icaoCode,
            @Param("since") LocalDateTime since);

    @Query("SELECT e FROM TurnaroundEvent e WHERE e.eventTimeStamp >= :since ORDER BY e.eventTimeStamp DESC")
    java.util.List<TurnaroundEvent> findRecentEvents(@Param("since") LocalDateTime since);
}
