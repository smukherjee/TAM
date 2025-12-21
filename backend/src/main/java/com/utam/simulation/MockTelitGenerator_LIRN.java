package com.utam.simulation;

import com.utam.model.Vehicle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Component
public class MockTelitGenerator_LIRN {

    private static final Logger log = LoggerFactory.getLogger(MockTelitGenerator_LIRN.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    private final RestTemplate restTemplate;
    private final Random random = new Random();
    private final io.micrometer.core.instrument.Counter vehicleCounter;

    @Value("${simulation.vehicle-url}")
    private String ingestionUrl;

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
            new VehicleConfig("NAP-BUS-01", "NAP01", "BUS"),
            new VehicleConfig("NAP-TUG-05", "NAP05", "TUG"),
            new VehicleConfig("NAP-FUEL-02", "NAP02", "FUEL"),
            new VehicleConfig("NAP-CATER-03", "NAP03", "CATERING"));

    private final String icao = "LIRN";

    public MockTelitGenerator_LIRN(RestTemplate restTemplate, io.micrometer.core.instrument.MeterRegistry registry) {
        this.restTemplate = restTemplate;
        this.vehicleCounter = io.micrometer.core.instrument.Counter.builder("simulator.events.generated")
                .tag("type", "vehicle")
                .tag("icao", "LIRN")
                .description("Number of mock vehicle events generated")
                .register(registry);
    }

    @Scheduled(fixedRate = 3000) // Every 3 seconds
    public void generateVehicleData() {
        VehicleConfig config = configs.get(random.nextInt(configs.size()));

        Vehicle vehicle = new Vehicle();
        vehicle.setIcaoCode(icao);
        vehicle.setVehicleName(config.name);
        vehicle.setVehicleNo(config.no);
        vehicle.setType(config.type);

        vehicle.setCompany("GESAC");
        vehicle.setBranch("RAMP");
        vehicle.setTemperature("--");
        vehicle.setGps("ON");

        LocalDateTime now = LocalDateTime.now();
        vehicle.setTimestamp(now);
        vehicle.setGpsActualTime(now.minusSeconds(1).format(DATE_FORMATTER));
        vehicle.setCreationTimestamp(System.currentTimeMillis());

        vehicle.setStatus(random.nextBoolean() ? "RUNNING" : "IDLE");
        vehicle.setDeviceModel("MT4G-CANV2-MQTT");

        // Random lat/lon around LIRN
        double baseLat = 40.8844;
        double baseLon = 14.2908;

        vehicle.setLatitude(baseLat + (random.nextDouble() - 0.5) * 0.01);
        vehicle.setLongitude(baseLon + (random.nextDouble() - 0.5) * 0.01);

        vehicle.setSpeed(random.nextDouble() * 100);
        vehicle.setAc("--");
        vehicle.setImeiNo("865214420" + (100000 + random.nextInt(900000)));
        vehicle.setOdometer(String.valueOf(random.nextInt(50000)));
        vehicle.setIgn("ON");
        vehicle.setAngle(random.nextDouble() * 360);
        vehicle.setLocation("Naples International Airport");

        log.info("Generated LIRN vehicle data: {}", vehicle);

        try {
            restTemplate.postForObject(ingestionUrl, Collections.singletonList(vehicle), Void.class);
            vehicleCounter.increment();
        } catch (Exception e) {
            log.error("Failed to send vehicle data to ingestion layer: {}", e.getMessage());
        }
    }
}
