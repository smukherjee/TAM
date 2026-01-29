package com.utam.tracking.controller;

import com.utam.tracking.dto.RestrictedZoneDTO;
import com.utam.tracking.service.RestrictedZoneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for restricted zone management.
 * Feature: 005-asset-tracking-security
 * Task: T062
 */
@RestController
@RequestMapping("/api/tracking/zones")
@Tag(name = "Restricted Zones", description = "Restricted zone management endpoints")
public class RestrictedZoneController {

    private final RestrictedZoneService zoneService;

    public RestrictedZoneController(RestrictedZoneService zoneService) {
        this.zoneService = zoneService;
    }

    @GetMapping
    @Operation(summary = "Get restricted zones", description = "Get all restricted zones for a tenant")
    @PreAuthorize("hasAnyRole('ADMIN', 'GH', 'AIRPORT_USER')")
    public ResponseEntity<List<RestrictedZoneDTO>> getZones(
            @Parameter(description = "Tenant code") @RequestParam String tenantCode,
            @Parameter(description = "Active only") @RequestParam(defaultValue = "true") Boolean activeOnly) {

        List<RestrictedZoneDTO> zones = activeOnly 
            ? zoneService.getActiveZones(tenantCode)
            : zoneService.getZones(tenantCode);
        
        return ResponseEntity.ok(zones);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get zone by ID", description = "Get a specific restricted zone by its ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'GH', 'AIRPORT_USER')")
    public ResponseEntity<RestrictedZoneDTO> getZoneById(@PathVariable UUID id) {
        RestrictedZoneDTO zone = zoneService.getZoneById(id);
        return ResponseEntity.ok(zone);
    }

    @PostMapping
    @Operation(summary = "Create restricted zone", description = "Create a new restricted zone (ADMIN only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestrictedZoneDTO> createZone(
            @Valid @RequestBody RestrictedZoneDTO zoneDTO) {

        RestrictedZoneDTO created = zoneService.createZone(zoneDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update restricted zone", description = "Update an existing restricted zone (ADMIN only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestrictedZoneDTO> updateZone(
            @PathVariable UUID id,
            @Valid @RequestBody RestrictedZoneDTO zoneDTO) {

        RestrictedZoneDTO updated = zoneService.updateZone(id, zoneDTO);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete restricted zone", description = "Delete a restricted zone (ADMIN only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteZone(@PathVariable UUID id) {
        zoneService.deleteZone(id);
        return ResponseEntity.noContent().build();
    }
}
