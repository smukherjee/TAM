package com.utam.tracking.mapper;

import com.utam.tracking.domain.AssetMovementTrail;
import com.utam.tracking.dto.MovementTrailDTO;
import com.utam.tracking.dto.MovementTrailPointDTO;
import com.utam.tracking.dto.TrailSummaryDTO;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for AssetMovementTrail entity to DTO conversions.
 * Feature: 005-asset-tracking-security
 * Task: T056
 */
@Component
public class TrailMapper {

    /**
     * Convert single trail point entity to DTO.
     */
    public MovementTrailPointDTO toPointDTO(AssetMovementTrail entity) {
        if (entity == null) {
            return null;
        }

        return MovementTrailPointDTO.builder()
                .id(entity.getId())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .speed(entity.getSpeed())
                .heading(entity.getHeading())
                .zoneName(entity.getZone())
                .status(entity.getStatus())
                .timestamp(entity.getTimestamp())
                .inRestrictedZone(entity.getRestrictedZoneId() != null)
                .metadata(entity.getMetadata())
                .build();
    }

    /**
     * Convert list of trail points to DTOs.
     */
    public List<MovementTrailPointDTO> toPointDTOList(List<AssetMovementTrail> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }
        return entities.stream()
                .map(this::toPointDTO)
                .collect(Collectors.toList());
    }

    /**
     * Create a complete trail DTO from a list of trail points.
     */
    public MovementTrailDTO toTrailDTO(List<AssetMovementTrail> trailPoints, 
                                        String assetName, 
                                        String assetCategory) {
        if (trailPoints == null || trailPoints.isEmpty()) {
            return null;
        }

        AssetMovementTrail first = trailPoints.get(0);
        AssetMovementTrail last = trailPoints.get(trailPoints.size() - 1);

        // Calculate statistics
        Double totalDistance = calculateTotalDistance(trailPoints);
        Double avgSpeed = calculateAverageSpeed(trailPoints);
        Double maxSpeed = calculateMaxSpeed(trailPoints);

        return MovementTrailDTO.builder()
                .assetId(first.getAssetId())
                .assetIdentifier(first.getAssetIdentifier())
                .assetName(assetName)
                .assetCategory(assetCategory)
                .startTime(first.getTimestamp())
                .endTime(last.getTimestamp())
                .totalPoints(trailPoints.size())
                .totalDistanceMeters(totalDistance)
                .avgSpeed(avgSpeed)
                .maxSpeed(maxSpeed)
                .points(toPointDTOList(trailPoints))
                .tenantCode(first.getTenantCode())
                .build();
    }

    /**
     * Create trail summary from trail points.
     */
    public TrailSummaryDTO toSummaryDTO(List<AssetMovementTrail> trailPoints) {
        if (trailPoints == null || trailPoints.isEmpty()) {
            return null;
        }

        AssetMovementTrail first = trailPoints.get(0);
        AssetMovementTrail last = trailPoints.get(trailPoints.size() - 1);

        Duration duration = Duration.between(first.getTimestamp(), last.getTimestamp());
        Double totalDistance = calculateTotalDistance(trailPoints);
        Double avgSpeed = calculateAverageSpeed(trailPoints);
        Double maxSpeed = calculateMaxSpeed(trailPoints);

        // Count zones visited
        long zonesVisited = trailPoints.stream()
                .map(AssetMovementTrail::getZone)
                .filter(zone -> zone != null && !zone.isEmpty())
                .distinct()
                .count();

        // Count restricted zones entered
        long restrictedZonesEntered = trailPoints.stream()
                .filter(p -> p.getRestrictedZoneId() != null)
                .map(AssetMovementTrail::getRestrictedZoneId)
                .distinct()
                .count();

        // Count status changes
        long statusChanges = countStatusChanges(trailPoints);

        return TrailSummaryDTO.builder()
                .totalPoints(trailPoints.size())
                .totalDistanceMeters(totalDistance)
                .totalDurationSeconds(duration.getSeconds())
                .averageSpeedKmh(avgSpeed)
                .maxSpeedKmh(maxSpeed)
                .zonesEntered((int) zonesVisited)
                .restrictedZonesEntered((int) restrictedZonesEntered)
                .build();
    }

    /**
     * Calculate total distance traveled using Haversine formula.
     */
    private Double calculateTotalDistance(List<AssetMovementTrail> points) {
        if (points == null || points.size() < 2) {
            return 0.0;
        }

        double totalDistance = 0.0;
        for (int i = 1; i < points.size(); i++) {
            AssetMovementTrail prev = points.get(i - 1);
            AssetMovementTrail curr = points.get(i);
            totalDistance += haversineDistance(
                    prev.getLatitude(), prev.getLongitude(),
                    curr.getLatitude(), curr.getLongitude()
            );
        }
        return Math.round(totalDistance * 100.0) / 100.0; // Round to 2 decimal places
    }

    /**
     * Calculate average speed from trail points.
     */
    private Double calculateAverageSpeed(List<AssetMovementTrail> points) {
        if (points == null || points.isEmpty()) {
            return 0.0;
        }

        double avgSpeed = points.stream()
                .filter(p -> p.getSpeed() != null)
                .mapToDouble(AssetMovementTrail::getSpeed)
                .average()
                .orElse(0.0);

        return Math.round(avgSpeed * 100.0) / 100.0;
    }

    /**
     * Calculate maximum speed from trail points.
     */
    private Double calculateMaxSpeed(List<AssetMovementTrail> points) {
        if (points == null || points.isEmpty()) {
            return 0.0;
        }

        return points.stream()
                .filter(p -> p.getSpeed() != null)
                .mapToDouble(AssetMovementTrail::getSpeed)
                .max()
                .orElse(0.0);
    }

    /**
     * Count number of status changes in trail.
     */
    private long countStatusChanges(List<AssetMovementTrail> points) {
        if (points == null || points.size() < 2) {
            return 0;
        }

        long changes = 0;
        String prevStatus = points.get(0).getStatus();
        for (int i = 1; i < points.size(); i++) {
            String currStatus = points.get(i).getStatus();
            if (currStatus != null && !currStatus.equals(prevStatus)) {
                changes++;
                prevStatus = currStatus;
            }
        }
        return changes;
    }

    /**
     * Haversine formula to calculate distance between two coordinates in meters.
     */
    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000; // Earth's radius in meters

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }
}
