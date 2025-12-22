package com.utam.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.utam.model.Flight;
import com.utam.turnaround.service.TurnaroundService;
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
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;
    private final RedisService redisService;
    private final MinioService minioService;
    private final TurnaroundService turnaroundService;

    public FlightConsumer(FlightService flightService, ObjectMapper objectMapper,
            io.micrometer.core.instrument.MeterRegistry registry,
            org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate,
            RedisService redisService,
            MinioService minioService,
            TurnaroundService turnaroundService) {
        this.flightService = flightService;
        this.objectMapper = objectMapper;
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.messagingTemplate = messagingTemplate;
        this.redisService = redisService;
        this.minioService = minioService;
        this.turnaroundService = turnaroundService;

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

                // WebSocket Push
                String icao = flight.getIcaoCode() != null ? flight.getIcaoCode() : "VIDP";
                messagingTemplate.convertAndSend("/topic/flights/" + icao, flight);

                // Redis Cache
                if (flight.getCallsign() != null) {
                    String redisKey = "flight:" + icao + ":" + flight.getCallsign();
                    redisService.set(redisKey, flight, 300, java.util.concurrent.TimeUnit.SECONDS);
                    redisService.addToSet("active_flights:" + icao, flight.getCallsign());
                }
            }

            flightService.saveAll(flights);

            // Process Turnaround Sessions
            for (Flight flight : flights) {
                try {
                    turnaroundService.processFlightUpdate(flight);
                } catch (Exception e) {
                    logger.error("Error processing turnaround session for flight {}: {}", flight.getCallsign(), e.getMessage());
                }
            }

            // Archive to MinIO
            String icao = !flights.isEmpty() && flights.get(0).getIcaoCode() != null ? flights.get(0).getIcaoCode() : "VIDP";
            String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd/HH"));
            String filename = "archives/raw/" + icao + "/" + timestamp + "/flight_" + java.util.UUID.randomUUID() + ".json";
            minioService.uploadJson(filename, message);

        } catch (Exception e) {
            logger.error("Error processing flight message: {}", message, e);
            errorCounter.increment();
        }
    }
}
