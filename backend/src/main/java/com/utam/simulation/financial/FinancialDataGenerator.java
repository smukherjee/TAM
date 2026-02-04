package com.utam.simulation.financial;

import com.utam.simulation.alert.SimAlertRepository;
import com.utam.simulation.config.SimulationConfig;
import com.utam.simulation.core.BaseDataGenerator;
import com.utam.simulation.dispatch.DispatchRepository;
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
 * Generates financial metrics based on simulation data.
 * Implements FR-102 to FR-108: Delay cost calculations at $125/min.
 */
@Component
public class FinancialDataGenerator extends BaseDataGenerator {

    private final FinancialMetricRepository financialMetricRepository;
    private final TurnaroundRepository turnaroundRepository;
    
    @SuppressWarnings("unused") // Reserved for dispatch-based cost correlation
    private final DispatchRepository dispatchRepository;
    private final SimAlertRepository alertRepository;
    private final Random random = new Random();

    // Cost per minute of delay (from spec)
    private static final BigDecimal DELAY_COST_PER_MINUTE = new BigDecimal("125.00");

    // Base operational costs
    private static final BigDecimal BASE_OPERATIONAL_COST = new BigDecimal("50.00");
    private static final BigDecimal SLA_PENALTY_RATE = new BigDecimal("500.00");

    public FinancialDataGenerator(SimulationConfig config, MeterRegistry meterRegistry,
                                  FinancialMetricRepository financialMetricRepository,
                                  TurnaroundRepository turnaroundRepository,
                                  DispatchRepository dispatchRepository,
                                  SimAlertRepository alertRepository) {
        super(config, meterRegistry);
        this.financialMetricRepository = financialMetricRepository;
        this.turnaroundRepository = turnaroundRepository;
        this.dispatchRepository = dispatchRepository;
        this.alertRepository = alertRepository;
    }

    @Override
    public String getGeneratorName() {
        return "FinancialDataGenerator";
    }

    @Override
    public String getEntityType() {
        return "FinancialMetric";
    }

    @PostConstruct
    public void init() {
        log.info("FinancialDataGenerator initialized with delay cost: ${}/min", DELAY_COST_PER_MINUTE);
    }

    @Override
    public int generateBatch(String tenantCode, int batchSize) {
        SimulationConfig.TenantConfig tenantConfig = config.getTenant(tenantCode);
        if (tenantConfig == null) {
            log.warn("No tenant config for: {}", tenantCode);
            return 0;
        }

        int generated = 0;
        LocalDate today = LocalDate.now();

        // Generate metrics for the last 30 days
        for (int daysAgo = 30; daysAgo >= 0 && generated < batchSize; daysAgo--) {
            LocalDate date = today.minus(daysAgo, ChronoUnit.DAYS);
            
            // Generate daily summary metric
            FinancialMetric dailyMetric = generateDailySummary(tenantCode, date);
            financialMetricRepository.save(dailyMetric);
            generated++;

            // Generate per-stand metrics (subset)
            List<String> standCodes = List.of("S1", "S2", "S3", "S5", "S10");
            for (String standCode : standCodes) {
                if (generated >= batchSize) break;
                
                FinancialMetric standMetric = generateStandMetric(tenantCode, date, standCode);
                financialMetricRepository.save(standMetric);
                generated++;
            }
        }

        log.info("Generated {} financial metrics for tenant {}", generated, tenantCode);
        return generated;
    }

    @Override
    public boolean generateSingleUpdate(String tenantCode) {
        // Update today's metrics based on current simulation state
        LocalDate today = LocalDate.now();
        
        // Calculate delay costs from turnarounds
        List<Turnaround> delayedTurnarounds = turnaroundRepository.findDelayedTurnarounds(tenantCode);
        int totalDelayMinutes = delayedTurnarounds.stream()
                .mapToInt(t -> t.getDelayMinutes() != null ? t.getDelayMinutes() : 0)
                .sum();

        // Create or update today's metric
        FinancialMetric metric = new FinancialMetric();
        metric.setId(UUID.randomUUID());
        metric.setTenantCode(tenantCode);
        metric.setReportDate(today);
        metric.setMetricType("DELAY_COST");
        
        // Calculate costs
        BigDecimal delayCost = DELAY_COST_PER_MINUTE.multiply(new BigDecimal(totalDelayMinutes));
        metric.setDelayCostUsd(delayCost);
        metric.setDelayMinutes(totalDelayMinutes);

        // Calculate operational cost
        int turnaroundCount = (int) turnaroundRepository.countByTenantCode(tenantCode);
        BigDecimal operationalCost = BASE_OPERATIONAL_COST.multiply(new BigDecimal(turnaroundCount / 10 + 1));
        metric.setOperationalCostUsd(operationalCost);
        metric.setTurnaroundCount(turnaroundCount);

        // Calculate SLA penalties
        long criticalAlerts = alertRepository.countNewAlerts(tenantCode);
        BigDecimal penaltyCost = SLA_PENALTY_RATE.multiply(new BigDecimal(criticalAlerts));
        metric.setPenaltyCostUsd(penaltyCost);
        metric.setAlertCount((int) criticalAlerts);

        // Calculate totals
        metric.calculateTotalCost();

        // Calculate utilization and SLA compliance
        metric.setUtilizationPercent(calculateUtilization(tenantCode));
        metric.setSlaCompliancePercent(calculateSlaCompliance(tenantCode, delayedTurnarounds.size(), turnaroundCount));

        metric.setCreatedAt(Instant.now());
        metric.setUpdatedAt(Instant.now());

        financialMetricRepository.save(metric);

        return true;
    }

    private FinancialMetric generateDailySummary(String tenantCode, LocalDate date) {
        FinancialMetric metric = new FinancialMetric();
        metric.setId(UUID.randomUUID());
        metric.setTenantCode(tenantCode);
        metric.setReportDate(date);
        metric.setMetricType("DAILY_SUMMARY");

        // Generate realistic values
        int baseDelayMinutes = 50 + random.nextInt(100); // 50-150 minutes per day
        int turnarounds = 80 + random.nextInt(40); // 80-120 turnarounds
        int dispatches = turnarounds * 5 + random.nextInt(50); // ~5 dispatches per turnaround
        int alerts = random.nextInt(20); // 0-20 alerts

        // Weekend variations
        if (date.getDayOfWeek().getValue() >= 6) {
            turnarounds = (int) (turnarounds * 1.2); // 20% more on weekends
            baseDelayMinutes = (int) (baseDelayMinutes * 1.3); // More delays
        }

        BigDecimal delayCost = DELAY_COST_PER_MINUTE.multiply(new BigDecimal(baseDelayMinutes));
        BigDecimal operationalCost = BASE_OPERATIONAL_COST.multiply(new BigDecimal(turnarounds));
        BigDecimal penaltyCost = SLA_PENALTY_RATE.multiply(new BigDecimal(Math.max(0, alerts - 10)));

        metric.setDelayCostUsd(delayCost);
        metric.setOperationalCostUsd(operationalCost);
        metric.setPenaltyCostUsd(penaltyCost);
        metric.setDelayMinutes(baseDelayMinutes);
        metric.setTurnaroundCount(turnarounds);
        metric.setDispatchCount(dispatches);
        metric.setAlertCount(alerts);

        // Utilization 65-95%
        metric.setUtilizationPercent(new BigDecimal(65 + random.nextInt(30)));
        // SLA compliance 85-99%
        metric.setSlaCompliancePercent(new BigDecimal(85 + random.nextInt(14)));

        metric.calculateTotalCost();
        metric.setCreatedAt(date.atStartOfDay(ZoneId.systemDefault()).toInstant());

        return metric;
    }

    private FinancialMetric generateStandMetric(String tenantCode, LocalDate date, String standCode) {
        FinancialMetric metric = new FinancialMetric();
        metric.setId(UUID.randomUUID());
        metric.setTenantCode(tenantCode);
        metric.setReportDate(date);
        metric.setMetricType("STAND_SUMMARY");
        metric.setStandCode(standCode);

        // Per-stand values (smaller)
        int delayMinutes = random.nextInt(30);
        int turnarounds = 3 + random.nextInt(8);

        metric.setDelayCostUsd(DELAY_COST_PER_MINUTE.multiply(new BigDecimal(delayMinutes)));
        metric.setDelayMinutes(delayMinutes);
        metric.setTurnaroundCount(turnarounds);
        metric.setUtilizationPercent(new BigDecimal(50 + random.nextInt(45)));
        metric.setSlaCompliancePercent(new BigDecimal(80 + random.nextInt(18)));

        metric.calculateTotalCost();
        metric.setCreatedAt(date.atStartOfDay(ZoneId.systemDefault()).toInstant());

        return metric;
    }

    private BigDecimal calculateUtilization(String tenantCode) {
        // Calculate based on active vs total vehicles/assets
        // Simplified: random between 60-90%
        return new BigDecimal(60 + random.nextInt(30));
    }

    private BigDecimal calculateSlaCompliance(String tenantCode, int delayedCount, int totalCount) {
        if (totalCount == 0) return new BigDecimal("100.00");
        double compliance = ((double) (totalCount - delayedCount) / totalCount) * 100;
        return new BigDecimal(compliance).setScale(2, RoundingMode.HALF_UP);
    }
}
