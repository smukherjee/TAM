package com.utam.repository;

import com.utam.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, String> {

    @Query(value = "SELECT DISTINCT ON (vehicle_no) * FROM vehicles WHERE timestamp > :since ORDER BY vehicle_no, timestamp DESC", nativeQuery = true)
    List<Vehicle> findLatestVehicles(LocalDateTime since);
}

