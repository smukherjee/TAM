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
public class MockAdsbGenerator_YBBN {

    private static final Logger log = LoggerFactory.getLogger(MockAdsbGenerator_YBBN.class);

    private final RestTemplate restTemplate;
    private final Random random = new Random();
    private final io.micrometer.core.instrument.Counter flightCounter;

    @Value("${simulation.adsb-url}")
    private String ingestionUrl;

    // Simulated flights for Brisbane
    private final List<String> callsigns = Arrays.asList("QF401", "VA823", "JQ520", "NZ175", "SQ245");
    private final String icao = "YBBN";

    public MockAdsbGenerator_YBBN(RestTemplate restTemplate, io.micrometer.core.instrument.MeterRegistry registry) {
        this.restTemplate = restTemplate;
        this.flightCounter = io.micrometer.core.instrument.Counter.builder("simulator.events.generated")
                .tag("type", "flight")
                .tag("icao", "YBBN")
                .description("Number of mock flight events generated for YBBN")
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

        // Random lat/lon around YBBN (Brisbane)
        double baseLat = -27.3842;
        double baseLon = 153.1175;

        flight.setLatitude(baseLat + (random.nextDouble() - 0.5) * 2); // +/- 1 degree
        flight.setLongitude(baseLon + (random.nextDouble() - 0.5) * 2);

        flight.setSpeed(400.0 + random.nextDouble() * 100); // 400-500 knots
        flight.setHeading(random.nextDouble() * 360);
        flight.setAltitude(10000.0 + random.nextDouble() * 30000); // 10000-40000 ft
        flight.setStatus("AIRBORNE");

        log.info("Generated YBBN flight data: {}", flight);

        try {
            restTemplate.postForObject(ingestionUrl, Collections.singletonList(flight), Void.class);
            flightCounter.increment();
        } catch (Exception e) {
            log.error("Failed to send YBBN flight data to ingestion layer: {}", e.getMessage());
        }
    }
}
