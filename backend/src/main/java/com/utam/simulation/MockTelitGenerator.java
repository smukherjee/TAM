package com.utam.simulation;

import com.utam.model.Vehicle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Component
public class MockTelitGenerator {

    private static final Logger log = LoggerFactory.getLogger(MockTelitGenerator.class);

    private final KafkaTemplate<String, Vehicle> kafkaTemplate;
    private final Random random = new Random();

    // Simulated vehicles
    private final List<String> vehicleNos = Arrays.asList("BUS-101", "TRUCK-55", "CAR-007", "BUS-202", "TRUCK-99");

    public MockTelitGenerator(KafkaTemplate<String, Vehicle> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedRate = 3000) // Every 3 seconds
    public void generateVehicleData() {
        String vehicleNo = vehicleNos.get(random.nextInt(vehicleNos.size()));

        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleNo(vehicleNo);
        vehicle.setTimestamp(LocalDateTime.now());
        
        // Determine type based on vehicleNo prefix
        if (vehicleNo.startsWith("BUS")) vehicle.setType("BUS");
        else if (vehicleNo.startsWith("TRUCK")) vehicle.setType("TRUCK");
        else vehicle.setType("CAR");

        // Random lat/lon around IGIA (New Delhi)
        double baseLat = 28.5562;
        double baseLon = 77.1000;

        vehicle.setLatitude(baseLat + (random.nextDouble() - 0.5) * 0.1); // Smaller range for ground vehicles
        vehicle.setLongitude(baseLon + (random.nextDouble() - 0.5) * 0.1);

        vehicle.setSpeed(random.nextDouble() * 80); // 0-80 km/h
        vehicle.setAltitude(0.0); // Ground level
        vehicle.setStatus("RUNNING");

        log.info("Generated Vehicle Data: {}", vehicle);
        kafkaTemplate.send("vehicle-events", vehicle.getVehicleNo(), vehicle);
    }
}
