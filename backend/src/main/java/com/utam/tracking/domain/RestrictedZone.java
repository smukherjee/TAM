package com.utam.tracking.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Polygon;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Entity representing restricted zones for airside security.
 * Uses PostGIS POLYGON for spatial queries.
 * 
 * Feature: 005-asset-tracking-security
 * Table: restricted_zones
 */
@Entity
@Table(name = "restricted_zones", schema = "public")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestrictedZone {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_code", length = 4, nullable = false)
    private String tenantCode;

    @Column(name = "zone_id", length = 20, unique = true, nullable = false)
    private String zoneId;

    @Column(name = "zone_name", length = 100, nullable = false)
    private String zoneName;

    @Column(name = "zone_type", length = 20, nullable = false)
    private String zoneType; // PROHIBITED, RESTRICTED, CONTROLLED, MAINTENANCE

    @Column(name = "geometry", columnDefinition = "geometry(Polygon,4326)", nullable = false)
    private Polygon boundary;

    @Column(name = "authorized_asset_categories", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] authorizedAssetCategories;

    @Column(name = "authorized_asset_ids", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] authorizedAssetIds;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "effective_from")
    private ZonedDateTime effectiveFrom;

    @Column(name = "effective_to")
    private ZonedDateTime effectiveTo;

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = ZonedDateTime.now();
        updatedAt = ZonedDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = ZonedDateTime.now();
    }
}
