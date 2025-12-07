package com.utam.simulation;

import com.utam.model.Vehicle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Component
public class MockTelitGenerator {

    private static final Logger log = LoggerFactory.getLogger(MockTelitGenerator.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    private final KafkaTemplate<String, Vehicle> kafkaTemplate;
    private final Random random = new Random();

    private static class VehicleConfig {
        String name;
        String no;
        String type;
        
        VehicleConfig(String name, String no, String type) {
            this.name = name;
            this.no = no;
            this.type = type;
        }
    }

    private final List<VehicleConfig> configs = Arrays.asList(
        new VehicleConfig("TMD-000013", "PBT11", "Compactor"),
        new VehicleConfig("TMD254HV-000009", "BFL30", "SUV"),
        new VehicleConfig("BUS-101", "DL1PC0001", "BUS"),
        new VehicleConfig("TRUCK-55", "HR55X9999", "TRUCK")
    );

    public MockTelitGenerator(KafkaTemplate<String, Vehicle> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedRate = 3000) // Every 3 seconds
    public void generateVehicleData() {
        VehicleConfig config = configs.get(random.nextInt(configs.size()));

        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleName(config.name);
        vehicle.setVehicleNo(config.no);
        vehicle.setType(config.type);
        
        vehicle.setCompany("Phase3_DIAL");
        vehicle.setBranch("Phase3_DIAL");
        vehicle.setTemperature("--");
        vehicle.setGps("ON");
        
        vehicle.setDoor1("--");
        vehicle.setDoor2("--");
        vehicle.setDoor3("--");
        vehicle.setDoor4("--");
        
        LocalDateTime now = LocalDateTime.now();
        vehicle.setTimestamp(now);
        vehicle.setGpsActualTime(now.minusSeconds(1).format(DATE_FORMATTER));
        
        vehicle.setStatus(random.nextBoolean() ? "RUNNING" : "IDLE");
        vehicle.setDeviceModel("MT4G-CANV2-MQTT");
        
        // Random lat/lon around IGIA (New Delhi)
        double baseLat = 28.5562;
        double baseLon = 77.1000;

        vehicle.setLatitude(baseLat + (random.nextDouble() - 0.5) * 0.02);
        vehicle.setLongitude(baseLon + (random.nextDouble() - 0.5) * 0.02);

        // Generate speed between 0 and 100 km/h to trigger alerts (> 70 km/h)
        vehicle.setSpeed(random.nextDouble() * 100); 
        vehicle.setAc("--");
        vehicle.setImeiNo("359214420" + (100000 + random.nextInt(900000)));
        vehicle.setOdometer(String.valueOf(100000 + random.nextInt(10000)));
        vehicle.setPoi("--");
        
        vehicle.setDriverFirstName("--");
        vehicle.setDriverMiddleName("--");
        vehicle.setDriverLastName("--");
        
        vehicle.setImmobilizeState("--");
        vehicle.setIgn("ON");
        vehicle.setAngle(random.nextDouble() * 360);
        vehicle.setSos("--");
        vehicle.setFuel(Collections.emptyList());
        vehicle.setBatteryPercentage("0");
        vehicle.setExternalVolt(random.nextBoolean() ? "28.00" : "12.70");
        vehicle.setPower("ON");
        vehicle.setAltitude(0.0);
        vehicle.setLocation("IGIA, New Delhi");

        log.info("Generated Vehicle Data: {}", vehicle.getVehicleNo());
        kafkaTemplate.send("vehicle-events", vehicle.getVehicleNo(), vehicle);
    }
}
