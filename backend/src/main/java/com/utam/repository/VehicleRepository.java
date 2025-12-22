package com.utam.repository;

import com.utam.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

    @Query(value = "SELECT DISTINCT ON (vehicle_id) * FROM vehicles WHERE timestamp > :since ORDER BY vehicle_id, timestamp DESC", nativeQuery = true)
    List<Vehicle> findLatestVehicles(Instant since);
}
