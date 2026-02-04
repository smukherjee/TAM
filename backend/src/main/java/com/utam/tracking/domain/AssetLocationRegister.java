package com.utam.tracking.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Entity representing current asset locations (snapshot table).
 * Stores the latest known position for each asset.
 * 
 * Feature: 005-asset-tracking-security
 * Table: asset_location_register
 */
@Entity
@Table(name = "asset_location_register", schema = "public")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetLocationRegister {

    @Id
    @Column(name = "asset_id")
    private UUID assetId;

    @Column(name = "asset_identifier", length = 50, nullable = false, unique = true)
    private String assetIdentifier;

    @Column(name = "tenant_code", length = 4, nullable = false)
    private String tenantCode;

    @Column(name = "current_latitude")
    private Double currentLatitude;

    @Column(name = "current_longitude")
    private Double currentLongitude;

    @Column(name = "current_location", columnDefinition = "geometry(Point,4326)")
    private Point currentLocation;

    @Column(name = "current_zone", length = 100)
    private String currentZone;

    @Column(name = "current_zone_name", length = 100)
    private String currentZoneName;

    @Column(name = "current_restricted_zone_id")
    private UUID currentRestrictedZoneId;

    @Column(name = "restricted_zone_id")
    private UUID restrictedZoneId;

    @Column(name = "speed")
    private Double speed;

    @Column(name = "heading")
    private Double heading;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "last_movement_at")
    private ZonedDateTime lastMovementAt;

    @Column(name = "is_in_restricted_zone")
    @Builder.Default
    private Boolean isInRestrictedZone = false;

    @Column(name = "is_authorized_for_zone")
    @Builder.Default
    private Boolean isAuthorizedForZone = true;

    @Column(name = "is_authorized")
    @Builder.Default
    private Boolean isAuthorized = true;

    @Column(name = "last_updated", nullable = false)
    private ZonedDateTime lastUpdated;

    @PreUpdate
    protected void onUpdate() {
        lastUpdated = ZonedDateTime.now();
    }
}
