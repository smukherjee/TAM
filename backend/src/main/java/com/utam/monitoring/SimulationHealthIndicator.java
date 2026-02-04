package com.utam.monitoring;

import com.utam.simulation.core.GeneratorOrchestrator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator for simulation generators.
 * Exposes generator status via Spring Boot Actuator at /actuator/health/simulation.
 * 
 * Health states:
 * - UP: Continuous mode enabled and at least one generator running, or not in continuous mode
 * - DOWN: Continuous mode enabled but no generators running
 * - UNKNOWN: Orchestrator not available
 */
@Component("simulationHealth")
public class SimulationHealthIndicator implements HealthIndicator {

    private final GeneratorOrchestrator orchestrator;

    public SimulationHealthIndicator(GeneratorOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @Override
    public Health health() {
        if (orchestrator == null) {
            return Health.unknown()
                    .withDetail("error", "GeneratorOrchestrator not available")
                    .build();
        }

        GeneratorOrchestrator.OrchestratorStatus status = orchestrator.getStatus();

        Health.Builder builder = orchestrator.isHealthy() ? Health.up() : Health.down();

        return builder
                .withDetail("batchMode", status.batchMode())
                .withDetail("continuousMode", status.continuousMode())
                .withDetail("totalGenerators", status.totalGenerators())
                .withDetail("runningGenerators", status.runningGenerators())
                .withDetail("generators", status.generatorStats().stream()
                        .map(stat -> new GeneratorHealthInfo(
                                stat.generatorName(),
                                stat.entityType(),
                                stat.running(),
                                stat.recordsGenerated(),
                                stat.errorCount()
                        ))
                        .toList())
                .build();
    }

    /**
     * Simplified generator info for health endpoint.
     */
    record GeneratorHealthInfo(
            String name,
            String entityType,
            boolean running,
            long recordsGenerated,
            long errors
    ) {}
}
