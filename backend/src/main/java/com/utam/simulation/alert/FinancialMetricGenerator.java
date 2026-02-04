package com.utam.simulation.alert;

import com.utam.simulation.config.SimulationConfig;
import com.utam.simulation.core.BaseDataGenerator;
import com.utam.simulation.dispatch.DispatchRepository;
import com.utam.simulation.financial.FinancialMetric;
import com.utam.simulation.financial.FinancialMetricRepository;
import com.utam.simulation.turnaround.Turnaround;
import com.utam.simulation.turnaround.TurnaroundRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Generates financial metrics from turnaround and alert data.
 * Implements FR-069 to FR-074: Financial impact tracking.
 * 
 * NOTE: This generator is temporarily simplified to allow builds.
 * Full implementation requires repository method additions.
 */
@Component
public class FinancialMetricGenerator extends BaseDataGenerator {

    private final FinancialMetricRepository financialMetricRepository;
    private final TurnaroundRepository turnaroundRepository;
    private final SimAlertRepository alertRepository;
    private final DispatchRepository dispatchRepository;
    private final FinancialImpactCalculator financialImpactCalculator;

    // Cost constants
    private static final BigDecimal DELAY_COST_PER_MINUTE = new BigDecimal("125.00");
    private static final BigDecimal CAPACITY_GAIN_PER_SLOT = new BigDecimal("5000.00");

    public FinancialMetricGenerator(SimulationConfig config, MeterRegistry meterRegistry,
                                     FinancialMetricRepository financialMetricRepository,
                                     TurnaroundRepository turnaroundRepository,
                                     SimAlertRepository alertRepository,
                                     DispatchRepository dispatchRepository,
                                     FinancialImpactCalculator financialImpactCalculator) {
        super(config, meterRegistry);
        this.financialMetricRepository = financialMetricRepository;
        this.turnaroundRepository = turnaroundRepository;
        this.alertRepository = alertRepository;
        this.dispatchRepository = dispatchRepository;
        this.financialImpactCalculator = financialImpactCalculator;
    }

    @Override
    public String getGeneratorName() {
        return "FinancialMetricGenerator";
    }

    @Override
    public String getEntityType() {
        return "FinancialMetric";
    }

    @PostConstruct
    public void init() {
        log.info("FinancialMetricGenerator initialized with delay cost: ${}/min", 
                DELAY_COST_PER_MINUTE);
    }

    @Override
    public int generateBatch(String tenantCode, int batchSize) {
        SimulationConfig.TenantConfig tenantConfig = config.getTenant(tenantCode);
        if (tenantConfig == null) {
            log.warn("No tenant config for: {}", tenantCode);
            return 0;
        }

        ZoneId timezone = tenantConfig.getTimezone();
        LocalDate today = LocalDate.now(timezone);
        int generated = 0;

        // Generate metrics for the last 30 days
        for (int daysAgo = 30; daysAgo >= 0 && generated < batchSize; daysAgo--) {
            LocalDate metricDate = today.minusDays(daysAgo);
            
            // Check if metric already exists
            if (metricExists(tenantCode, metricDate)) {
                continue;
            }

            FinancialMetric metric = generateDailyMetric(tenantCode, metricDate, timezone);
            financialMetricRepository.save(metric);
            generated++;
        }

        return generated;
    }

    @Override
    public boolean generateSingleUpdate(String tenantCode) {
        SimulationConfig.TenantConfig tenantConfig = config.getTenant(tenantCode);
        if (tenantConfig == null) {
            return false;
        }

        ZoneId timezone = tenantConfig.getTimezone();
        LocalDate today = LocalDate.now(timezone);

        Optional<FinancialMetric> existing = financialMetricRepository
                .findByTenantCodeAndMetricDate(tenantCode, today);

        if (existing.isPresent()) {
            updateDailyMetric(existing.get(), tenantCode, today, timezone);
            financialMetricRepository.save(existing.get());
            return true;
        } else {
            FinancialMetric metric = generateDailyMetric(tenantCode, today, timezone);
            financialMetricRepository.save(metric);
            return true;
        }
    }

    /**
     * Generate daily financial summary.
     */
    public FinancialMetric generateDailyMetric(String tenantCode, LocalDate date, ZoneId timezone) {
        FinancialMetric metric = new FinancialMetric();
        metric.setTenantCode(tenantCode);
        metric.setMetricDate(date);

        // Simplified generation - uses random values for demo
        Random random = new Random();
        
        int turnaroundsCompleted = 10 + random.nextInt(20);
        int turnaroundsDelayed = random.nextInt(5);
        int totalDelayMinutes = turnaroundsDelayed * (5 + random.nextInt(15));
        BigDecimal totalDelayCost = DELAY_COST_PER_MINUTE.multiply(BigDecimal.valueOf(totalDelayMinutes));
        
        int alertsGenerated = 5 + random.nextInt(15);
        int alertsResolved = Math.min(alertsGenerated, 3 + random.nextInt(10));
        
        int preventedDelayMinutes = alertsResolved * (3 + random.nextInt(10));
        BigDecimal preventedDelayCost = DELAY_COST_PER_MINUTE.multiply(BigDecimal.valueOf(preventedDelayMinutes));
        
        BigDecimal capacityGainValue = turnaroundsCompleted > turnaroundsDelayed * 4 
                ? CAPACITY_GAIN_PER_SLOT.multiply(BigDecimal.valueOf(1 + random.nextInt(3)))
                : BigDecimal.ZERO;

        // Populate metric
        metric.setTotalDelayCost(totalDelayCost.setScale(2, RoundingMode.HALF_UP));
        metric.setPreventedDelayCost(preventedDelayCost.setScale(2, RoundingMode.HALF_UP));
        metric.setCapacityGainValue(capacityGainValue.setScale(2, RoundingMode.HALF_UP));
        metric.setTotalDelayMinutes(totalDelayMinutes);
        metric.setPreventedDelayMinutes(preventedDelayMinutes);
        metric.setTurnaroundsCompleted(turnaroundsCompleted);
        metric.setTurnaroundsDelayed(turnaroundsDelayed);
        metric.setAlertsGenerated(alertsGenerated);
        metric.setAlertsResolved(alertsResolved);

        return metric;
    }

    /**
     * Update existing daily metric with current data.
     */
    private void updateDailyMetric(FinancialMetric metric, String tenantCode, 
                                    LocalDate date, ZoneId timezone) {
        FinancialMetric updated = generateDailyMetric(tenantCode, date, timezone);
        
        // Copy updated values
        metric.setTotalDelayCost(updated.getTotalDelayCost());
        metric.setPreventedDelayCost(updated.getPreventedDelayCost());
        metric.setCapacityGainValue(updated.getCapacityGainValue());
        metric.setTotalDelayMinutes(updated.getTotalDelayMinutes());
        metric.setPreventedDelayMinutes(updated.getPreventedDelayMinutes());
        metric.setTurnaroundsCompleted(updated.getTurnaroundsCompleted());
        metric.setTurnaroundsDelayed(updated.getTurnaroundsDelayed());
        metric.setAlertsGenerated(updated.getAlertsGenerated());
        metric.setAlertsResolved(updated.getAlertsResolved());
        metric.setUpdatedAt(Instant.now());
    }

    private boolean metricExists(String tenantCode, LocalDate date) {
        return financialMetricRepository.findByTenantCodeAndMetricDate(tenantCode, date).isPresent();
    }

    /**
     * Get weekly summary for a tenant.
     */
    public Map<String, Object> getWeeklySummary(String tenantCode) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(7);

        List<FinancialMetric> metrics = financialMetricRepository
                .findByTenantCodeAndMetricDateBetween(tenantCode, weekStart, today);

        BigDecimal totalDelayCost = metrics.stream()
                .map(FinancialMetric::getTotalDelayCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPreventedCost = metrics.stream()
                .map(FinancialMetric::getPreventedDelayCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCapacityGain = metrics.stream()
                .map(FinancialMetric::getCapacityGainValue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalTurnarounds = metrics.stream()
                .map(FinancialMetric::getTurnaroundsCompleted)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("weekStart", weekStart.toString());
        summary.put("weekEnd", today.toString());
        summary.put("totalDelayCost", totalDelayCost);
        summary.put("totalPreventedCost", totalPreventedCost);
        summary.put("totalCapacityGain", totalCapacityGain);
        summary.put("totalTurnarounds", totalTurnarounds);
        summary.put("netSavings", totalPreventedCost.add(totalCapacityGain).subtract(totalDelayCost));

        return summary;
    }
}
