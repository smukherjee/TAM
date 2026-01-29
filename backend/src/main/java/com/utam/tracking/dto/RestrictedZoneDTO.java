package com.utam.tracking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for restricted zone data transfer.
 * Feature: 005-asset-tracking-security
 * Task: T055
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestrictedZoneDTO {
    
    private UUID id;
    
    @NotBlank(message = "Zone ID is required")
    private String zoneId;
    
    @NotBlank(message = "Zone name is required")
    private String zoneName;
    
    @NotBlank(message = "Zone type is required")
    private String zoneType;
    
    private String description;
    
    @NotNull(message = "Boundary coordinates are required")
    private List<List<Double>> boundaryCoordinates; // [[lng, lat], [lng, lat], ...]
    
    private String[] authorizedAssetCategories;
    
    private Boolean isActive;
    
    private ZonedDateTime effectiveFrom;
    
    private ZonedDateTime effectiveTo;
    
    private String tenantCode;
    
    private ZonedDateTime createdAt;
    
    private ZonedDateTime updatedAt;
}
