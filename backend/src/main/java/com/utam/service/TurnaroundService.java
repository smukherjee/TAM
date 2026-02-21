package com.utam.service;

import com.utam.model.TurnaroundEvent;
import com.utam.repository.TurnaroundEventRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;

@Service
public class TurnaroundService {

    private static final Logger logger = LoggerFactory.getLogger(TurnaroundService.class);
    private final TurnaroundEventRepository repository;
    private final ObjectMapper objectMapper;
    private final io.micrometer.core.instrument.Counter consumedCounter;
    private final io.micrometer.core.instrument.Timer latencyTimer;
    private final io.micrometer.core.instrument.Counter errorCounter;

    public TurnaroundService(TurnaroundEventRepository repository, ObjectMapper objectMapper,
            io.micrometer.core.instrument.MeterRegistry registry) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.registerModule(new JavaTimeModule());
        this.consumedCounter = io.micrometer.core.instrument.Counter.builder("kafka.events.consumed")
                .tag("type", "turnaround")
                .description("Number of turnaround events consumed from Kafka")
                .register(registry);

        this.latencyTimer = io.micrometer.core.instrument.Timer.builder("pipeline.latency.seconds")
                .tag("type", "turnaround")
                .description("End-to-end latency for turnaround events")
                .register(registry);

        this.errorCounter = io.micrometer.core.instrument.Counter.builder("pipeline.events.failed")
                .tag("type", "turnaround")
                .description("Number of failed turnaround events")
                .register(registry);
    }

    @KafkaListener(topics = "turnaround-raw-json", groupId = "utam-turnaround-group")
    public void consumeTurnaroundEvent(String message) {
        try {
            List<TurnaroundEvent> events;
            if (message.trim().startsWith("[")) {
                events = Arrays.asList(objectMapper.readValue(message, TurnaroundEvent[].class));
            } else {
                events = Collections.singletonList(objectMapper.readValue(message, TurnaroundEvent.class));
            }

            logger.info("Received Raw Turnaround JSON: {}", message); // DEBUG

            int persisted = 0;
            for (TurnaroundEvent event : events) {
                if (event.getEventUniqueId() == null || event.getEventUniqueId().isBlank()) {
                    logger.debug("Skipping turnaround payload without event_unique_id");
                    continue;
                }
                if (event.getActivityType() == null || event.getActivityType().isBlank()) {
                    logger.debug("Skipping turnaround payload {} without activity_type", event.getEventUniqueId());
                    continue;
                }
                if (event.getTenantCode() == null || event.getTenantCode().isBlank()) {
                    logger.debug("Skipping turnaround payload {} without tenant_code/icao_code", event.getEventUniqueId());
                    continue;
                }

                logger.info("Saving Event: {}, Tenant: {}", event.getActivityType(), event.getTenantCode()); // DEBUG

                if (event.getCreationTimestamp() != null) {
                    long latency = System.currentTimeMillis() - event.getCreationTimestamp();
                    latencyTimer.record(java.time.Duration.ofMillis(Math.max(0, latency)));
                }

                repository.save(event);
                consumedCounter.increment();
                persisted++;
            }
            logger.debug("Consumed {} turnaround events", persisted);
        } catch (Exception e) {
            logger.error("Error processing turnaround message: {}", message, e);
            errorCounter.increment();
        }
    }

    public List<TurnaroundEvent> getAllEvents(String tenantCode) {
        // Return events from last 24 hours to ensure data visibility during demos/dev
        java.time.LocalDateTime since = java.time.LocalDateTime.now().minusHours(24);

        if (tenantCode != null && !tenantCode.isEmpty()) {
            return repository.findRecentByTenantCode(tenantCode, since);
        }
        return repository.findRecentEvents(since);
    }
}
