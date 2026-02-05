package com.utam.simulation.turnaround;

import com.utam.simulation.config.SimulationConfig;
import com.utam.simulation.core.BaseDataGenerator;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Generates simulated turnaround event data.
 * Creates realistic aircraft turnaround scenarios for testing.
 * 
 * Stub implementation for simulation infrastructure.
 */
@Component
public class TurnaroundDataGenerator extends BaseDataGenerator {

    private final TurnaroundRepository turnaroundRepository;
    private final java.util.Random random = new java.util.Random();

    public TurnaroundDataGenerator(SimulationConfig config,
            MeterRegistry meterRegistry,
            TurnaroundRepository turnaroundRepository) {
        super(config, meterRegistry);
        this.turnaroundRepository = turnaroundRepository;
    }

    @Override
    public String getGeneratorName() {
        return "TurnaroundDataGenerator";
    }

    @Override
    public String getEntityType() {
        return "Turnaround";
    }

    @Override
    public int generateBatch(String tenantCode, int batchSize) {
        log.debug("Generating batch of {} turnaround events for tenant: {}", batchSize, tenantCode);
        java.time.Instant now = java.time.Instant.now();
        java.util.List<Turnaround> batchedEvents = new java.util.ArrayList<>();

        for (int i = 0; i < batchSize; i++) {
            Turnaround t = createRandomTurnaround(tenantCode,
                    now.plus(java.time.Duration.ofMinutes(random.nextInt(120))));
            batchedEvents.add(t);
        }

        turnaroundRepository.saveAll(batchedEvents);
        log.info("Persisted {} turnaround events for {}", batchedEvents.size(), tenantCode);
        return batchedEvents.size();
    }

    @Override
    public boolean generateSingleUpdate(String tenantCode) {
        log.debug("Generating single turnaround update for tenant: {}", tenantCode);
        // For simplicity, just create a new 'live' one periodically or update existing.
        // Here we just create a new one to ensure flow.
        Turnaround t = createRandomTurnaround(tenantCode, java.time.Instant.now());
        turnaroundRepository.save(t);
        return true;
    }

    private Turnaround createRandomTurnaround(String tenantCode, java.time.Instant startTime) {
        Turnaround t = new Turnaround();
        t.setId(java.util.UUID.randomUUID());
        t.setTenantCode(tenantCode);
        t.setFlightNumber("FL" + (100 + random.nextInt(900)));
        t.setStandCode("ST-" + (10 + random.nextInt(50)));
        t.setAircraftType(random.nextBoolean() ? "A320" : "B737");

        t.setStartTime(startTime);
        t.setPlannedDurationMinutes(45 + random.nextInt(30));

        // Random status
        int statusPick = random.nextInt(4);
        if (statusPick == 0)
            t.setStatus("SCHEDULED");
        else if (statusPick == 1)
            t.setStatus("IN_PROGRESS");
        else if (statusPick == 2)
            t.setStatus("COMPLETED");
        else
            t.setStatus("DELAYED");

        t.setCurrentPhase("SERVICING");

        if ("DELAYED".equals(t.getStatus()) || random.nextDouble() > 0.8) {
            t.setDelayMinutes(10 + random.nextInt(50));
            t.setDelayReason("Operational Delay");
        } else {
            t.setDelayMinutes(0);
        }

        t.setLastUpdated(java.time.Instant.now());
        return t;
    }
}
