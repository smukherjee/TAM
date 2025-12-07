package com.utam.repository;

import com.utam.model.Flight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface FlightRepository extends JpaRepository<Flight, UUID> {
    
    @Query(value = "SELECT DISTINCT ON (callsign) * FROM flights WHERE time > :since ORDER BY callsign, time DESC", nativeQuery = true)
    List<Flight> findLatestFlights(Instant since);
}
