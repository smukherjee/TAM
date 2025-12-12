package com.utam.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.utam.model.Flight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class FlightConsumer {

    private static final Logger logger = LoggerFactory.getLogger(FlightConsumer.class);
    private final FlightService flightService;
    private final ObjectMapper objectMapper;

    public FlightConsumer(FlightService flightService, ObjectMapper objectMapper) {
        this.flightService = flightService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "flight-raw-json", groupId = "utam-group")
    public void consume(String message) {
        try {
            // NiFi sends an array of objects
            List<Flight> flights = Arrays.asList(objectMapper.readValue(message, Flight[].class));
            logger.info("Received {} flights from Kafka", flights.size());
            flightService.saveAll(flights);
        } catch (Exception e) {
            logger.error("Error processing flight message: {}", message, e);
        }
    }
}
