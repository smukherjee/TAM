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
        // specific rule for delay
        if (session.getDelayMinutes() != null && session.getDelayMinutes() > 0) {
            // Check if active alert already exists for this session and type DELAY
            boolean alertExists = alertRepository.findBySessionIdAndTypeAndIsActiveTrue(session.getId(), "DELAY")
                    .isPresent();

            if (!alertExists) {
                Alert alert = new Alert();
                alert.setId(UUID.randomUUID());
                alert.setTenantCode(session.getTenantCode());
                alert.setSession(session);
                alert.setSeverity(getSeverity(session.getDelayMinutes()));
                alert.setType("DELAY");
                alert.setMessage("Turnaround delayed by " + session.getDelayMinutes() + " minutes. Reason: "
                        + (session.getDelayReason() != null ? session.getDelayReason() : "Unknown"));
                alert.setTimestamp(ZonedDateTime.now());
                alert.setIsActive(true);

                alertRepository.save(alert);
                System.out.println("⚠️ Generated Alert for Session " + session.getId());
            }
        }
    }

    private String getSeverity(int delayMinutes) {
        if (delayMinutes >= 30)
            return "CRITICAL";
        if (delayMinutes >= 20)
            return "HIGH";
        if (delayMinutes >= 10)
            return "MEDIUM";
        return "LOW";
    }
}
