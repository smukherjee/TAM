package com.utam.simulation.turnaround;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Correlates turnaround delays with downstream impacts.
 * Calculates cascade effects and financial impact of delays.
 * 
 * Stub implementation for simulation infrastructure.
 */
@Component
public class TurnaroundCorrelator {

    /**
     * Represents a cascade impact assessment.
     */
    public record CascadeImpact(
            UUID turnaroundId,
            int affectedFlights,
            int totalDelayMinutes,
            BigDecimal estimatedCost,
            List<String> affectedFlightIds
    ) {
        /**
         * Get the estimated cost in dollars as a double value.
         */
        public double getEstimatedCostDollars() {
            return estimatedCost != null ? estimatedCost.doubleValue() : 0.0;
        }
    }

    /**
     * Turnaround record for correlation analysis.
     */
    public record Turnaround(
            UUID id,
            String flightId,
            String standId,
            String tenantCode,
            String status
    ) {}

    /**
     * Calculate cascade impact from a turnaround delay.
     */
    public CascadeImpact calculateCascadeImpact(Turnaround turnaround, int delayMinutes) {
        // Stub implementation - estimate based on delay duration
        int affectedFlights = delayMinutes > 30 ? Math.min(delayMinutes / 15, 5) : 0;
        int totalDelay = delayMinutes + (affectedFlights * 15);
        BigDecimal cost = BigDecimal.valueOf(delayMinutes * 100L + affectedFlights * 5000L);
        
        return new CascadeImpact(
                turnaround.id(),
                affectedFlights,
                totalDelay,
                cost,
                Collections.emptyList()
        );
    }

    /**
     * Find turnarounds affected by a specific delay.
     */
    public List<Turnaround> findAffectedTurnarounds(String standId, int windowMinutes) {
        // Stub implementation - returns empty list
        return Collections.emptyList();
    }
}
