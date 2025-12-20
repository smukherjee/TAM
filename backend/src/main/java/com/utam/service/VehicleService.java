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
import java.util.List;

@Service
public class VehicleService {

    private static final Logger logger = LoggerFactory.getLogger(VehicleService.class);
    private final VehicleRepository vehicleRepository;
    private final ObjectMapper objectMapper;

    public VehicleService(VehicleRepository vehicleRepository, ObjectMapper objectMapper) {
        this.vehicleRepository = vehicleRepository;
        this.objectMapper = objectMapper;
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @KafkaListener(topics = "vehicle-raw-json", groupId = "utam-group")
    public void consumeVehicleEvent(String message) {
        try {
            Vehicle vehicle = objectMapper.readValue(message, Vehicle.class);
            // Ensure timestamp is set if missing
            if (vehicle.getTimestamp() == null) {
                vehicle.setTimestamp(LocalDateTime.now());
            }
            logger.info("Consumed vehicle from Kafka: {}", vehicle.getVehicleNo());
            vehicleRepository.save(vehicle);
        } catch (Exception e) {
            logger.error("Error processing vehicle message: {}", message, e);
        }
    }

    public List<Vehicle> getActiveVehicles() {
        // Get vehicles from the last 5 minutes
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        return vehicleRepository.findLatestVehicles(fiveMinutesAgo);
    }
}
