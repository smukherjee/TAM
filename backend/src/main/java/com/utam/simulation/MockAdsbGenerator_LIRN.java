package com.utam.simulation;

import com.utam.model.Flight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
public class MockAdsbGenerator_LIRN {

    private static final Logger log = LoggerFactory.getLogger(MockAdsbGenerator_LIRN.class);

    private final RestTemplate restTemplate;
    private final Random random = new Random();
    private final io.micrometer.core.instrument.Counter flightCounter;

    @Value("${simulation.adsb-url}")
    private String ingestionUrl;

    // Simulated flights for Naples
    private final List<String> callsigns = Arrays.asList("AZ123", "RYR45", "EJU99", "LH333", "BA777");
    private final String icao = "LIRN";

    public MockAdsbGenerator_LIRN(RestTemplate restTemplate, io.micrometer.core.instrument.MeterRegistry registry) {
        this.restTemplate = restTemplate;
        this.flightCounter = io.micrometer.core.instrument.Counter.builder("simulator.events.generated")
                .tag("type", "flight")
                .tag("icao", "LIRN")
                .description("Number of mock flight events generated")
                .register(registry);
    }

    @Scheduled(fixedRate = 2000) // Every 2 seconds
    public void generateFlightData() {
        String callsign = callsigns.get(random.nextInt(callsigns.size()));

        Flight flight = new Flight();
        flight.setId(UUID.randomUUID());
        flight.setTimestamp(Instant.now());
        flight.setCallsign(callsign);
        flight.setTenantCode(icao);
        flight.setFlightNumber(callsign);

        // Random lat/lon around LIRN (Naples)
        double baseLat = 40.8844;
        double baseLon = 14.2908;

        flight.setLatitude(baseLat + (random.nextDouble() - 0.5) * 0.5); // +/- 0.25 degree
        flight.setLongitude(baseLon + (random.nextDouble() - 0.5) * 0.5);

        flight.setSpeed(250.0 + random.nextDouble() * 100);
        flight.setHeading(random.nextDouble() * 360);
        flight.setAltitude(2000.0 + random.nextDouble() * 5000);
        flight.setStatus("APPROACHING");

        // New fields per JSON signature
        // flight.setTrackId("TRK" + Math.abs(callsign.hashCode() % 1000)); // Removed
        // flight.setModeSId(Integer.toHexString(callsign.hashCode()).toUpperCase()); // Removed
        // flight.setFlightLevel(flight.getAltitude() / 100.0); // Removed
        // flight.setRoc(random.nextDouble() * 1000 - 500); // Removed
        // flight.setSsr(String.format("%04d", random.nextInt(10000))); // Removed
        // flight.setSafetyAlert(false); // Removed
        // flight.setSystemStatus("OK"); // Removed
        // flight.setSpi(false); // Removed
        // flight.setUpdateType("TRACK_UPDATE"); // Removed
        // flight.setCreationTimestamp(System.currentTimeMillis()); // Removed

        log.info("Generated LIRN flight data: {}", flight);

        try {
            restTemplate.postForObject(ingestionUrl, Collections.singletonList(flight), Void.class);
            flightCounter.increment();
        } catch (Exception e) {
            log.error("Failed to send flight data to ingestion layer: {}", e.getMessage());
        }
    }
}
