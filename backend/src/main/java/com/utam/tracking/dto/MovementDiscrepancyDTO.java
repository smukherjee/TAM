package com.utam.tracking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * DTO for movement discrepancy data transfer.
 * Feature: 005-asset-tracking-security
 * Task: T055
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovementDiscrepancyDTO {
    
    private UUID id;
    private String discrepancyId;
    private UUID assetId;
    private String assetIdentifier;
    private String assetName;
    private String assetCategory;
    private String discrepancyType;
    private String expectedLocation;
    private String actualLocation;
    private Double expectedLatitude;
    private Double expectedLongitude;
    private Double actualLatitude;
    private Double actualLongitude;
    private Double deviationMeters;
    private String expectedStatus;
    private String actualStatus;
    private String description;
    private String severity;
    private ZonedDateTime timestamp;
    private Boolean acknowledged;
    private UUID acknowledgedBy;
    private ZonedDateTime acknowledgedAt;
    private String resolutionNotes;
    private String tenantCode;
}
