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
public class MockAdsbGenerator_LIRN {

    private static final Logger log = LoggerFactory.getLogger(MockAdsbGenerator_LIRN.class);

    private final RestTemplate restTemplate;
    private final Random random = new Random();
    private final io.micrometer.core.instrument.Counter flightCounter;
    private final SimulationConfig simulationConfig;

    @Value("${simulation.adsb-url}")
    private String ingestionUrl;

    // Simulated flights for Naples - 15 European airlines operating at LIRN
    private final List<String> callsigns = Arrays.asList(
        // Alitalia / ITA Airways
        "AZ123", "AZ456", "AZ789", "AZ234", "AZ567",
        // Ryanair
        "RYR101", "RYR202", "RYR303", "RYR404", "RYR505",
        // EasyJet
        "EJU111", "EJU222", "EJU333", "EJU444", "EJU555",
        // Wizz Air
        "WZZ161", "WZZ262", "WZZ363", "WZZ464",
        // Vueling
        "VLG171", "VLG272", "VLG373",
        // Lufthansa
        "LH181", "LH282", "LH383",
        // British Airways
        "BA191", "BA292", "BA393",
        // Air France
        "AFR201", "AFR302", "AFR403",
        // KLM
        "KLM211", "KLM312", "KLM413",
        // Iberia
        "IBE221", "IBE322",
        // TAP Portugal
        "TAP231", "TAP332",
        // SAS
        "SAS241", "SAS342",
        // Austrian
        "AUA251", "AUA352",
        // Swiss
        "SWR261", "SWR362",
        // Aer Lingus
        "EIN271", "EIN372"
    );
    private final String icao = "LIRN";

    public MockAdsbGenerator_LIRN(RestTemplate restTemplate, io.micrometer.core.instrument.MeterRegistry registry, SimulationConfig simulationConfig) {
        this.restTemplate = restTemplate;
        this.simulationConfig = simulationConfig;
        this.flightCounter = io.micrometer.core.instrument.Counter.builder("simulator.events.generated")
                .tag("type", "flight")
                .tag("icao", "LIRN")
                .description("Number of mock flight events generated")
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
