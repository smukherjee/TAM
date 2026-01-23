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
import java.util.ArrayList;
import java.util.List;
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
            // Topic contains mixed schemas (camera CV events + flight turnaround events).
            // Handle arrays (camera CV batches) without throwing; only process events that carry flight_id.
            if (message == null || message.isBlank()) {
                return;
            }

            if (message.trim().startsWith("[")) {
                List<Map<String, Object>> events = objectMapper.readValue(message, List.class);
                for (Map<String, Object> event : events) {
                    processOne(event);
                }
            } else {
                Map<String, Object> event = objectMapper.readValue(message, Map.class);
                processOne(event);
            }

        } catch (Exception e) {
            logger.error("Error processing CV event: {}", message, e);
        }
    }

    private void processOne(Map<String, Object> event) {
        if (event == null || event.isEmpty()) {
            return;
        }

        String flightId = (String) (event.containsKey("flight_id") ? event.get("flight_id") : event.get("flightId"));
        String eventType = (String) (event.containsKey("event_type") ? event.get("event_type") : event.get("eventType"));
        String timestampStr = (String) event.getOrDefault("timestamp", event.get("eventTimeStamp"));

        // Prefer explicit tenant_code; fall back to icao_code for now (VIDP/LIRN/YBBN), else default.
        String tenantCode = (String) event.getOrDefault("tenant_code", event.getOrDefault("icao_code", "VIDP"));

        // Optional stand fields; default to A1 for demo data.
        String standId = (String) event.getOrDefault("stand", event.getOrDefault("stand_id", "A1"));

        if (flightId == null || flightId.isBlank()) {
            // Camera events don't have flight_id; ignore quietly to avoid log spam.
            return;
        }

        logger.info("Received CV event for flight {}: {}", flightId, event);

        Optional<TurnaroundSession> sessionOpt = sessionRepository.findByFlightIdAndTenantCode(flightId, tenantCode);
        TurnaroundSession session;
        if (sessionOpt.isPresent()) {
            session = sessionOpt.get();
        } else {
            // Auto-create a session so turnaround data can be generated from CV events alone.
            session = new TurnaroundSession();
            session.setId(UUID.randomUUID());
            session.setTenantCode(tenantCode);
            session.setFlightId(flightId);
            session.setStandId(standId);
            session.setStatus("SCHEDULED");
            session.setCreatedAt(ZonedDateTime.now());
            session.setUpdatedAt(ZonedDateTime.now());
            session = sessionRepository.save(session);
            logger.info("Created new turnaround session {} for flight {} (tenant={})", session.getId(), flightId, tenantCode);
        }

        updateSessionWithEvent(session, eventType, timestampStr);
        session.setUpdatedAt(ZonedDateTime.now());
        sessionRepository.save(session);
    }

    private void updateSessionWithEvent(TurnaroundSession session, String eventType, String timestampStr) {
        if (eventType == null || timestampStr == null) {
            return;
        }
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
        if (session.getTasks() == null) {
            session.setTasks(new ArrayList<>());
        }
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
            task.setTenantCode(session.getTenantCode()); // Set Tenant code
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
