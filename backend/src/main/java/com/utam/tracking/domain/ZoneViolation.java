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
 * Entity representing zone violations (TimescaleDB hypertable).
 * Records when assets enter unauthorized restricted zones.
 * 
 * Feature: 005-asset-tracking-security
 * Table: zone_violations (hypertable partitioned by timestamp)
 */
@Entity
@Table(name = "zone_violations", schema = "public")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZoneViolation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "violation_id", length = 50, unique = true, nullable = false)
    private String violationId;

    @Column(name = "asset_id", nullable = false)
    private UUID assetId;

    @Column(name = "asset_identifier", length = 20, nullable = false)
    private String assetIdentifier;

    @Column(name = "asset_name", length = 100)
    private String assetName;

    @Column(name = "asset_category", length = 50)
    private String assetCategory;

    @Column(name = "restricted_zone_id", nullable = false)
    private UUID restrictedZoneId;

    @Column(name = "zone_name", length = 100, nullable = false)
    private String zoneName;

    @Column(name = "zone_type", length = 20, nullable = false)
    private String zoneType;

    @Column(name = "violation_type", length = 50, nullable = false)
    private String violationType; // UNAUTHORIZED_ENTRY, OVERSTAY, etc.

    @Column(name = "entry_location", columnDefinition = "geometry(Point,4326)")
    private Point entryLocation;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "severity", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private ViolationSeverity severity;

    @Column(name = "acknowledged", nullable = false)
    @Builder.Default
    private Boolean acknowledged = false;

    @Column(name = "acknowledged_by", length = 100)
    private String acknowledgedBy;

    @Column(name = "acknowledged_at")
    private ZonedDateTime acknowledgedAt;

    @Column(name = "resolution_notes", columnDefinition = "text")
    private String resolutionNotes;

    @Column(name = "timestamp", nullable = false)
    private ZonedDateTime timestamp;

    @Column(name = "tenant_code", length = 4, nullable = false)
    private String tenantCode;

    public enum ViolationSeverity {
        CRITICAL,
        HIGH,
        MEDIUM,
        LOW
    }
}
