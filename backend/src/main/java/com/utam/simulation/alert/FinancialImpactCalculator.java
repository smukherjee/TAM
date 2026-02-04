package com.utam.simulation.alert;

import com.utam.simulation.turnaround.Turnaround;
import com.utam.simulation.turnaround.TurnaroundCorrelator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calculates financial impact of delays and alerts.
 * Implements FR-067 to FR-074: Financial impact tracking at $100-150/min.
 */
@Service
@SuppressWarnings("unused") // log field reserved for future detailed logging
public class FinancialImpactCalculator {

    private static final Logger log = LoggerFactory.getLogger(FinancialImpactCalculator.class);

    // Cost constants from specification
    private static final BigDecimal BASE_DELAY_COST_PER_MINUTE = new BigDecimal("125.00");
    private static final BigDecimal MIN_DELAY_COST_PER_MINUTE = new BigDecimal("100.00");
    private static final BigDecimal MAX_DELAY_COST_PER_MINUTE = new BigDecimal("150.00");
    
    // Slot value ranges by time of day
    private static final BigDecimal PEAK_SLOT_VALUE = new BigDecimal("80000.00");
    private static final BigDecimal OFF_PEAK_SLOT_VALUE = new BigDecimal("20000.00");
    private static final BigDecimal STANDARD_SLOT_VALUE = new BigDecimal("50000.00");
    
    // SLA penalty rates
    private static final BigDecimal SLA_BREACH_PENALTY = new BigDecimal("500.00");
    private static final BigDecimal SLA_CRITICAL_MULTIPLIER = new BigDecimal("2.0");

    private final TurnaroundCorrelator turnaroundCorrelator;

    public FinancialImpactCalculator(TurnaroundCorrelator turnaroundCorrelator) {
        this.turnaroundCorrelator = turnaroundCorrelator;
    }

    /**
     * Calculate delay cost for a turnaround.
     */
    public BigDecimal calculateDelayCost(Turnaround turnaround) {
        if (turnaround.getDelayMinutes() == null || turnaround.getDelayMinutes() <= 0) {
            return BigDecimal.ZERO;
        }

        int delayMinutes = turnaround.getDelayMinutes();
        BigDecimal costPerMinute = getDelayRateForAircraftType(turnaround.getAircraftType());
        
        BigDecimal directCost = costPerMinute.multiply(BigDecimal.valueOf(delayMinutes));
        
        // Add cascade impact if significant delay
        if (delayMinutes > 15) {
            // Convert to TurnaroundCorrelator.Turnaround record
            TurnaroundCorrelator.Turnaround correlatorTurnaround = new TurnaroundCorrelator.Turnaround(
                    turnaround.getId(),
                    turnaround.getFlightNumber(),
                    turnaround.getStandCode(),
                    turnaround.getTenantCode(),
                    turnaround.getStatus()
            );
            TurnaroundCorrelator.CascadeImpact cascade = 
                    turnaroundCorrelator.calculateCascadeImpact(correlatorTurnaround, delayMinutes);
            BigDecimal cascadeCost = BigDecimal.valueOf(cascade.getEstimatedCostDollars());
            directCost = directCost.add(cascadeCost);
        }

        return directCost.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate potential cost if delay is not prevented.
     */
    public BigDecimal calculatePreventedDelayCost(int projectedDelayMinutes, String aircraftType) {
        if (projectedDelayMinutes <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal costPerMinute = getDelayRateForAircraftType(aircraftType);
        return costPerMinute.multiply(BigDecimal.valueOf(projectedDelayMinutes))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate slot value at risk.
     */
    public BigDecimal calculateSlotValueAtRisk(int hourOfDay, boolean isPeakSeason) {
        BigDecimal baseValue;
        
        // Peak hours: 06:00-09:00 and 17:00-21:00
        if ((hourOfDay >= 6 && hourOfDay < 9) || (hourOfDay >= 17 && hourOfDay < 21)) {
            baseValue = PEAK_SLOT_VALUE;
        } else if (hourOfDay >= 23 || hourOfDay < 5) {
            baseValue = OFF_PEAK_SLOT_VALUE;
        } else {
            baseValue = STANDARD_SLOT_VALUE;
        }

        // Adjust for peak season
        if (isPeakSeason) {
            baseValue = baseValue.multiply(new BigDecimal("1.25"));
        }

        return baseValue.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate SLA breach penalty.
     */
    public BigDecimal calculateSLAPenalty(int breachMinutes, String severity) {
        BigDecimal basePenalty = SLA_BREACH_PENALTY.multiply(BigDecimal.valueOf(breachMinutes));
        
        if ("CRITICAL".equals(severity)) {
            basePenalty = basePenalty.multiply(SLA_CRITICAL_MULTIPLIER);
        } else if ("HIGH".equals(severity)) {
            basePenalty = basePenalty.multiply(new BigDecimal("1.5"));
        }

        return basePenalty.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate total financial impact for an alert.
     */
    public FinancialImpact calculateAlertImpact(Alert alert) {
        FinancialImpact impact = new FinancialImpact();
        
        switch (alert.getAlertType()) {
            case "DELAY" -> {
                int delayMinutes = extractDelayMinutes(alert);
                impact.setDelayCost(BASE_DELAY_COST_PER_MINUTE
                        .multiply(BigDecimal.valueOf(delayMinutes)));
                
                if (delayMinutes > 30) {
                    impact.setSlotValueAtRisk(STANDARD_SLOT_VALUE);
                }
            }
            case "SLA_BREACH" -> {
                int breachMinutes = extractDelayMinutes(alert);
                impact.setSlaPenalty(calculateSLAPenalty(breachMinutes, alert.getSeverity()));
            }
            case "GEOFENCE", "MAINTENANCE" -> {
                // Operational costs, not direct financial impact
                impact.setOperationalCost(new BigDecimal("100.00"));
            }
        }

        impact.calculateTotal();
        return impact;
    }

    /**
     * Calculate annualized savings projection.
     */
    public BigDecimal calculateAnnualizedSavings(BigDecimal dailyPreventedCost) {
        // 365 days * daily average
        return dailyPreventedCost.multiply(BigDecimal.valueOf(365))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate capacity gain value from turnaround improvement.
     */
    public BigDecimal calculateCapacityGainValue(int minutesSavedPerTurnaround, 
                                                  int turnaroundsPerDay) {
        // Each 5-minute improvement enables more ATMs
        BigDecimal slotsEnabled = BigDecimal.valueOf(minutesSavedPerTurnaround)
                .divide(BigDecimal.valueOf(5), 2, RoundingMode.HALF_UP);
        
        // Value per enabled slot
        BigDecimal valuePerSlot = STANDARD_SLOT_VALUE.divide(
                BigDecimal.valueOf(10), 2, RoundingMode.HALF_UP);
        
        return slotsEnabled.multiply(valuePerSlot)
                .multiply(BigDecimal.valueOf(turnaroundsPerDay))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal getDelayRateForAircraftType(String aircraftType) {
        // Wide-body aircraft have higher delay costs
        return switch (aircraftType) {
            case "A388", "B77W" -> MAX_DELAY_COST_PER_MINUTE;
            case "B789", "A359" -> new BigDecimal("140.00");
            case "A321", "B738" -> new BigDecimal("130.00");
            case "A319" -> MIN_DELAY_COST_PER_MINUTE;
            default -> BASE_DELAY_COST_PER_MINUTE;
        };
    }

    private int extractDelayMinutes(Alert alert) {
        // Try to extract delay minutes from alert message
        String message = alert.getMessage();
        if (message == null) {
            return 5; // Default
        }

        // Look for pattern "X minutes" in message
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(\\d+)\\s*minute")
                .matcher(message);
        
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }

        return 5; // Default
    }

    /**
     * Financial impact breakdown.
     */
    public static class FinancialImpact {
        private BigDecimal delayCost = BigDecimal.ZERO;
        private BigDecimal slotValueAtRisk = BigDecimal.ZERO;
        private BigDecimal slaPenalty = BigDecimal.ZERO;
        private BigDecimal operationalCost = BigDecimal.ZERO;
        private BigDecimal totalImpact = BigDecimal.ZERO;

        public void calculateTotal() {
            this.totalImpact = delayCost
                    .add(slotValueAtRisk)
                    .add(slaPenalty)
                    .add(operationalCost)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // Getters and setters
        public BigDecimal getDelayCost() { return delayCost; }
        public void setDelayCost(BigDecimal c) { this.delayCost = c; }
        public BigDecimal getSlotValueAtRisk() { return slotValueAtRisk; }
        public void setSlotValueAtRisk(BigDecimal v) { this.slotValueAtRisk = v; }
        public BigDecimal getSlaPenalty() { return slaPenalty; }
        public void setSlaPenalty(BigDecimal p) { this.slaPenalty = p; }
        public BigDecimal getOperationalCost() { return operationalCost; }
        public void setOperationalCost(BigDecimal c) { this.operationalCost = c; }
        public BigDecimal getTotalImpact() { return totalImpact; }
    }
}
