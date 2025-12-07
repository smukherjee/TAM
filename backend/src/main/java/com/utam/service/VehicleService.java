package com.utam.service;

import com.utam.model.Vehicle;
import com.utam.repository.VehicleRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    @KafkaListener(topics = "vehicle-events", groupId = "utam-group")
    public void consumeVehicleEvent(Vehicle vehicle) {
        vehicleRepository.save(vehicle);
    }

    public List<Vehicle> getActiveVehicles() {
        // Get vehicles from the last 5 minutes
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        return vehicleRepository.findLatestVehicles(fiveMinutesAgo);
    }
}

