package com.utam.repository;

import com.utam.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {
    java.util.List<Alert> findTop10ByIcaoCodeOrderByTimestampDesc(String icaoCode);

    java.util.List<Alert> findTop10ByOrderByTimestampDesc();
}
