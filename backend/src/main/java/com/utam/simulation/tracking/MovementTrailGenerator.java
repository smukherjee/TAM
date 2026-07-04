package com.utam.simulation.tracking;

import com.utam.asset.service.VehicleAssetMapService;
import com.utam.simulation.config.SimulationConfig;
import com.utam.simulation.flight.FlightDataGenerator;
import com.utam.simulation.flight.FlightPositionDTO;
import com.utam.simulation.vehicle.SimVehicleRepository;
import com.utam.simulation.vehicle.Vehicle;
import com.utam.simulation.vehicle.VehicleDataGenerator;
import com.utam.simulation.vehicle.VehiclePositionDTO;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * T072: Movement Trail Generator for position history visualization.
 * Maintains rolling window of position history for vehicles and flights.
 * Used for trail visualization on the real-time map.
 */
@Slf4j
@Component
public class MovementTrailGenerator {

    private final SimulationConfig config;
    private final VehicleDataGenerator vehicleDataGenerator;
    private final FlightDataGenerator flightDataGenerator;
    private final MeterRegistry meterRegistry;
    private final com.utam.tracking.service.MovementTrailIngestionService ingestionService;
    private final SimVehicleRepository simVehicleRepository;
    private final VehicleAssetMapService vehicleAssetMapService;

    // Position history with rolling window (default 5 minutes)
    private final Map<String, Deque<TrailPoint>> vehicleTrails = new ConcurrentHashMap<>();
    private final Map<String, Deque<TrailPoint>> flightTrails = new ConcurrentHashMap<>();

    // Configuration
    private static final int DEFAULT_MAX_TRAIL_POINTS = 600;  // 5 min at 500ms intervals
    private static final int TRAIL_RETENTION_MINUTES = 5;

    private Counter trailPointsCreated;

    public MovementTrailGenerator(SimulationConfig config,
                                  VehicleDataGenerator vehicleDataGenerator,
                                  FlightDataGenerator flightDataGenerator,
                                  MeterRegistry meterRegistry,
                                  com.utam.tracking.service.MovementTrailIngestionService ingestionService,
                                  SimVehicleRepository simVehicleRepository,
                                  VehicleAssetMapService vehicleAssetMapService) {
        this.config = config;
        this.vehicleDataGenerator = vehicleDataGenerator;
        this.flightDataGenerator = flightDataGenerator;
        this.meterRegistry = meterRegistry;
        this.ingestionService = ingestionService;
        this.simVehicleRepository = simVehicleRepository;
        this.vehicleAssetMapService = vehicleAssetMapService;
    }

    @PostConstruct
    public void init() {
        trailPointsCreated = Counter.builder("simulation.trails.points.created")
            .description("Number of trail points created")
            .register(meterRegistry);
        log.info("MovementTrailGenerator initialized with {} minute retention", TRAIL_RETENTION_MINUTES);
    }

    /**
     * Capture current positions and add to trails.
     * Called by GeneratorOrchestrator at regular intervals.
     */
    public void captureTrails(String tenantCode) {
        Instant now = Instant.now();

        // Capture vehicle positions
        captureVehicleTrails(tenantCode, now);

        // Capture flight positions
        captureFlightTrails(tenantCode, now);

        // Prune old trail points
        pruneOldTrailPoints(now);
    }

    private void captureVehicleTrails(String tenantCode, Instant timestamp) {
        List<VehiclePositionDTO> positions = vehicleDataGenerator.getActiveVehiclePositions(tenantCode);
        
        for (VehiclePositionDTO pos : positions) {
            String key = "vehicle:" + pos.getVehicleId();
            
            TrailPoint point = TrailPoint.builder()
                .timestamp(timestamp)
                .latitude(pos.getLatitude())
                .longitude(pos.getLongitude())
                .speed(pos.getSpeed())
                .heading(pos.getHeading())
                .status(pos.getStatus())
                .build();

            vehicleTrails.computeIfAbsent(key, k -> new LinkedList<>()).addLast(point);
            trailPointsCreated.increment();

            // Trim if exceeds max
            Deque<TrailPoint> trail = vehicleTrails.get(key);
            while (trail.size() > DEFAULT_MAX_TRAIL_POINTS) {
                trail.removeFirst();
            }
        }
    }

    private void captureFlightTrails(String tenantCode, Instant timestamp) {
        List<FlightPositionDTO> positions = flightDataGenerator.getActiveFlightPositions(tenantCode);
        
        for (FlightPositionDTO pos : positions) {
            String key = "flight:" + pos.getFlightNumber();
            
            TrailPoint point = TrailPoint.builder()
                .timestamp(timestamp)
                .latitude(pos.getLatitude())
                .longitude(pos.getLongitude())
                .altitude(pos.getAltitude())
                .speed(pos.getSpeed())
                .heading(pos.getHeading())
                .status(pos.getStatus())
                .build();

            flightTrails.computeIfAbsent(key, k -> new LinkedList<>()).addLast(point);
            trailPointsCreated.increment();

            // Trim if exceeds max
            Deque<TrailPoint> trail = flightTrails.get(key);
            while (trail.size() > DEFAULT_MAX_TRAIL_POINTS) {
                trail.removeFirst();
            }
        }
    }

    private void pruneOldTrailPoints(Instant now) {
        Instant cutoff = now.minus(TRAIL_RETENTION_MINUTES, ChronoUnit.MINUTES);

        // Prune vehicle trails
        for (Deque<TrailPoint> trail : vehicleTrails.values()) {
            while (!trail.isEmpty() && trail.peekFirst().getTimestamp().isBefore(cutoff)) {
                trail.removeFirst();
            }
        }

        // Prune flight trails
        for (Deque<TrailPoint> trail : flightTrails.values()) {
            while (!trail.isEmpty() && trail.peekFirst().getTimestamp().isBefore(cutoff)) {
                trail.removeFirst();
            }
        }

        // Remove empty trail entries
        vehicleTrails.entrySet().removeIf(e -> e.getValue().isEmpty());
        flightTrails.entrySet().removeIf(e -> e.getValue().isEmpty());
    }

    /**
     * Get trail for a specific vehicle.
     */
    public List<TrailPoint> getVehicleTrail(String vehicleId) {
        Deque<TrailPoint> trail = vehicleTrails.get("vehicle:" + vehicleId);
        return trail != null ? new ArrayList<>(trail) : Collections.emptyList();
    }

    /**
     * Get trail for a specific flight.
     */
    public List<TrailPoint> getFlightTrail(String flightNumber) {
        Deque<TrailPoint> trail = flightTrails.get("flight:" + flightNumber);
        return trail != null ? new ArrayList<>(trail) : Collections.emptyList();
    }

    /**
     * Get all vehicle trails for a tenant.
     */
    public Map<String, List<TrailPoint>> getVehicleTrailsForTenant(String tenantCode) {
        Map<String, List<TrailPoint>> result = new HashMap<>();
        vehicleTrails.forEach((key, trail) -> {
            if (!trail.isEmpty()) {
                result.put(key.replace("vehicle:", ""), new ArrayList<>(trail));
            }
        });
        return result;
    }

    /**
     * Get all flight trails for a tenant.
     */
    public Map<String, List<TrailPoint>> getFlightTrailsForTenant(String tenantCode) {
        Map<String, List<TrailPoint>> result = new HashMap<>();
        flightTrails.forEach((key, trail) -> {
            if (!trail.isEmpty()) {
                result.put(key.replace("flight:", ""), new ArrayList<>(trail));
            }
        });
        return result;
    }

    /**
     * Get trail statistics for monitoring.
     */
    public TrailStatistics getStatistics() {
        int vehicleTrailCount = vehicleTrails.size();
        int flightTrailCount = flightTrails.size();
        int totalPoints = vehicleTrails.values().stream().mapToInt(Deque::size).sum()
                        + flightTrails.values().stream().mapToInt(Deque::size).sum();

        return TrailStatistics.builder()
            .vehicleTrailCount(vehicleTrailCount)
            .flightTrailCount(flightTrailCount)
            .totalTrailPoints(totalPoints)
            .maxTrailPointsPerEntity(DEFAULT_MAX_TRAIL_POINTS)
            .retentionMinutes(TRAIL_RETENTION_MINUTES)
            .build();
    }

    /**
     * Clear all trails (for testing or reset).
     */
    public void clearAllTrails() {
        vehicleTrails.clear();
        flightTrails.clear();
        log.info("All movement trails cleared");
    }

    /**
     * T081: Generate multi-day historical movement trail data.
     * Creates realistic historical trails for analytics and hotspot analysis.
     *
     * @param tenantCode The tenant code
     * @param daysBack Number of days to backfill
     * @param samplesPerDay Number of position samples per entity per day
     * @return Number of historical trail points generated
     */
    @SuppressWarnings("unused") // Local variables used for simulation state tracking, not output
    public int generateHistoricalTrails(String tenantCode, int daysBack, int samplesPerDay) {
        log.info("Generating {} days of historical trail data for tenant {} ({} samples/day)",
                 daysBack, tenantCode, samplesPerDay);

        SimulationConfig.TenantConfig tenantConfig = config.getTenant(tenantCode);
        if (tenantConfig == null) {
            log.warn("No tenant config for: {}", tenantCode);
            return 0;
        }

        double centerLat = tenantConfig.getCenterLatitude();
        double centerLon = tenantConfig.getCenterLongitude();
        Instant now = Instant.now();
        Random random = new Random();
        int totalGenerated = 0;

        // Backfill trails for the real vehicles registered for this tenant, so the
        // history actually shows up when looking up an asset's Movement Trail (e.g. VIDP-WT-004).
        List<Vehicle> tenantVehicles = simVehicleRepository.findByTenantCode(tenantCode);
        if (tenantVehicles.isEmpty()) {
            log.warn("No vehicles found for tenant {}, skipping historical vehicle trail generation", tenantCode);
        }
        int numFlights = 20;   // Simulate 20 flights per day

        for (int day = daysBack; day >= 1; day--) {
            Instant dayStart = now.minus(java.time.Duration.ofDays(day));
            long intervalSeconds = 86400 / samplesPerDay;

            // Generate vehicle trails for this day
            for (Vehicle vehicle : tenantVehicles) {
                String vehicleId = vehicle.getVehicleId();
                Optional<com.utam.asset.service.VehicleAssetMapService.AssetInfo> assetInfo =
                        vehicleAssetMapService.findByVehicle(vehicleId, tenantCode);
                if (assetInfo.isEmpty()) {
                    log.warn("No asset mapping for vehicle {} (tenant {}), skipping historical trail", vehicleId, tenantCode);
                    continue;
                }
                UUID assetId = assetInfo.get().id();
                double vLat = (vehicle.getLatitude() != null ? vehicle.getLatitude() : centerLat)
                        + (random.nextDouble() - 0.5) * 0.01;
                double vLon = (vehicle.getLongitude() != null ? vehicle.getLongitude() : centerLon)
                        + (random.nextDouble() - 0.5) * 0.01;
                // Bias speeds toward ~30 km/h with small variance
                double speed = 30.0 + (random.nextDouble() - 0.5) * 6.0; // ~27-33 km/h
                double heading = random.nextDouble() * 360;

                for (int s = 0; s < samplesPerDay; s++) {
                    Instant timestamp = dayStart.plusSeconds(s * intervalSeconds);

                    // Simulate movement pattern (biased walk with realistic unit conversion)
                    // speed (km/h) -> meters/sec = speed/3.6
                    double metersPerSecond = speed / 3.6;
                    double metersMoved = metersPerSecond * intervalSeconds;
                    double moveDegrees = metersMoved / 111000.0; // approx degrees latitude

                    heading += (random.nextDouble() - 0.5) * 10; // Gradual heading change
                    vLat += Math.cos(Math.toRadians(heading)) * moveDegrees;
                    vLon += Math.sin(Math.toRadians(heading)) * moveDegrees;

                    // Small speed variation
                    speed = Math.max(5.0, Math.min(60.0, speed + (random.nextDouble() - 0.5) * 2.0));

                    // Persist via ingestion service so normal processing (zone detection, register update) runs
                    try {
                        ZonedDateTime zts = ZonedDateTime.ofInstant(timestamp, ZoneOffset.UTC);
                        ingestionService.processPositionUpdate(
                                assetId,
                                vehicleId,
                                assetInfo.get().name(),
                                "Vehicle",
                                vehicleId,
                                vLat,
                                vLon,
                                speed,
                                heading,
                                "ACTIVE",
                                zts,
                                tenantCode
                        );
                        totalGenerated++;
                    } catch (Exception e) {
                        log.warn("Failed to persist historical trail point for {}: {}", vehicleId, e.getMessage());
                    }
                }
            }

            // Generate flight trails for this day
            for (int f = 0; f < numFlights; f++) {
                String flightId = String.format("HIST-F%03d-D%d", f, day);
                
                // Simulate approach or departure
                boolean arriving = random.nextBoolean();
                double fLat = centerLat + (arriving ? 0.2 : 0) * (random.nextDouble() - 0.5);
                double fLon = centerLon + (arriving ? 0.2 : 0) * (random.nextDouble() - 0.5);
                double altitude = arriving ? 35000 : 0;
                double fSpeed = arriving ? 450 : 0;
                double fHeading = arriving ? random.nextDouble() * 360 : random.nextDouble() * 360;

                int flightSamples = Math.min(30, samplesPerDay / numFlights);
                for (int s = 0; s < flightSamples; s++) {
                    Instant timestamp = dayStart.plusSeconds(
                        (f * 86400 / numFlights) + (s * 60)); // Each flight ~30 min
                    
                    // Simulate flight path
                    if (arriving) {
                        altitude = Math.max(0, altitude - 1000);
                        fSpeed = Math.max(180, fSpeed - 10);
                        fLat += (centerLat - fLat) * 0.1;
                        fLon += (centerLon - fLon) * 0.1;
                    } else {
                        altitude = Math.min(35000, altitude + 1000);
                        fSpeed = Math.min(450, fSpeed + 10);
                        double moveAmt = fSpeed / 3600.0 / 111.0 * 60;
                        fLat += Math.cos(Math.toRadians(fHeading)) * moveAmt;
                        fLon += Math.sin(Math.toRadians(fHeading)) * moveAmt;
                    }

                    totalGenerated++;
                }
            }
        }

        log.info("Generated {} historical trail points for tenant {}", totalGenerated, tenantCode);
        return totalGenerated;
    }

    /**
     * Single trail point data.
     */
    @lombok.Data
    @lombok.Builder
    public static class TrailPoint {
        private Instant timestamp;
        private double latitude;
        private double longitude;
        private double altitude;  // For flights
        private double speed;
        private double heading;
        private String status;
    }

    /**
     * Trail statistics DTO.
     */
    @lombok.Data
    @lombok.Builder
    public static class TrailStatistics {
        private int vehicleTrailCount;
        private int flightTrailCount;
        private int totalTrailPoints;
        private int maxTrailPointsPerEntity;
        private int retentionMinutes;
    }
}
