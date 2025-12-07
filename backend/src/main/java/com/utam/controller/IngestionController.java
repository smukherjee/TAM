package com.utam.controller;

import com.utam.model.Flight;
import com.utam.model.Vehicle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class IngestionController {

    private static final Logger log = LoggerFactory.getLogger(IngestionController.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public IngestionController(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostMapping("/api/adsblivedata")
    public void ingestFlightData(@RequestBody List<Flight> flights) {
        log.info("Received {} flight records via HTTP ingestion", flights.size());
        for (Flight flight : flights) {
            kafkaTemplate.send("flight-events", flight.getCallsign(), flight);
        }
    }

    @PostMapping("/api/veh_live_data_con")
    public void ingestVehicleData(@RequestBody List<Vehicle> vehicles) {
        log.info("Received {} vehicle records via HTTP ingestion", vehicles.size());
        for (Vehicle vehicle : vehicles) {
            kafkaTemplate.send("vehicle-events", vehicle.getVehicleNo(), vehicle);
        }
    }
}
