package com.utam.turnaround.service;

import com.utam.turnaround.domain.TurnaroundSession;
import com.utam.turnaround.domain.TurnaroundTask;
import com.utam.turnaround.dto.TaskSummaryDTO;
import com.utam.turnaround.dto.TurnaroundSessionDetailDTO;
import com.utam.turnaround.dto.TaskDetailDTO;
import com.utam.turnaround.dto.TurnaroundSessionSummaryDTO;
import com.utam.turnaround.repository.TurnaroundSessionRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TurnaroundService {

    private final TurnaroundSessionRepository sessionRepository;

    public TurnaroundService(TurnaroundSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    public List<TurnaroundSessionSummaryDTO> getAllSessions(String tenantCode) {
        List<TurnaroundSession> sessions = sessionRepository.findByTenantCode(tenantCode);
        return sessions.stream()
                .map(this::convertToSummaryDTO)
                .collect(Collectors.toList());
    }

    public TurnaroundSessionDetailDTO getSessionDetails(UUID id, String tenantCode) {
        TurnaroundSession session = sessionRepository.findByIdAndTenantCode(id, tenantCode)
                .orElseThrow(() -> new RuntimeException("Session not found or access denied"));
        return convertToDetailDTO(session);
    }

    @org.springframework.transaction.annotation.Transactional
    public void processFlightUpdate(com.utam.model.Flight flight) {
        String tenantCode = flight.getTenantCode() != null ? flight.getTenantCode() : "VIDP";
        String flightId = flight.getCallsign();
        
        if (flightId == null) return;

        java.util.Optional<TurnaroundSession> existingSession = sessionRepository.findByFlightIdAndTenantCode(flightId, tenantCode);
        
        if (existingSession.isEmpty()) {
            TurnaroundSession session = new TurnaroundSession();
            session.setId(UUID.randomUUID());
            session.setTenantCode(tenantCode);
            session.setFlightId(flightId);
            session.setStatus("SCHEDULED");
            session.setCreatedAt(java.time.ZonedDateTime.now());
            session.setUpdatedAt(java.time.ZonedDateTime.now());
            session.setStandId("A1"); // Default stand
            
            sessionRepository.save(session);
        }
    }

    private TurnaroundSessionSummaryDTO convertToSummaryDTO(TurnaroundSession session) {
        TurnaroundSessionSummaryDTO dto = new TurnaroundSessionSummaryDTO();
        dto.setId(session.getId());
        dto.setFlightId(session.getFlightId());
        dto.setStandId(session.getStandId());
        dto.setStatus(session.getStatus());
        
        if (session.getTasks() != null) {
            dto.setTasks(session.getTasks().stream()
                    .map(this::convertToTaskSummaryDTO)
                    .collect(Collectors.toList()));
        }
        
        return dto;
    }

    private TaskSummaryDTO convertToTaskSummaryDTO(TurnaroundTask task) {
        return new TaskSummaryDTO(task.getTaskType(), task.getStatus());
    }

    private TurnaroundSessionDetailDTO convertToDetailDTO(TurnaroundSession session) {
        List<TaskDetailDTO> taskDTOs = session.getTasks().stream()
                .map(this::convertToTaskDetailDTO)
                .collect(Collectors.toList());

        return new TurnaroundSessionDetailDTO(
                session.getId(),
                session.getFlightId(),
                session.getTenantCode(),
                session.getStandId(),
                session.getStatus(),
                session.getSirt(),
                session.getEibt(),
                session.getAibt(),
                session.getTobt(),
                session.getTsat(),
                session.getAobt(),
                taskDTOs
        );
    }

    private TaskDetailDTO convertToTaskDetailDTO(TurnaroundTask task) {
        return new TaskDetailDTO(
                task.getId(),
                task.getTaskType(),
                task.getStatus(),
                task.getPlannedStart(),
                task.getPlannedEnd(),
                task.getActualStart(),
                task.getActualEnd()
        );
    }
}
