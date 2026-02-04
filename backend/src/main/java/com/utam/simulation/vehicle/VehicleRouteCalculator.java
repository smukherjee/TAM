package com.utam.simulation.vehicle;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Calculates vehicle routes between waypoints for path playback.
 * Stub implementation for simulation infrastructure.
 * 
 * TODO: Implement actual route calculation using A* or Dijkstra algorithms.
 */
@Component
public class VehicleRouteCalculator {

    /**
     * Represents a position with coordinates and heading.
     */
    public record Position(double latitude, double longitude, double heading) {}

    /**
     * Represents a waypoint in a route.
     */
    public record Waypoint(double latitude, double longitude, int durationSeconds) {}

    /**
     * Represents a calculated route between waypoints.
     */
    public record Route(UUID vehicleId, List<Waypoint> waypoints, int estimatedSeconds) {
        /**
         * Check if route has no waypoints.
         */
        public boolean isEmpty() {
            return waypoints == null || waypoints.isEmpty();
        }
        
        /**
         * Get the destination (last waypoint) of the route.
         */
        public String destination() {
            if (isEmpty()) {
                return "unknown";
            }
            Waypoint last = waypoints.get(waypoints.size() - 1);
            return String.format("(%.4f, %.4f)", last.latitude(), last.longitude());
        }
    }

    /**
     * Calculate a route between origin and destination.
     */
    public Route calculateRoute(double originLat, double originLon,
                                 double destLat, double destLon) {
        // Stub implementation - direct route
        List<Waypoint> waypoints = List.of(
                new Waypoint(originLat, originLon, 0),
                new Waypoint(destLat, destLon, 60)
        );
        return new Route(null, waypoints, 60);
    }

    /**
     * Calculate a patrol route around the airport.
     */
    public Route calculatePatrolRoute(UUID vehicleId, double centerLat, double centerLon, 
                                       double radiusKm) {
        // Stub implementation - circular patrol
        List<Waypoint> waypoints = List.of(
                new Waypoint(centerLat, centerLon, 0),
                new Waypoint(centerLat + 0.001, centerLon + 0.001, 30),
                new Waypoint(centerLat, centerLon + 0.002, 60),
                new Waypoint(centerLat - 0.001, centerLon + 0.001, 90),
                new Waypoint(centerLat, centerLon, 120)
        );
        return new Route(vehicleId, waypoints, 120);
    }

    /**
     * Get position at a specific time along a route.
     */
    public Position getPositionAtTime(Route route, int elapsedSeconds) {
        if (route.waypoints().isEmpty()) {
            return new Position(0, 0, 0);
        }
        
        // Simple linear interpolation between waypoints
        int totalSeconds = 0;
        for (int i = 0; i < route.waypoints().size() - 1; i++) {
            Waypoint current = route.waypoints().get(i);
            Waypoint next = route.waypoints().get(i + 1);
            int segmentDuration = next.durationSeconds() - current.durationSeconds();
            
            if (elapsedSeconds <= totalSeconds + segmentDuration) {
                double progress = (elapsedSeconds - totalSeconds) / (double) segmentDuration;
                double lat = current.latitude() + progress * (next.latitude() - current.latitude());
                double lon = current.longitude() + progress * (next.longitude() - current.longitude());
                double heading = calculateHeading(current.latitude(), current.longitude(),
                        next.latitude(), next.longitude());
                return new Position(lat, lon, heading);
            }
            totalSeconds += segmentDuration;
        }
        
        // Return last position
        Waypoint last = route.waypoints().get(route.waypoints().size() - 1);
        return new Position(last.latitude(), last.longitude(), 0);
    }

    private double calculateHeading(double lat1, double lon1, double lat2, double lon2) {
        double dLon = Math.toRadians(lon2 - lon1);
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        
        double x = Math.sin(dLon) * Math.cos(lat2Rad);
        double y = Math.cos(lat1Rad) * Math.sin(lat2Rad) - 
                   Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(dLon);
        
        double heading = Math.toDegrees(Math.atan2(x, y));
        return (heading + 360) % 360;
    }
}
