package com.utam.simulation.vehicle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Plays back vehicle movements along defined paths.
 * Implements FR-032 to FR-039: Admin path drawing and vehicle following.
 */
@Service
public class VehiclePathPlayer {

    private static final Logger log = LoggerFactory.getLogger(VehiclePathPlayer.class);

    private final SimVehicleRepository vehicleRepository;
    private final VehicleRouteCalculator routeCalculator;

    // Active path players
    private final Map<UUID, PathPlayback> activePlaybacks = new ConcurrentHashMap<>();

    public VehiclePathPlayer(SimVehicleRepository vehicleRepository,
                              VehicleRouteCalculator routeCalculator) {
        this.vehicleRepository = vehicleRepository;
        this.routeCalculator = routeCalculator;
    }

    /**
     * Start a vehicle following a route to a destination.
     */
    public void startPlayback(Vehicle vehicle, VehicleRouteCalculator.Route route) {
        if (route.isEmpty()) {
            log.warn("Cannot start playback with empty route for vehicle {}", vehicle.getId());
            return;
        }

        PathPlayback playback = new PathPlayback(
                vehicle.getId(),
                route,
                Instant.now(),
                false
        );

        activePlaybacks.put(vehicle.getId(), playback);
        
        // Update vehicle status
        vehicle.setStatus("EN_ROUTE");
        vehicleRepository.save(vehicle);

        log.debug("Started path playback for vehicle {} to {}", 
                vehicle.getId(), route.destination());
    }

    /**
     * Start a looping patrol path for an idle vehicle.
     */
    public void startPatrol(Vehicle vehicle, String tenantCode) {
        VehicleRouteCalculator.Route route = routeCalculator.calculatePatrolRoute(
                vehicle.getId(), 
                vehicle.getLatitude(), 
                vehicle.getLongitude(), 
                0.5); // 500m patrol radius

        PathPlayback playback = new PathPlayback(
                vehicle.getId(),
                route,
                Instant.now(),
                true // Loop enabled
        );

        activePlaybacks.put(vehicle.getId(), playback);
        vehicle.setStatus("PATROLLING");
        vehicleRepository.save(vehicle);
    }

    /**
     * Update all active playbacks and return vehicles that have reached their destination.
     */
    public List<UUID> updatePlaybacks() {
        List<UUID> completedVehicles = new ArrayList<>();
        Instant now = Instant.now();

        for (Map.Entry<UUID, PathPlayback> entry : activePlaybacks.entrySet()) {
            UUID vehicleId = entry.getKey();
            PathPlayback playback = entry.getValue();

            int elapsedSeconds = (int) ChronoUnit.SECONDS.between(playback.startTime(), now);
            
            if (elapsedSeconds >= playback.route().estimatedSeconds()) {
                // Route complete
                if (playback.loop()) {
                    // Restart the playback
                    activePlaybacks.put(vehicleId, new PathPlayback(
                            vehicleId,
                            playback.route(),
                            now,
                            true
                    ));
                } else {
                    // Remove completed playback
                    activePlaybacks.remove(vehicleId);
                    completedVehicles.add(vehicleId);
                    
                    // Update vehicle position to final destination
                    updateVehicleToFinalPosition(vehicleId, playback.route());
                }
            } else {
                // Update vehicle position along route
                updateVehiclePosition(vehicleId, playback.route(), elapsedSeconds);
            }
        }

        return completedVehicles;
    }

    /**
     * Stop playback for a specific vehicle.
     */
    public void stopPlayback(UUID vehicleId) {
        PathPlayback removed = activePlaybacks.remove(vehicleId);
        if (removed != null) {
            vehicleRepository.findById(vehicleId).ifPresent(vehicle -> {
                vehicle.setStatus("IDLE");
                vehicleRepository.save(vehicle);
            });
            log.debug("Stopped playback for vehicle {}", vehicleId);
        }
    }

    /**
     * Stop all playbacks.
     */
    public void stopAllPlaybacks() {
        for (UUID vehicleId : new ArrayList<>(activePlaybacks.keySet())) {
            stopPlayback(vehicleId);
        }
    }

    /**
     * Get current position for a vehicle in playback.
     */
    public Optional<VehicleRouteCalculator.Position> getCurrentPosition(UUID vehicleId) {
        PathPlayback playback = activePlaybacks.get(vehicleId);
        if (playback == null) {
            return Optional.empty();
        }

        int elapsedSeconds = (int) ChronoUnit.SECONDS.between(playback.startTime(), Instant.now());
        VehicleRouteCalculator.Position position = routeCalculator.getPositionAtTime(
                playback.route(), elapsedSeconds);

        return Optional.ofNullable(position);
    }

    /**
     * Get progress percentage for a vehicle in playback.
     */
    public double getPlaybackProgress(UUID vehicleId) {
        PathPlayback playback = activePlaybacks.get(vehicleId);
        if (playback == null) {
            return 0.0;
        }

        int elapsedSeconds = (int) ChronoUnit.SECONDS.between(playback.startTime(), Instant.now());
        double progress = (double) elapsedSeconds / playback.route().estimatedSeconds();
        return Math.min(100.0, Math.max(0.0, progress * 100.0));
    }

    /**
     * Check if vehicle is currently in playback.
     */
    public boolean isInPlayback(UUID vehicleId) {
        return activePlaybacks.containsKey(vehicleId);
    }

    /**
     * Get count of active playbacks.
     */
    public int getActivePlaybackCount() {
        return activePlaybacks.size();
    }

    /**
     * Get estimated time remaining for a vehicle's playback.
     */
    public Optional<Integer> getTimeRemaining(UUID vehicleId) {
        PathPlayback playback = activePlaybacks.get(vehicleId);
        if (playback == null) {
            return Optional.empty();
        }

        int elapsedSeconds = (int) ChronoUnit.SECONDS.between(playback.startTime(), Instant.now());
        int remaining = playback.route().estimatedSeconds() - elapsedSeconds;
        return Optional.of(Math.max(0, remaining));
    }

    private void updateVehiclePosition(UUID vehicleId, VehicleRouteCalculator.Route route, 
                                        int elapsedSeconds) {
        VehicleRouteCalculator.Position position = routeCalculator.getPositionAtTime(
                route, elapsedSeconds);

        if (position != null) {
            vehicleRepository.findById(vehicleId).ifPresent(vehicle -> {
                vehicle.setLatitude(position.latitude());
                vehicle.setLongitude(position.longitude());
                vehicle.setLastUpdated(Instant.now());
                vehicleRepository.save(vehicle);
            });
        }
    }

    private void updateVehicleToFinalPosition(UUID vehicleId, VehicleRouteCalculator.Route route) {
        if (!route.waypoints().isEmpty()) {
            VehicleRouteCalculator.Waypoint lastWaypoint = route.waypoints()
                    .get(route.waypoints().size() - 1);

            vehicleRepository.findById(vehicleId).ifPresent(vehicle -> {
                vehicle.setLatitude(lastWaypoint.latitude());
                vehicle.setLongitude(lastWaypoint.longitude());
                vehicle.setStatus("ARRIVED");
                vehicle.setLastUpdated(Instant.now());
                vehicleRepository.save(vehicle);
                log.debug("Vehicle {} arrived at destination {}", vehicleId, route.destination());
            });
        }
    }

    // Record for tracking active playbacks
    private record PathPlayback(UUID vehicleId, VehicleRouteCalculator.Route route, 
                                 Instant startTime, boolean loop) {}
}
