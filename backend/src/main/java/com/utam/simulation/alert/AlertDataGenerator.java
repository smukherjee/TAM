package com.utam.simulation.alert;

import com.utam.simulation.config.SimulationConfig;
import com.utam.simulation.core.BaseDataGenerator;
import com.utam.simulation.dispatch.Dispatch;
import com.utam.simulation.dispatch.DispatchRepository;
import com.utam.simulation.turnaround.Turnaround;
import com.utam.simulation.turnaround.TurnaroundRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generates realistic alerts based on simulation events.
 * Implements FR-091 to FR-094: Alert generation for delays and SLA breaches.
 */
@Component
public class AlertDataGenerator extends BaseDataGenerator {

    private final SimAlertRepository alertRepository;
    private final TurnaroundRepository turnaroundRepository;
    private final DispatchRepository dispatchRepository;
    private final Random random = new Random();

    // Track already-alerted entities to prevent duplicates
    private final Set<UUID> alertedTurnarounds = ConcurrentHashMap.newKeySet();
    private final Set<UUID> alertedDispatches = ConcurrentHashMap.newKeySet();

    // Alert templates
    private static final List<AlertTemplate> DELAY_TEMPLATES = List.of(
            new AlertTemplate("DELAY", "Turnaround Delay", "Turnaround for flight %s at stand %s is delayed by %d minutes"),
            new AlertTemplate("DELAY", "Dispatch Delay", "Vehicle dispatch to stand %s is delayed by %d minutes"),
            new AlertTemplate("DELAY", "Service Delay", "Service completion at stand %s is running %d minutes behind schedule")
    );

    private static final List<AlertTemplate> GEOFENCE_TEMPLATES = List.of(
            new AlertTemplate("GEOFENCE", "Vehicle Outside Boundary", "Vehicle %s detected outside airport boundary at coordinates (%.4f, %.4f)"),
            new AlertTemplate("GEOFENCE", "Unauthorized Area Entry", "Vehicle %s entered restricted area near stand %s")
    );

    private static final List<AlertTemplate> MAINTENANCE_TEMPLATES = List.of(
            new AlertTemplate("MAINTENANCE", "Scheduled Maintenance Due", "Vehicle %s is due for scheduled maintenance"),
            new AlertTemplate("MAINTENANCE", "Equipment Inspection Required", "Equipment inspection overdue for vehicle %s")
    );

    private static final List<AlertTemplate> SLA_TEMPLATES = List.of(
            new AlertTemplate("SLA_BREACH", "SLA Threshold Exceeded", "Response time SLA exceeded for stand %s - %d minutes over limit"),
            new AlertTemplate("SLA_BREACH", "Service Level Warning", "Service level at risk for turnaround %s")
    );

    public AlertDataGenerator(SimulationConfig config, MeterRegistry meterRegistry,
                              SimAlertRepository alertRepository,
                              TurnaroundRepository turnaroundRepository,
                              DispatchRepository dispatchRepository) {
        super(config, meterRegistry);
        this.alertRepository = alertRepository;
        this.turnaroundRepository = turnaroundRepository;
        this.dispatchRepository = dispatchRepository;
    }

    @Override
    public String getGeneratorName() {
        return "AlertDataGenerator";
    }

    @Override
    public String getEntityType() {
        return "Alert";
    }

    @PostConstruct
    public void init() {
        log.info("AlertDataGenerator initialized");
    }

    @Override
    public int generateBatch(String tenantCode, int batchSize) {
        SimulationConfig.TenantConfig tenantConfig = config.getTenant(tenantCode);
        if (tenantConfig == null) {
            log.warn("No tenant config for: {}", tenantCode);
            return 0;
        }

        int generated = 0;
        Instant baseTime = Instant.now().minus(2, ChronoUnit.HOURS);

        // Generate alerts based on existing delayed turnarounds
        List<Turnaround> delayedTurnarounds = turnaroundRepository.findDelayedTurnarounds(tenantCode);
        for (Turnaround ta : delayedTurnarounds) {
            if (!alertedTurnarounds.contains(ta.getId()) && generated < batchSize) {
                Alert alert = createTurnaroundDelayAlert(tenantCode, ta);
                alertRepository.save(alert);
                alertedTurnarounds.add(ta.getId());
                generated++;
            }
        }

        // Generate alerts based on delayed dispatches
        List<Dispatch> delayedDispatches = dispatchRepository.findDelayedDispatches(tenantCode);
        for (Dispatch dispatch : delayedDispatches) {
            if (!alertedDispatches.contains(dispatch.getId()) && generated < batchSize) {
                Alert alert = createDispatchDelayAlert(tenantCode, dispatch);
                alertRepository.save(alert);
                alertedDispatches.add(dispatch.getId());
                generated++;
            }
        }

        // Fill remaining with random alerts
        while (generated < batchSize) {
            Alert alert = createRandomAlert(tenantCode, baseTime, generated);
            alertRepository.save(alert);
            generated++;
        }

        log.info("Generated {} alerts for tenant {}", generated, tenantCode);
        return generated;
    }

    @Override
    public boolean generateSingleUpdate(String tenantCode) {
        // Check for new delayed turnarounds
        int delayThreshold = config.getAlerts().getDelayThresholdMinutes();
        
        List<Turnaround> delayedTurnarounds = turnaroundRepository.findDelayedTurnarounds(tenantCode);
        for (Turnaround ta : delayedTurnarounds) {
            if (!alertedTurnarounds.contains(ta.getId()) && ta.getDelayMinutes() >= delayThreshold) {
                Alert alert = createTurnaroundDelayAlert(tenantCode, ta);
                alertRepository.save(alert);
                alertedTurnarounds.add(ta.getId());
                log.debug("Created delay alert for turnaround: {}", ta.getFlightNumber());
            }
        }

        // Check for delayed dispatches
        List<Dispatch> delayedDispatches = dispatchRepository.findDelayedDispatches(tenantCode);
        for (Dispatch dispatch : delayedDispatches) {
            if (!alertedDispatches.contains(dispatch.getId())) {
                Alert alert = createDispatchDelayAlert(tenantCode, dispatch);
                alertRepository.save(alert);
                alertedDispatches.add(dispatch.getId());
                log.debug("Created delay alert for dispatch: {}", dispatch.getId());
            }
        }

        // Randomly generate other alert types (low probability)
        if (random.nextDouble() < 0.02) { // 2% chance per tick
            Alert alert = createRandomAlert(tenantCode, Instant.now(), 0);
            alertRepository.save(alert);
        }

        // Auto-resolve old alerts
        autoResolveOldAlerts(tenantCode);

        return true;
    }

    private Alert createTurnaroundDelayAlert(String tenantCode, Turnaround turnaround) {
        Alert alert = new Alert();
        alert.setId(UUID.randomUUID());
        alert.setTenantCode(tenantCode);
        alert.setAlertType("DELAY");
        alert.setSeverity(getSeverityForDelay(turnaround.getDelayMinutes()));
        alert.setStatus("NEW");
        alert.setTitle("Turnaround Delay Alert");
        alert.setDescription(String.format(
                "Turnaround for flight %s at stand %s is delayed by %d minutes. Reason: %s",
                turnaround.getFlightNumber(),
                turnaround.getStandCode(),
                turnaround.getDelayMinutes(),
                turnaround.getDelayReason() != null ? turnaround.getDelayReason() : "Unknown"
        ));
        alert.setCreatedAt(Instant.now());
        alert.setEntityType("TURNAROUND");
        alert.setEntityId(turnaround.getId());
        alert.setEntityReference(turnaround.getFlightNumber());
        alert.setDelayMinutes(turnaround.getDelayMinutes());

        return alert;
    }

    private Alert createDispatchDelayAlert(String tenantCode, Dispatch dispatch) {
        Alert alert = new Alert();
        alert.setId(UUID.randomUUID());
        alert.setTenantCode(tenantCode);
        alert.setAlertType("DELAY");
        alert.setSeverity(getSeverityForDelaySeconds(dispatch.getDelaySeconds()));
        alert.setStatus("NEW");
        alert.setTitle("Dispatch Delay Alert");
        alert.setDescription(String.format(
                "Vehicle dispatch to stand %s is delayed by %d seconds",
                dispatch.getStandCode(),
                dispatch.getDelaySeconds()
        ));
        alert.setCreatedAt(Instant.now());
        alert.setEntityType("DISPATCH");
        alert.setEntityId(dispatch.getId());
        alert.setEntityReference(dispatch.getStandCode());
        alert.setDelayMinutes(dispatch.getDelaySeconds() / 60);

        return alert;
    }

    private Alert createRandomAlert(String tenantCode, Instant baseTime, int index) {
        List<List<AlertTemplate>> allTemplates = List.of(
                DELAY_TEMPLATES, GEOFENCE_TEMPLATES, MAINTENANCE_TEMPLATES, SLA_TEMPLATES
        );
        List<AlertTemplate> templates = allTemplates.get(random.nextInt(allTemplates.size()));
        AlertTemplate template = templates.get(random.nextInt(templates.size()));

        Alert alert = new Alert();
        alert.setId(UUID.randomUUID());
        alert.setTenantCode(tenantCode);
        alert.setAlertType(template.type);
        alert.setSeverity(List.of("LOW", "MEDIUM", "HIGH", "CRITICAL").get(random.nextInt(4)));
        
        // Status distribution: 60% NEW, 20% ACKNOWLEDGED, 15% RESOLVED, 5% DISMISSED
        double statusRoll = random.nextDouble();
        if (statusRoll < 0.6) alert.setStatus("NEW");
        else if (statusRoll < 0.8) alert.setStatus("ACKNOWLEDGED");
        else if (statusRoll < 0.95) alert.setStatus("RESOLVED");
        else alert.setStatus("DISMISSED");

        alert.setTitle(template.title);
        alert.setDescription(generateAlertDescription(template));
        alert.setCreatedAt(baseTime.plus(index * 15L + random.nextInt(10), ChronoUnit.MINUTES));

        if (!"NEW".equals(alert.getStatus())) {
            alert.setAcknowledgedAt(alert.getCreatedAt().plus(5 + random.nextInt(15), ChronoUnit.MINUTES));
        }
        if ("RESOLVED".equals(alert.getStatus()) || "DISMISSED".equals(alert.getStatus())) {
            alert.setResolvedAt(alert.getAcknowledgedAt().plus(10 + random.nextInt(30), ChronoUnit.MINUTES));
        }

        return alert;
    }

    private String generateAlertDescription(AlertTemplate template) {
        return switch (template.type) {
            case "DELAY" -> String.format(template.descriptionTemplate, 
                    "AI" + (100 + random.nextInt(900)), "S" + (1 + random.nextInt(20)), 5 + random.nextInt(20));
            case "GEOFENCE" -> String.format(template.descriptionTemplate,
                    "VH-" + (100 + random.nextInt(900)), 28.5 + random.nextDouble(), 77.1 + random.nextDouble());
            case "MAINTENANCE" -> String.format(template.descriptionTemplate,
                    "VH-" + (100 + random.nextInt(900)));
            case "SLA_BREACH" -> String.format(template.descriptionTemplate,
                    "S" + (1 + random.nextInt(20)), 3 + random.nextInt(10));
            default -> template.descriptionTemplate;
        };
    }

    private String getSeverityForDelay(int delayMinutes) {
        if (delayMinutes >= 30) return "CRITICAL";
        if (delayMinutes >= 20) return "HIGH";
        if (delayMinutes >= 10) return "MEDIUM";
        return "LOW";
    }

    private String getSeverityForDelaySeconds(int delaySeconds) {
        int minutes = delaySeconds / 60;
        return getSeverityForDelay(minutes);
    }

    private void autoResolveOldAlerts(String tenantCode) {
        Instant cutoff = Instant.now().minus(1, ChronoUnit.HOURS);
        List<Alert> oldAlerts = alertRepository.findActiveAlerts(tenantCode);
        
        for (Alert alert : oldAlerts) {
            if (alert.getCreatedAt().isBefore(cutoff) && "LOW".equals(alert.getSeverity())) {
                alert.setStatus("RESOLVED");
                alert.setResolvedAt(Instant.now());
                alertRepository.save(alert);
            }
        }
    }

    private record AlertTemplate(String type, String title, String descriptionTemplate) {}
}
