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

    public TurnaroundService(TurnaroundEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @KafkaListener(topics = "turnaround-raw-json", groupId = "utam-group")
    public void consumeTurnaroundEvent(String message) {
        try {
            List<TurnaroundEvent> events;
            if (message.trim().startsWith("[")) {
                events = Arrays.asList(objectMapper.readValue(message, TurnaroundEvent[].class));
            } else {
                events = Collections.singletonList(objectMapper.readValue(message, TurnaroundEvent.class));
            }

            for (TurnaroundEvent event : events) {
                repository.save(event);
            }
            logger.debug("Consumed {} turnaround events", events.size());
        } catch (Exception e) {
            logger.error("Error processing turnaround message: {}", message, e);
        }
    }

    public List<TurnaroundEvent> getAllEvents() {
        return repository.findAll();
    }
}
