package com.utam.asset.controller;

import com.utam.asset.dto.HeatmapDataDTO;
import com.utam.asset.dto.HotspotDetailDTO;
import com.utam.asset.service.HeatmapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * REST API controller for heatmap visualization data.
 * Provides activity density, violation density, and dwell time heatmaps.
 * <p>
 * Feature: 005-asset-tracking-security (US6 - Heatmap Visualization)
 * Task: T034
 */
@RestController
@RequestMapping("/api/tracking/heatmap")
@Tag(name = "Heatmap API", description = "Activity and violation density heatmap APIs")
public class HeatmapController {

    private static final Logger logger = LoggerFactory.getLogger(HeatmapController.class);

    private final HeatmapService heatmapService;

    // Max date range (30 days as per requirements)
    private static final int MAX_DATE_RANGE_DAYS = 30;

    public HeatmapController(HeatmapService heatmapService) {
        this.heatmapService = heatmapService;
    }

    /**
     * Get activity density heatmap.
     * Shows movement hotspots across the airside.
     *
     * @param tenantCode Tenant filter (required)
     * @param startDate  Start date ISO 8601 (default: 7 days ago)
     * @param endDate    End date ISO 8601 (default: now)
     * @param gridSize   Grid resolution (10m, 25m, 50m, 100m - default: 25m)
     * @return List of heatmap cells with normalized intensity
     */
    @GetMapping("/activity")
    @PreAuthorize("hasAnyRole('ADMIN', 'GH', 'AIRPORT_USER')")
    @Operation(
            summary = "Get activity density heatmap",
            description = "Retrieve movement activity heatmap data from asset_activity_heatmap view. " +
                    "Returns grid cells with normalized intensity (0-1 scale). " +
                    "Supports 4 grid resolutions: 10m, 25m, 50m, 100m. " +
                    "Max date range: 30 days."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved activity heatmap"),
            @ApiResponse(responseCode = "400", description = "Invalid date range or parameters"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<List<HeatmapDataDTO>> getActivityHeatmap(
            @Parameter(description = "Tenant code (required)", required = true)
            @RequestParam String tenantCode,

            @Parameter(description = "Start date (ISO 8601, default: 7 days ago)")
            @RequestParam(required = false) String startDate,

            @Parameter(description = "End date (ISO 8601, default: now)")
            @RequestParam(required = false) String endDate,

            @Parameter(description = "Grid size (10m, 25m, 50m, 100m - default: 25m)")
            @RequestParam(defaultValue = "25m") @Pattern(regexp = "10m|25m|50m|100m") String gridSize) {

        logger.info("GET /api/tracking/heatmap/activity - tenant={}, start={}, end={}, grid={}",
                tenantCode, startDate, endDate, gridSize);

        // Set defaults
        String effectiveStartDate = startDate != null ? startDate : LocalDate.now().minusDays(7).toString();
        String effectiveEndDate = endDate != null ? endDate : LocalDate.now().toString();

        // Validate date range
        validateDateRange(effectiveStartDate, effectiveEndDate);

        List<HeatmapDataDTO> heatmap = heatmapService.getActivityHeatmap(
                tenantCode, effectiveStartDate, effectiveEndDate, gridSize);

        return ResponseEntity.ok(heatmap);
    }

    /**
     * Get violation density heatmap.
     * Shows zone violation hotspots.
     *
     * @param tenantCode Tenant filter (required)
     * @param startDate  Start date ISO 8601
     * @param endDate    End date ISO 8601
     * @param gridSize   Grid resolution
     * @return List of heatmap cells
     */
    @GetMapping("/violations")
    @PreAuthorize("hasAnyRole('ADMIN', 'GH')")
    @Operation(
            summary = "Get violation density heatmap",
            description = "Retrieve zone violation heatmap data from violation_heatmap view. " +
                    "Returns grid cells with normalized intensity based on violation count. " +
                    "Includes severity breakdown (critical, high, medium, low)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved violation heatmap"),
            @ApiResponse(responseCode = "400", description = "Invalid date range or parameters"),
            @ApiResponse(responseCode = "403", description = "Access denied - requires ADMIN or GH role")
    })
    public ResponseEntity<List<HeatmapDataDTO>> getViolationHeatmap(
            @Parameter(description = "Tenant code (required)", required = true)
            @RequestParam String tenantCode,

            @Parameter(description = "Start date (ISO 8601, default: 7 days ago)")
            @RequestParam(required = false) String startDate,

            @Parameter(description = "End date (ISO 8601, default: now)")
            @RequestParam(required = false) String endDate,

            @Parameter(description = "Grid size (10m, 25m, 50m, 100m - default: 25m)")
            @RequestParam(defaultValue = "25m") @Pattern(regexp = "10m|25m|50m|100m") String gridSize) {

        logger.info("GET /api/tracking/heatmap/violations - tenant={}, start={}, end={}, grid={}",
                tenantCode, startDate, endDate, gridSize);

        String effectiveStartDate = startDate != null ? startDate : LocalDate.now().minusDays(7).toString();
        String effectiveEndDate = endDate != null ? endDate : LocalDate.now().toString();

        validateDateRange(effectiveStartDate, effectiveEndDate);

        List<HeatmapDataDTO> heatmap = heatmapService.getViolationHeatmap(
                tenantCode, effectiveStartDate, effectiveEndDate, gridSize);

        return ResponseEntity.ok(heatmap);
    }

    /**
     * Get dwell time heatmap (future enhancement).
     *
     * @param tenantCode Tenant filter
     * @param startDate  Start date
     * @param endDate    End date
     * @param gridSize   Grid resolution
     * @return List of heatmap cells
     */
    @GetMapping("/dwell")
    @PreAuthorize("hasAnyRole('ADMIN', 'GH')")
    @Operation(
            summary = "Get dwell time heatmap (future enhancement)",
            description = "Retrieve dwell time heatmap showing areas where assets remain stationary. " +
                    "Currently returns empty list - feature not yet implemented."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved dwell heatmap (empty)"),
            @ApiResponse(responseCode = "501", description = "Not implemented yet")
    })
    public ResponseEntity<List<HeatmapDataDTO>> getDwellHeatmap(
            @Parameter(description = "Tenant code (required)", required = true)
            @RequestParam String tenantCode,

            @Parameter(description = "Start date (ISO 8601)")
            @RequestParam(required = false) String startDate,

            @Parameter(description = "End date (ISO 8601)")
            @RequestParam(required = false) String endDate,

            @Parameter(description = "Grid size (10m, 25m, 50m, 100m - default: 25m)")
            @RequestParam(defaultValue = "25m") String gridSize) {

        logger.warn("GET /api/tracking/heatmap/dwell - NOT YET IMPLEMENTED");

        String effectiveStartDate = startDate != null ? startDate : LocalDate.now().minusDays(7).toString();
        String effectiveEndDate = endDate != null ? endDate : LocalDate.now().toString();

        List<HeatmapDataDTO> heatmap = heatmapService.getDwellHeatmap(
                tenantCode, effectiveStartDate, effectiveEndDate, gridSize);

        return ResponseEntity.ok(heatmap);
    }

    /**
     * Get hotspot details for a clicked grid cell.
     * Provides drill-down information for modal/popup.
     *
     * @param latitude   Grid cell latitude
     * @param longitude  Grid cell longitude
     * @param mode       Heatmap mode (activity, violations, dwell)
     * @param tenantCode Tenant filter
     * @param startDate  Start date
     * @param endDate    End date
     * @param gridSize   Grid resolution
     * @return Hotspot details
     */
    @GetMapping("/hotspot")
    @PreAuthorize("hasAnyRole('ADMIN', 'GH', 'AIRPORT_USER')")
    @Operation(
            summary = "Get hotspot details",
            description = "Retrieve detailed information for a clicked heatmap cell. " +
                    "Includes asset list, time distribution, and violation breakdown (if applicable). " +
                    "Used for drill-down modals/popups."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved hotspot details"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<HotspotDetailDTO> getHotspotDetails(
            @Parameter(description = "Grid cell latitude", required = true)
            @RequestParam Double latitude,

            @Parameter(description = "Grid cell longitude", required = true)
            @RequestParam Double longitude,

            @Parameter(description = "Heatmap mode (activity, violations, dwell)", required = true)
            @RequestParam String mode,

            @Parameter(description = "Tenant code", required = true)
            @RequestParam String tenantCode,

            @Parameter(description = "Start date (ISO 8601)")
            @RequestParam(required = false) String startDate,

            @Parameter(description = "End date (ISO 8601)")
            @RequestParam(required = false) String endDate,

            @Parameter(description = "Grid size (10m, 25m, 50m, 100m - default: 25m)")
            @RequestParam(defaultValue = "25m") String gridSize) {

        logger.info("GET /api/tracking/heatmap/hotspot - lat={}, lng={}, mode={}, tenant={}",
                latitude, longitude, mode, tenantCode);

        String effectiveStartDate = startDate != null ? startDate : LocalDate.now().minusDays(7).toString();
        String effectiveEndDate = endDate != null ? endDate : LocalDate.now().toString();

        HotspotDetailDTO hotspot = heatmapService.getHotspotDetails(
                latitude, longitude, mode, tenantCode, effectiveStartDate, effectiveEndDate, gridSize);

        return ResponseEntity.ok(hotspot);
    }

    /**
     * Validate date range (max 30 days).
     */
    private void validateDateRange(String startDate, String endDate) {
        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);

            if (end.isBefore(start)) {
                throw new IllegalArgumentException("End date must be after start date");
            }

            long daysBetween = ChronoUnit.DAYS.between(start, end);
            if (daysBetween > MAX_DATE_RANGE_DAYS) {
                throw new IllegalArgumentException(
                        String.format("Date range exceeds maximum of %d days (requested: %d days)",
                                MAX_DATE_RANGE_DAYS, daysBetween));
            }

        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format. Use ISO 8601 (YYYY-MM-DD): " + e.getMessage());
        }
    }
}
