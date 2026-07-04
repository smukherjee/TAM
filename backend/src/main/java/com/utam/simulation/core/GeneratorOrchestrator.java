package com.utam.simulation.core;

import com.utam.simulation.api.GeneratorController;
import com.utam.simulation.config.SimulationConfig;
import com.utam.simulation.tracking.LocationRegisterUpdater;
import com.utam.simulation.tracking.MovementTrailGenerator;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Orchestrates all data generators, providing centralized control for:
 * - Starting/stopping generators
 * - Batch population mode
 * - Continuous simulation mode
 * - Health status aggregation
 */
@Service
@SuppressWarnings("unused") // meterRegistry reserved for future metrics implementation
public class GeneratorOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(GeneratorOrchestrator.class);

    private final SimulationConfig config;
    private final MeterRegistry meterRegistry;
    private final Map<String, BaseDataGenerator> generators = new ConcurrentHashMap<>();
    private final ExecutorService executorService;

    // T072 & T073: Tracking components (optional, may be null during testing)
    @Autowired(required = false)
    private MovementTrailGenerator trailGenerator;

    @Autowired(required = false)
    private LocationRegisterUpdater registerUpdater;

    // Track running simulations per tenant
    private final Set<String> runningTenants = ConcurrentHashMap.newKeySet();

    private volatile boolean batchMode = false;
    private volatile boolean continuousMode = false;

    public GeneratorOrchestrator(SimulationConfig config, MeterRegistry meterRegistry) {
        this.config = config;
        this.meterRegistry = meterRegistry;
        this.executorService = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors(),
                r -> {
                    Thread t = new Thread(r, "generator-worker");
                    t.setDaemon(true);
                    return t;
                }
        );
        log.info("GeneratorOrchestrator initialized with {} available processors", 
                Runtime.getRuntime().availableProcessors());
    }

    /**
     * Register a generator with the orchestrator.
     */
    public void registerGenerator(BaseDataGenerator generator) {
        generators.put(generator.getGeneratorName(), generator);
        log.info("Registered generator: {} for entity type: {}", 
                generator.getGeneratorName(), generator.getEntityType());
    }

    /**
     * Unregister a generator.
     */
    public void unregisterGenerator(String generatorName) {
        BaseDataGenerator removed = generators.remove(generatorName);
        if (removed != null) {
            removed.stop();
            log.info("Unregistered generator: {}", generatorName);
        }
    }

    /**
     * Start batch population mode - generates initial data for all tenants.
     */
    public BatchPopulationResult startBatchPopulation(String tenantCode) {
        if (batchMode) {
            log.warn("Batch population already in progress");
            return new BatchPopulationResult(false, "Batch already in progress", Map.of());
        }

        batchMode = true;
        log.info("Starting batch population for tenant: {}", tenantCode);

        Map<String, Integer> results = new ConcurrentHashMap<>();
        int batchSize = config.getBatchSize();

        try {
            // Start all generators
            generators.values().forEach(BaseDataGenerator::start);

            // Run batch generation for each generator
            List<java.util.concurrent.Future<?>> futures = new ArrayList<>();
            
            for (BaseDataGenerator generator : generators.values()) {
                futures.add(executorService.submit(() -> {
                    int generated = generator.generateBatchWithMetrics(tenantCode, batchSize);
                    results.put(generator.getGeneratorName(), generated);
                }));
            }

            // Wait for all to complete (with timeout)
            for (var future : futures) {
                try {
                    future.get(120, TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.error("Generator timeout or error: {}", e.getMessage());
                }
            }

            log.info("Batch population complete for tenant: {}. Results: {}", tenantCode, results);
            return new BatchPopulationResult(true, "Batch complete", results);

        } finally {
            batchMode = false;
        }
    }

    /**
     * Start batch population for all configured tenants.
     */
    public Map<String, BatchPopulationResult> startBatchPopulationAllTenants() {
        Map<String, BatchPopulationResult> results = new ConcurrentHashMap<>();
        
        for (String tenantCode : config.getTenants().keySet()) {
            results.put(tenantCode, startBatchPopulation(tenantCode));
        }
        
        return results;
    }

    /**
     * Start continuous simulation mode.
     */
    public void startContinuousSimulation() {
        if (continuousMode) {
            log.warn("Continuous simulation already running");
            return;
        }

        runningTenants.clear();
        runningTenants.addAll(config.getTenants().keySet());
        continuousMode = true;
        generators.values().forEach(BaseDataGenerator::start);
        log.info("Continuous simulation started with {} generators for tenants {}", generators.size(), runningTenants);
    }

    /**
     * Stop continuous simulation mode.
     */
    public void stopContinuousSimulation() {
        if (!continuousMode) {
            log.warn("Continuous simulation is not running");
            return;
        }

        continuousMode = false;
        generators.values().forEach(BaseDataGenerator::stop);
        runningTenants.clear();
        log.info("Continuous simulation stopped");
    }

    /**
     * Scheduled task for continuous generation (runs when continuous mode is enabled).
     */
    @Scheduled(fixedRateString = "${simulation.continuous-interval-ms:1000}")
    public void continuousGenerationTick() {
        if (!continuousMode || !config.isContinuousEnabled()) {
            return;
        }

        // Generate updates only for running tenants
        for (String tenantCode : runningTenants) {
            for (BaseDataGenerator generator : generators.values()) {
                if (generator.isRunning()) {
                    generator.generateSingleUpdateWithMetrics(tenantCode);
                }
            }
        }
    }

    /**
     * Scheduled task for position updates (runs at higher frequency for smooth movement).
     * T069: Enhanced continuous generation for real-time movement.
     * T072/T073: Also updates trail history and location register.
     */
    @Scheduled(fixedRateString = "${simulation.position-update-interval-ms:500}")
    public void positionUpdateTick() {
        if (!continuousMode || !config.isContinuousEnabled()) {
            return;
        }

        // Update positions for all running tenants
        for (String tenantCode : runningTenants) {
            updatePositionsForTenant(tenantCode);
            
            // T072: Capture movement trails
            if (trailGenerator != null) {
                try {
                    trailGenerator.captureTrails(tenantCode);
                } catch (Exception e) {
                    log.trace("Trail capture error: {}", e.getMessage());
                }
            }
            
            // T073: Update location register
            if (registerUpdater != null) {
                try {
                    registerUpdater.updateRegister(tenantCode);
                } catch (Exception e) {
                    log.trace("Register update error: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * Update all entity positions for a tenant (vehicles, flights, assets).
     */
    private void updatePositionsForTenant(String tenantCode) {
        // Position updates are handled by specific generators
        BaseDataGenerator vehicleGen = generators.get("VehicleDataGenerator");
        BaseDataGenerator flightGen = generators.get("FlightDataGenerator");
        
        if (vehicleGen != null && vehicleGen.isRunning()) {
            try {
                vehicleGen.generatePositionUpdate(tenantCode);
            } catch (Exception e) {
                log.trace("Vehicle position update: {}", e.getMessage());
            }
        }
        
        if (flightGen != null && flightGen.isRunning()) {
            try {
                flightGen.generatePositionUpdate(tenantCode);
            } catch (Exception e) {
                log.trace("Flight position update: {}", e.getMessage());
            }
        }
    }

    /**
     * T069: Enable/disable real-time position tracking mode.
     * When enabled, positions are updated at high frequency for smooth animation.
     */
    public void setRealTimeTrackingEnabled(boolean enabled) {
        if (enabled) {
            log.info("Real-time position tracking enabled");
        } else {
            log.info("Real-time position tracking disabled");
        }
        // This is controlled by continuous mode
    }

    /**
     * T080: Generate historical data for analytics and hotspot analysis.
     * Backfills data for a specified number of days in the past.
     * Used for populating analytics dashboards with meaningful patterns.
     *
     * @param tenantCode The tenant to generate data for
     * @param daysBack Number of days of historical data to generate
     * @param samplesPerDay Number of data points per entity per day
     * @return Result with statistics about generated data
     */
    @SuppressWarnings("unused") // now and intervalSeconds prepared for future time-based generation
    public HistoricalDataResult generateHistoricalData(String tenantCode, int daysBack, int samplesPerDay) {
        log.info("Starting historical data generation for tenant {} ({} days, {} samples/day)", 
                 tenantCode, daysBack, samplesPerDay);

        long startTime = System.currentTimeMillis();
        Map<String, Integer> generatedCounts = new ConcurrentHashMap<>();

        // Calculate time intervals
        java.time.Instant now = java.time.Instant.now();
        long intervalSeconds = 86400 / samplesPerDay; // Seconds between samples

        // Generate historical data for each generator that supports it
        for (BaseDataGenerator generator : generators.values()) {
            int count = 0;
            try {
                count = generator.generateHistoricalData(tenantCode, daysBack, samplesPerDay);
                generatedCounts.put(generator.getGeneratorName(), count);
            } catch (UnsupportedOperationException e) {
                log.debug("Generator {} does not support historical data generation", 
                         generator.getGeneratorName());
            } catch (Exception e) {
                log.error("Failed to generate historical data for {}: {}", 
                         generator.getGeneratorName(), e.getMessage());
                generatedCounts.put(generator.getGeneratorName(), -1);
            }
        }

        // T072: Backfill asset_movement_trail for real vehicles (none of the generators above write it)
        if (trailGenerator != null) {
            try {
                int trailPoints = trailGenerator.generateHistoricalTrails(tenantCode, daysBack, samplesPerDay);
                generatedCounts.put("MovementTrailGenerator", trailPoints);
            } catch (Exception e) {
                log.error("Failed to generate historical movement trails for {}: {}", tenantCode, e.getMessage());
                generatedCounts.put("MovementTrailGenerator", -1);
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        int totalRecords = generatedCounts.values().stream()
            .filter(c -> c > 0)
            .mapToInt(Integer::intValue)
            .sum();

        log.info("Historical data generation complete. Total: {} records in {}ms", 
                totalRecords, duration);

        return new HistoricalDataResult(true, totalRecords, duration, generatedCounts);
    }

    /**
     * Result of historical data generation.
     */
    public record HistoricalDataResult(
        boolean success,
        int totalRecords,
        long durationMs,
        Map<String, Integer> recordsByGenerator
    ) {}

    /**
     * Start a specific generator by name.
     */
    public boolean startGenerator(String generatorName) {
        BaseDataGenerator generator = generators.get(generatorName);
        if (generator != null) {
            generator.start();
            return true;
        }
        log.warn("Generator not found: {}", generatorName);
        return false;
    }

    /**
     * Stop a specific generator by name.
     */
    public boolean stopGenerator(String generatorName) {
        BaseDataGenerator generator = generators.get(generatorName);
        if (generator != null) {
            generator.stop();
            return true;
        }
        log.warn("Generator not found: {}", generatorName);
        return false;
    }

    /**
     * Get the status of all generators.
     */
    public OrchestratorStatus getStatus() {
        List<BaseDataGenerator.GeneratorStats> stats = generators.values().stream()
                .map(BaseDataGenerator::getStats)
                .toList();

        return new OrchestratorStatus(
                batchMode,
                continuousMode,
                generators.size(),
                stats.stream().filter(s -> s.running()).count(),
                stats
        );
    }

    /**
     * Get the status of a specific generator.
     */
    public BaseDataGenerator.GeneratorStats getGeneratorStatus(String generatorName) {
        BaseDataGenerator generator = generators.get(generatorName);
        return generator != null ? generator.getStats() : null;
    }

    /**
     * Check if the orchestrator is healthy (at least one generator running when in continuous mode).
     */
    public boolean isHealthy() {
        if (!continuousMode) {
            return true; // Not in continuous mode, so healthy by default
        }
        return generators.values().stream().anyMatch(BaseDataGenerator::isRunning);
    }

    /**
     * Get list of all registered generator names.
     */
    public List<String> getRegisteredGenerators() {
        return new ArrayList<>(generators.keySet());
    }

    /**
     * Shutdown the orchestrator.
     */
    public void shutdown() {
        log.info("Shutting down GeneratorOrchestrator...");
        continuousMode = false;
        runningTenants.clear();
        generators.values().forEach(BaseDataGenerator::stop);
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("GeneratorOrchestrator shutdown complete");
    }

    // =====================================================
    // Methods for GeneratorController (Tenant-specific API)
    // =====================================================

    /**
     * Populate batch data for a specific tenant (API method).
     */
    public Map<String, Integer> populateBatch(String tenantCode, int batchSize) {
        log.info("Populating batch for tenant: {} with batchSize: {}", tenantCode, batchSize);
        
        Map<String, Integer> results = new ConcurrentHashMap<>();
        
        // Ensure generators are started
        generators.values().forEach(BaseDataGenerator::start);
        
        // Run batch generation for each generator
        for (BaseDataGenerator generator : generators.values()) {
            try {
                int generated = generator.generateBatchWithMetrics(tenantCode, batchSize);
                results.put(generator.getEntityType(), generated);
            } catch (Exception e) {
                log.error("Error in generator {}: {}", generator.getGeneratorName(), e.getMessage());
                results.put(generator.getEntityType(), 0);
            }
        }
        
        log.info("Batch population complete for tenant: {}. Results: {}", tenantCode, results);
        return results;
    }

    /**
     * Start continuous simulation for a specific tenant.
     */
    public void startContinuousSimulation(String tenantCode) {
        if (runningTenants.contains(tenantCode)) {
            log.warn("Simulation already running for tenant: {}", tenantCode);
            return;
        }
        
        runningTenants.add(tenantCode);
        generators.values().forEach(BaseDataGenerator::start);
        continuousMode = true;
        log.info("Started continuous simulation for tenant: {}", tenantCode);
    }

    /**
     * Stop simulation for a specific tenant.
     */
    public void stopSimulation(String tenantCode) {
        if (!runningTenants.contains(tenantCode)) {
            log.warn("Simulation not running for tenant: {}", tenantCode);
            return;
        }
        
        runningTenants.remove(tenantCode);
        
        // If no tenants running, stop continuous mode
        if (runningTenants.isEmpty()) {
            continuousMode = false;
            generators.values().forEach(BaseDataGenerator::stop);
        }
        
        log.info("Stopped simulation for tenant: {}", tenantCode);
    }

    /**
     * Get simulation status for a specific tenant.
     */
    public SimulationStatus getStatus(String tenantCode) {
        boolean running = runningTenants.contains(tenantCode);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("running", running);
        stats.put("generatorCount", generators.size());
        
        if (running) {
            for (BaseDataGenerator generator : generators.values()) {
                stats.put(generator.getEntityType() + "_generated", 
                        generator.getStats().recordsGenerated());
            }
        }
        
        return new SimulationStatus(running, stats);
    }

    /**
     * Get status for all tenants.
     */
    public Map<String, SimulationStatus> getAllStatuses() {
        Map<String, SimulationStatus> statuses = new HashMap<>();
        for (String tenantCode : config.getTenants().keySet()) {
            statuses.put(tenantCode, getStatus(tenantCode));
        }
        return statuses;
    }

    /**
     * Populate all tenants with batch data.
     */
    public Map<String, GeneratorController.BatchPopulationResponse> populateAllTenants(int batchSize) {
        Map<String, GeneratorController.BatchPopulationResponse> results = new HashMap<>();
        
        for (String tenantCode : config.getTenants().keySet()) {
            try {
                Map<String, Integer> generated = populateBatch(tenantCode, batchSize);
                results.put(tenantCode, new GeneratorController.BatchPopulationResponse(
                        tenantCode, "SUCCESS", "Batch complete", generated
                ));
            } catch (Exception e) {
                results.put(tenantCode, new GeneratorController.BatchPopulationResponse(
                        tenantCode, "ERROR", e.getMessage(), Map.of()
                ));
            }
        }
        
        return results;
    }

    /**
     * Start simulation for all tenants.
     */
    public Map<String, String> startAllSimulations() {
        Map<String, String> results = new HashMap<>();
        for (String tenantCode : config.getTenants().keySet()) {
            startContinuousSimulation(tenantCode);
            results.put(tenantCode, "STARTED");
        }
        return results;
    }

    /**
     * Stop simulation for all tenants.
     */
    public Map<String, String> stopAllSimulations() {
        Map<String, String> results = new HashMap<>();
        for (String tenantCode : new ArrayList<>(runningTenants)) {
            stopSimulation(tenantCode);
            results.put(tenantCode, "STOPPED");
        }
        return results;
    }

    /**
     * Clear simulation data for a specific tenant.
     * Delegates to SimulationDataService for actual database operations.
     */
    @Autowired(required = false)
    private com.utam.simulation.service.SimulationDataService simulationDataService;

    @Autowired(required = false)
    private com.utam.asset.service.VehicleAssetMapService vehicleAssetMapService;

    public int clearSimulationData(String tenantCode) {
        log.info("Clearing simulation data for tenant: {}", tenantCode);
        if (simulationDataService != null) {
            return simulationDataService.clearDataForTenant(tenantCode);
        }
        log.warn("SimulationDataService not available, no data cleared");
        return 0;
    }

    /**
     * Clear simulation data for all tenants.
     */
    public int clearAllSimulationData() {
        log.info("Clearing simulation data for all tenants");
        if (simulationDataService != null) {
            return simulationDataService.clearAllData();
        }
        log.warn("SimulationDataService not available, no data cleared");
        return 0;
    }

    /**
     * Refresh materialized views after data generation.
     */
    public void refreshMaterializedViews() {
        log.info("Refreshing materialized views");
        if (simulationDataService != null) {
            simulationDataService.refreshMaterializedViews();
        } else {
            log.warn("SimulationDataService not available, views not refreshed");
        }
    }

    /**
     * Sync all vehicles to assets with proper mappings.
     * This ensures every vehicle has a corresponding asset entry.
     * 
     * @param tenantCode The tenant code, or null for all tenants
     * @return Number of mappings created
     */
    public int syncVehiclesToAssets(String tenantCode) {
        log.info("Syncing vehicles to assets for tenant: {}", tenantCode != null ? tenantCode : "ALL");
        if (vehicleAssetMapService != null) {
            return vehicleAssetMapService.syncVehiclesToAssets(tenantCode);
        }
        log.warn("VehicleAssetMapService not available, no sync performed");
        return 0;
    }

    /**
     * Status record for a tenant's simulation.
     */
    public record SimulationStatus(boolean isRunning, Map<String, Object> generatorStats) {
        public boolean isRunning() {
            return isRunning;
        }
    }

    /**
     * Result of batch population operation.
     */
    public record BatchPopulationResult(
            boolean success,
            String message,
            Map<String, Integer> recordsGenerated
    ) {}

    /**
     * Status of the orchestrator.
     */
    public record OrchestratorStatus(
            boolean batchMode,
            boolean continuousMode,
            int totalGenerators,
            long runningGenerators,
            List<BaseDataGenerator.GeneratorStats> generatorStats
    ) {}
}
