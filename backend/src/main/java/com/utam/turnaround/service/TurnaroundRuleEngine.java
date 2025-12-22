package com.utam.turnaround.service;

import com.utam.turnaround.domain.Alert;
import com.utam.turnaround.domain.TurnaroundSession;
import com.utam.turnaround.repository.AlertRepository;
import org.springframework.stereotype.Service;
import java.util.UUID;
import java.time.ZonedDateTime;

@Service
public class TurnaroundRuleEngine {

    private final AlertRepository alertRepository;

    public TurnaroundRuleEngine(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    public void evaluate(TurnaroundSession session) {
        // Simple rule: If status is ON_BLOCK, generate a mock alert for demonstration
        if ("ON_BLOCK".equals(session.getStatus())) {
            Alert alert = new Alert();
            alert.setId(UUID.randomUUID());
            alert.setIcaoCode(session.getIcaoCode());
            alert.setSession(session);
            alert.setSeverity("MEDIUM");
            alert.setType("PROCESS_DELAY");
            alert.setMessage("Turnaround started but no tasks active.");
            alert.setTimestamp(ZonedDateTime.now());
            alert.setIsActive(true);
            
            alertRepository.save(alert);
        }
    }
}
