package com.utam.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.utam.model.Flight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class FlightConsumer {

    private static final Logger logger = LoggerFactory.getLogger(FlightConsumer.class);
    private final FlightService flightService;
    private final ObjectMapper objectMapper;
    private final io.micrometer.core.instrument.Timer latencyTimer;
    private final io.micrometer.core.instrument.Counter errorCounter;

    public FlightConsumer(FlightService flightService, ObjectMapper objectMapper,
            io.micrometer.core.instrument.MeterRegistry registry) {
        this.flightService = flightService;
        // Configure ObjectMapper for flexible JSON parsing
        this.objectMapper = objectMapper;
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        this.latencyTimer = io.micrometer.core.instrument.Timer.builder("pipeline.latency.seconds")
                .tag("type", "flight")
                .description("End-to-end latency for flight events")
                .register(registry);

        this.errorCounter = io.micrometer.core.instrument.Counter.builder("pipeline.events.failed")
                .tag("type", "flight")
                .description("Number of failed flight events")
                .register(registry);
    }

    @KafkaListener(topics = "flight-raw-json", groupId = "utam-group")
    public void consume(String message) {
        try {
            List<Flight> flights;
            // Check if it's an array or single object
            if (message.trim().startsWith("[")) {
                flights = Arrays.asList(objectMapper.readValue(message, Flight[].class));
            } else {
                Flight flight = objectMapper.readValue(message, Flight.class);
                flights = Collections.singletonList(flight);
            }
            logger.info("Received {} flights from Kafka", flights.size());

            for (Flight flight : flights) {
                if (flight.getCreationTimestamp() != null) {
                    long latency = System.currentTimeMillis() - flight.getCreationTimestamp();
                    latencyTimer.record(java.time.Duration.ofMillis(Math.max(0, latency)));
                }
            }

            flightService.saveAll(flights);
        } catch (Exception e) {
            logger.error("Error processing flight message: {}", message, e);
            errorCounter.increment();
        }
    }
}
