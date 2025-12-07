package com.utam.service;

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

    public AlertService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @KafkaListener(topics = "vehicle-raw-json", groupId = "utam-alert-group")
    public void checkVehicleAlert(Vehicle vehicle) {
        // Simple rule: Speed > 70 km/h is a violation
        if (vehicle.getSpeed() > 70.0) {
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
    }

    public List<Alert> getAllAlerts() {
        return alertRepository.findAll();
    }
}
