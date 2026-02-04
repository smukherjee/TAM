package com.utam.simulation;

import com.utam.model.Vehicle;
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
public class MockTelitGenerator {

    private static final Logger log = LoggerFactory.getLogger(MockTelitGenerator.class);

    private final RestTemplate restTemplate;
    private final Random random = new Random();
    private final io.micrometer.core.instrument.Counter vehicleCounter;
    private final SimulationConfig simulationConfig;

    @Value("${simulation.vehicle-url}")
    private String ingestionUrl;

    // VIDP (Delhi) only - YBBN has its own generator
    private final String icao = "VIDP";

    private static class VehicleConfig {
        final String no;
        final String name;
        final String type;

        VehicleConfig(String no, String name, String type) {
            this.no = no;
            this.name = name;
            this.type = type;
        }
        
        String getName() {
            return name;
        }
    }

    private final List<VehicleConfig> configs = Arrays.asList(
            new VehicleConfig("DL1GC0001", "Tug 1", "Tug"),
            new VehicleConfig("DL1GC0002", "Bus 1", "Bus"),
            new VehicleConfig("DL1GC0003", "Fuel Truck 1", "Fuel Truck"),
            new VehicleConfig("DL1GC0004", "Catering 1", "Catering Truck"),
            new VehicleConfig("DL1GC0005", "Baggage 1", "Baggage Loader")
    );

    public MockTelitGenerator(io.micrometer.core.instrument.MeterRegistry registry, SimulationConfig simulationConfig) {
        this.restTemplate = new RestTemplate();
        this.simulationConfig = simulationConfig;
        this.vehicleCounter = io.micrometer.core.instrument.Counter.builder("simulation.vehicles.generated")
                .description("Number of simulated vehicle events")
                .register(registry);
    }

    @Scheduled(fixedRate = 3000) // Every 3 seconds
    public void generateVehicleData() {
        // Respect simulation config - only generate if enabled and continuous mode is on
        if (!simulationConfig.isEnabled() || !simulationConfig.isContinuousEnabled()) {
            return;
        }

        VehicleConfig config = configs.get(random.nextInt(configs.size()));

        Vehicle vehicle = new Vehicle();
        vehicle.setId(UUID.randomUUID());
        // vehicle.setVehicleName(config.name); // Removed from entity
        vehicle.setVehicleId(config.no);
        vehicle.setVehicleType(config.type);

        vehicle.setTenantCode(icao); // Always VIDP for this generator
        
        vehicle.setTimestamp(Instant.now());

        vehicle.setStatus(random.nextBoolean() ? "RUNNING" : "IDLE");

        // Random lat/lon around IGIA (New Delhi)
        double baseLat = 28.5562;
        double baseLon = 77.1000;

        vehicle.setLatitude(baseLat + (random.nextDouble() - 0.5) * 0.02);
        vehicle.setLongitude(baseLon + (random.nextDouble() - 0.5) * 0.02);

        // Generate speed between 0 and 100 km/h to trigger alerts (> 70 km/h)
        vehicle.setSpeed(random.nextDouble() * 100);
        
        // vehicle.setIgn("ON"); // Removed from entity
        // vehicle.setLocation("IGIA, New Delhi"); // Removed from entity

        log.info("Generated vehicle data for {}: {}", config.getName(), vehicle);

        try {
            restTemplate.postForObject(ingestionUrl, Collections.singletonList(vehicle), Void.class);
            vehicleCounter.increment();
        } catch (Exception e) {
            log.error("Failed to send simulated vehicle data: {}", e.getMessage());
        }
    }
}
