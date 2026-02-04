package com.utam.admin.service;

import com.utam.entity.VehiclePath;
import com.utam.repository.VehiclePathRepository;
import com.utam.simulation.security.BoundaryValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * Service for managing vehicle paths.
 * Implements FR-032 to FR-039: Admin path drawing interface.
 */
@Service
@Transactional
public class VehiclePathService {

    private static final Logger log = LoggerFactory.getLogger(VehiclePathService.class);

    private final VehiclePathRepository pathRepository;
    private final BoundaryValidator boundaryValidator;
    private final ObjectMapper objectMapper;

    public VehiclePathService(VehiclePathRepository pathRepository,
                               BoundaryValidator boundaryValidator,
                               ObjectMapper objectMapper) {
        this.pathRepository = pathRepository;
        this.boundaryValidator = boundaryValidator;
        this.objectMapper = objectMapper;
    }

    /**
     * Get all paths for a tenant.
     */
    public List<VehiclePath> getPathsForTenant(String tenantCode) {
        return pathRepository.findByTenantCode(tenantCode);
    }

    /**
     * Get active paths for a tenant.
     */
    public List<VehiclePath> getActivePathsForTenant(String tenantCode) {
        return pathRepository.findByTenantCodeAndActive(tenantCode, true);
    }

    /**
     * Get paths by vehicle type.
     */
    public List<VehiclePath> getPathsByVehicleType(String tenantCode, String vehicleTypeCode) {
        return pathRepository.findByTenantCodeAndVehicleTypeCodeAndActive(
                tenantCode, vehicleTypeCode, true);
    }

    /**
     * Get a single path by ID.
     */
    public Optional<VehiclePath> getPath(Long id) {
        return pathRepository.findById(id);
    }

    /**
     * Create a new path.
     */
    public VehiclePath createPath(VehiclePath path) {
        validatePath(path);
        
        // Calculate distance and duration
        calculatePathMetrics(path);
        
        path.setCreatedAt(Instant.now());
        VehiclePath saved = pathRepository.save(path);
        
        log.info("Created vehicle path '{}' for tenant {} with {} waypoints",
                path.getName(), path.getTenantCode(), countWaypoints(path.getWaypoints()));
        
        return saved;
    }

    /**
     * Update an existing path.
     */
    public VehiclePath updatePath(Long id, VehiclePath updated) {
        VehiclePath existing = pathRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Path not found: " + id));

        validatePath(updated);

        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setVehicleTypeCode(updated.getVehicleTypeCode());
        existing.setWaypoints(updated.getWaypoints());
        existing.setSchedule(updated.getSchedule());
        existing.setActive(updated.getActive());
        existing.setLoop(updated.getLoop());
        existing.setUpdatedAt(Instant.now());

        calculatePathMetrics(existing);

        VehiclePath saved = pathRepository.save(existing);
        log.info("Updated vehicle path '{}' (ID: {})", saved.getName(), id);
        
        return saved;
    }

    /**
     * Delete a path.
     */
    public void deletePath(Long id) {
        if (!pathRepository.existsById(id)) {
            throw new IllegalArgumentException("Path not found: " + id);
        }
        pathRepository.deleteById(id);
        log.info("Deleted vehicle path ID: {}", id);
    }

    /**
     * Activate or deactivate a path.
     */
    public VehiclePath setPathActive(Long id, boolean active) {
        VehiclePath path = pathRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Path not found: " + id));
        
        path.setActive(active);
        path.setUpdatedAt(Instant.now());
        
        return pathRepository.save(path);
    }

    /**
     * Get preview data for animating a path.
     */
    public Map<String, Object> getPathPreview(Long id) {
        VehiclePath path = pathRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Path not found: " + id));

        Map<String, Object> preview = new LinkedHashMap<>();
        preview.put("id", path.getId());
        preview.put("name", path.getName());
        preview.put("vehicleType", path.getVehicleTypeCode());
        preview.put("waypoints", parseWaypoints(path.getWaypoints()));
        preview.put("distanceKm", path.getTotalDistanceKm());
        preview.put("durationSeconds", path.getEstimatedDurationSeconds());
        preview.put("loop", path.getLoop());

        return preview;
    }

    /**
     * Validate all waypoints are within airport boundary.
     */
    public ValidationResult validatePathBoundary(String tenantCode, String waypointsJson) {
        List<double[]> waypoints = parseWaypoints(waypointsJson);
        ValidationResult result = new ValidationResult();
        result.setValid(true);

        for (int i = 0; i < waypoints.size(); i++) {
            double[] point = waypoints.get(i);
            if (!boundaryValidator.isWithinBoundary(tenantCode, point[0], point[1])) {
                result.setValid(false);
                result.addError(String.format("Waypoint %d is outside airport boundary", i + 1));
            }
        }

        return result;
    }

    private void validatePath(VehiclePath path) {
        if (path.getTenantCode() == null || path.getTenantCode().isEmpty()) {
            throw new IllegalArgumentException("Tenant code is required");
        }
        if (path.getName() == null || path.getName().isEmpty()) {
            throw new IllegalArgumentException("Path name is required");
        }
        if (path.getWaypoints() == null || path.getWaypoints().isEmpty()) {
            throw new IllegalArgumentException("Waypoints are required");
        }

        // Validate waypoints format
        List<double[]> waypoints = parseWaypoints(path.getWaypoints());
        if (waypoints.size() < 2) {
            throw new IllegalArgumentException("Path must have at least 2 waypoints");
        }

        // Validate boundary
        ValidationResult boundaryResult = validatePathBoundary(
                path.getTenantCode(), path.getWaypoints());
        if (!boundaryResult.isValid()) {
            throw new IllegalArgumentException("Path validation failed: " + 
                    String.join(", ", boundaryResult.getErrors()));
        }
    }

    private void calculatePathMetrics(VehiclePath path) {
        List<double[]> waypoints = parseWaypoints(path.getWaypoints());
        
        // Calculate total distance
        double totalDistance = 0;
        for (int i = 0; i < waypoints.size() - 1; i++) {
            totalDistance += haversineDistance(
                    waypoints.get(i)[0], waypoints.get(i)[1],
                    waypoints.get(i + 1)[0], waypoints.get(i + 1)[1]
            );
        }
        path.setTotalDistanceKm(Math.round(totalDistance * 1000.0) / 1000.0);

        // Estimate duration (assuming 20 km/h average)
        double averageSpeedKmh = getAverageSpeedForVehicleType(path.getVehicleTypeCode());
        int durationSeconds = (int) (totalDistance / averageSpeedKmh * 3600);
        path.setEstimatedDurationSeconds(durationSeconds);
    }

    private List<double[]> parseWaypoints(String waypointsJson) {
        List<double[]> result = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(waypointsJson);
            if (root.isArray()) {
                for (JsonNode node : root) {
                    if (node.isArray() && node.size() >= 2) {
                        double[] point = new double[]{
                                node.get(0).asDouble(),
                                node.get(1).asDouble()
                        };
                        result.add(point);
                    } else if (node.has("lat") && node.has("lng")) {
                        double[] point = new double[]{
                                node.get("lat").asDouble(),
                                node.get("lng").asDouble()
                        };
                        result.add(point);
                    }
                }
            }
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse waypoints: {}", e.getMessage());
        }
        return result;
    }

    private int countWaypoints(String waypointsJson) {
        return parseWaypoints(waypointsJson).size();
    }

    private double getAverageSpeedForVehicleType(String vehicleType) {
        if (vehicleType == null) return 15.0;
        return switch (vehicleType) {
            case "BUS" -> 25.0;
            case "BAGGAGE_TUG" -> 22.0;
            case "PUSHBACK" -> 8.0;
            case "GPU" -> 10.0;
            default -> 18.0;
        };
    }

    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    public static class ValidationResult {
        private boolean valid;
        private List<String> errors = new ArrayList<>();

        public boolean isValid() { return valid; }
        public void setValid(boolean v) { this.valid = v; }
        public List<String> getErrors() { return errors; }
        public void addError(String e) { this.errors.add(e); }
    }
}
