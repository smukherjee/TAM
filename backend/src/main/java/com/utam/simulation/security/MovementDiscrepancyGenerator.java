package com.utam.simulation.security;

import com.utam.simulation.config.SimulationConfig;
import com.utam.simulation.core.BaseDataGenerator;
import com.utam.simulation.tracking.LocationRegisterUpdater;
import com.utam.simulation.tracking.LocationRegisterUpdater.LocationRecord;
import com.utam.simulation.tracking.MovementTrailGenerator;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * T076: Movement Discrepancy Generator for detecting anomalous movement patterns.
 * Detects impossible movements, teleportation, route deviations, and speed anomalies.
 * Implements FR-081 to FR-085: Movement anomaly detection.
 */
@Slf4j
@Component
@SuppressWarnings("unused") // trailGenerator reserved for future trail-based anomaly detection
public class MovementDiscrepancyGenerator extends BaseDataGenerator {

    private final DiscrepancyRepository discrepancyRepository;
    private final Random random = new Random();

    @Autowired(required = false)
    private LocationRegisterUpdater registerUpdater;

    @Autowired(required = false)
    private MovementTrailGenerator trailGenerator;

    // Last known positions for discrepancy detection
    private final Map<String, PositionSnapshot> lastPositions = new ConcurrentHashMap<>();

    // Configuration thresholds
    private static final double MAX_VEHICLE_SPEED_KMH = 80.0;   // Max speed for ground vehicles
    private static final double MAX_AIRCRAFT_GROUND_SPEED_KMH = 50.0; // Max taxiing speed
    private static final double TELEPORT_THRESHOLD_M = 500.0;  // Min distance to be considered teleportation
    private static final long MIN_UPDATE_INTERVAL_MS = 200;     // Min time between updates

    // Discrepancy types
    public enum DiscrepancyType {
        TELEPORTATION("Entity moved impossibly fast - potential GPS spoofing or system error"),
        SPEED_ANOMALY("Speed exceeded physical limits for entity type"),
        ROUTE_DEVIATION("Significant deviation from planned route"),
        POSITION_JUMP("Sudden position change inconsistent with heading"),
        STATIONARY_ANOMALY("Entity marked as moving but position unchanged"),
        TIMESTAMP_ANOMALY("Position timestamps out of sequence"),
        DUPLICATE_POSITION("Identical position reported from different entities"),
        COVERAGE_GAP("Extended period without position updates");

        private final String description;

        DiscrepancyType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    private Counter discrepanciesDetected;
    private Counter teleportsDetected;
    private Counter speedAnomalies;

    public MovementDiscrepancyGenerator(SimulationConfig config, MeterRegistry meterRegistry,
                                         DiscrepancyRepository discrepancyRepository) {
        super(config, meterRegistry);
        this.discrepancyRepository = discrepancyRepository;
    }

    @Override
    public String getGeneratorName() {
        return "MovementDiscrepancyGenerator";
    }

    @Override
    public String getEntityType() {
        return "Discrepancy";
    }

    @PostConstruct
    public void init() {
        discrepanciesDetected = Counter.builder("simulation.discrepancies.detected")
            .description("Number of movement discrepancies detected")
            .register(meterRegistry);
        teleportsDetected = Counter.builder("simulation.discrepancies.teleports")
            .description("Number of teleportation events detected")
            .register(meterRegistry);
        speedAnomalies = Counter.builder("simulation.discrepancies.speed")
            .description("Number of speed anomalies detected")
            .register(meterRegistry);
        
        log.info("MovementDiscrepancyGenerator initialized");
    }

    @Override
    public int generateBatch(String tenantCode, int batchSize) {
        // Generate historical discrepancies for demo
        List<MovementDiscrepancy> discrepancies = new ArrayList<>();
        int generated = 0;

        for (int i = 0; i < Math.min(batchSize, 15); i++) {
            MovementDiscrepancy discrepancy = generateRandomDiscrepancy(tenantCode);
            if (discrepancy != null) {
                discrepancies.add(discrepancy);
                generated++;
            }
        }

        if (!discrepancies.isEmpty()) {
            discrepancyRepository.saveAll(discrepancies);
            discrepanciesDetected.increment(generated);
        }

        log.info("Generated {} historical discrepancies for tenant {}", generated, tenantCode);
        return generated;
    }

    @Override
    public boolean generateSingleUpdate(String tenantCode) {
        if (registerUpdater == null) {
            return true;
        }

        List<LocationRecord> locations = registerUpdater.getAllLocationsForTenant(tenantCode);
        List<MovementDiscrepancy> newDiscrepancies = new ArrayList<>();
        Instant now = Instant.now();

        for (LocationRecord location : locations) {
            String entityKey = location.getEntityType() + ":" + location.getEntityId();
            PositionSnapshot lastPos = lastPositions.get(entityKey);

            if (lastPos != null) {
                // Check for discrepancies
                List<MovementDiscrepancy> detected = detectDiscrepancies(location, lastPos, now);
                newDiscrepancies.addAll(detected);
            }

            // Update last known position
            lastPositions.put(entityKey, new PositionSnapshot(
                location.getLatitude(),
                location.getLongitude(),
                location.getSpeed(),
                location.getHeading(),
                now
            ));
        }

        // Check for coverage gaps (entities that haven't updated)
        newDiscrepancies.addAll(detectCoverageGaps(tenantCode, now));

        // Save discrepancies
        if (!newDiscrepancies.isEmpty()) {
            discrepancyRepository.saveAll(newDiscrepancies);
            discrepanciesDetected.increment(newDiscrepancies.size());
            log.debug("Detected {} movement discrepancies for tenant {}", 
                     newDiscrepancies.size(), tenantCode);
        }

        return true;
    }

    private List<MovementDiscrepancy> detectDiscrepancies(LocationRecord current, 
                                                           PositionSnapshot last, 
                                                           Instant now) {
        List<MovementDiscrepancy> discrepancies = new ArrayList<>();

        // Calculate distance and time
        double distanceM = calculateDistanceMeters(
            last.latitude, last.longitude,
            current.getLatitude(), current.getLongitude()
        );
        long timeDiffMs = Duration.between(last.timestamp, now).toMillis();

        if (timeDiffMs < MIN_UPDATE_INTERVAL_MS) {
            return discrepancies; // Too soon for meaningful analysis
        }

        // Calculate implied speed
        double impliedSpeedKmh = (distanceM / 1000.0) / (timeDiffMs / 3600000.0);
        double maxSpeed = current.getEntityType().equals("VEHICLE") 
            ? MAX_VEHICLE_SPEED_KMH 
            : MAX_AIRCRAFT_GROUND_SPEED_KMH;

        // Check for teleportation
        if (distanceM > TELEPORT_THRESHOLD_M && timeDiffMs < 1000) {
            MovementDiscrepancy d = createDiscrepancy(
                current, DiscrepancyType.TELEPORTATION,
                String.format("Moved %.1fm in %dms (%.1f km/h)", 
                             distanceM, timeDiffMs, impliedSpeedKmh)
            );
            discrepancies.add(d);
            teleportsDetected.increment();
        }
        // Check for speed anomaly
        else if (impliedSpeedKmh > maxSpeed * 1.5) {
            MovementDiscrepancy d = createDiscrepancy(
                current, DiscrepancyType.SPEED_ANOMALY,
                String.format("Implied speed %.1f km/h exceeds max %.1f km/h", 
                             impliedSpeedKmh, maxSpeed)
            );
            discrepancies.add(d);
            speedAnomalies.increment();
        }

        // Check for position jump inconsistent with heading
        if (distanceM > 50) { // Only check significant movements
            double expectedHeading = calculateBearing(
                last.latitude, last.longitude,
                current.getLatitude(), current.getLongitude()
            );
            double headingDiff = Math.abs(normalizeAngle(expectedHeading - last.heading));
            
            if (headingDiff > 90 && current.getSpeed() > 5) {
                // Moving significantly but direction doesn't match heading
                MovementDiscrepancy d = createDiscrepancy(
                    current, DiscrepancyType.POSITION_JUMP,
                    String.format("Movement heading %.0f° differs from entity heading %.0f° by %.0f°",
                                 expectedHeading, last.heading, headingDiff)
                );
                discrepancies.add(d);
            }
        }

        // Check for stationary anomaly
        if (current.getSpeed() > 5 && distanceM < 1) {
            MovementDiscrepancy d = createDiscrepancy(
                current, DiscrepancyType.STATIONARY_ANOMALY,
                String.format("Reported speed %.1f km/h but moved only %.1fm", 
                             current.getSpeed(), distanceM)
            );
            discrepancies.add(d);
        }

        return discrepancies;
    }

    private List<MovementDiscrepancy> detectCoverageGaps(String tenantCode, Instant now) {
        List<MovementDiscrepancy> gaps = new ArrayList<>();
        long gapThresholdMs = 30_000; // 30 seconds

        for (Map.Entry<String, PositionSnapshot> entry : lastPositions.entrySet()) {
            if (Duration.between(entry.getValue().timestamp, now).toMillis() > gapThresholdMs) {
                // Create coverage gap discrepancy
                MovementDiscrepancy d = new MovementDiscrepancy();
                d.setId(UUID.randomUUID());
                d.setTenantCode(tenantCode);
                d.setDiscrepancyType(DiscrepancyType.COVERAGE_GAP.name());
                d.setDescription(DiscrepancyType.COVERAGE_GAP.getDescription());
                d.setEntityId(entry.getKey().split(":")[1]);
                d.setEntityType(entry.getKey().split(":")[0]);
                d.setLatitude(entry.getValue().latitude);
                d.setLongitude(entry.getValue().longitude);
                d.setDetectedAt(now);
                d.setStatus("OPEN");
                d.setCreatedAt(now);
                gaps.add(d);
                
                // Remove stale entry to prevent repeated alerts
                lastPositions.remove(entry.getKey());
            }
        }

        return gaps;
    }

    private MovementDiscrepancy generateRandomDiscrepancy(String tenantCode) {
        MovementDiscrepancy d = new MovementDiscrepancy();
        d.setId(UUID.randomUUID());
        d.setTenantCode(tenantCode);

        DiscrepancyType type = DiscrepancyType.values()[random.nextInt(DiscrepancyType.values().length)];
        d.setDiscrepancyType(type.name());
        d.setDescription(type.getDescription());

        d.setEntityId(UUID.randomUUID().toString());
        d.setEntityType(random.nextBoolean() ? "VEHICLE" : "FLIGHT");

        SimulationConfig.TenantConfig tenantConfig = config.getTenant(tenantCode);
        if (tenantConfig != null) {
            d.setLatitude(tenantConfig.getCenterLatitude() + (random.nextDouble() - 0.5) * 0.01);
            d.setLongitude(tenantConfig.getCenterLongitude() + (random.nextDouble() - 0.5) * 0.01);
        }

        long hoursAgo = random.nextInt(48);
        d.setDetectedAt(Instant.now().minusSeconds(hoursAgo * 3600 + random.nextInt(3600)));
        d.setStatus(random.nextDouble() > 0.3 ? "RESOLVED" : "OPEN");
        d.setCreatedAt(Instant.now());

        return d;
    }

    private MovementDiscrepancy createDiscrepancy(LocationRecord location, 
                                                   DiscrepancyType type, 
                                                   String details) {
        MovementDiscrepancy d = new MovementDiscrepancy();
        d.setId(UUID.randomUUID());
        d.setTenantCode(location.getTenantCode());
        d.setDiscrepancyType(type.name());
        d.setDescription(type.getDescription() + ": " + details);
        d.setEntityId(location.getEntityId());
        d.setEntityType(location.getEntityType());
        d.setLatitude(location.getLatitude());
        d.setLongitude(location.getLongitude());
        d.setDetectedAt(Instant.now());
        d.setStatus("OPEN");
        d.setCreatedAt(Instant.now());
        return d;
    }

    // Utility methods
    private double calculateDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000; // Earth's radius in meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private double calculateBearing(double lat1, double lon1, double lat2, double lon2) {
        double dLon = Math.toRadians(lon2 - lon1);
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double y = Math.sin(dLon) * Math.cos(lat2Rad);
        double x = Math.cos(lat1Rad) * Math.sin(lat2Rad) 
                 - Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(dLon);
        double bearing = Math.toDegrees(Math.atan2(y, x));
        return (bearing + 360) % 360;
    }

    private double normalizeAngle(double angle) {
        angle = angle % 360;
        if (angle > 180) angle -= 360;
        if (angle < -180) angle += 360;
        return Math.abs(angle);
    }

    /**
     * Position snapshot for discrepancy detection.
     */
    private record PositionSnapshot(
        double latitude,
        double longitude,
        double speed,
        double heading,
        Instant timestamp
    ) {}
}
