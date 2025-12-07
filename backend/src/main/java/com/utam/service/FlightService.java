package com.utam.service;

import com.utam.model.Flight;
import com.utam.repository.FlightRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class FlightService {

    private final FlightRepository flightRepository;

    public FlightService(FlightRepository flightRepository) {
        this.flightRepository = flightRepository;
    }

    @KafkaListener(topics = "flight-events", groupId = "utam-group")
    public void consumeFlightEvent(Flight flight) {
        flightRepository.save(flight);
    }

    public List<Flight> getActiveFlights() {
        // Get flights from the last 5 minutes
        Instant fiveMinutesAgo = Instant.now().minus(5, ChronoUnit.MINUTES);
        return flightRepository.findLatestFlights(fiveMinutesAgo);
    }
}
