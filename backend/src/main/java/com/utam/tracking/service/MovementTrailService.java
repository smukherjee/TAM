package com.utam.tracking.service;

import com.utam.tracking.domain.AssetMovementTrail;
import com.utam.tracking.dto.*;
import com.utam.tracking.repository.AssetMovementTrailRepository;
import com.utam.tracking.repository.ZoneViolationRepository;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing movement trails.
 * Feature: 005-asset-tracking-security
 * Task: T053
 */
@Service
public class MovementTrailService {

    @SuppressWarnings("unused") // Reserved for enhanced logging
    private static final Logger logger = LoggerFactory.getLogger(MovementTrailService.class);
    private static final int MAX_TRAIL_DAYS = 30;

    private final AssetMovementTrailRepository trailRepository;

    @SuppressWarnings("unused") // Reserved for violation correlation
    private final ZoneViolationRepository violationRepository;

    public MovementTrailService(
            AssetMovementTrailRepository trailRepository,
            ZoneViolationRepository violationRepository) {
        this.trailRepository = trailRepository;
        this.violationRepository = violationRepository;
    }

    /**
     * Get movement trail for an asset within a date range.
     */
    @Transactional(readOnly = true)
    public MovementTrailDTO getTrail(UUID assetId, String tenantCode, ZonedDateTime startDate, ZonedDateTime endDate) {
        // Validate date range
        long days = ChronoUnit.DAYS.between(startDate, endDate);
        if (days > MAX_TRAIL_DAYS) {
            throw new IllegalArgumentException("Date range cannot exceed " + MAX_TRAIL_DAYS + " days");
        }

        // Validate tenant code - if null, we search without it
        List<AssetMovementTrail> trailPoints;
        if (tenantCode == null || tenantCode.isEmpty()) {
            trailPoints = trailRepository.findByAssetIdAndTimestampBetweenOrderByTimestampAsc(
                    assetId, startDate, endDate);
        } else {
            trailPoints = trailRepository.findByAssetIdAndTenantCodeAndTimestampBetweenOrderByTimestampAsc(
                    assetId, tenantCode, startDate, endDate);
        }

        if (trailPoints.isEmpty()) {
            return MovementTrailDTO.builder()
                    .assetId(assetId)
                    .startTime(startDate)
                    .endTime(endDate)
                    .totalPoints(0)
                    .points(Collections.emptyList())
                    .zoneEntries(Collections.emptyList())
                    .build();
        }

        AssetMovementTrail first = trailPoints.get(0);
        List<MovementTrailPointDTO> points = trailPoints.stream()
                .map(this::toPointDTO)
                .collect(Collectors.toList());

        List<ZoneEntryDTO> zoneEntries = calculateZoneEntries(trailPoints);
        TrailSummaryDTO summary = calculateTrailSummary(trailPoints, zoneEntries);

        return MovementTrailDTO.builder()
                .assetId(assetId)
                .assetIdentifier(first.getAssetIdentifier())
                .tenantCode(first.getTenantCode())
                .startTime(startDate)
                .endTime(endDate)
                .totalPoints(points.size())
                .points(points)
                .zoneEntries(zoneEntries)
                .summary(summary)
                .totalDistanceMeters(summary.getTotalDistanceMeters())
                .avgSpeed(summary.getAverageSpeedKmh())
                .maxSpeed(summary.getMaxSpeedKmh())
                .build();
    }

    /**
     * Calculate zone entries from trail points.
     */
    public List<ZoneEntryDTO> calculateZoneEntries(List<AssetMovementTrail> trailPoints) {
        List<ZoneEntryDTO> entries = new ArrayList<>();
        String currentZone = null;
        ZonedDateTime entryTime = null;
        Double entryLat = null, entryLng = null;

        for (AssetMovementTrail point : trailPoints) {
            String zoneName = point.getZone();
            Point location = point.getLocation();

            if (zoneName != null && !zoneName.equals(currentZone)) {
                // Exited previous zone
                if (currentZone != null && entryTime != null) {
                    long dwellSeconds = ChronoUnit.SECONDS.between(entryTime, point.getTimestamp());
                    entries.add(ZoneEntryDTO.builder()
                            .zoneName(currentZone)
                            .entryLatitude(entryLat)
                            .entryLongitude(entryLng)
                            .exitLatitude(location.getY())
                            .exitLongitude(location.getX())
                            .entryTime(entryTime)
                            .exitTime(point.getTimestamp())
                            .dwellTimeSeconds(dwellSeconds)
                            .build());
                }
                // Enter new zone
                currentZone = zoneName;
                entryTime = point.getTimestamp();
                entryLat = location.getY();
                entryLng = location.getX();
            } else if (zoneName == null && currentZone != null) {
                // Exited zone without entering another
                long dwellSeconds = ChronoUnit.SECONDS.between(entryTime, point.getTimestamp());
                entries.add(ZoneEntryDTO.builder()
                        .zoneName(currentZone)
                        .entryLatitude(entryLat)
                        .entryLongitude(entryLng)
                        .exitLatitude(location.getY())
                        .exitLongitude(location.getX())
                        .entryTime(entryTime)
                        .exitTime(point.getTimestamp())
                        .dwellTimeSeconds(dwellSeconds)
                        .build());
                currentZone = null;
                entryTime = null;
            }
        }

        // Handle still in zone at end
        if (currentZone != null && entryTime != null && !trailPoints.isEmpty()) {
            AssetMovementTrail last = trailPoints.get(trailPoints.size() - 1);
            long dwellSeconds = ChronoUnit.SECONDS.between(entryTime, last.getTimestamp());
            entries.add(ZoneEntryDTO.builder()
                    .zoneName(currentZone)
                    .entryLatitude(entryLat)
                    .entryLongitude(entryLng)
                    .entryTime(entryTime)
                    .dwellTimeSeconds(dwellSeconds)
                    .build());
        }

        return entries;
    }

    /**
     * Calculate trail summary statistics.
     */
    public TrailSummaryDTO calculateTrailSummary(List<AssetMovementTrail> trailPoints, List<ZoneEntryDTO> zoneEntries) {
        if (trailPoints.isEmpty()) {
            return TrailSummaryDTO.builder()
                    .totalPoints(0)
                    .totalDistanceMeters(0.0)
                    .build();
        }

        double totalDistance = 0.0;
        double maxSpeed = 0.0;
        double totalSpeed = 0.0;
        int speedCount = 0;
        Map<String, Integer> statusBreakdown = new HashMap<>();
        Set<String> restrictedZones = new HashSet<>();

        Point prevPoint = null;
        for (AssetMovementTrail trail : trailPoints) {
            Point current = trail.getLocation();

            // Calculate distance
            if (prevPoint != null) {
                totalDistance += calculateDistance(
                        prevPoint.getY(), prevPoint.getX(),
                        current.getY(), current.getX());
            }
            prevPoint = current;

            // Track speed
            if (trail.getSpeed() != null) {
                totalSpeed += trail.getSpeed();
                speedCount++;
                maxSpeed = Math.max(maxSpeed, trail.getSpeed());
            }

            // Track status
            String status = trail.getStatus() != null ? trail.getStatus() : "Unknown";
            statusBreakdown.merge(status, 1, (a, b) -> a + b);

            // Track restricted zones
            if (trail.getRestrictedZoneId() != null) {
                restrictedZones.add(trail.getZone());
            }
        }

        // Calculate dwell time by zone
        Map<String, Long> dwellTimeByZone = zoneEntries.stream()
                .filter(e -> e.getZoneName() != null)
                .collect(Collectors.groupingBy(
                        ZoneEntryDTO::getZoneName,
                        Collectors.summingLong(e -> e.getDwellTimeSeconds() != null ? e.getDwellTimeSeconds() : 0)));

        long totalDwellTime = dwellTimeByZone.values().stream().mapToLong(Long::longValue).sum();
        long totalDuration = 0;
        if (trailPoints.size() >= 2) {
            totalDuration = ChronoUnit.SECONDS.between(
                    trailPoints.get(0).getTimestamp(),
                    trailPoints.get(trailPoints.size() - 1).getTimestamp());
        }

        return TrailSummaryDTO.builder()
                .totalPoints(trailPoints.size())
                .totalDistanceMeters(totalDistance)
                .totalDurationSeconds(totalDuration)
                .averageSpeedKmh(speedCount > 0 ? totalSpeed / speedCount : 0.0)
                .maxSpeedKmh(maxSpeed)
                .zonesEntered(zoneEntries.size())
                .restrictedZonesEntered(restrictedZones.size())
                .totalDwellTimeSeconds(totalDwellTime)
                .dwellTimeByZone(dwellTimeByZone)
                .statusBreakdown(statusBreakdown)
                .build();
    }

    /**
     * Export trail data as CSV string.
     */
    public String exportTrailCsv(UUID assetId, String tenantCode, ZonedDateTime startDate, ZonedDateTime endDate) {
        MovementTrailDTO trail = getTrail(assetId, tenantCode, startDate, endDate);

        StringBuilder csv = new StringBuilder();
        csv.append("Timestamp,Latitude,Longitude,Speed,Heading,Status,Zone\n");

        for (MovementTrailPointDTO point : trail.getPoints()) {
            csv.append(String.format("%s,%f,%f,%s,%s,%s,%s\n",
                    point.getTimestamp(),
                    point.getLatitude(),
                    point.getLongitude(),
                    point.getSpeed() != null ? point.getSpeed() : "",
                    point.getHeading() != null ? point.getHeading() : "",
                    point.getStatus() != null ? point.getStatus() : "",
                    point.getZoneName() != null ? point.getZoneName() : ""));
        }

        return csv.toString();
    }

    /**
     * Convert trail entity to point DTO.
     */
    private MovementTrailPointDTO toPointDTO(AssetMovementTrail entity) {
        return MovementTrailPointDTO.builder()
                .id(entity.getId())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .speed(entity.getSpeed() != null ? entity.getSpeed() : 0.0)
                .heading(entity.getHeading() != null ? entity.getHeading() : 0.0)
                .status(entity.getStatus())
                .timestamp(entity.getTimestamp())
                .zoneName(entity.getZone())
                .inRestrictedZone(entity.getRestrictedZoneId() != null)
                .metadata(entity.getMetadata())
                .build();
    }

    /**
     * Calculate distance between two points using Haversine formula.
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Earth radius in meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
