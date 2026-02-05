package com.utam.simulation;

import com.utam.model.Flight;
import com.utam.simulation.config.SimulationConfig;
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
    private final SimulationConfig simulationConfig;

    @Value("${simulation.adsb-url}")
    private String ingestionUrl;

    // Simulated flights for Brisbane - Australian and Asia-Pacific carriers
    private final List<String> callsigns = Arrays.asList(
        // Qantas
        "QF401", "QF402", "QF403", "QF501", "QF502", "QF601", "QF602", "QF603",
        // Virgin Australia
        "VA823", "VA824", "VA825", "VA901", "VA902", "VA903",
        // Jetstar
        "JQ520", "JQ521", "JQ522", "JQ620", "JQ621", "JQ622",
        // Air New Zealand
        "NZ175", "NZ176", "NZ177", "NZ275", "NZ276",
        // Singapore Airlines
        "SQ245", "SQ246", "SQ247", "SQ345",
        // Emirates
        "EK432", "EK433", "EK434",
        // Cathay Pacific
        "CX155", "CX156", "CX157",
        // Malaysia Airlines
        "MH123", "MH124", "MH125",
        // China Southern
        "CZ305", "CZ306",
        // Korean Air
        "KE123", "KE124"
    );
    private final String icao = "YBBN";

    public MockAdsbGenerator_YBBN(RestTemplate restTemplate, io.micrometer.core.instrument.MeterRegistry registry, SimulationConfig simulationConfig) {
        this.restTemplate = restTemplate;
        this.simulationConfig = simulationConfig;
        this.flightCounter = io.micrometer.core.instrument.Counter.builder("simulator.events.generated")
                .tag("type", "flight")
                .tag("icao", "YBBN")
                .description("Number of mock flight events generated for YBBN")
                .register(registry);
    }

    @Scheduled(fixedRate = 2000) // Every 2 seconds
    public void generateFlightData() {
        // Respect simulation config - only generate if enabled and continuous mode is on
        if (!simulationConfig.isEnabled() || !simulationConfig.isContinuousEnabled()) {
            return;
        }

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
