package com.utam.simulation.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.utam.entity.AirportBoundary;
import com.utam.repository.AirportBoundaryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for validating positions against airport boundaries.
 * Implements FR-028, FR-029, FR-031: All ground positions must stay within airport perimeter.
 */
@Service
public class BoundaryValidator {

    private static final Logger log = LoggerFactory.getLogger(BoundaryValidator.class);

    private final AirportBoundaryRepository boundaryRepository;
    private final ObjectMapper objectMapper;

    public BoundaryValidator(AirportBoundaryRepository boundaryRepository, ObjectMapper objectMapper) {
        this.boundaryRepository = boundaryRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Check if a position is within the airport boundary.
     * Uses a quick bounding box check first, then ray casting for polygon containment.
     *
     * @param tenantCode The tenant code
     * @param latitude The latitude to check
     * @param longitude The longitude to check
     * @return true if the position is within bounds, false otherwise
     */
    public boolean isWithinBoundary(String tenantCode, double latitude, double longitude) {
        Optional<AirportBoundary> boundaryOpt = boundaryRepository.findByTenantCode(tenantCode);
        
        if (boundaryOpt.isEmpty()) {
            log.warn("No boundary defined for tenant {}, allowing all positions", tenantCode);
            return true;
        }

        AirportBoundary boundary = boundaryOpt.get();

        // Quick bounding box check
        if (!boundary.isWithinBoundingBox(latitude, longitude)) {
            return false;
        }

        // Full polygon containment check using ray casting algorithm
        try {
            return isPointInPolygon(boundary.getBoundaryGeoJson(), latitude, longitude);
        } catch (Exception e) {
            log.error("Error checking polygon containment: {}", e.getMessage());
            return true; // Fail open - allow the position if we can't check
        }
    }

    /**
     * Clamp a position to the nearest point within the airport boundary.
     * If the position is already within bounds, return it unchanged.
     *
     * @param tenantCode The tenant code
     * @param latitude The latitude
     * @param longitude The longitude
     * @return A ClampedPosition with potentially adjusted coordinates
     */
    public ClampedPosition clampToBoundary(String tenantCode, double latitude, double longitude) {
        Optional<AirportBoundary> boundaryOpt = boundaryRepository.findByTenantCode(tenantCode);
        
        if (boundaryOpt.isEmpty()) {
            return new ClampedPosition(latitude, longitude, false);
        }

        AirportBoundary boundary = boundaryOpt.get();

        // If within bounds, return unchanged
        if (isWithinBoundary(tenantCode, latitude, longitude)) {
            return new ClampedPosition(latitude, longitude, false);
        }

        // Clamp to bounding box as simple approximation
        double clampedLat = clamp(latitude, boundary.getMinLatitude(), boundary.getMaxLatitude());
        double clampedLon = clamp(longitude, boundary.getMinLongitude(), boundary.getMaxLongitude());

        return new ClampedPosition(clampedLat, clampedLon, true);
    }

    /**
     * Get the center point of the airport boundary.
     */
    public Optional<double[]> getBoundaryCenter(String tenantCode) {
        Optional<AirportBoundary> boundaryOpt = boundaryRepository.findByTenantCode(tenantCode);
        
        if (boundaryOpt.isEmpty()) {
            return Optional.empty();
        }

        AirportBoundary boundary = boundaryOpt.get();
        
        if (boundary.getMinLatitude() == null || boundary.getMaxLatitude() == null ||
            boundary.getMinLongitude() == null || boundary.getMaxLongitude() == null) {
            return Optional.empty();
        }

        double centerLat = (boundary.getMinLatitude() + boundary.getMaxLatitude()) / 2;
        double centerLon = (boundary.getMinLongitude() + boundary.getMaxLongitude()) / 2;

        return Optional.of(new double[]{centerLat, centerLon});
    }

    /**
     * Ray casting algorithm for point-in-polygon check.
     */
    private boolean isPointInPolygon(String geoJson, double latitude, double longitude) {
        try {
            JsonNode root = objectMapper.readTree(geoJson);
            JsonNode coordinates = root.get("coordinates");
            
            if (coordinates == null || !coordinates.isArray() || coordinates.size() == 0) {
                return true;
            }

            JsonNode ring = coordinates.get(0);
            if (ring == null || !ring.isArray() || ring.size() < 3) {
                return true;
            }

            int n = ring.size();
            boolean inside = false;

            double x = longitude;
            double y = latitude;

            for (int i = 0, j = n - 1; i < n; j = i++) {
                JsonNode pi = ring.get(i);
                JsonNode pj = ring.get(j);
                
                double xi = pi.get(0).asDouble();
                double yi = pi.get(1).asDouble();
                double xj = pj.get(0).asDouble();
                double yj = pj.get(1).asDouble();

                if (((yi > y) != (yj > y)) &&
                    (x < (xj - xi) * (y - yi) / (yj - yi) + xi)) {
                    inside = !inside;
                }
            }

            return inside;

        } catch (Exception e) {
            log.error("Error parsing GeoJSON for polygon check: {}", e.getMessage());
            return true; // Fail open
        }
    }

    private double clamp(double value, Double min, Double max) {
        if (min != null && value < min) return min;
        if (max != null && value > max) return max;
        return value;
    }

    /**
     * Result of clamping a position to the boundary.
     */
    public record ClampedPosition(double latitude, double longitude, boolean wasClamped) {}
}
