package com.utam.admin.controller;

import com.utam.simulation.core.BaseDataGenerator;
import com.utam.simulation.core.GeneratorOrchestrator;
import com.utam.simulation.tracking.LocationRegisterUpdater;
import com.utam.simulation.tracking.MovementTrailGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin REST controller for generator management.
 * Provides endpoints for the admin UI to control and monitor data generators.
 * Maps to /api/admin/generators/* endpoints expected by frontend.
 */
@RestController
@RequestMapping("/api/admin/generators")
@Tag(name = "Admin Generators", description = "Admin endpoints for data generator control")
public class AdminGeneratorController {

    private static final Logger log = LoggerFactory.getLogger(AdminGeneratorController.class);

    private final GeneratorOrchestrator orchestrator;

    @Autowired(required = false)
    private MovementTrailGenerator trailGenerator;

    @Autowired(required = false)
    private LocationRegisterUpdater registerUpdater;

    public AdminGeneratorController(GeneratorOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    /**
     * Get status of all generators.
     * GET /api/admin/generators/status
     */
    @GetMapping("/status")
    @Operation(summary = "Get all generator statuses")
    public ResponseEntity<GeneratorStatusResponse> getStatus() {
        GeneratorOrchestrator.OrchestratorStatus status = orchestrator.getStatus();
        return ResponseEntity.ok(new GeneratorStatusResponse(
                status.batchMode(),
                status.continuousMode(),
                status.totalGenerators(),
                (int) status.runningGenerators(),
                status.generatorStats()
        ));
    }

    /**
     * Start batch population for a tenant.
     * POST /api/admin/generators/batch/{tenantCode}
     */
    @PostMapping("/batch/{tenantCode}")
    @Operation(summary = "Start batch population for a tenant")
    public ResponseEntity<BatchResponse> startBatch(
            @PathVariable String tenantCode,
            @RequestParam(defaultValue = "100") int batchSize) {
        try {
            log.info("Starting batch population for tenant: {} with batchSize: {}", tenantCode, batchSize);
            Map<String, Integer> results = orchestrator.populateBatch(tenantCode, batchSize);
            return ResponseEntity.ok(new BatchResponse(
                    true,
                    "Batch population completed",
                    tenantCode,
                    results
            ));
        } catch (Exception e) {
            log.error("Batch population failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(new BatchResponse(
                    false,
                    "Failed: " + e.getMessage(),
                    tenantCode,
                    Map.of()
            ));
        }
    }

    /**
     * Start batch population for all tenants.
     * POST /api/admin/generators/batch/all
     */
    @PostMapping("/batch/all")
    @Operation(summary = "Start batch population for all tenants")
    public ResponseEntity<Map<String, BatchResponse>> startBatchAll(
            @RequestParam(defaultValue = "100") int batchSize) {
        try {
            log.info("Starting batch population for all tenants with batchSize: {}", batchSize);
            Map<String, GeneratorOrchestrator.BatchPopulationResult> results = 
                    orchestrator.startBatchPopulationAllTenants();
            
            Map<String, BatchResponse> responses = new java.util.HashMap<>();
            results.forEach((tenant, result) -> {
                responses.put(tenant, new BatchResponse(
                        result.success(),
                        result.message(),
                        tenant,
                        result.recordsGenerated()
                ));
            });
            
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Batch population failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of());
        }
    }

    /**
     * Start continuous simulation mode.
     * POST /api/admin/generators/continuous/start
     */
    @PostMapping("/continuous/start")
    @Operation(summary = "Start continuous simulation")
    public ResponseEntity<ControlResponse> startContinuous() {
        try {
            orchestrator.startContinuousSimulation();
            return ResponseEntity.ok(new ControlResponse(
                    true,
                    "Continuous simulation started",
                    "RUNNING"
            ));
        } catch (Exception e) {
            log.error("Failed to start continuous simulation: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(new ControlResponse(
                    false,
                    "Failed: " + e.getMessage(),
                    "ERROR"
            ));
        }
    }

    /**
     * Stop continuous simulation mode.
     * POST /api/admin/generators/continuous/stop
     */
    @PostMapping("/continuous/stop")
    @Operation(summary = "Stop continuous simulation")
    public ResponseEntity<ControlResponse> stopContinuous() {
        try {
            orchestrator.stopContinuousSimulation();
            return ResponseEntity.ok(new ControlResponse(
                    true,
                    "Continuous simulation stopped",
                    "STOPPED"
            ));
        } catch (Exception e) {
            log.error("Failed to stop continuous simulation: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(new ControlResponse(
                    false,
                    "Failed: " + e.getMessage(),
                    "ERROR"
            ));
        }
    }

    /**
     * Start a specific generator.
     * POST /api/admin/generators/{name}/start
     */
    @PostMapping("/{name}/start")
    @Operation(summary = "Start a specific generator")
    public ResponseEntity<ControlResponse> startGenerator(@PathVariable String name) {
        boolean started = orchestrator.startGenerator(name);
        if (started) {
            return ResponseEntity.ok(new ControlResponse(true, "Generator started", "RUNNING"));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Stop a specific generator.
     * POST /api/admin/generators/{name}/stop
     */
    @PostMapping("/{name}/stop")
    @Operation(summary = "Stop a specific generator")
    public ResponseEntity<ControlResponse> stopGenerator(@PathVariable String name) {
        boolean stopped = orchestrator.stopGenerator(name);
        if (stopped) {
            return ResponseEntity.ok(new ControlResponse(true, "Generator stopped", "STOPPED"));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get trail statistics.
     * GET /api/admin/generators/trails/stats
     */
    @GetMapping("/trails/stats")
    @Operation(summary = "Get movement trail statistics")
    public ResponseEntity<TrailStatsResponse> getTrailStats() {
        if (trailGenerator == null) {
            return ResponseEntity.ok(new TrailStatsResponse(0, 0, 0, 0, 30));
        }
        
        MovementTrailGenerator.TrailStatistics stats = trailGenerator.getStatistics();
        return ResponseEntity.ok(new TrailStatsResponse(
                stats.getVehicleTrailCount(),
                stats.getFlightTrailCount(),
                stats.getTotalTrailPoints(),
                stats.getMaxTrailPointsPerEntity(),
                stats.getRetentionMinutes()
        ));
    }

    /**
     * Get location register statistics.
     * GET /api/admin/generators/register/stats
     */
    @GetMapping("/register/stats")
    @Operation(summary = "Get location register statistics")
    public ResponseEntity<RegisterStatsResponse> getRegisterStats() {
        if (registerUpdater == null) {
            return ResponseEntity.ok(new RegisterStatsResponse(0, 0, 5000));
        }
        
        LocationRegisterUpdater.RegisterStatistics stats = registerUpdater.getStatistics();
        return ResponseEntity.ok(new RegisterStatsResponse(
                stats.getVehicleCount(),
                stats.getFlightCount(),
                stats.getStaleThresholdMs()
        ));
    }

    /**
     * Generate historical data for analytics.
     * POST /api/admin/generators/historical
     */
    @PostMapping("/historical")
    @Operation(summary = "Generate historical data for analytics")
    public ResponseEntity<HistoricalDataResponse> generateHistoricalData(
            @RequestParam(defaultValue = "VIDP") String tenantCode,
            @RequestParam(defaultValue = "7") int daysBack,
            @RequestParam(defaultValue = "24") int samplesPerDay) {
        return handleHistoricalGeneration(tenantCode, daysBack, samplesPerDay);
    }

    /**
     * Backward-compatible path variant for frontend (/historical/{tenant}?days=7&samplesPerDay=24).
     */
    @PostMapping("/historical/{tenantCode}")
    @Operation(summary = "Generate historical data for analytics (tenant path param)")
    public ResponseEntity<HistoricalDataResponse> generateHistoricalDataWithPath(
            @PathVariable String tenantCode,
            @RequestParam(name = "days", defaultValue = "7") int days,
            @RequestParam(name = "samplesPerDay", defaultValue = "24") int samplesPerDay) {
        return handleHistoricalGeneration(tenantCode, days, samplesPerDay);
    }

    private ResponseEntity<HistoricalDataResponse> handleHistoricalGeneration(String tenantCode, int daysBack, int samplesPerDay) {
        try {
            GeneratorOrchestrator.HistoricalDataResult result =
                    orchestrator.generateHistoricalData(tenantCode, daysBack, samplesPerDay);
            return ResponseEntity.ok(new HistoricalDataResponse(
                    result.success(),
                    result.totalRecords(),
                    result.durationMs(),
                    result.recordsByGenerator()
            ));
        } catch (Exception e) {
            log.error("Historical data generation failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(new HistoricalDataResponse(
                    false, 0, 0, Map.of()
            ));
        }
    }

    /**
     * Export generator logs.
     * GET /api/admin/generators/logs/export
     */
    @GetMapping("/logs/export")
    @Operation(summary = "Export generator logs")
    public ResponseEntity<Map<String, Object>> exportLogs() {
        // Return a simple log export structure
        return ResponseEntity.ok(Map.of(
                "exportedAt", java.time.Instant.now().toString(),
                "generators", orchestrator.getRegisteredGenerators(),
                "status", orchestrator.getStatus()
        ));
    }

    /**
     * Get list of registered generators.
     * GET /api/admin/generators/list
     */
    @GetMapping("/list")
    @Operation(summary = "List all registered generators")
    public ResponseEntity<List<String>> listGenerators() {
        return ResponseEntity.ok(orchestrator.getRegisteredGenerators());
    }

    // Response DTOs
    public record GeneratorStatusResponse(
            boolean batchMode,
            boolean continuousMode,
            int generatorCount,
            int runningCount,
            List<BaseDataGenerator.GeneratorStats> generators
    ) {}

    public record BatchResponse(
            boolean success,
            String message,
            String tenantCode,
            Map<String, Integer> results
    ) {}

    public record ControlResponse(
            boolean success,
            String message,
            String status
    ) {}

    public record TrailStatsResponse(
            int vehicleTrailCount,
            int flightTrailCount,
            int totalTrailPoints,
            int maxTrailPointsPerEntity,
            int retentionMinutes
    ) {}

    public record RegisterStatsResponse(
            int vehicleCount,
            int flightCount,
            long staleThresholdMs
    ) {}

    public record HistoricalDataResponse(
            boolean success,
            int totalRecords,
            long durationMs,
            Map<String, Integer> recordsByGenerator
    ) {}
}
