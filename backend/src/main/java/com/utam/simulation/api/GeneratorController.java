package com.utam.simulation.api;

import com.utam.simulation.core.GeneratorOrchestrator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for simulation management.
 * Implements FR-001 to FR-010: Simulation control endpoints.
 */
@RestController
@RequestMapping("/api/simulation")
@Tag(name = "Simulation", description = "Simulation data generation controls")
public class GeneratorController {

    private final GeneratorOrchestrator orchestrator;

    public GeneratorController(GeneratorOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    /**
     * Start batch population for a tenant.
     * POST /api/simulation/{tenantCode}/populate
     */
    @PostMapping("/{tenantCode}/populate")
    @Operation(summary = "Populate tenant with batch data", 
               description = "Generates initial batch of simulation data for the specified tenant")
    public ResponseEntity<BatchPopulationResponse> populateBatch(
            @PathVariable String tenantCode,
            @RequestBody(required = false) BatchPopulationRequest request) {
        
        int batchSize = (request != null && request.batchSize() > 0) 
                ? request.batchSize() 
                : 100; // default

        try {
            Map<String, Integer> results = orchestrator.populateBatch(tenantCode, batchSize);
            return ResponseEntity.ok(new BatchPopulationResponse(
                    tenantCode,
                    "SUCCESS",
                    "Batch population completed",
                    results
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new BatchPopulationResponse(
                    tenantCode,
                    "ERROR",
                    e.getMessage(),
                    Map.of()
            ));
        }
    }

    /**
     * Start continuous simulation for a tenant.
     * POST /api/simulation/{tenantCode}/start
     */
    @PostMapping("/{tenantCode}/start")
    @Operation(summary = "Start continuous simulation", 
               description = "Starts the continuous simulation for the specified tenant")
    public ResponseEntity<SimulationControlResponse> startSimulation(@PathVariable String tenantCode) {
        try {
            orchestrator.startContinuousSimulation(tenantCode);
            return ResponseEntity.ok(new SimulationControlResponse(
                    tenantCode,
                    "STARTED",
                    "Continuous simulation started for tenant"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new SimulationControlResponse(
                    tenantCode,
                    "ERROR",
                    e.getMessage()
            ));
        }
    }

    /**
     * Stop simulation for a tenant.
     * POST /api/simulation/{tenantCode}/stop
     */
    @PostMapping("/{tenantCode}/stop")
    @Operation(summary = "Stop simulation", 
               description = "Stops the simulation for the specified tenant")
    public ResponseEntity<SimulationControlResponse> stopSimulation(@PathVariable String tenantCode) {
        try {
            orchestrator.stopSimulation(tenantCode);
            return ResponseEntity.ok(new SimulationControlResponse(
                    tenantCode,
                    "STOPPED",
                    "Simulation stopped for tenant"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new SimulationControlResponse(
                    tenantCode,
                    "ERROR",
                    e.getMessage()
            ));
        }
    }

    /**
     * Get simulation status for a tenant.
     * GET /api/simulation/{tenantCode}/status
     */
    @GetMapping("/{tenantCode}/status")
    @Operation(summary = "Get simulation status", 
               description = "Returns the current simulation status for the specified tenant")
    public ResponseEntity<SimulationStatusResponse> getStatus(@PathVariable String tenantCode) {
        GeneratorOrchestrator.SimulationStatus status = orchestrator.getStatus(tenantCode);
        return ResponseEntity.ok(new SimulationStatusResponse(
                tenantCode,
                status.isRunning(),
                status.generatorStats()
        ));
    }

    /**
     * Get global simulation status.
     * GET /api/simulation/status
     */
    @GetMapping("/status")
    @Operation(summary = "Get global simulation status", 
               description = "Returns simulation status for all tenants")
    public ResponseEntity<Map<String, GeneratorOrchestrator.SimulationStatus>> getGlobalStatus() {
        return ResponseEntity.ok(orchestrator.getAllStatuses());
    }

    /**
     * Populate all tenants with batch data.
     * POST /api/simulation/populate-all
     */
    @PostMapping("/populate-all")
    @Operation(summary = "Populate all tenants", 
               description = "Generates initial batch data for all configured tenants")
    public ResponseEntity<Map<String, BatchPopulationResponse>> populateAll(
            @RequestBody(required = false) BatchPopulationRequest request) {
        
        int batchSize = (request != null && request.batchSize() > 0) 
                ? request.batchSize() 
                : 100;

        Map<String, BatchPopulationResponse> results = orchestrator.populateAllTenants(batchSize);
        return ResponseEntity.ok(results);
    }

    /**
     * Start simulation for all tenants.
     * POST /api/simulation/start-all
     */
    @PostMapping("/start-all")
    @Operation(summary = "Start all simulations", 
               description = "Starts continuous simulation for all configured tenants")
    public ResponseEntity<Map<String, String>> startAll() {
        Map<String, String> results = orchestrator.startAllSimulations();
        return ResponseEntity.ok(results);
    }

    /**
     * Stop simulation for all tenants.
     * POST /api/simulation/stop-all
     */
    @PostMapping("/stop-all")
    @Operation(summary = "Stop all simulations", 
               description = "Stops simulation for all tenants")
    public ResponseEntity<Map<String, String>> stopAll() {
        Map<String, String> results = orchestrator.stopAllSimulations();
        return ResponseEntity.ok(results);
    }

    // Request/Response DTOs
    public record BatchPopulationRequest(int batchSize) {}

    public record BatchPopulationResponse(
            String tenantCode,
            String status,
            String message,
            Map<String, Integer> generatedCounts
    ) {}

    public record SimulationControlResponse(
            String tenantCode,
            String status,
            String message
    ) {}

    public record SimulationStatusResponse(
            String tenantCode,
            boolean running,
            Map<String, Object> stats
    ) {}
}
