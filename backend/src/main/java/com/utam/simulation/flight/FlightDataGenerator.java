package com.utam.simulation.flight;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.utam.model.Flight;
import com.utam.repository.FlightRepository;
import com.utam.simulation.config.SimulationConfig;
import com.utam.simulation.core.BaseDataGenerator;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generates realistic flight data with arrivals and departures.
 * Implements FR-022 to FR-027: Realistic flight generation with approach/departure patterns.
 * 
 * Data Flow: Generator -> NiFi (adsb-ingest) -> Kafka (flight-raw-json) -> FlightConsumer -> WebSocket -> Frontend
 */
@Component
public class FlightDataGenerator extends BaseDataGenerator {

    private final FlightRepository flightRepository;
    private final FlightTrajectoryCalculator trajectoryCalculator;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final Random random = new Random();

    // Use same property as MockAdsbGenerator for proper NiFi integration
    @Value("${simulation.adsb-url}")
    private String nifiUrl;


    // Active flights being simulated
    private final Map<String, ActiveFlight> activeFlights = new ConcurrentHashMap<>();

    // Airline codes loaded from seed data
    private final Map<String, List<String>> airlineCodes = new ConcurrentHashMap<>();

    public FlightDataGenerator(SimulationConfig config, MeterRegistry meterRegistry,
                               FlightRepository flightRepository,
                               FlightTrajectoryCalculator trajectoryCalculator,
                               ObjectMapper objectMapper,
                               RestTemplate restTemplate) {
        super(config, meterRegistry);
        this.flightRepository = flightRepository;
        this.trajectoryCalculator = trajectoryCalculator;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    @Override
    public String getGeneratorName() {
        return "FlightDataGenerator";
    }

    @Override
    public String getEntityType() {
        return "Flight";
    }

    @PostConstruct
    public void init() {
        loadAirlineCodes();
        log.info("FlightDataGenerator initialized with {} tenant airline configurations", airlineCodes.size());
    }

    private void loadAirlineCodes() {
        for (String tenantCode : config.getTenants().keySet()) {
            try {
                JsonNode rootNode = loadSeedData(tenantCode);
                if (rootNode != null && rootNode.has("airlines")) {
                    List<String> codes = new ArrayList<>();
                    for (JsonNode airline : rootNode.get("airlines")) {
                        codes.add(airline.get("code").asText());
                    }
                    airlineCodes.put(tenantCode, codes);
                }
            } catch (Exception e) {
                log.warn("Could not load airline codes for {}: {}", tenantCode, e.getMessage());
            }
        }
    }

    @Override
    public int generateBatch(String tenantCode, int batchSize) {
        SimulationConfig.TenantConfig tenantConfig = config.getTenant(tenantCode);
        if (tenantConfig == null) {
            log.warn("No tenant config for: {}", tenantCode);
            return 0;
        }

        int generated = 0;
        int arrivals = batchSize / 2;
        int departures = batchSize - arrivals;

        // Generate arriving flights
        for (int i = 0; i < arrivals; i++) {
            Flight flight = createFlight(tenantCode, tenantConfig, FlightType.ARRIVING);
            if (flight != null) {
                flightRepository.save(flight);
                registerActiveFlight(flight, FlightType.ARRIVING);
                generated++;
            }
        }

        // Generate departing flights
        for (int i = 0; i < departures; i++) {
            Flight flight = createFlight(tenantCode, tenantConfig, FlightType.DEPARTING);
            if (flight != null) {
                flightRepository.save(flight);
                registerActiveFlight(flight, FlightType.DEPARTING);
                generated++;
            }
        }

        log.info("Generated {} flights for tenant {} ({} arrivals, {} departures)", 
                generated, tenantCode, arrivals, departures);
        return generated;
    }

    @Override
    public boolean generateSingleUpdate(String tenantCode) {
        // Update positions of active flights
        List<String> toRemove = new ArrayList<>();
        List<Flight> flightUpdates = new ArrayList<>(); // Batch updates

        for (Map.Entry<String, ActiveFlight> entry : activeFlights.entrySet()) {
            ActiveFlight activeFlight = entry.getValue();
            if (!activeFlight.tenantCode.equals(tenantCode)) {
                continue;
            }

            // Update position using trajectory calculator
            FlightTrajectoryCalculator.Position newPos = trajectoryCalculator.calculateNextPosition(
                    activeFlight.latitude,
                    activeFlight.longitude,
                    activeFlight.altitude,
                    activeFlight.speed,
                    activeFlight.heading,
                    activeFlight.type
            );

            // Check if flight has completed
            if (activeFlight.type == FlightType.ARRIVING && newPos.altitude() < 100) {
                toRemove.add(entry.getKey());
                continue;
            }
            if (activeFlight.type == FlightType.DEPARTING && newPos.altitude() > 35000) {
                toRemove.add(entry.getKey());
                continue;
            }

            // Update active flight state
            activeFlight.latitude = newPos.latitude();
            activeFlight.longitude = newPos.longitude();
            activeFlight.altitude = newPos.altitude();
            activeFlight.speed = newPos.speed();
            activeFlight.heading = newPos.heading();

            // Create flight update object
            Flight flight = new Flight();
            flight.setId(UUID.randomUUID());
            flight.setTenantCode(tenantCode);
            flight.setFlightNumber(activeFlight.flightNumber);
            flight.setCallsign(activeFlight.callsign);
            flight.setTimestamp(Instant.now());
            flight.setLatitude(activeFlight.latitude);
            flight.setLongitude(activeFlight.longitude);
            flight.setAltitude(activeFlight.altitude);
            flight.setSpeed(activeFlight.speed);
            flight.setHeading(activeFlight.heading);
            flight.setStatus(activeFlight.type == FlightType.ARRIVING ? "APPROACHING" : "DEPARTING");

            flightUpdates.add(flight);
        }

        // Send batched updates to NiFi
        if (!flightUpdates.isEmpty()) {
            try {
                restTemplate.postForObject(nifiUrl, flightUpdates, String.class);
            } catch (Exception e) {
                if (random.nextDouble() < 0.01) {
                    log.error("Failed to send flight batch update to NiFi: {}", e.getMessage());
                }
            }
        }

        // Remove completed flights
        toRemove.forEach(activeFlights::remove);

        // Replenish flights to maintain target count
        int targetCount = config.getBatchSize();
        long currentCount = activeFlights.values().stream()
                .filter(f -> f.tenantCode.equals(tenantCode))
                .count();
        
        if (currentCount < targetCount) {
             // Spawn difference, but limit to small batch to avoid spikes
             int toSpawn = Math.min(targetCount - (int)currentCount, 5);
             
             SimulationConfig.TenantConfig tenantConfig = config.getTenant(tenantCode);
             if (tenantConfig != null) {
                 for(int i = 0; i < toSpawn; i++) {
                    FlightType type = random.nextBoolean() ? FlightType.ARRIVING : FlightType.DEPARTING;
                    Flight flight = createFlight(tenantCode, tenantConfig, type);
                    if (flight != null) {
                        // Do not save to DB directly. Let the consumer handle persistence via NiFi update.
                        registerActiveFlight(flight, type);
                    }
                 }
             }
        }

        return true;
    }

    private Flight createFlight(String tenantCode, SimulationConfig.TenantConfig tenantConfig, FlightType type) {
        Flight flight = new Flight();
        flight.setId(UUID.randomUUID());
        flight.setTenantCode(tenantCode);
        
        // Generate flight number
        String airline = getRandomAirline(tenantCode);
        int flightNum = 100 + random.nextInt(900);
        flight.setFlightNumber(airline + flightNum);
        flight.setCallsign(airline + flightNum);
        
        flight.setTimestamp(Instant.now());

        // Calculate initial position based on flight type
        FlightTrajectoryCalculator.Position startPos = trajectoryCalculator.calculateStartPosition(
                tenantConfig.getCenterLatitude(),
                tenantConfig.getCenterLongitude(),
                type
        );

        flight.setLatitude(startPos.latitude());
        flight.setLongitude(startPos.longitude());
        flight.setAltitude(startPos.altitude());
        flight.setSpeed(startPos.speed());
        flight.setHeading(startPos.heading());
        flight.setStatus(type == FlightType.ARRIVING ? "APPROACHING" : "DEPARTING");

        return flight;
    }

    private void registerActiveFlight(Flight flight, FlightType type) {
        ActiveFlight active = new ActiveFlight();
        active.flightNumber = flight.getFlightNumber();
        active.callsign = flight.getCallsign();
        active.tenantCode = flight.getTenantCode();
        active.latitude = flight.getLatitude();
        active.longitude = flight.getLongitude();
        active.altitude = flight.getAltitude();
        active.speed = flight.getSpeed();
        active.heading = flight.getHeading();
        active.type = type;
        active.startTime = Instant.now();

        activeFlights.put(flight.getFlightNumber() + "_" + active.startTime.toEpochMilli(), active);
    }

    private String getRandomAirline(String tenantCode) {
        List<String> codes = airlineCodes.get(tenantCode);
        if (codes == null || codes.isEmpty()) {
            // Default airlines
            return List.of("AI", "6E", "QF", "VA", "EK", "BA").get(random.nextInt(6));
        }
        return codes.get(random.nextInt(codes.size()));
    }

    private JsonNode loadSeedData(String tenantCode) {
        String fileName = "data/" + tenantCode.toLowerCase() + ".json";
        try {
            ClassPathResource resource = new ClassPathResource(fileName);
            if (!resource.exists()) return null;
            try (InputStream is = resource.getInputStream()) {
                return objectMapper.readTree(is);
            }
        } catch (Exception e) {
            return null;
        }
    }

    public enum FlightType {
        ARRIVING, DEPARTING
    }

    /**
     * T071: Lightweight position update for real-time tracking.
     * Called at high frequency (every 500ms) by GeneratorOrchestrator.
     * Only updates positions for active in-air flights.
     */
    public void generatePositionUpdate(String tenantCode) {
        if (!isEnabled() || activeFlights.isEmpty()) {
            return;
        }

        List<String> toRemove = new ArrayList<>();

        for (Map.Entry<String, ActiveFlight> entry : activeFlights.entrySet()) {
            ActiveFlight activeFlight = entry.getValue();
            if (!activeFlight.tenantCode.equals(tenantCode)) {
                continue;
            }

            // Use trajectory calculator for smoother position interpolation
            FlightTrajectoryCalculator.Position newPos = trajectoryCalculator.calculateNextPosition(
                    activeFlight.latitude,
                    activeFlight.longitude,
                    activeFlight.altitude,
                    activeFlight.speed,
                    activeFlight.heading,
                    activeFlight.type
            );

            // Check if flight has completed its trajectory
            if (isFlightComplete(activeFlight, newPos)) {
                toRemove.add(entry.getKey());
                continue;
            }

            // Update active flight state
            activeFlight.latitude = newPos.latitude();
            activeFlight.longitude = newPos.longitude();
            activeFlight.altitude = newPos.altitude();
            activeFlight.speed = newPos.speed();
            activeFlight.heading = newPos.heading();
        }

        // Remove completed flights
        toRemove.forEach(activeFlights::remove);
    }

    /**
     * Check if a flight has completed based on type and altitude.
     */
    private boolean isFlightComplete(ActiveFlight flight, FlightTrajectoryCalculator.Position pos) {
        return (flight.type == FlightType.ARRIVING && pos.altitude() < 100) ||
               (flight.type == FlightType.DEPARTING && pos.altitude() > 35000);
    }

    /**
     * Get count of active in-air flights for a tenant.
     */
    public long getActiveFlightCount(String tenantCode) {
        return activeFlights.values().stream()
            .filter(f -> f.tenantCode.equals(tenantCode))
            .count();
    }

    /**
     * Get all active flight positions for a tenant (for real-time display).
     */
    public List<FlightPositionDTO> getActiveFlightPositions(String tenantCode) {
        return activeFlights.values().stream()
            .filter(f -> f.tenantCode.equals(tenantCode))
            .map(this::toPositionDTO)
            .toList();
    }

    private FlightPositionDTO toPositionDTO(ActiveFlight flight) {
        return FlightPositionDTO.builder()
            .flightNumber(flight.flightNumber)
            .callsign(flight.callsign)
            .latitude(flight.latitude)
            .longitude(flight.longitude)
            .altitude(flight.altitude)
            .speed(flight.speed)
            .heading(flight.heading)
            .status(flight.type == FlightType.ARRIVING ? "APPROACHING" : "DEPARTING")
            .build();
    }

    private static class ActiveFlight {
        String flightNumber;
        String callsign;
        String tenantCode;
        double latitude;
        double longitude;
        double altitude;
        double speed;
        double heading;
        FlightType type;
        Instant startTime;
    }
}
