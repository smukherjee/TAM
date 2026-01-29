package com.utam.tracking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for individual movement trail point.
 * Feature: 005-asset-tracking-security
 * Task: T055
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovementTrailPointDTO {
    
    private UUID id;
    private Double latitude;
    private Double longitude;
    private Double speed;
    private Double heading;
    private String status;
    private ZonedDateTime timestamp;
    private String zoneName;
    private String zoneType;
    private Boolean inRestrictedZone;
    private Map<String, Object> metadata;
}
