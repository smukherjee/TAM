package com.utam.tracking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * DTO for zone entry/exit events.
 * Feature: 005-asset-tracking-security
 * Task: T055
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZoneEntryDTO {
    
    private UUID zoneId;
    private String zoneName;
    private String zoneType;
    private Double entryLatitude;
    private Double entryLongitude;
    private Double exitLatitude;
    private Double exitLongitude;
    private ZonedDateTime entryTime;
    private ZonedDateTime exitTime;
    private Long dwellTimeSeconds;
    private Boolean authorized;
    private Boolean violationCreated;
}
