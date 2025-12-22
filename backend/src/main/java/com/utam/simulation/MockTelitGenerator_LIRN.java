package com.utam.simulation;

import com.utam.model.Vehicle;
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
public class MockTelitGenerator_LIRN {

    private static final Logger log = LoggerFactory.getLogger(MockTelitGenerator_LIRN.class);

    private final RestTemplate restTemplate;
    private final Random random = new Random();
    private final io.micrometer.core.instrument.Counter vehicleCounter;

    @Value("${simulation.vehicle-url}")
    private String ingestionUrl;

    private final String icao = "LIRN";

    private static class VehicleConfig {
        String no;
        String name;
        String type;

        VehicleConfig(String no, String name, String type) {
            this.no = no;
            this.name = name;
            this.type = type;
        }
    }

    private final List<VehicleConfig> configs = Arrays.asList(
            new VehicleConfig("GESAC001", "Tug 1", "Tug"),
            new VehicleConfig("GESAC002", "Bus 1", "Bus"),
            new VehicleConfig("GESAC003", "Fuel Truck 1", "Fuel Truck"),
            new VehicleConfig("GESAC004", "Catering 1", "Catering Truck"),
            new VehicleConfig("GESAC005", "Baggage 1", "Baggage Loader")
    );

    public MockTelitGenerator_LIRN(io.micrometer.core.instrument.MeterRegistry registry) {
        this.restTemplate = new RestTemplate();
        this.vehicleCounter = io.micrometer.core.instrument.Counter.builder("simulation.vehicles.generated")
                .tag("icao", icao)
                .description("Number of simulated vehicle events for LIRN")
                .register(registry);
    }

    @Scheduled(fixedRate = 3000) // Every 3 seconds
    public void generateVehicleData() {
        VehicleConfig config = configs.get(random.nextInt(configs.size()));

        Vehicle vehicle = new Vehicle();
        vehicle.setId(UUID.randomUUID());
        // vehicle.setIcaoCode(icao); // Removed
        // vehicle.setVehicleName(config.name); // Removed
        vehicle.setVehicleId(config.no);
        vehicle.setVehicleType(config.type);

        vehicle.setTenantCode(icao);
        
        vehicle.setTimestamp(Instant.now());

        vehicle.setStatus(random.nextBoolean() ? "RUNNING" : "IDLE");

        // Random lat/lon around LIRN
        double baseLat = 40.8844;
        double baseLon = 14.2908;

        vehicle.setLatitude(baseLat + (random.nextDouble() - 0.5) * 0.01);
        vehicle.setLongitude(baseLon + (random.nextDouble() - 0.5) * 0.01);

        vehicle.setSpeed(random.nextDouble() * 100);
        
        // vehicle.setIgn("ON"); // Removed
        // vehicle.setLocation("Naples International Airport"); // Removed

        log.info("Generated LIRN vehicle data: {}", vehicle);

        try {
            restTemplate.postForObject(ingestionUrl, Collections.singletonList(vehicle), Void.class);
            vehicleCounter.increment();
        } catch (Exception e) {
            log.error("Failed to send simulated vehicle data: {}", e.getMessage());
        }
    }
}
