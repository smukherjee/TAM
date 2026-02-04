package com.utam.simulation.alert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Enriches alerts with severity, recommendations, and context.
 * Implements FR-064 to FR-068: Extensive alert generation.
 */
@Service
public class AlertEnricher {

    @SuppressWarnings("unused") // Reserved for detailed enrichment logging
    private static final Logger log = LoggerFactory.getLogger(AlertEnricher.class);

    private final FinancialImpactCalculator financialImpactCalculator;

    // Recommendation templates by alert type
    private static final Map<String, List<String>> RECOMMENDATIONS = Map.of(
            "DELAY", List.of(
                    "Dispatch additional ground support vehicles",
                    "Notify gate agent of expected delay",
                    "Consider reassigning boarding gate if delay exceeds 30 minutes",
                    "Alert connecting passengers of potential missed connections"
            ),
            "GEOFENCE", List.of(
                    "Verify vehicle GPS accuracy",
                    "Contact vehicle operator immediately",
                    "Review vehicle assignment and route",
                    "Check for unauthorized access attempt"
            ),
            "MAINTENANCE", List.of(
                    "Schedule maintenance during next idle period",
                    "Prepare backup vehicle for reassignment",
                    "Update maintenance logs in fleet system",
                    "Notify fleet manager for critical equipment"
            ),
            "SLA_BREACH", List.of(
                    "Escalate to operations manager",
                    "Document incident for customer report",
                    "Identify root cause for process improvement",
                    "Prepare compensation documentation"
            )
    );

    // Severity thresholds
    private static final int CRITICAL_DELAY_MINUTES = 45;
    private static final int HIGH_DELAY_MINUTES = 30;
    private static final int MEDIUM_DELAY_MINUTES = 15;

    public AlertEnricher(FinancialImpactCalculator financialImpactCalculator) {
        this.financialImpactCalculator = financialImpactCalculator;
    }

    /**
     * Enrich an alert with additional context.
     */
    public Alert enrich(Alert alert) {
        // Calculate severity if not set
        if (alert.getSeverity() == null || alert.getSeverity().isEmpty()) {
            alert.setSeverity(calculateSeverity(alert));
        }

        // Add financial impact
        enrichWithFinancialImpact(alert);

        // Add recommendations
        enrichWithRecommendations(alert);

        // Add time context
        enrichWithTimeContext(alert);

        return alert;
    }

    /**
     * Calculate severity based on alert type and details.
     */
    public String calculateSeverity(Alert alert) {
        String alertType = alert.getAlertType();
        
        return switch (alertType) {
            case "DELAY" -> calculateDelaySeverity(alert);
            case "GEOFENCE" -> "HIGH"; // Always high for security
            case "MAINTENANCE" -> calculateMaintenanceSeverity(alert);
            case "SLA_BREACH" -> "HIGH"; // SLA breaches are always significant
            default -> "MEDIUM";
        };
    }

    /**
     * Get recommended actions for an alert.
     */
    public List<String> getRecommendations(Alert alert) {
        List<String> recs = RECOMMENDATIONS.getOrDefault(alert.getAlertType(), 
                List.of("Review alert details and take appropriate action"));
        
        // Filter recommendations based on severity
        if ("LOW".equals(alert.getSeverity()) && recs.size() > 2) {
            return recs.subList(0, 2);
        }
        
        return new ArrayList<>(recs);
    }

    /**
     * Calculate time remaining to resolve before escalation.
     */
    public int calculateTimeToEscalation(Alert alert) {
        // Based on severity, calculate when alert should escalate
        return switch (alert.getSeverity()) {
            case "CRITICAL" -> 5; // 5 minutes
            case "HIGH" -> 15; // 15 minutes
            case "MEDIUM" -> 30; // 30 minutes
            default -> 60; // 1 hour
        };
    }

    /**
     * Generate alert summary for display.
     */
    public String generateSummary(Alert alert) {
        StringBuilder summary = new StringBuilder();
        summary.append(alert.getAlertType()).append(" Alert: ");
        summary.append(alert.getMessage());
        
        if (alert.getFinancialImpact() != null) {
            summary.append(String.format(" | Impact: $%.2f", alert.getFinancialImpact()));
        }
        
        summary.append(" | Severity: ").append(alert.getSeverity());
        
        return summary.toString();
    }

    /**
     * Determine if alert should trigger notification.
     */
    public boolean shouldNotify(Alert alert) {
        // Always notify for CRITICAL and HIGH
        if ("CRITICAL".equals(alert.getSeverity()) || "HIGH".equals(alert.getSeverity())) {
            return true;
        }
        
        // Notify for delays over threshold
        if ("DELAY".equals(alert.getAlertType())) {
            return extractDelayMinutes(alert.getMessage()) >= MEDIUM_DELAY_MINUTES;
        }
        
        // Always notify for geofence violations
        return "GEOFENCE".equals(alert.getAlertType());
    }

    private String calculateDelaySeverity(Alert alert) {
        int delayMinutes = extractDelayMinutes(alert.getMessage());
        
        if (delayMinutes >= CRITICAL_DELAY_MINUTES) {
            return "CRITICAL";
        } else if (delayMinutes >= HIGH_DELAY_MINUTES) {
            return "HIGH";
        } else if (delayMinutes >= MEDIUM_DELAY_MINUTES) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    private String calculateMaintenanceSeverity(Alert alert) {
        String message = alert.getMessage();
        if (message != null) {
            if (message.contains("critical") || message.contains("safety")) {
                return "CRITICAL";
            }
            if (message.contains("overdue") || message.contains("required")) {
                return "HIGH";
            }
        }
        return "MEDIUM";
    }

    private void enrichWithFinancialImpact(Alert alert) {
        FinancialImpactCalculator.FinancialImpact impact = 
                financialImpactCalculator.calculateAlertImpact(alert);
        
        alert.setFinancialImpact(impact.getTotalImpact().doubleValue());
    }

    private void enrichWithRecommendations(Alert alert) {
        List<String> recommendations = getRecommendations(alert);
        if (!recommendations.isEmpty()) {
            alert.setRecommendation(recommendations.get(0));
        }
    }

    private void enrichWithTimeContext(Alert alert) {
        // Add resolution deadline
        int timeToEscalation = calculateTimeToEscalation(alert);
        Instant deadline = Instant.now().plusSeconds(timeToEscalation * 60L);
        alert.setEscalationDeadline(deadline);
    }

    private int extractDelayMinutes(String message) {
        if (message == null) {
            return 0;
        }
        
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(\\d+)\\s*minute")
                .matcher(message);
        
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        
        return 0;
    }
}
