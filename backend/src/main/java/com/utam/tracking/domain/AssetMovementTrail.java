package com.utam.tracking.domain;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.locationtech.jts.geom.Point;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing movement trail history (TimescaleDB hypertable).
 * Tracks every position update for all assets.
 * 
 * Feature: 005-asset-tracking-security
 * Table: asset_movement_trail (hypertable partitioned by timestamp)
 */
@Entity
@Table(name = "asset_movement_trail", schema = "public")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetMovementTrail {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "asset_id")
    private UUID assetId;

    @Column(name = "asset_identifier", length = 50, nullable = false)
    private String assetIdentifier;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "location", columnDefinition = "geometry(Point,4326)")
    private Point location;

    @Column(name = "speed")
    private Double speed;

    @Column(name = "heading")
    private Double heading;

    @Column(name = "altitude")
    private Double altitude;

    @Column(name = "zone", length = 100)
    private String zone;

    @Column(name = "restricted_zone_id")
    private UUID restrictedZoneId;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "timestamp", nullable = false)
    private ZonedDateTime timestamp;

    @Type(JsonBinaryType.class)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "tenant_code", length = 4, nullable = false)
    private String tenantCode;

    @Column(name = "created_at")
    private ZonedDateTime createdAt;
}
