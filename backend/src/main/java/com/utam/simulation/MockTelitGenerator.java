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

//@Component
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

    // 100 vehicles for VIDP (Delhi) - GSE fleet
    private final List<VehicleConfig> configs;

    {
        List<VehicleConfig> list = new java.util.ArrayList<>();
        // Fuel Trucks (10)
        for (int i = 1; i <= 10; i++)
            list.add(new VehicleConfig("DEL-FT-" + String.format("%02d", i), "Fuel Truck " + i, "Fuel Truck"));
        // Catering Trucks (8)
        for (int i = 1; i <= 8; i++)
            list.add(new VehicleConfig("DEL-CT-" + String.format("%02d", i), "Catering Truck " + i, "Catering Truck"));
        // Baggage Tugs (12)
        for (int i = 1; i <= 12; i++)
            list.add(new VehicleConfig("DEL-BT-" + String.format("%02d", i), "Baggage Tug " + i, "Baggage Tug"));
        // Baggage Carts (15)
        for (int i = 1; i <= 15; i++)
            list.add(new VehicleConfig("DEL-BC-" + String.format("%02d", i), "Baggage Cart " + i, "Baggage Cart"));
        // Belt Loaders (8)
        for (int i = 1; i <= 8; i++)
            list.add(new VehicleConfig("DEL-BL-" + String.format("%02d", i), "Belt Loader " + i, "Belt Loader"));
        // GPUs (6)
        for (int i = 1; i <= 6; i++)
            list.add(new VehicleConfig("DEL-GP-" + String.format("%02d", i), "GPU " + i, "GPU"));
        // Pushback Tractors (8)
        for (int i = 1; i <= 8; i++)
            list.add(new VehicleConfig("DEL-PB-" + String.format("%02d", i), "Pushback " + i, "Pushback"));
        // Stairs (6)
        for (int i = 1; i <= 6; i++)
            list.add(new VehicleConfig("DEL-ST-" + String.format("%02d", i), "Stairs " + i, "Stairs"));
        // Water Trucks (4)
        for (int i = 1; i <= 4; i++)
            list.add(new VehicleConfig("DEL-WT-" + String.format("%02d", i), "Water Truck " + i, "Water Truck"));
        // Lavatory Trucks (4)
        for (int i = 1; i <= 4; i++)
            list.add(new VehicleConfig("DEL-LV-" + String.format("%02d", i), "Lavatory Truck " + i, "Lavatory Truck"));
        // De-icing Trucks (3)
        for (int i = 1; i <= 3; i++)
            list.add(new VehicleConfig("DEL-DI-" + String.format("%02d", i), "De-icing Truck " + i, "De-icing Truck"));
        // ASUs (4)
        for (int i = 1; i <= 4; i++)
            list.add(new VehicleConfig("DEL-AS-" + String.format("%02d", i), "ASU " + i, "ASU"));
        // Buses (8)
        for (int i = 1; i <= 8; i++)
            list.add(new VehicleConfig("DEL-BU-" + String.format("%02d", i), "Bus " + i, "Bus"));
        // Cargo Loaders (3)
        for (int i = 1; i <= 3; i++)
            list.add(new VehicleConfig("DEL-CG-" + String.format("%02d", i), "Cargo Loader " + i, "Cargo Loader"));
        // Ambulifts (1)
        list.add(new VehicleConfig("DEL-AM-01", "Ambulift 1", "Ambulift"));
        configs = list;
    }

    public MockTelitGenerator(io.micrometer.core.instrument.MeterRegistry registry, SimulationConfig simulationConfig) {
        this.restTemplate = new RestTemplate();
        this.simulationConfig = simulationConfig;
        this.vehicleCounter = io.micrometer.core.instrument.Counter.builder("simulation.vehicles.generated")
                .description("Number of simulated vehicle events")
                .register(registry);
    }

    @Scheduled(fixedRate = 3000) // Every 3 seconds
    public void generateVehicleData() {
        // Respect simulation config - only generate if enabled and continuous mode is
        // on
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
