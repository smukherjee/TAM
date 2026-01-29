package com.utam.tracking.controller;

import com.utam.tracking.dto.MovementTrailDTO;
import com.utam.tracking.service.MovementTrailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * REST controller for movement trail management.
 * Feature: 005-asset-tracking-security
 * Task: T061
 */
@RestController
@RequestMapping("/api/tracking/trail")
@Tag(name = "Movement Trail", description = "Movement trail endpoints")
@PreAuthorize("hasAnyRole('ADMIN', 'GH', 'AIRPORT_USER')")
public class MovementTrailController {

    private final MovementTrailService trailService;

    public MovementTrailController(MovementTrailService trailService) {
        this.trailService = trailService;
    }

    @GetMapping("/{assetId}")
    @Operation(summary = "Get movement trail", description = "Get movement trail for an asset within a date range")
    public ResponseEntity<MovementTrailDTO> getTrail(
            @Parameter(description = "Asset ID") @PathVariable UUID assetId,
            @Parameter(description = "Start date") @RequestParam 
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,
            @Parameter(description = "End date") @RequestParam 
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate) {

        // Validate date range
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before end date");
        }

        MovementTrailDTO trail = trailService.getTrail(assetId, startDate, endDate);
        return ResponseEntity.ok(trail);
    }

    @GetMapping("/{assetId}/export")
    @Operation(summary = "Export movement trail", description = "Export movement trail as CSV")
    public ResponseEntity<String> exportTrail(
            @Parameter(description = "Asset ID") @PathVariable UUID assetId,
            @Parameter(description = "Start date") @RequestParam 
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,
            @Parameter(description = "End date") @RequestParam 
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate,
            @Parameter(description = "Export format (csv, json)") 
                @RequestParam(defaultValue = "csv") String format) {

        if ("csv".equalsIgnoreCase(format)) {
            String csv = trailService.exportTrailCsv(assetId, startDate, endDate);
            String filename = String.format("trail_%s_%s.csv", 
                assetId.toString().substring(0, 8),
                ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
        } else if ("json".equalsIgnoreCase(format)) {
            MovementTrailDTO trail = trailService.getTrail(assetId, startDate, endDate);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(trail.toString()); // Use proper JSON serialization
        } else {
            throw new IllegalArgumentException("Unsupported export format: " + format);
        }
    }

    @GetMapping("/{assetId}/summary")
    @Operation(summary = "Get trail summary", description = "Get summary statistics for a movement trail")
    public ResponseEntity<MovementTrailDTO> getTrailSummary(
            @Parameter(description = "Asset ID") @PathVariable UUID assetId,
            @Parameter(description = "Start date") @RequestParam 
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,
            @Parameter(description = "End date") @RequestParam 
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate) {

        MovementTrailDTO trail = trailService.getTrail(assetId, startDate, endDate);
        // Return trail with summary but without points for efficiency
        trail.setPoints(null);
        return ResponseEntity.ok(trail);
    }
}
