package com.utam.simulation.alert;

import com.utam.simulation.turnaround.Turnaround;
import com.utam.simulation.turnaround.TurnaroundRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Analyzes network cascade impact of delays.
 * Implements FR-068: Network cascade alerts.
 */
@Service
public class CascadeAnalyzer {

    @SuppressWarnings("unused") // Reserved for detailed cascade logging
    private static final Logger log = LoggerFactory.getLogger(CascadeAnalyzer.class);

    private final TurnaroundRepository turnaroundRepository;
    
    @SuppressWarnings("unused") // Reserved for financial impact integration
    private final FinancialImpactCalculator financialImpactCalculator;

    // Cascade thresholds
    private static final int MIN_AFFECTED_FLIGHTS = 2;
    private static final int CASCADE_TIME_WINDOW_HOURS = 4;
    private static final int MAX_CASCADE_DEPTH = 3;

    public CascadeAnalyzer(TurnaroundRepository turnaroundRepository,
                           FinancialImpactCalculator financialImpactCalculator) {
        this.turnaroundRepository = turnaroundRepository;
        this.financialImpactCalculator = financialImpactCalculator;
    }

    /**
     * Analyze cascade impact of a delayed turnaround.
     */
    public CascadeResult analyzeCascade(Turnaround originTurnaround, int delayMinutes) {
        CascadeResult result = new CascadeResult();
        result.setOriginTurnaroundId(originTurnaround.getId());
        result.setOriginDelayMinutes(delayMinutes);

        if (delayMinutes < 10) {
            // Minor delays don't cause cascades
            return result;
        }

        String tenantCode = originTurnaround.getTenantCode();
        Instant cascadeWindowEnd = originTurnaround.getStartTime()
                .plus(CASCADE_TIME_WINDOW_HOURS, ChronoUnit.HOURS);

        // Find potentially affected turnarounds
        List<Turnaround> potentiallyAffected = findPotentiallyAffectedTurnarounds(
                tenantCode, originTurnaround, cascadeWindowEnd);

        // Build cascade chain
        List<CascadeLink> cascadeChain = buildCascadeChain(
                originTurnaround, potentiallyAffected, delayMinutes, 1);

        result.setCascadeChain(cascadeChain);
        result.setTotalAffectedFlights(cascadeChain.size());
        result.setTotalCascadeDelayMinutes(calculateTotalCascadeDelay(cascadeChain));
        result.setTotalFinancialImpact(calculateTotalFinancialImpact(
                originTurnaround, delayMinutes, cascadeChain));

        // Determine severity
        result.setSeverity(determineCascadeSeverity(result));

        return result;
    }

    /**
     * Check if a delay would trigger a cascade warning.
     */
    public boolean shouldTriggerCascadeWarning(Turnaround turnaround, int delayMinutes) {
        if (delayMinutes < 15) {
            return false;
        }

        CascadeResult result = analyzeCascade(turnaround, delayMinutes);
        return result.getTotalAffectedFlights() >= MIN_AFFECTED_FLIGHTS;
    }

    /**
     * Get cascade summary for display.
     */
    public String getCascadeSummary(CascadeResult result) {
        if (result.getCascadeChain().isEmpty()) {
            return "No downstream impact expected";
        }

        return String.format(
                "NETWORK IMPACT: %d flight(s) affected, %d min total cascade delay, $%.2f impact",
                result.getTotalAffectedFlights(),
                result.getTotalCascadeDelayMinutes(),
                result.getTotalFinancialImpact()
        );
    }

    /**
     * Get recommended actions for cascade mitigation.
     */
    public List<String> getCascadeMitigationActions(CascadeResult result) {
        List<String> actions = new ArrayList<>();

        if (result.getTotalAffectedFlights() >= 5) {
            actions.add("URGENT: Activate contingency operations center");
            actions.add("Consider gate reassignments for affected flights");
        }

        if (result.getSeverity().equals("CRITICAL")) {
            actions.add("Notify station manager immediately");
            actions.add("Prepare passenger rebooking options");
        }

        actions.add("Monitor connecting passenger status");
        actions.add("Update flight displays with delay information");
        
        if (result.getTotalCascadeDelayMinutes() > 60) {
            actions.add("Consider crew duty time implications");
        }

        return actions;
    }

    private List<Turnaround> findPotentiallyAffectedTurnarounds(String tenantCode,
                                                                  Turnaround origin,
                                                                  Instant windowEnd) {
        return turnaroundRepository.findByTenantCodeAndStatus(tenantCode, "SCHEDULED")
                .stream()
                .filter(t -> !t.getId().equals(origin.getId()))
                .filter(t -> t.getStartTime().isAfter(origin.getStartTime()))
                .filter(t -> t.getStartTime().isBefore(windowEnd))
                .sorted(Comparator.comparing(Turnaround::getStartTime))
                .collect(Collectors.toList());
    }

    private List<CascadeLink> buildCascadeChain(Turnaround origin,
                                                 List<Turnaround> potentiallyAffected,
                                                 int currentDelay,
                                                 int depth) {
        List<CascadeLink> chain = new ArrayList<>();

        if (depth > MAX_CASCADE_DEPTH || potentiallyAffected.isEmpty()) {
            return chain;
        }

        // Calculate propagated delay (diminishes with each hop)
        int propagatedDelay = (int) (currentDelay * 0.7);
        if (propagatedDelay < 5) {
            return chain;
        }

        for (Turnaround affected : potentiallyAffected) {
            // Check if this turnaround would be affected
            // (using same stand or within time proximity)
            if (isAffectedBy(affected, origin, currentDelay)) {
                CascadeLink link = new CascadeLink();
                link.setTurnaroundId(affected.getId());
                link.setFlightNumber(affected.getFlightNumber());
                link.setStandCode(affected.getStandCode());
                link.setOriginalStartTime(affected.getStartTime());
                link.setDelayedStartTime(affected.getStartTime().plus(propagatedDelay, ChronoUnit.MINUTES));
                link.setDelayMinutes(propagatedDelay);
                link.setDepth(depth);

                chain.add(link);

                // Recursively analyze further cascade
                if (propagatedDelay >= 10) {
                    List<Turnaround> remaining = potentiallyAffected.stream()
                            .filter(t -> t.getStartTime().isAfter(affected.getStartTime()))
                            .collect(Collectors.toList());
                    
                    chain.addAll(buildCascadeChain(affected, remaining, propagatedDelay, depth + 1));
                }
            }
        }

        return chain;
    }

    private boolean isAffectedBy(Turnaround target, Turnaround source, int delay) {
        // Same stand = definitely affected
        if (target.getStandCode().equals(source.getStandCode())) {
            return true;
        }

        // Close time proximity with shared resources
        long minutesBetween = ChronoUnit.MINUTES.between(
                source.getStartTime(), target.getStartTime());
        return minutesBetween < delay + 30;
    }

    private int calculateTotalCascadeDelay(List<CascadeLink> chain) {
        return chain.stream()
                .mapToInt(CascadeLink::getDelayMinutes)
                .sum();
    }

    private double calculateTotalFinancialImpact(Turnaround origin, int originDelay,
                                                  List<CascadeLink> chain) {
        // Origin impact
        double total = originDelay * 125.0; // $125/min

        // Cascade impact (each affected flight adds cost)
        for (CascadeLink link : chain) {
            total += link.getDelayMinutes() * 125.0;
        }

        return total;
    }

    private String determineCascadeSeverity(CascadeResult result) {
        if (result.getTotalAffectedFlights() >= 5 || result.getTotalFinancialImpact() > 50000) {
            return "CRITICAL";
        }
        if (result.getTotalAffectedFlights() >= 3 || result.getTotalFinancialImpact() > 25000) {
            return "HIGH";
        }
        if (result.getTotalAffectedFlights() >= 1) {
            return "MEDIUM";
        }
        return "LOW";
    }

    // Result classes
    public static class CascadeResult {
        private UUID originTurnaroundId;
        private int originDelayMinutes;
        private List<CascadeLink> cascadeChain = new ArrayList<>();
        private int totalAffectedFlights;
        private int totalCascadeDelayMinutes;
        private double totalFinancialImpact;
        private String severity;

        // Getters and setters
        public UUID getOriginTurnaroundId() { return originTurnaroundId; }
        public void setOriginTurnaroundId(UUID id) { this.originTurnaroundId = id; }
        public int getOriginDelayMinutes() { return originDelayMinutes; }
        public void setOriginDelayMinutes(int m) { this.originDelayMinutes = m; }
        public List<CascadeLink> getCascadeChain() { return cascadeChain; }
        public void setCascadeChain(List<CascadeLink> c) { this.cascadeChain = c; }
        public int getTotalAffectedFlights() { return totalAffectedFlights; }
        public void setTotalAffectedFlights(int f) { this.totalAffectedFlights = f; }
        public int getTotalCascadeDelayMinutes() { return totalCascadeDelayMinutes; }
        public void setTotalCascadeDelayMinutes(int m) { this.totalCascadeDelayMinutes = m; }
        public double getTotalFinancialImpact() { return totalFinancialImpact; }
        public void setTotalFinancialImpact(double i) { this.totalFinancialImpact = i; }
        public String getSeverity() { return severity; }
        public void setSeverity(String s) { this.severity = s; }
    }

    public static class CascadeLink {
        private UUID turnaroundId;
        private String flightNumber;
        private String standCode;
        private Instant originalStartTime;
        private Instant delayedStartTime;
        private int delayMinutes;
        private int depth;

        // Getters and setters
        public UUID getTurnaroundId() { return turnaroundId; }
        public void setTurnaroundId(UUID id) { this.turnaroundId = id; }
        public String getFlightNumber() { return flightNumber; }
        public void setFlightNumber(String f) { this.flightNumber = f; }
        public String getStandCode() { return standCode; }
        public void setStandCode(String s) { this.standCode = s; }
        public Instant getOriginalStartTime() { return originalStartTime; }
        public void setOriginalStartTime(Instant t) { this.originalStartTime = t; }
        public Instant getDelayedStartTime() { return delayedStartTime; }
        public void setDelayedStartTime(Instant t) { this.delayedStartTime = t; }
        public int getDelayMinutes() { return delayMinutes; }
        public void setDelayMinutes(int m) { this.delayMinutes = m; }
        public int getDepth() { return depth; }
        public void setDepth(int d) { this.depth = d; }
    }
}
