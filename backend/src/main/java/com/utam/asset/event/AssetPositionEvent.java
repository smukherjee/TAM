package com.utam.asset.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Kafka event model for asset position updates from NiFi pipeline.
 * Maps to the asset-positions-json Kafka topic.
 * <p>
 * Consumed by MovementTrailProcessor to:
 * - Write to asset_movement_trail
 * - Update asset_location_register
 * - Detect zone violations
 * - Detect movement discrepancies
 * - Broadcast WebSocket events
 * <p>
 * Feature: 005-asset-tracking-security (US1-US4)
 * Task: T026a
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetPositionEvent {

    /**
     * Vehicle unique identifier (from vehicles.vehicle_id)
     */
    @JsonProperty("vehicle_id")
    private String vehicleId;

    /**
     * Asset unique identifier (from assets.id)
     * Null if vehicle not matched to asset via qr_id
     */
    @JsonProperty("asset_id")
    private UUID assetId;

    /**
     * Latitude coordinate (WGS84)
     */
    @JsonProperty("latitude")
    private Double latitude;

    /**
     * Longitude coordinate (WGS84)
     */
    @JsonProperty("longitude")
    private Double longitude;

    /**
     * Speed in km/h
     */
    @JsonProperty("speed")
    private Double speed;

    /**
     * Heading in degrees (0-360)
     */
    @JsonProperty("heading")
    private Double heading;

    /**
     * Vehicle status (e.g., MOVING, IDLE, STOPPED)
     */
    @JsonProperty("status")
    private String status;

    /**
     * Position timestamp from GPS (UTC)
     */
    @JsonProperty("timestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX", timezone = "UTC")
    private Instant timestamp;

    /**
     * Tenant code (VIDP, LIRN, YBBN)
     */
    @JsonProperty("tenant_code")
    private String tenantCode;
}
