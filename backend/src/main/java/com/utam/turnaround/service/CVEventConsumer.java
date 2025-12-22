package com.utam.turnaround.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.utam.turnaround.domain.TurnaroundSession;
import com.utam.turnaround.domain.TurnaroundTask;
import com.utam.turnaround.repository.TurnaroundSessionRepository;
import com.utam.turnaround.repository.TurnaroundTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class CVEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(CVEventConsumer.class);
    private final TurnaroundSessionRepository sessionRepository;
    private final TurnaroundTaskRepository taskRepository;
    private final ObjectMapper objectMapper;

    public CVEventConsumer(TurnaroundSessionRepository sessionRepository, 
                           TurnaroundTaskRepository taskRepository,
                           ObjectMapper objectMapper) {
        this.sessionRepository = sessionRepository;
        this.taskRepository = taskRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "turnaround-raw-json", groupId = "turnaround-cv-group")
    @Transactional
    public void consume(String message) {
        try {
            // Assuming message is JSON for now, even if topic says avro (NiFi publishes JSON)
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            logger.info("Received CV event: {}", event);

            String flightId = (String) event.get("flight_id");
            String eventType = (String) event.get("event_type"); // e.g., "bridge_connect", "baggage_start"
            String timestampStr = (String) event.get("timestamp");
            String icaoCode = (String) event.getOrDefault("icao_code", "VIDP");

            if (flightId == null) {
                logger.warn("CV event missing flight_id: {}", message);
                return;
            }

            Optional<TurnaroundSession> sessionOpt = sessionRepository.findByFlightIdAndIcaoCode(flightId, icaoCode);
            if (sessionOpt.isEmpty()) {
                logger.warn("No active session found for flight {}", flightId);
                return;
            }

            TurnaroundSession session = sessionOpt.get();
            updateSessionWithEvent(session, eventType, timestampStr);
            sessionRepository.save(session);

        } catch (Exception e) {
            logger.error("Error processing CV event: {}", message, e);
        }
    }

    private void updateSessionWithEvent(TurnaroundSession session, String eventType, String timestampStr) {
        ZonedDateTime timestamp = ZonedDateTime.parse(timestampStr); // Ensure format matches

        // Map CV events to Tasks
        String taskType = mapEventToTaskType(eventType);
        if (taskType != null) {
            updateTask(session, taskType, eventType, timestamp);
        }

        // Update Session Milestones
        if ("bridge_connect".equalsIgnoreCase(eventType)) {
            session.setAibt(timestamp);
            session.setStatus("ON_BLOCK");
        } else if ("pushback_start".equalsIgnoreCase(eventType)) {
            session.setAobt(timestamp);
            session.setStatus("OFF_BLOCK");
        }
    }

    private String mapEventToTaskType(String eventType) {
        switch (eventType.toLowerCase()) {
            case "bridge_connect": return "DISEMBARKATION"; // Start of disembarkation
            case "baggage_first": return "UNLOADING";
            case "fuel_truck_connect": return "FUELING";
            case "catering_truck_connect": return "CATERING";
            case "cleaner_enter": return "CLEANING";
            default: return null;
        }
    }

    private void updateTask(TurnaroundSession session, String taskType, String eventType, ZonedDateTime timestamp) {
        Optional<TurnaroundTask> taskOpt = session.getTasks().stream()
                .filter(t -> t.getTaskType().equals(taskType))
                .findFirst();

        TurnaroundTask task;
        if (taskOpt.isPresent()) {
            task = taskOpt.get();
        } else {
            task = new TurnaroundTask();
            task.setId(UUID.randomUUID());
            task.setSession(session);
            task.setIcaoCode(session.getIcaoCode()); // Set ICAO code
            task.setTaskType(taskType);
            task.setStatus("IN_PROGRESS");
            task.setPlannedStart(session.getSirt() != null ? session.getSirt() : ZonedDateTime.now()); 
            task.setPlannedEnd(session.getSirt() != null ? session.getSirt().plusMinutes(30) : ZonedDateTime.now().plusMinutes(30));
            session.getTasks().add(task);
        }

        // Logic to determine start vs end based on eventType suffix or specific events
        // For simplicity, assuming these events mark the START of the task
        if (task.getActualStart() == null) {
            task.setActualStart(timestamp);
            task.setStatus("IN_PROGRESS");
        } else {
            // If start exists, maybe this is the end? 
            // Need more specific events like "baggage_last" or "fuel_truck_disconnect"
            // For now, just logging start.
        }
        
        taskRepository.save(task);
    }
}
