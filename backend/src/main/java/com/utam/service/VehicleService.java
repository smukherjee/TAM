package com.utam.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.utam.model.Vehicle;
import com.utam.repository.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class VehicleService {

    private static final Logger logger = LoggerFactory.getLogger(VehicleService.class);
    private final VehicleRepository vehicleRepository;
    private final ObjectMapper objectMapper;
    private final io.micrometer.core.instrument.Timer latencyTimer;
    private final io.micrometer.core.instrument.Counter errorCounter;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;
    private final RedisService redisService;
    private final MinioService minioService;

    public VehicleService(VehicleRepository vehicleRepository, ObjectMapper objectMapper,
            io.micrometer.core.instrument.MeterRegistry registry,
            org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate,
            RedisService redisService,
            MinioService minioService) {
        this.vehicleRepository = vehicleRepository;
        this.objectMapper = objectMapper;
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.registerModule(new JavaTimeModule());
        this.messagingTemplate = messagingTemplate;
        this.redisService = redisService;
        this.minioService = minioService;

        this.latencyTimer = io.micrometer.core.instrument.Timer.builder("pipeline.latency.seconds")
                .tag("type", "vehicle")
                .description("End-to-end latency for vehicle events")
                .register(registry);

        this.errorCounter = io.micrometer.core.instrument.Counter.builder("pipeline.events.failed")
                .tag("type", "vehicle")
                .description("Number of failed vehicle events")
                .register(registry);
    }

    @KafkaListener(topics = "vehicle-raw-json", groupId = "utam-group")
    public void consumeVehicleEvent(String message) {
        try {
            List<Vehicle> vehicles;
            if (message.trim().startsWith("[")) {
                vehicles = Arrays.asList(objectMapper.readValue(message, Vehicle[].class));
            } else {
                vehicles = Collections.singletonList(objectMapper.readValue(message, Vehicle.class));
            }

            for (Vehicle vehicle : vehicles) {
                // Ensure timestamp is set if missing
                if (vehicle.getTimestamp() == null) {
                    vehicle.setTimestamp(LocalDateTime.now());
                }

                if (vehicle.getCreationTimestamp() != null) {
                    long latency = System.currentTimeMillis() - vehicle.getCreationTimestamp();
                    latencyTimer.record(java.time.Duration.ofMillis(Math.max(0, latency)));
                }

                // WebSocket Push
                String icao = vehicle.getIcaoCode() != null ? vehicle.getIcaoCode() : "VIDP";
                messagingTemplate.convertAndSend("/topic/vehicles/" + icao, vehicle);

                // Redis Cache
                if (vehicle.getVehicleNo() != null) {
                    String redisKey = "vehicle:" + icao + ":" + vehicle.getVehicleNo();
                    redisService.set(redisKey, vehicle, 300, java.util.concurrent.TimeUnit.SECONDS);
                    redisService.addToSet("active_vehicles:" + icao, vehicle.getVehicleNo());
                }

                logger.info("Consumed vehicle from Kafka: {}", vehicle.getVehicleNo());
                vehicleRepository.save(vehicle);
            }

            // Archive to MinIO
            String icao = !vehicles.isEmpty() && vehicles.get(0).getIcaoCode() != null ? vehicles.get(0).getIcaoCode() : "VIDP";
            String timestamp = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd/HH"));
            String filename = "archives/raw/" + icao + "/" + timestamp + "/vehicle_" + java.util.UUID.randomUUID() + ".json";
            minioService.uploadJson(filename, message);

        } catch (Exception e) {
            logger.error("Error processing vehicle message: {}", message, e);
            errorCounter.increment();
        }
    }

    public List<Vehicle> getActiveVehicles(String icaoCode) {
        // Try Redis first
        String icao = icaoCode != null && !icaoCode.isEmpty() ? icaoCode : "VIDP";
        java.util.Set<Object> activeIds = redisService.getSetMembers("active_vehicles:" + icao);

        if (activeIds != null && !activeIds.isEmpty()) {
            List<Vehicle> vehicles = new java.util.ArrayList<>();
            for (Object idObj : activeIds) {
                String id = (String) idObj;
                java.util.Optional<Vehicle> vehicleOpt = redisService.get("vehicle:" + icao + ":" + id, Vehicle.class);
                vehicleOpt.ifPresent(vehicles::add);
            }
            if (!vehicles.isEmpty()) {
                return vehicles;
            }
        }

        // Fallback to DB
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        if (icaoCode != null && !icaoCode.isEmpty()) {
            return vehicleRepository.findLatestVehiclesByIcao(fiveMinutesAgo, icaoCode);
        }
        return vehicleRepository.findLatestVehicles(fiveMinutesAgo);
    }
}
