package com.utam.asset.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO for live asset location data displayed on Universal Asset Map.
 * Combines data from asset_location_register, assets, and restricted_zones tables.
 * <p>
 * Feature: 005-asset-tracking-security (US5 - Universal Asset Map)
 * Task: T027
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssetLocationDTO {

    /**
     * Asset UUID (from assets.id)
     */
    @NotNull
    private UUID assetId;

    /**
     * Asset identifier (from assets.asset_id, e.g., "A-VIDP-001")
     */
    @NotNull
    @Size(max = 20)
    private String assetIdentifier;

    /**
     * Asset name (from assets.name)
     */
    @NotNull
    @Size(max = 100)
    private String name;

    /**
     * Asset category (Emergency, Fueling, Cargo, etc.)
     */
    @Size(max = 50)
    private String category;

    /**
     * Current latitude (WGS84) from asset_location_register
     */
    @NotNull
    private Double latitude;

    /**
     * Current longitude (WGS84) from asset_location_register
     */
    @NotNull
    private Double longitude;

    /**
     * Asset status (Available, In Use, Maintenance, Out of Service)
     */
    @Size(max = 20)
    private String status;

    /**
     * Current zone name (if inside restricted zone)
     * Null if not in any zone
     */
    @Size(max = 100)
    private String currentZone;

    /**
     * Current zone type (RESTRICTED, STERILE, CARGO, FUEL, etc.)
     * Null if not in any zone
     */
    @Size(max = 50)
    private String currentZoneType;

    /**
     * Zone status (IN_AUTHORIZED_ZONE, IN_RESTRICTED_ZONE, OUTSIDE_ZONES)
     */
    @Size(max = 20)
    private String zoneStatus;

    /**
     * Current speed in km/h (from latest movement trail)
     * Null if stationary or no recent movement
     */
    private Double speed;

    /**
     * Current heading in degrees (0-360)
     * Null if stationary
     */
    private Double heading;

    /**
     * Last seen timestamp (from asset_location_register.last_seen)
     */
    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX", timezone = "UTC")
    private Instant lastSeen;

    /**
     * Owner/operator name (from assets.owner or tenant)
     */
    @Size(max = 100)
    private String owner;

    /**
     * Tenant code (VIDP, LIRN, YBBN)
     */
    @NotNull
    @Size(max = 4)
    private String tenantCode;

    /**
     * QR code identifier (from assets.qr_id)
     * Used for vehicle-asset matching
     */
    @Size(max = 50)
    private String qrId;

    /**
     * Asset value in USD (from assets.value)
     */
    private Double value;

    /**
     * Asset description
     */
    @Size(max = 500)
    private String description;

    /**
     * Whether asset is currently moving (speed > 0)
     */
    private Boolean isMoving;

    /**
     * Whether asset has active zone violation
     */
    private Boolean hasViolation;

    /**
     * Category color code for map marker
     * (Red, Orange, Blue, Green, Purple, Yellow, Teal, Gray)
     */
    @Size(max = 20)
    private String categoryColor;
}
