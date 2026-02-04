package com.utam.turnaround.service;

import com.fasterxml.jackson.core.type.TypeReference;
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
            System.out.println("TurnaroundEventConsumer received message: " + message.substring(0, Math.min(200, message.length())) + "...");
            System.out.println("Message starts with quote: " + message.startsWith("\"") + ", ends with quote: " + message.endsWith("\""));
            
            // Handle double-serialized JSON from NiFi
            String actualMessage = message;
            if (message.startsWith("\"") && message.endsWith("\"")) {
                System.out.println("DOUBLE-SERIALIZED! Deserializing twice...");
                // Remove outer quotes and unescape
                actualMessage = objectMapper.readValue(message, String.class);
                System.out.println("After first deserialization: " + actualMessage.substring(0, Math.min(100, actualMessage.length())));
            }
            
            Map<String, Object> event = objectMapper.readValue(actualMessage, 
                    new TypeReference<Map<String, Object>>() {});
            @SuppressWarnings("unchecked") // Payload structure is well-defined by upstream contract
            Map<String, Object> payload = (Map<String, Object>) event.get("payload");
            String tenantCode = (String) event.getOrDefault("tenantCode", "VIDP");
            
            String flightId = (String) payload.get("flightId");
            String standId = (String) payload.get("standId");
            String status = (String) payload.get("status");
            
            System.out.println("Processing: flightId=" + flightId + ", standId=" + standId + ", status=" + status);

            TurnaroundSession session = new TurnaroundSession();
            session.setId(UUID.randomUUID());
            session.setTenantCode(tenantCode);
            session.setFlightId(flightId);
            session.setStandId(standId);
            session.setStatus(status);
            session.setCreatedAt(ZonedDateTime.now());
            session.setUpdatedAt(ZonedDateTime.now());
            
            // Parse timing fields from payload if provided, otherwise generate them
            ZonedDateTime now = ZonedDateTime.now();
            
            // Try to parse provided timestamps
            try {
                if (payload.containsKey("eibt")) {
                    session.setEibt(ZonedDateTime.parse((String) payload.get("eibt")));
                }
                if (payload.containsKey("aibt")) {
                    session.setAibt(ZonedDateTime.parse((String) payload.get("aibt")));
                }
                if (payload.containsKey("sirt")) {
                    session.setSirt(ZonedDateTime.parse((String) payload.get("sirt")));
                }
                if (payload.containsKey("tsat")) {
                    session.setTsat(ZonedDateTime.parse((String) payload.get("tsat")));
                }
                if (payload.containsKey("tobt")) {
                    session.setTobt(ZonedDateTime.parse((String) payload.get("tobt")));
                }
                if (payload.containsKey("aobt")) {
                    session.setAobt(ZonedDateTime.parse((String) payload.get("aobt")));
                }
            } catch (Exception parseEx) {
                // If parsing fails, generate timestamps based on status
                System.err.println("Failed to parse timestamps from payload, generating defaults: " + parseEx.getMessage());
            }
            
            // Generate default timestamps if not provided
            if ("SCHEDULED".equals(status)) {
                if (session.getEibt() == null) session.setEibt(now.plusMinutes(30));
                if (session.getTsat() == null) session.setTsat(now.plusMinutes(75));
                if (session.getTobt() == null) session.setTobt(now.plusMinutes(80));
            } else if ("ON_BLOCK".equals(status)) {
                if (session.getAibt() == null) session.setAibt(now);
                if (session.getEibt() == null) session.setEibt(now.minusMinutes(5));
                if (session.getSirt() == null) session.setSirt(now.plusMinutes(10));
                if (session.getTsat() == null) session.setTsat(now.plusMinutes(45));
                if (session.getTobt() == null) session.setTobt(now.plusMinutes(50));
            } else if ("OFF_BLOCK".equals(status) || "DEPARTED".equals(status)) {
                if (session.getAibt() == null) session.setAibt(now.minusMinutes(60));
                if (session.getEibt() == null) session.setEibt(now.minusMinutes(65));
                if (session.getSirt() == null) session.setSirt(now.minusMinutes(50));
                if (session.getTsat() == null) session.setTsat(now.minusMinutes(10));
                if (session.getTobt() == null) session.setTobt(now.minusMinutes(5));
                if (session.getAobt() == null) session.setAobt(now);
            }
            
            session = sessionRepository.save(session);
            System.out.println("✅ Saved turnaround session: " + session.getId() + " for flight " + flightId + " status=" + status);
            
            ruleEngine.evaluate(session);
            
        } catch (Exception e) {
            System.err.println("Error processing turnaround event: " + e.getMessage());
        }
    }
}
