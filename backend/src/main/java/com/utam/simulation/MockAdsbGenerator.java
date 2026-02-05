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
public class MockAdsbGenerator {

    private static final Logger log = LoggerFactory.getLogger(MockAdsbGenerator.class);

    private final RestTemplate restTemplate;
    private final Random random = new Random();
    private final io.micrometer.core.instrument.Counter flightCounter;
    private final SimulationConfig simulationConfig;

    @Value("${simulation.adsb-url}")
    private String ingestionUrl;

    // Simulated flights for VIDP (Delhi) - Indian and international carriers
    private final List<String> callsigns = Arrays.asList(
        // Air India
        "AI101", "AI102", "AI103", "AI201", "AI202", "AI303", "AI404", "AI505",
        // IndiGo
        "IGO101", "IGO202", "IGO303", "IGO404", "IGO505", "IGO606", "IGO707",
        // Vistara (now merged with Air India)
        "VTI101", "VTI202", "VTI303", "VTI404",
        // SpiceJet
        "SPC101", "SPC202", "SPC303", "SPC404",
        // British Airways
        "BA142", "BA143", "BA249", "BA256",
        // Emirates
        "EK510", "EK511", "EK512", "EK500",
        // Lufthansa
        "LH760", "LH761", "LH762",
        // Singapore Airlines
        "SQ402", "SQ403", "SQ404",
        // Thai Airways
        "TG315", "TG316", "TG317",
        // Cathay Pacific
        "CX697", "CX698",
        // Qantas
        "QF1", "QF2"
    );
    
    /**
     * List of supported airport ICAO codes.
     */
    @SuppressWarnings("unused") // Reserved for future tenant filtering
    private final List<String> airports = Arrays.asList("VIDP", "YBBN");

    public MockAdsbGenerator(RestTemplate restTemplate, io.micrometer.core.instrument.MeterRegistry registry, SimulationConfig simulationConfig) {
        this.restTemplate = restTemplate;
        this.simulationConfig = simulationConfig;
        this.flightCounter = io.micrometer.core.instrument.Counter.builder("simulator.events.generated")
                .tag("type", "flight")
                .tag("icao", "VIDP")
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
        
        // Map flights to their correct airports
        String tenant;
        if (callsign.equals("EK500") || callsign.equals("QF1")) {
            tenant = "YBBN"; // Emirates and Qantas flights go to Brisbane
        } else {
            tenant = "VIDP"; // AI101, BA249, LH760 go to Delhi
        }

        Flight flight = new Flight();
        flight.setId(UUID.randomUUID());
        flight.setTimestamp(Instant.now());
        flight.setCallsign(callsign);
        flight.setTenantCode(tenant);
        flight.setFlightNumber(callsign); // Assuming flight number same as callsign for mock

        // Set lat/lon based on tenant
        double baseLat, baseLon;
        if (tenant.equals("YBBN")) {
            baseLat = -27.3842; // Brisbane
            baseLon = 153.1175;
        } else {
            baseLat = 28.5562; // Delhi
            baseLon = 77.1000;
        }

        flight.setLatitude(baseLat + (random.nextDouble() - 0.5) * 2); // +/- 1 degree
        flight.setLongitude(baseLon + (random.nextDouble() - 0.5) * 2);

        flight.setSpeed(400.0 + random.nextDouble() * 100); // 400-500 knots
        flight.setHeading(random.nextDouble() * 360);
        flight.setAltitude(10000.0 + random.nextDouble() * 30000); // 10000-40000 ft
        flight.setStatus("AIRBORNE");

        // New fields per JSON signature
        // flight.setTrackId("TRK" + Math.abs(callsign.hashCode() % 1000)); // Removed
        // flight.setModeSId(Integer.toHexString(callsign.hashCode()).toUpperCase()); // Removed
        // flight.setFlightLevel(flight.getAltitude() / 100.0); // Removed
        // flight.setRoc(random.nextDouble() * 2000 - 1000); // +/- 1000 fpm // Removed
        // flight.setSsr(String.format("%04d", random.nextInt(10000))); // Removed
        // flight.setSafetyAlert(false); // Removed
        // flight.setSystemStatus("OK"); // Removed
        // flight.setSpi(false); // Removed
        // flight.setUpdateType("TRACK_UPDATE"); // Removed
        // flight.setCreationTimestamp(System.currentTimeMillis()); // Removed

        log.info("Generated flight data: {}", flight);

        try {
            restTemplate.postForObject(ingestionUrl, Collections.singletonList(flight), Void.class);
            flightCounter.increment();
        } catch (Exception e) {
            log.error("Failed to send flight data to ingestion layer: {}", e.getMessage());
        }
    }
}
