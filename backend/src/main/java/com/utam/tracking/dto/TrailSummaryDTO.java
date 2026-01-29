package com.utam.tracking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for movement trail summary statistics.
 * Feature: 005-asset-tracking-security
 * Task: T055
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrailSummaryDTO {
    
    private Integer totalPoints;
    private Double totalDistanceMeters;
    private Long totalDurationSeconds;
    private Double averageSpeedKmh;
    private Double maxSpeedKmh;
    private Integer zonesEntered;
    private Integer restrictedZonesEntered;
    private Integer violationsCount;
    private Long totalDwellTimeSeconds;
    private Map<String, Long> dwellTimeByZone; // zoneName -> seconds
    private Map<String, Integer> statusBreakdown; // status -> count
}
