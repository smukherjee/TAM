package com.utam.service;

import com.utam.model.Flight;
import com.utam.repository.FlightRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class FlightService {

    private final FlightRepository flightRepository;

    public FlightService(FlightRepository flightRepository) {
        this.flightRepository = flightRepository;
    }

    @Transactional
    public void saveAll(List<Flight> flights) {
        flightRepository.saveAll(flights);
    }

    public List<Flight> getActiveFlights(String icaoCode) {
        // Get flights from the last 5 minutes
        Instant fiveMinutesAgo = Instant.now().minus(5, ChronoUnit.MINUTES);
        if (icaoCode != null && !icaoCode.isEmpty()) {
            return flightRepository.findLatestFlightsByIcao(fiveMinutesAgo, icaoCode);
        }
        return flightRepository.findLatestFlights(fiveMinutesAgo);
    }
}
