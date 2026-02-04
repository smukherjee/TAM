package com.utam.simulation;

import com.utam.model.Vehicle;
import com.utam.simulation.config.SimulationConfig;
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
public class MockTelitGenerator_YBBN {

    private static final Logger log = LoggerFactory.getLogger(MockTelitGenerator_YBBN.class);

    private final RestTemplate restTemplate;
    private final Random random = new Random();
    private final io.micrometer.core.instrument.Counter vehicleCounter;
    private final SimulationConfig simulationConfig;

    @org.springframework.beans.factory.annotation.Value("${simulation.vehicle-url}")
    private String ingestionUrl;

    private final String icao = "YBBN";

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
            new VehicleConfig("BNE-TUG-01", "Brisbane Tug 1", "Tug"),
            new VehicleConfig("BNE-BUS-01", "Brisbane Bus 1", "Bus"),
            new VehicleConfig("BNE-FUEL-01", "Brisbane Fuel 1", "Fuel Truck"),
            new VehicleConfig("BNE-CAT-01", "Brisbane Catering 1", "Catering Truck"),
            new VehicleConfig("BNE-BAG-01", "Brisbane Baggage 1", "Baggage Loader")
    );

    public MockTelitGenerator_YBBN(io.micrometer.core.instrument.MeterRegistry registry, SimulationConfig simulationConfig) {
        this.restTemplate = new RestTemplate();
        this.simulationConfig = simulationConfig;
        this.vehicleCounter = io.micrometer.core.instrument.Counter.builder("simulation.vehicles.generated")
                .tag("icao", "YBBN")
                .description("Number of simulated vehicle events for YBBN")
                .register(registry);
    }

    @Scheduled(fixedRate = 3000) // Every 3 seconds
    public void generateVehicleData() {
        // Respect simulation config
        if (!simulationConfig.isEnabled() || !simulationConfig.isContinuousEnabled()) {
            return;
        }

        VehicleConfig config = configs.get(random.nextInt(configs.size()));

        Vehicle vehicle = new Vehicle();
        vehicle.setId(UUID.randomUUID());
        vehicle.setVehicleId(config.no);
        vehicle.setVehicleType(config.type);
        vehicle.setTenantCode(icao);
        vehicle.setTimestamp(Instant.now());
        vehicle.setStatus(random.nextBoolean() ? "RUNNING" : "IDLE");

        // Random lat/lon around YBBN
        double baseLat = -27.3842;
        double baseLon = 153.1175;

        vehicle.setLatitude(baseLat + (random.nextDouble() - 0.5) * 0.02);
        vehicle.setLongitude(baseLon + (random.nextDouble() - 0.5) * 0.02);

        // Generate speed between 0 and 100 km/h to trigger alerts (> 70 km/h)
        vehicle.setSpeed(random.nextDouble() * 100);

        log.info("Generated YBBN vehicle data for {}: {}", config.getName(), vehicle);

        try {
            restTemplate.postForObject(ingestionUrl, Collections.singletonList(vehicle), Void.class);
            vehicleCounter.increment();
        } catch (Exception e) {
            log.error("Failed to send simulated YBBN vehicle data: {}", e.getMessage());
        }
    }
}
