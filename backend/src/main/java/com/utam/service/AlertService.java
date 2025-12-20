package com.utam.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.utam.model.Alert;
import com.utam.model.Vehicle;
import com.utam.repository.AlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);
    private final AlertRepository alertRepository;
    private final ObjectMapper objectMapper;

    public AlertService(AlertRepository alertRepository, ObjectMapper objectMapper) {
        this.alertRepository = alertRepository;
        this.objectMapper = objectMapper;
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @KafkaListener(topics = "vehicle-raw-json", groupId = "utam-alert-group")
    public void checkVehicleAlert(String message) {
        try {
            Vehicle vehicle = objectMapper.readValue(message, Vehicle.class);
            // Simple rule: Speed > 70 km/h is a violation
            if (vehicle.getSpeed() != null && vehicle.getSpeed() > 70.0) {
                Alert alert = new Alert();
                alert.setAlertId(UUID.randomUUID());
                alert.setType("SPEED_VIOLATION");
                alert.setEntityId(vehicle.getVehicleNo());
                alert.setValue(vehicle.getSpeed());
                alert.setTimestamp(LocalDateTime.now());
                alert.setLatitude(vehicle.getLatitude());
                alert.setLongitude(vehicle.getLongitude());

                alertRepository.save(alert);
                log.warn("Speed Violation Detected: {} at {} km/h", vehicle.getVehicleNo(), vehicle.getSpeed());
            }
        } catch (Exception e) {
            log.error("Error processing vehicle alert message: {}", e.getMessage());
        }
    }

    public List<Alert> getAllAlerts() {
        return alertRepository.findAll();
    }
}
