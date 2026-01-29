package com.utam.tracking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for movement trail data transfer.
 * Feature: 005-asset-tracking-security
 * Task: T055
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovementTrailDTO {
    
    private UUID assetId;
    private String assetIdentifier;
    private String assetName;
    private String assetCategory;
    private ZonedDateTime startTime;
    private ZonedDateTime endTime;
    private Integer totalPoints;
    private Double totalDistanceMeters;
    private Double avgSpeed;
    private Double maxSpeed;
    private List<MovementTrailPointDTO> points;
    private List<ZoneEntryDTO> zoneEntries;
    private TrailSummaryDTO summary;
    private String tenantCode;
}
