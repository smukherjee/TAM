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

    @SuppressWarnings("unused") // Reserved for turnaround event persistence
    private final TurnaroundRepository turnaroundRepository;

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
        // Stub implementation - generate mock turnaround events
        log.debug("Generating batch of {} turnaround events for tenant: {}", batchSize, tenantCode);
        // TODO: Implement realistic turnaround event batch generation
        return 0;
    }

    @Override
    public boolean generateSingleUpdate(String tenantCode) {
        // Stub implementation - generate single turnaround update
        log.debug("Generating single turnaround update for tenant: {}", tenantCode);
        // TODO: Implement realistic single turnaround update
        return true;
    }
}
