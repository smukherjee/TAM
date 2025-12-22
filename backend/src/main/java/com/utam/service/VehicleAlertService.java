package com.utam.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.utam.model.VehicleAlert;
import com.utam.model.Vehicle;
import com.utam.repository.VehicleAlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class VehicleAlertService {

    private static final Logger log = LoggerFactory.getLogger(VehicleAlertService.class);
    private final VehicleAlertRepository alertRepository;
    private final ObjectMapper objectMapper;

    public VehicleAlertService(VehicleAlertRepository alertRepository, ObjectMapper objectMapper) {
        this.alertRepository = alertRepository;
        this.objectMapper = objectMapper;
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @KafkaListener(topics = "vehicle-raw-json", groupId = "utam-vehicle-alert-group")
    public void checkVehicleAlert(String message) {
        try {
            List<Vehicle> vehicles;
            if (message.trim().startsWith("[")) {
                vehicles = Arrays.asList(objectMapper.readValue(message, Vehicle[].class));
            } else {
                vehicles = Collections.singletonList(objectMapper.readValue(message, Vehicle.class));
            }

            for (Vehicle vehicle : vehicles) {
                // Simple rule: Speed > 70 km/h is a violation
                if (vehicle.getSpeed() != null && vehicle.getSpeed() > 70.0) {
                    VehicleAlert alert = new VehicleAlert();
                    alert.setAlertId(UUID.randomUUID());
                    alert.setType("SPEED_VIOLATION");
                    alert.setEntityId(vehicle.getVehicleNo());
                    alert.setValue(vehicle.getSpeed());
                    alert.setTimestamp(LocalDateTime.now());
                    alert.setLatitude(vehicle.getLatitude());
                    alert.setLongitude(vehicle.getLongitude());
                    alert.setIcaoCode(vehicle.getIcaoCode());

                    alertRepository.save(alert);
                    log.warn("Speed Violation Detected: {} at {} km/h", vehicle.getVehicleNo(), vehicle.getSpeed());
                }
            }
        } catch (Exception e) {
            log.error("Error processing vehicle alert message: {}", e.getMessage());
        }
    }

    public List<VehicleAlert> getAllAlerts(String icaoCode) {
        if (icaoCode != null && !icaoCode.isEmpty()) {
            return alertRepository.findTop10ByIcaoCodeOrderByTimestampDesc(icaoCode);
        }
        return alertRepository.findTop10ByOrderByTimestampDesc();
    }
}
