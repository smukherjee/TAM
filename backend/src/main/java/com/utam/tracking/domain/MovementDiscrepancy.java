package com.utam.tracking.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Entity representing movement discrepancies (TimescaleDB hypertable).
 * Records anomalies like unexpected movements or status mismatches.
 * 
 * Feature: 005-asset-tracking-security
 * Table: movement_discrepancies (hypertable partitioned by timestamp)
 */
@Entity
@Table(name = "movement_discrepancies", schema = "public")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovementDiscrepancy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "discrepancy_id", length = 50, nullable = false)
    private String discrepancyId;

    @Column(name = "asset_id")
    private UUID assetId;

    @Column(name = "asset_identifier", length = 50, nullable = false)
    private String assetIdentifier;

    @Column(name = "asset_name", length = 100)
    private String assetName;

    @Column(name = "asset_category", length = 50)
    private String assetCategory;

    @Column(name = "discrepancy_type", length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    private DiscrepancyType discrepancyType;

    @Column(name = "expected_location", length = 100)
    private String expectedLocation;

    @Column(name = "actual_location", length = 100)
    private String actualLocation;

    @Column(name = "expected_latitude")
    private Double expectedLatitude;

    @Column(name = "expected_longitude")
    private Double expectedLongitude;

    @Column(name = "actual_latitude")
    private Double actualLatitude;

    @Column(name = "actual_longitude")
    private Double actualLongitude;

    @Column(name = "distance_deviation_meters")
    private Double deviationMeters;

    @Column(name = "expected_status", length = 50)
    private String expectedStatus;

    @Column(name = "actual_status", length = 50)
    private String actualStatus;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "severity", length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DiscrepancySeverity severity = DiscrepancySeverity.MEDIUM;

    @Column(name = "acknowledged")
    @Builder.Default
    private Boolean acknowledged = false;

    @Column(name = "acknowledged_by")
    private UUID acknowledgedBy;

    @Column(name = "acknowledged_at")
    private ZonedDateTime acknowledgedAt;

    @Column(name = "resolution_notes", columnDefinition = "text")
    private String resolutionNotes;

    @Column(name = "timestamp", nullable = false)
    private ZonedDateTime timestamp;

    @Column(name = "tenant_code", length = 4, nullable = false)
    private String tenantCode;

    @Column(name = "created_at")
    private ZonedDateTime createdAt;

    public enum DiscrepancyType {
        UNEXPECTED_MOVEMENT,
        LOCATION_MISMATCH,
        SPEED_ANOMALY,
        MISSING_TRACKING,
        DUPLICATE_SIGNAL
    }

    public enum DiscrepancySeverity {
        CRITICAL,
        HIGH,
        MEDIUM,
        LOW
    }
}
