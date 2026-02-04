package com.utam.admin.controller;

import com.utam.admin.service.VehiclePathService;
import com.utam.entity.VehiclePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for vehicle path management.
 * Implements FR-032 to FR-039: Admin path drawing interface.
 */
@RestController
@RequestMapping("/api/admin/paths")
public class PathEditorController {

    private static final Logger log = LoggerFactory.getLogger(PathEditorController.class);

    private final VehiclePathService pathService;

    public PathEditorController(VehiclePathService pathService) {
        this.pathService = pathService;
    }

    /**
     * Get all paths for a tenant.
     */
    @GetMapping
    public ResponseEntity<List<VehiclePath>> getPaths(
            @RequestParam String tenantCode,
            @RequestParam(required = false) Boolean activeOnly) {
        
        List<VehiclePath> paths;
        if (Boolean.TRUE.equals(activeOnly)) {
            paths = pathService.getActivePathsForTenant(tenantCode);
        } else {
            paths = pathService.getPathsForTenant(tenantCode);
        }
        
        return ResponseEntity.ok(paths);
    }

    /**
     * Get paths by vehicle type.
     */
    @GetMapping("/by-vehicle-type")
    public ResponseEntity<List<VehiclePath>> getPathsByVehicleType(
            @RequestParam String tenantCode,
            @RequestParam String vehicleType) {
        
        List<VehiclePath> paths = pathService.getPathsByVehicleType(tenantCode, vehicleType);
        return ResponseEntity.ok(paths);
    }

    /**
     * Get a single path by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<VehiclePath> getPath(@PathVariable Long id) {
        return pathService.getPath(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a new path.
     */
    @PostMapping
    public ResponseEntity<?> createPath(@RequestBody VehiclePath path) {
        try {
            VehiclePath created = pathService.createPath(path);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.warn("Path creation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Validation failed",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Update an existing path.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePath(@PathVariable Long id, @RequestBody VehiclePath path) {
        try {
            VehiclePath updated = pathService.updatePath(id, path);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("Path update failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Validation failed",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Delete a path.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePath(@PathVariable Long id) {
        try {
            pathService.deletePath(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Activate a path.
     */
    @PostMapping("/{id}/activate")
    public ResponseEntity<VehiclePath> activatePath(@PathVariable Long id) {
        try {
            VehiclePath path = pathService.setPathActive(id, true);
            return ResponseEntity.ok(path);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Deactivate a path.
     */
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<VehiclePath> deactivatePath(@PathVariable Long id) {
        try {
            VehiclePath path = pathService.setPathActive(id, false);
            return ResponseEntity.ok(path);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get preview data for path animation.
     */
    @GetMapping("/{id}/preview")
    public ResponseEntity<?> getPathPreview(@PathVariable Long id) {
        try {
            Map<String, Object> preview = pathService.getPathPreview(id);
            return ResponseEntity.ok(preview);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Validate path waypoints against airport boundary.
     */
    @PostMapping("/validate")
    public ResponseEntity<VehiclePathService.ValidationResult> validatePath(
            @RequestParam String tenantCode,
            @RequestBody String waypointsJson) {
        
        VehiclePathService.ValidationResult result = 
                pathService.validatePathBoundary(tenantCode, waypointsJson);
        
        return ResponseEntity.ok(result);
    }
}
