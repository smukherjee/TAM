package com.utam.simulation.tracking;

import com.utam.simulation.config.SimulationConfig;
import com.utam.simulation.flight.FlightDataGenerator;
import com.utam.simulation.flight.FlightPositionDTO;
import com.utam.simulation.vehicle.VehicleDataGenerator;
import com.utam.simulation.vehicle.VehiclePositionDTO;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * T073: Location Register Updater for maintaining current position state.
 * Maintains an in-memory register of all entity positions for fast lookup.
 * Supports real-time queries without hitting the database.
 */
@Slf4j
@Component
@SuppressWarnings("unused") // config field reserved for future configuration-based tuning
public class LocationRegisterUpdater {

    private final SimulationConfig config;
    private final VehicleDataGenerator vehicleDataGenerator;
    private final FlightDataGenerator flightDataGenerator;
    private final MeterRegistry meterRegistry;

    // In-memory location registers (keyed by entityId)
    private final Map<String, LocationRecord> vehicleRegister = new ConcurrentHashMap<>();
    private final Map<String, LocationRecord> flightRegister = new ConcurrentHashMap<>();

    // Zone membership tracking
    private final Map<String, Set<String>> entityZones = new ConcurrentHashMap<>();

    // Metrics
    private final AtomicInteger vehicleCount = new AtomicInteger(0);
    private final AtomicInteger flightCount = new AtomicInteger(0);

    // Stale threshold - entities not updated within this time are considered stale
    private static final long STALE_THRESHOLD_MS = 30_000; // 30 seconds

    public LocationRegisterUpdater(SimulationConfig config,
                                   VehicleDataGenerator vehicleDataGenerator,
                                   FlightDataGenerator flightDataGenerator,
                                   MeterRegistry meterRegistry) {
        this.config = config;
        this.vehicleDataGenerator = vehicleDataGenerator;
        this.flightDataGenerator = flightDataGenerator;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void init() {
        // Register gauge metrics
        Gauge.builder("simulation.register.vehicles", vehicleCount, AtomicInteger::get)
            .description("Number of vehicles in location register")
            .register(meterRegistry);

        Gauge.builder("simulation.register.flights", flightCount, AtomicInteger::get)
            .description("Number of flights in location register")
            .register(meterRegistry);

        log.info("LocationRegisterUpdater initialized");
    }

    /**
     * Update location register from current positions.
     * Called by GeneratorOrchestrator at regular intervals.
     */
    public void updateRegister(String tenantCode) {
        Instant now = Instant.now();

        // Update vehicle locations
        updateVehicleLocations(tenantCode, now);

        // Update flight locations
        updateFlightLocations(tenantCode, now);

        // Update metrics
        vehicleCount.set(vehicleRegister.size());
        flightCount.set(flightRegister.size());
    }

    private void updateVehicleLocations(String tenantCode, Instant timestamp) {
        List<VehiclePositionDTO> positions = vehicleDataGenerator.getActiveVehiclePositions(tenantCode);

        for (VehiclePositionDTO pos : positions) {
            LocationRecord record = LocationRecord.builder()
                .entityId(pos.getVehicleId())
                .entityType("VEHICLE")
                .tenantCode(tenantCode)
                .latitude(pos.getLatitude())
                .longitude(pos.getLongitude())
                .altitude(0.0) // Ground vehicles
                .speed(pos.getSpeed())
                .heading(pos.getHeading())
                .status(pos.getStatus())
                .lastUpdated(timestamp)
                .build();

            vehicleRegister.put(pos.getVehicleId(), record);
        }
    }

    private void updateFlightLocations(String tenantCode, Instant timestamp) {
        List<FlightPositionDTO> positions = flightDataGenerator.getActiveFlightPositions(tenantCode);

        for (FlightPositionDTO pos : positions) {
            LocationRecord record = LocationRecord.builder()
                .entityId(pos.getFlightNumber())
                .entityType("FLIGHT")
                .tenantCode(tenantCode)
                .latitude(pos.getLatitude())
                .longitude(pos.getLongitude())
                .altitude(pos.getAltitude())
                .speed(pos.getSpeed())
                .heading(pos.getHeading())
                .status(pos.getStatus())
                .lastUpdated(timestamp)
                .build();

            flightRegister.put(pos.getFlightNumber(), record);
        }
    }

    /**
     * Periodic cleanup of stale entries.
     */
    @Scheduled(fixedRate = 10_000) // Every 10 seconds
    public void cleanupStaleEntries() {
        Instant cutoff = Instant.now().minusMillis(STALE_THRESHOLD_MS);

        int removedVehicles = 0;
        int removedFlights = 0;

        Iterator<Map.Entry<String, LocationRecord>> vehicleIt = vehicleRegister.entrySet().iterator();
        while (vehicleIt.hasNext()) {
            if (vehicleIt.next().getValue().getLastUpdated().isBefore(cutoff)) {
                vehicleIt.remove();
                removedVehicles++;
            }
        }

        Iterator<Map.Entry<String, LocationRecord>> flightIt = flightRegister.entrySet().iterator();
        while (flightIt.hasNext()) {
            if (flightIt.next().getValue().getLastUpdated().isBefore(cutoff)) {
                flightIt.remove();
                removedFlights++;
            }
        }

        if (removedVehicles > 0 || removedFlights > 0) {
            log.debug("Cleaned up {} stale vehicles and {} stale flights from register",
                     removedVehicles, removedFlights);
        }

        vehicleCount.set(vehicleRegister.size());
        flightCount.set(flightRegister.size());
    }

    // ========== Query Methods ==========

    /**
     * Get location for a specific vehicle.
     */
    public Optional<LocationRecord> getVehicleLocation(String vehicleId) {
        return Optional.ofNullable(vehicleRegister.get(vehicleId));
    }

    /**
     * Get location for a specific flight.
     */
    public Optional<LocationRecord> getFlightLocation(String flightNumber) {
        return Optional.ofNullable(flightRegister.get(flightNumber));
    }

    /**
     * Get all vehicle locations for a tenant.
     */
    public List<LocationRecord> getVehicleLocationsForTenant(String tenantCode) {
        return vehicleRegister.values().stream()
            .filter(r -> r.getTenantCode().equals(tenantCode))
            .toList();
    }

    /**
     * Get all flight locations for a tenant.
     */
    public List<LocationRecord> getFlightLocationsForTenant(String tenantCode) {
        return flightRegister.values().stream()
            .filter(r -> r.getTenantCode().equals(tenantCode))
            .toList();
    }

    /**
     * Get all locations (vehicles + flights) for a tenant.
     */
    public List<LocationRecord> getAllLocationsForTenant(String tenantCode) {
        List<LocationRecord> all = new ArrayList<>();
        all.addAll(getVehicleLocationsForTenant(tenantCode));
        all.addAll(getFlightLocationsForTenant(tenantCode));
        return all;
    }

    /**
     * Find entities within a bounding box.
     */
    public List<LocationRecord> findEntitiesInBounds(String tenantCode,
                                                      double minLat, double maxLat,
                                                      double minLon, double maxLon) {
        return getAllLocationsForTenant(tenantCode).stream()
            .filter(r -> r.getLatitude() >= minLat && r.getLatitude() <= maxLat)
            .filter(r -> r.getLongitude() >= minLon && r.getLongitude() <= maxLon)
            .toList();
    }

    /**
     * Find entities within a radius of a point.
     */
    public List<LocationRecord> findEntitiesNearPoint(String tenantCode,
                                                       double lat, double lon,
                                                       double radiusKm) {
        return getAllLocationsForTenant(tenantCode).stream()
            .filter(r -> calculateDistanceKm(r.getLatitude(), r.getLongitude(), lat, lon) <= radiusKm)
            .toList();
    }

    private double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        // Haversine formula
        double R = 6371; // Earth's radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * Register an entity's zone membership.
     */
    public void registerZoneMembership(String entityId, String zoneId) {
        entityZones.computeIfAbsent(entityId, k -> ConcurrentHashMap.newKeySet()).add(zoneId);
    }

    /**
     * Remove an entity's zone membership.
     */
    public void removeZoneMembership(String entityId, String zoneId) {
        Set<String> zones = entityZones.get(entityId);
        if (zones != null) {
            zones.remove(zoneId);
        }
    }

    /**
     * Get zones an entity is currently in.
     */
    public Set<String> getEntityZones(String entityId) {
        return entityZones.getOrDefault(entityId, Collections.emptySet());
    }

    /**
     * Get register statistics.
     */
    public RegisterStatistics getStatistics() {
        return RegisterStatistics.builder()
            .vehicleCount(vehicleRegister.size())
            .flightCount(flightRegister.size())
            .staleThresholdMs(STALE_THRESHOLD_MS)
            .build();
    }

    /**
     * Location record for fast lookup.
     */
    @lombok.Data
    @lombok.Builder
    public static class LocationRecord {
        private String entityId;
        private String entityType;
        private String tenantCode;
        private double latitude;
        private double longitude;
        private double altitude;
        private double speed;
        private double heading;
        private String status;
        private Instant lastUpdated;
    }

    /**
     * Register statistics DTO.
     */
    @lombok.Data
    @lombok.Builder
    public static class RegisterStatistics {
        private int vehicleCount;
        private int flightCount;
        private long staleThresholdMs;
    }
}
