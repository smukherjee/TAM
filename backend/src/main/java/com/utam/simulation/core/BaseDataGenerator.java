package com.utam.simulation.core;

import com.utam.simulation.config.SimulationConfig;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Abstract base class for all data generators.
 * Provides common functionality for:
 * - Tenant-aware generation
 * - Micrometer metrics
 * - Lifecycle management (start/stop)
 * - Error handling and logging
 */
public abstract class BaseDataGenerator {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final SimulationConfig config;
    protected final MeterRegistry meterRegistry;

    // State management
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong recordsGenerated = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    private Instant startedAt;

    // Metrics
    private final Counter recordsCounter;
    private final Counter errorsCounter;
    private final Timer generationTimer;

    protected BaseDataGenerator(SimulationConfig config, MeterRegistry meterRegistry) {
        this.config = config;
        this.meterRegistry = meterRegistry;

        // Initialize metrics with generator-specific tags
        String generatorName = getGeneratorName();
        
        this.recordsCounter = Counter.builder("simulation.records.generated")
                .tag("generator", generatorName)
                .description("Number of records generated")
                .register(meterRegistry);

        this.errorsCounter = Counter.builder("simulation.errors")
                .tag("generator", generatorName)
                .description("Number of generation errors")
                .register(meterRegistry);

        this.generationTimer = Timer.builder("simulation.generation.time")
                .tag("generator", generatorName)
                .description("Time taken for generation batches")
                .register(meterRegistry);
    }

    /**
     * Get the unique name for this generator (used in metrics and logging).
     */
    public abstract String getGeneratorName();

    /**
     * Get the entity type this generator produces.
     */
    public abstract String getEntityType();

    /**
     * Generate a batch of data for the specified tenant.
     * 
     * @param tenantCode The tenant code (e.g., "VIDP", "YBBN")
     * @param batchSize Number of records to generate
     * @return Number of records actually generated
     */
    public abstract int generateBatch(String tenantCode, int batchSize);

    /**
     * Generate a single update for continuous simulation.
     * 
     * @param tenantCode The tenant code
     * @return true if generation was successful
     */
    public abstract boolean generateSingleUpdate(String tenantCode);

    /**
     * Called when the generator is started.
     */
    protected void onStart() {
        log.info("{} generator started", getGeneratorName());
    }

    /**
     * Called when the generator is stopped.
     */
    protected void onStop() {
        log.info("{} generator stopped. Total records: {}, Errors: {}", 
                getGeneratorName(), recordsGenerated.get(), errorCount.get());
    }

    /**
     * Start the generator.
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            startedAt = Instant.now();
            onStart();
        } else {
            log.warn("{} generator is already running", getGeneratorName());
        }
    }

    /**
     * Stop the generator.
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            onStop();
        } else {
            log.warn("{} generator is not running", getGeneratorName());
        }
    }

    /**
     * Check if the generator is currently running.
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Generate batch with metrics and error handling.
     */
    public int generateBatchWithMetrics(String tenantCode, int batchSize) {
        if (!isRunning()) {
            log.warn("{} generator is not running, skipping batch generation", getGeneratorName());
            return 0;
        }

        // Validate tenant
        if (!isValidTenant(tenantCode)) {
            log.error("Invalid tenant code: {}", tenantCode);
            return 0;
        }

        return generationTimer.record(() -> {
            try {
                int generated = generateBatch(tenantCode, batchSize);
                recordsGenerated.addAndGet(generated);
                recordsCounter.increment(generated);
                log.debug("{}: Generated {} records for tenant {}", 
                        getGeneratorName(), generated, tenantCode);
                return generated;
            } catch (Exception e) {
                errorCount.incrementAndGet();
                errorsCounter.increment();
                log.error("{}: Error generating batch for tenant {}: {}", 
                        getGeneratorName(), tenantCode, e.getMessage(), e);
                return 0;
            }
        });
    }

    /**
     * Generate single update with metrics and error handling.
     */
    public boolean generateSingleUpdateWithMetrics(String tenantCode) {
        if (!isRunning()) {
            return false;
        }

        if (!isValidTenant(tenantCode)) {
            return false;
        }

        try {
            boolean success = generateSingleUpdate(tenantCode);
            if (success) {
                recordsGenerated.incrementAndGet();
                recordsCounter.increment();
            }
            return success;
        } catch (Exception e) {
            errorCount.incrementAndGet();
            errorsCounter.increment();
            log.error("{}: Error generating update for tenant {}: {}", 
                    getGeneratorName(), tenantCode, e.getMessage());
            return false;
        }
    }

    /**
     * Validate that the tenant code is configured.
     */
    protected boolean isValidTenant(String tenantCode) {
        return config.getTenants().containsKey(tenantCode);
    }

    /**
     * Get current statistics for this generator.
     */
    public GeneratorStats getStats() {
        return new GeneratorStats(
                getGeneratorName(),
                getEntityType(),
                isRunning(),
                recordsGenerated.get(),
                errorCount.get(),
                startedAt
        );
    }

    /**
     * Check if the generator is enabled (configured and runnable).
     */
    public boolean isEnabled() {
        return config != null && !config.getTenants().isEmpty();
    }

    /**
     * Generate a lightweight position update (for real-time tracking).
     * Default implementation does nothing. Override in generators that track positions.
     *
     * @param tenantCode The tenant code
     */
    public void generatePositionUpdate(String tenantCode) {
        // Default: no-op. Override in position-tracking generators.
    }

    /**
     * T080: Generate historical data for analytics and hotspot analysis.
     * Default implementation throws UnsupportedOperationException.
     * Override in generators that support historical data generation.
     *
     * @param tenantCode The tenant code
     * @param daysBack Number of days to backfill
     * @param samplesPerDay Number of samples per day per entity
     * @return Number of records generated
     */
    public int generateHistoricalData(String tenantCode, int daysBack, int samplesPerDay) {
        throw new UnsupportedOperationException(
            getGeneratorName() + " does not support historical data generation");
    }

    /**
     * Statistics record for generator state.
     */
    public record GeneratorStats(
            String generatorName,
            String entityType,
            boolean running,
            long recordsGenerated,
            long errorCount,
            Instant startedAt
    ) {}
}
