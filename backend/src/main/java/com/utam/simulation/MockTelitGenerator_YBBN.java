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

    // 100 vehicles for YBBN (Brisbane) - GSE fleet
    private final List<VehicleConfig> configs;

    {
        List<VehicleConfig> list = new java.util.ArrayList<>();
        // Fuel Trucks (10)
        for (int i = 1; i <= 10; i++) list.add(new VehicleConfig("BNE-FT-" + String.format("%02d", i), "Fuel Truck " + i, "Fuel Truck"));
        // Catering Trucks (8)
        for (int i = 1; i <= 8; i++) list.add(new VehicleConfig("BNE-CT-" + String.format("%02d", i), "Catering Truck " + i, "Catering Truck"));
        // Baggage Tugs (12)
        for (int i = 1; i <= 12; i++) list.add(new VehicleConfig("BNE-BT-" + String.format("%02d", i), "Baggage Tug " + i, "Baggage Tug"));
        // Baggage Carts (15)
        for (int i = 1; i <= 15; i++) list.add(new VehicleConfig("BNE-BC-" + String.format("%02d", i), "Baggage Cart " + i, "Baggage Cart"));
        // Belt Loaders (8)
        for (int i = 1; i <= 8; i++) list.add(new VehicleConfig("BNE-BL-" + String.format("%02d", i), "Belt Loader " + i, "Belt Loader"));
        // GPUs (6)
        for (int i = 1; i <= 6; i++) list.add(new VehicleConfig("BNE-GP-" + String.format("%02d", i), "GPU " + i, "GPU"));
        // Pushback Tractors (8)
        for (int i = 1; i <= 8; i++) list.add(new VehicleConfig("BNE-PB-" + String.format("%02d", i), "Pushback " + i, "Pushback"));
        // Stairs (6)
        for (int i = 1; i <= 6; i++) list.add(new VehicleConfig("BNE-ST-" + String.format("%02d", i), "Stairs " + i, "Stairs"));
        // Water Trucks (4)
        for (int i = 1; i <= 4; i++) list.add(new VehicleConfig("BNE-WT-" + String.format("%02d", i), "Water Truck " + i, "Water Truck"));
        // Lavatory Trucks (4)
        for (int i = 1; i <= 4; i++) list.add(new VehicleConfig("BNE-LV-" + String.format("%02d", i), "Lavatory Truck " + i, "Lavatory Truck"));
        // De-icing Trucks (3)
        for (int i = 1; i <= 3; i++) list.add(new VehicleConfig("BNE-DI-" + String.format("%02d", i), "De-icing Truck " + i, "De-icing Truck"));
        // ASUs (4)
        for (int i = 1; i <= 4; i++) list.add(new VehicleConfig("BNE-AS-" + String.format("%02d", i), "ASU " + i, "ASU"));
        // Buses (8)
        for (int i = 1; i <= 8; i++) list.add(new VehicleConfig("BNE-BU-" + String.format("%02d", i), "Bus " + i, "Bus"));
        // Cargo Loaders (3)
        for (int i = 1; i <= 3; i++) list.add(new VehicleConfig("BNE-CG-" + String.format("%02d", i), "Cargo Loader " + i, "Cargo Loader"));
        // Ambulifts (1)
        list.add(new VehicleConfig("BNE-AM-01", "Ambulift 1", "Ambulift"));
        configs = list;
    }

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
