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

    @Column(name = "asset_identifier", length = 20, nullable = false)
    private String assetIdentifier;

    @Column(name = "current_location", columnDefinition = "geometry(Point,4326)", nullable = false)
    private Point currentLocation;

    @Column(name = "current_zone_name", length = 100)
    private String currentZoneName;

    @Column(name = "restricted_zone_id")
    private UUID restrictedZoneId;

    @Column(name = "is_in_restricted_zone", nullable = false)
    @Builder.Default
    private Boolean isInRestrictedZone = false;

    @Column(name = "is_authorized", nullable = false)
    @Builder.Default
    private Boolean isAuthorized = true;

    @Column(name = "speed")
    private Double speed;

    @Column(name = "heading")
    private Double heading;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "last_updated", nullable = false)
    private ZonedDateTime lastUpdated;

    @Column(name = "tenant_code", length = 4, nullable = false)
    private String tenantCode;

    @PreUpdate
    protected void onUpdate() {
        lastUpdated = ZonedDateTime.now();
    }
}
