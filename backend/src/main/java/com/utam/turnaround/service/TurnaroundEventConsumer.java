package com.utam.turnaround.service;

import com.utam.turnaround.domain.TurnaroundSession;
import com.utam.turnaround.repository.TurnaroundSessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.UUID;
import java.time.ZonedDateTime;

@Service
public class TurnaroundEventConsumer {

    private final TurnaroundSessionRepository sessionRepository;
    private final TurnaroundRuleEngine ruleEngine;
    private final ObjectMapper objectMapper;

    public TurnaroundEventConsumer(TurnaroundSessionRepository sessionRepository, TurnaroundRuleEngine ruleEngine, ObjectMapper objectMapper) {
        this.sessionRepository = sessionRepository;
        this.ruleEngine = ruleEngine;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "turnaround-events", groupId = "turnaround-group")
    public void consume(String message) {
        try {
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            Map<String, Object> payload = (Map<String, Object>) event.get("payload");
            String icaoCode = (String) event.get("icaoCode");
            
            String flightId = (String) payload.get("flightId");
            String standId = (String) payload.get("standId");
            String status = (String) payload.get("status");

            TurnaroundSession session = new TurnaroundSession();
            session.setId(UUID.randomUUID());
            session.setIcaoCode(icaoCode);
            session.setFlightId(flightId);
            session.setStandId(standId);
            session.setStatus(status);
            session.setCreatedAt(ZonedDateTime.now());
            session.setUpdatedAt(ZonedDateTime.now());
            
            session = sessionRepository.save(session);
            
            ruleEngine.evaluate(session);
            
        } catch (Exception e) {
            System.err.println("Error processing turnaround event: " + e.getMessage());
        }
    }
}
