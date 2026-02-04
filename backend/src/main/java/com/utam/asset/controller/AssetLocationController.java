package com.utam.asset.controller;

import com.utam.asset.dto.AssetLocationDTO;
import com.utam.asset.dto.AssetLocationResponse;
import com.utam.asset.service.AssetLocationService;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * REST API controller for live asset location tracking.
 * Provides endpoints for Universal Asset Map (US5).
 * <p>
 * Feature: 005-asset-tracking-security
 * Task: T029
 */
@RestController
@RequestMapping("/api/tracking/assets")
@Tag(name = "Asset Tracking", description = "Live asset location and tracking APIs")
public class AssetLocationController {

    private static final Logger logger = LoggerFactory.getLogger(AssetLocationController.class);

    private final AssetLocationService assetLocationService;

    public AssetLocationController(AssetLocationService assetLocationService) {
        this.assetLocationService = assetLocationService;
    }

    /**
     * Get all live assets with optional filters.
     * Supports pagination and filtering by tenant, category, status, and zone.
     *
     * @param tenantCode Required tenant filter (VIDP, LIRN, YBBN)
     * @param category   Optional category filter (Emergency, Fueling, Cargo, etc.)
     * @param status     Optional status filter (Available, In Use, Maintenance, Out of Service)
     * @param zoneId     Optional zone filter (UUID)
     * @param page       Page number (1-indexed, default 1)
     * @param pageSize   Page size (default 50, max 500)
     * @return Paginated list of live assets
     */
        @GetMapping("/live")
        @PreAuthorize("permitAll()")
    @Operation(
            summary = "Get all live assets",
            description = "Retrieve live asset positions with optional filters. " +
                    "Results are cached for 5 seconds. Supports pagination."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved live assets"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<AssetLocationResponse> getAllLiveAssets(
            @Parameter(description = "Tenant code (required)", required = true)
            @RequestParam String tenantCode,

            @Parameter(description = "Asset category filter (optional)")
            @RequestParam(required = false) String category,

            @Parameter(description = "Asset status filter (optional)")
            @RequestParam(required = false) String status,

            @Parameter(description = "Restricted zone UUID filter (optional)")
            @RequestParam(required = false) UUID zoneId,

            @Parameter(description = "Page number (1-indexed, default 1)")
            @RequestParam(defaultValue = "1") Integer page,

            @Parameter(description = "Page size (default 50, max 500)")
            @RequestParam(defaultValue = "50") Integer pageSize) {

        logger.info("GET /api/tracking/assets/live - tenant={}, category={}, status={}, zone={}, page={}, pageSize={}",
                tenantCode, category, status, zoneId, page, pageSize);

        // Validate page parameters
        if (page < 1) {
            page = 1;
        }
        if (pageSize < 1 || pageSize > 500) {
            pageSize = 50;
        }

        // Calculate offset
        int offset = (page - 1) * pageSize;

        // Query assets
        List<AssetLocationDTO> assets = assetLocationService.getAllLiveAssets(
                tenantCode, category, status, zoneId, pageSize, offset);

        // Get total count for pagination metadata
        long totalCount = assetLocationService.countLiveAssets(tenantCode, category, status, zoneId);
        int totalPages = (int) Math.ceil((double) totalCount / pageSize);

        // Build response
        AssetLocationResponse response = AssetLocationResponse.builder()
                .assets(assets)
                .totalCount(totalCount)
                .count(assets.size())
                .page(page)
                .pageSize(pageSize)
                .totalPages(totalPages)
                .timestamp(Instant.now().toString())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get single live asset by ID.
     *
     * @param assetId Asset UUID
     * @return Asset location details
     */
    @GetMapping("/live/{assetId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GH', 'AIRPORT_USER')")
    @Operation(
            summary = "Get live asset by ID",
            description = "Retrieve current location and details for a specific asset. " +
                    "Results are cached for 5 seconds."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved asset"),
            @ApiResponse(responseCode = "404", description = "Asset not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<AssetLocationDTO> getLiveAssetById(
            @Parameter(description = "Asset UUID", required = true)
            @PathVariable UUID assetId) {

        logger.info("GET /api/tracking/assets/live/{} - assetId={}", assetId, assetId);

        AssetLocationDTO asset = assetLocationService.getLiveAssetById(assetId);

        if (asset == null) {
            logger.warn("Asset not found: {}", assetId);
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(asset);
    }

    /**
     * Get assets in a specific zone.
     *
     * @param zoneId     Restricted zone UUID
     * @param tenantCode Tenant filter
     * @return List of assets in zone
     */
    @GetMapping("/live/zone/{zoneId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GH')")
    @Operation(
            summary = "Get assets in zone",
            description = "Retrieve all assets currently located within a specific restricted zone."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved assets in zone"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<List<AssetLocationDTO>> getAssetsInZone(
            @Parameter(description = "Restricted zone UUID", required = true)
            @PathVariable UUID zoneId,

            @Parameter(description = "Tenant code filter", required = true)
            @RequestParam String tenantCode) {

        logger.info("GET /api/tracking/assets/live/zone/{} - zoneId={}, tenant={}",
                zoneId, zoneId, tenantCode);

        List<AssetLocationDTO> assets = assetLocationService.getAssetsInZone(zoneId, tenantCode);

        return ResponseEntity.ok(assets);
    }

    /**
     * Get assets by category.
     *
     * @param category   Asset category
     * @param tenantCode Tenant filter
     * @return List of assets
     */
    @GetMapping("/live/category/{category}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GH', 'AIRPORT_USER')")
    @Operation(
            summary = "Get assets by category",
            description = "Retrieve all live assets of a specific category " +
                    "(Emergency, Fueling, Cargo, Ground Support, Transport, Power, Services, Other)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved assets by category"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<List<AssetLocationDTO>> getAssetsByCategory(
            @Parameter(description = "Asset category", required = true)
            @PathVariable String category,

            @Parameter(description = "Tenant code filter", required = true)
            @RequestParam String tenantCode) {

        logger.info("GET /api/tracking/assets/live/category/{} - category={}, tenant={}",
                category, category, tenantCode);

        List<AssetLocationDTO> assets = assetLocationService.getAssetsByCategory(category, tenantCode);

        return ResponseEntity.ok(assets);
    }

    /**
     * Health check endpoint for asset tracking service.
     *
     * @return Service status
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if asset tracking service is operational")
    @ApiResponse(responseCode = "200", description = "Service is healthy")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Asset tracking service is healthy");
    }
}
