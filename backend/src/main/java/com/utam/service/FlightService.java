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
    private final RedisService redisService;

    public FlightService(FlightRepository flightRepository, RedisService redisService) {
        this.flightRepository = flightRepository;
        this.redisService = redisService;
    }

    @Transactional
    public void saveAll(List<Flight> flights) {
        flightRepository.saveAll(flights);
    }

    public List<Flight> getActiveFlights(String icaoCode) {
        // Try Redis first, but only trust it if we have enough active flights to be meaningful
        String icao = icaoCode != null && !icaoCode.isEmpty() ? icaoCode : "VIDP";
        java.util.Set<Object> activeCallsigns = redisService.getSetMembers("active_flights:" + icao);

        if (activeCallsigns != null && !activeCallsigns.isEmpty()) {
            List<Flight> flights = new java.util.ArrayList<>();
            for (Object callsignObj : activeCallsigns) {
                String callsign = (String) callsignObj;
                java.util.Optional<Flight> flightOpt = redisService.get("flight:" + icao + ":" + callsign, Flight.class);
                flightOpt.ifPresent(flights::add);
            }
            // If Redis has at least 20 flights, assume it is authoritative; otherwise fall back to DB
            if (flights.size() >= 20) {
                return flights;
            }
        }

        // Fallback to DB
        Instant fiveMinutesAgo = Instant.now().minus(5, ChronoUnit.MINUTES);
        if (icaoCode != null && !icaoCode.isEmpty()) {
            return flightRepository.findLatestFlightsByIcao(fiveMinutesAgo, icaoCode);
        }
        return flightRepository.findLatestFlights(fiveMinutesAgo);
    }
}
