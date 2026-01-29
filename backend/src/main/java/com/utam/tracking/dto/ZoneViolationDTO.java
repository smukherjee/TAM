package com.utam.tracking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * DTO for zone violation data transfer.
 * Feature: 005-asset-tracking-security
 * Task: T055
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZoneViolationDTO {
    
    private UUID id;
    private String violationId;
    private UUID assetId;
    private String assetIdentifier;
    private String assetName;
    private String assetCategory;
    private UUID restrictedZoneId;
    private String zoneName;
    private String zoneType;
    private String violationType;
    private Double entryLatitude;
    private Double entryLongitude;
    private Double exitLatitude;
    private Double exitLongitude;
    private String severity;
    private ZonedDateTime timestamp;
    private ZonedDateTime exitTimestamp;
    private Long durationSeconds;
    private Boolean acknowledged;
    private String acknowledgedBy;
    private ZonedDateTime acknowledgedAt;
    private String resolutionNotes;
    private String tenantCode;
}
