package com.utam.tracking.service;

import com.utam.tracking.domain.*;
import com.utam.tracking.repository.*;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for movement trail ingestion with zone detection and violation
 * tracking.
 * Core logic for Phase 4 implementation.
 * 
 * Feature: 005-asset-tracking-security
 * Tasks: T041-T047
 */
@Service
public class MovementTrailIngestionService {

    private static final Logger logger = LoggerFactory.getLogger(MovementTrailIngestionService.class);
    private static final double ZONE_BUFFER_METERS = 50.0; // 50m buffer for zone containment
    private static final double SPEED_THRESHOLD_MOVING = 0.5; // km/h - below this is considered stationary

    private final AssetMovementTrailRepository movementTrailRepository;
    private final RestrictedZoneRepository restrictedZoneRepository;
    private final ZoneViolationRepository zoneViolationRepository;
    private final MovementDiscrepancyRepository movementDiscrepancyRepository;
    private final AssetLocationRegisterRepository locationRegisterRepository;
    private final GeometryFactory geometryFactory;

    public MovementTrailIngestionService(
            AssetMovementTrailRepository movementTrailRepository,
            RestrictedZoneRepository restrictedZoneRepository,
            ZoneViolationRepository zoneViolationRepository,
            MovementDiscrepancyRepository movementDiscrepancyRepository,
            AssetLocationRegisterRepository locationRegisterRepository) {
        this.movementTrailRepository = movementTrailRepository;
        this.restrictedZoneRepository = restrictedZoneRepository;
        this.zoneViolationRepository = zoneViolationRepository;
        this.movementDiscrepancyRepository = movementDiscrepancyRepository;
        this.locationRegisterRepository = locationRegisterRepository;
        this.geometryFactory = new GeometryFactory();
    }

    /**
     * Process asset position update.
     * Main entry point for position ingestion.
     */
    @Transactional
    public void processPositionUpdate(
            UUID assetId,
            String assetIdentifier,
            String assetName,
            String assetCategory,
            String vehicleId,
            Double latitude,
            Double longitude,
            Double speed,
            Double heading,
            String status,
            ZonedDateTime timestamp,
            String tenantCode) {

        Point location = createPoint(longitude, latitude);

        // Step 1: Detect zone containment
        ZoneDetectionResult zoneResult = detectZones(location, tenantCode);

        // Step 2: Write movement trail
        writeMovementTrail(
                assetId, assetIdentifier, location, latitude, longitude,
                speed, heading, status, timestamp, tenantCode,
                zoneResult);

        // Step 3: Update location register
        updateLocationRegister(
                assetId, assetIdentifier, location, latitude, longitude,
                speed, heading,
                status, timestamp, tenantCode, zoneResult);

        // Step 4: Check for zone violations
        if (zoneResult.isInRestrictedZone() && !zoneResult.isAuthorized()) {
            createZoneViolation(
                    assetId, assetIdentifier, assetName, assetCategory,
                    zoneResult, location, timestamp, tenantCode);
        }

        // Step 5: Check for movement discrepancies
        checkMovementDiscrepancies(
                assetId, assetIdentifier, assetName, assetCategory,
                location, speed, status, timestamp, tenantCode);

        logger.debug("Processed position for asset {}: zone={}, authorized={}",
                assetIdentifier, zoneResult.getZoneName(), zoneResult.isAuthorized());
    }

    /**
     * Detect if asset is in any restricted zones using PostGIS ST_Contains or
     * ST_DWithin
     */
    public ZoneDetectionResult detectZones(Point location, String tenantCode) {
        // First check exact containment
        List<RestrictedZone> containingZones = restrictedZoneRepository
                .findZonesContainingPoint(location, tenantCode);

        if (!containingZones.isEmpty()) {
            RestrictedZone zone = containingZones.get(0);
            boolean authorized = isAssetAuthorizedForZone(zone, null); // TODO: get asset category
            return new ZoneDetectionResult(true, zone.getZoneId(), zone.getZoneName(),
                    zone.getZoneType(), zone.getId(), authorized);
        }

        // Check zones within buffer distance
        List<RestrictedZone> nearbyZones = restrictedZoneRepository
                .findZonesWithinDistance(location, ZONE_BUFFER_METERS, tenantCode);

        if (!nearbyZones.isEmpty()) {
            RestrictedZone zone = nearbyZones.get(0);
            boolean authorized = isAssetAuthorizedForZone(zone, null);
            return new ZoneDetectionResult(true, zone.getZoneId(), zone.getZoneName(),
                    zone.getZoneType(), zone.getId(), authorized);
        }

        return new ZoneDetectionResult(false, null, null, null, null, true);
    }

    /**
     * Check if asset category is authorized for the zone
     */
    private boolean isAssetAuthorizedForZone(RestrictedZone zone, String assetCategory) {
        if (zone.getAuthorizedAssetCategories() == null ||
                zone.getAuthorizedAssetCategories().length == 0) {
            return true; // No restrictions
        }

        if (assetCategory == null) {
            return false; // Unknown category, assume unauthorized
        }

        for (String authorizedCategory : zone.getAuthorizedAssetCategories()) {
            if (authorizedCategory.equalsIgnoreCase(assetCategory)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Write movement trail record
     */
    private AssetMovementTrail writeMovementTrail(
            UUID assetId, String assetIdentifier,
            Point location, Double latitude, Double longitude,
            Double speed, Double heading, String status,
            ZonedDateTime timestamp, String tenantCode,
            ZoneDetectionResult zoneResult) {

        AssetMovementTrail trail = AssetMovementTrail.builder()
                .assetId(assetId)
                .assetIdentifier(assetIdentifier)
                .latitude(latitude)
                .longitude(longitude)
                .location(location)
                .speed(speed)
                .heading(heading)
                .status(status)
                .timestamp(timestamp)
                .tenantCode(tenantCode)
                .zone(zoneResult.getZoneName())
                .restrictedZoneId(zoneResult.getZoneUuid())
                .build();

        return movementTrailRepository.save(trail);
    }

    /**
     * Update asset location register (current state snapshot)
     */
    private void updateLocationRegister(
            UUID assetId, String assetIdentifier, Point location,
            Double latitude, Double longitude,
            Double speed, Double heading, String status, ZonedDateTime timestamp,
            String tenantCode, ZoneDetectionResult zoneResult) {

        Optional<AssetLocationRegister> existing = locationRegisterRepository.findByAssetId(assetId);

        AssetLocationRegister register = existing.orElse(new AssetLocationRegister());
        register.setAssetId(assetId);
        register.setAssetIdentifier(assetIdentifier);
        register.setCurrentLocation(location);
        register.setCurrentLatitude(latitude);
        register.setCurrentLongitude(longitude);
        register.setSpeed(speed);
        register.setHeading(heading);
        register.setStatus(status);
        register.setLastUpdated(timestamp);
        register.setTenantCode(tenantCode);
        register.setCurrentZoneName(zoneResult.getZoneName());
        register.setRestrictedZoneId(zoneResult.getZoneUuid());
        register.setIsInRestrictedZone(zoneResult.isInRestrictedZone());
        register.setIsAuthorized(zoneResult.isAuthorized());

        locationRegisterRepository.save(register);
    }

    /**
     * Create zone violation record
     */
    private void createZoneViolation(
            UUID assetId, String assetIdentifier, String assetName, String assetCategory,
            ZoneDetectionResult zoneResult, Point location, ZonedDateTime timestamp,
            String tenantCode) {

        String violationId = generateViolationId(assetIdentifier, timestamp);

        ZoneViolation violation = ZoneViolation.builder()
                .violationId(violationId)
                .assetId(assetId)
                .assetIdentifier(assetIdentifier)
                .assetName(assetName)
                .assetCategory(assetCategory)
                .restrictedZoneId(zoneResult.getZoneUuid())
                .zoneName(zoneResult.getZoneName())
                .zoneType(zoneResult.getZoneType())
                .violationType("UNAUTHORIZED_ENTRY")
                .entryLocation(location)
                .severity(determineSeverity(zoneResult.getZoneType()))
                .timestamp(timestamp)
                .tenantCode(tenantCode)
                .build();

        zoneViolationRepository.save(violation);

        logger.warn("Zone violation detected: asset={}, zone={}, type={}",
                assetIdentifier, zoneResult.getZoneName(), zoneResult.getZoneType());
    }

    /**
     * Check for movement discrepancies
     */
    private void checkMovementDiscrepancies(
            UUID assetId, String assetIdentifier, String assetName, String assetCategory,
            Point location, Double speed, String status, ZonedDateTime timestamp,
            String tenantCode) {

        // Check for unexpected movement (status=Available but moving)
        if ("Available".equalsIgnoreCase(status) && speed != null && speed > SPEED_THRESHOLD_MOVING) {
            createMovementDiscrepancy(
                    assetId, assetIdentifier, assetName, assetCategory,
                    MovementDiscrepancy.DiscrepancyType.UNEXPECTED_MOVEMENT,
                    location, speed, timestamp, tenantCode,
                    "Asset marked as Available but moving at " + speed + " km/h");
        }

        // Check for status mismatch (status=Maintenance but moving)
        if ("Maintenance".equalsIgnoreCase(status) && speed != null && speed > SPEED_THRESHOLD_MOVING) {
            createMovementDiscrepancy(
                    assetId, assetIdentifier, assetName, assetCategory,
                    MovementDiscrepancy.DiscrepancyType.UNEXPECTED_MOVEMENT,
                    location, speed, timestamp, tenantCode,
                    "Asset marked as Maintenance but moving at " + speed + " km/h");
        }
    }

    /**
     * Create movement discrepancy record
     */
    private void createMovementDiscrepancy(
            UUID assetId, String assetIdentifier, String assetName, String assetCategory,
            MovementDiscrepancy.DiscrepancyType type, Point location, Double speed,
            ZonedDateTime timestamp, String tenantCode, String description) {

        String discrepancyId = generateDiscrepancyId(assetIdentifier, timestamp);

        MovementDiscrepancy discrepancy = MovementDiscrepancy.builder()
                .discrepancyId(discrepancyId)
                .assetId(assetId)
                .assetIdentifier(assetIdentifier)
                .assetName(assetName)
                .assetCategory(assetCategory)
                .discrepancyType(type)
                .actualLocation(location != null ? String.format("%.6f, %.6f", location.getY(), location.getX()) : null)
                .actualLatitude(location != null ? location.getY() : null)
                .actualLongitude(location != null ? location.getX() : null)
                .description(description)
                .severity(MovementDiscrepancy.DiscrepancySeverity.MEDIUM)
                .timestamp(timestamp)
                .tenantCode(tenantCode)
                .build();

        movementDiscrepancyRepository.save(discrepancy);

        logger.warn("Movement discrepancy detected: asset={}, type={}, description={}",
                assetIdentifier, type, description);
    }

    /**
     * Helper method to create PostGIS Point
     */
    private Point createPoint(double longitude, double latitude) {
        Coordinate coordinate = new Coordinate(longitude, latitude);
        Point point = geometryFactory.createPoint(coordinate);
        point.setSRID(4326); // WGS84
        return point;
    }

    /**
     * Determine violation severity based on zone type
     */
    private ZoneViolation.ViolationSeverity determineSeverity(String zoneType) {
        return switch (zoneType.toUpperCase()) {
            case "PROHIBITED" -> ZoneViolation.ViolationSeverity.CRITICAL;
            case "RESTRICTED" -> ZoneViolation.ViolationSeverity.HIGH;
            case "CONTROLLED" -> ZoneViolation.ViolationSeverity.MEDIUM;
            default -> ZoneViolation.ViolationSeverity.LOW;
        };
    }

    /**
     * Generate unique violation ID
     */
    private String generateViolationId(String assetIdentifier, ZonedDateTime timestamp) {
        return String.format("VIO-%s-%d", assetIdentifier, timestamp.toEpochSecond());
    }

    /**
     * Generate unique discrepancy ID
     */
    private String generateDiscrepancyId(String assetIdentifier, ZonedDateTime timestamp) {
        return String.format("DIS-%s-%d", assetIdentifier, timestamp.toEpochSecond());
    }

    /**
     * Inner class for zone detection results
     */
    public static class ZoneDetectionResult {
        private final boolean inRestrictedZone;
        private final String zoneId;
        private final String zoneName;
        private final String zoneType;
        private final UUID zoneUuid;
        private final boolean authorized;

        public ZoneDetectionResult(boolean inRestrictedZone, String zoneId, String zoneName,
                String zoneType, UUID zoneUuid, boolean authorized) {
            this.inRestrictedZone = inRestrictedZone;
            this.zoneId = zoneId;
            this.zoneName = zoneName;
            this.zoneType = zoneType;
            this.zoneUuid = zoneUuid;
            this.authorized = authorized;
        }

        public boolean isInRestrictedZone() {
            return inRestrictedZone;
        }

        public String getZoneId() {
            return zoneId;
        }

        public String getZoneName() {
            return zoneName;
        }

        public String getZoneType() {
            return zoneType;
        }

        public UUID getZoneUuid() {
            return zoneUuid;
        }

        public boolean isAuthorized() {
            return authorized;
        }
    }
}
