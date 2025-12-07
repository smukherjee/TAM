package com.utam.simulation;

import com.utam.model.Flight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Component
public class MockAdsbGenerator {

    private static final Logger log = LoggerFactory.getLogger(MockAdsbGenerator.class);

    private final RestTemplate restTemplate;
    private final Random random = new Random();
    
    // Simulated flights
    private final List<String> callsigns = Arrays.asList("AI101", "BA249", "LH760", "EK500", "QF1");

    public MockAdsbGenerator(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Scheduled(fixedRate = 2000) // Every 2 seconds
    public void generateFlightData() {
        String callsign = callsigns.get(random.nextInt(callsigns.size()));
        
        Flight flight = new Flight();
        flight.setLivePlotId(UUID.randomUUID());
        flight.setTime(Instant.now());
        flight.setCallsign(callsign);
        
        // Random lat/lon around IGIA (New Delhi)
        double baseLat = 28.5562;
        double baseLon = 77.1000;
        
        flight.setLatitude(baseLat + (random.nextDouble() - 0.5) * 2); // +/- 1 degree
        flight.setLongitude(baseLon + (random.nextDouble() - 0.5) * 2);
        
        flight.setSpeed(400.0 + random.nextDouble() * 100); // 400-500 knots
        flight.setHeading(random.nextDouble() * 360);
        flight.setAltitude(10000.0 + random.nextDouble() * 30000); // 10000-40000 ft
        flight.setStatus("AIRBORNE");

        // New fields per JSON signature
        flight.setTrackId("TRK" + Math.abs(callsign.hashCode() % 1000));
        flight.setModeSId(Integer.toHexString(callsign.hashCode()).toUpperCase());
        flight.setFlightLevel(flight.getAltitude() / 100.0);
        flight.setRoc(random.nextDouble() * 2000 - 1000); // +/- 1000 fpm
        flight.setSsr(String.format("%04d", random.nextInt(10000)));
        flight.setSafetyAlert(false);
        flight.setSystemStatus("OK");
        flight.setSpi(false);
        flight.setUpdateType("TRACK_UPDATE");

        log.info("Generated flight data: {}", flight);
        
        try {
            restTemplate.postForObject("http://localhost:8080/api/adsblivedata", Collections.singletonList(flight), Void.class);
        } catch (Exception e) {
            log.error("Failed to send flight data to ingestion layer: {}", e.getMessage());
        }
    }
}
