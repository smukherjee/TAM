package com.utam.simulation.security;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * T076: MovementDiscrepancy entity for anomaly tracking.
 * Records detected movement anomalies like teleportation, speed violations, etc.
 */
@Entity(name = "SimulationMovementDiscrepancy")
@Table(name = "simulation_discrepancies", indexes = {
        @Index(name = "idx_discrepancy_tenant", columnList = "tenantCode"),
        @Index(name = "idx_discrepancy_type", columnList = "discrepancyType"),
        @Index(name = "idx_discrepancy_status", columnList = "status"),
        @Index(name = "idx_discrepancy_detected", columnList = "detectedAt"),
        @Index(name = "idx_discrepancy_entity", columnList = "entityId")
})
public class MovementDiscrepancy {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, length = 10)
    private String tenantCode;

    @Column(nullable = false, length = 50)
    private String discrepancyType;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, length = 100)
    private String entityId;

    @Column(nullable = false, length = 20)
    private String entityType;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column
    private Double previousLatitude;

    @Column
    private Double previousLongitude;

    @Column
    private Double impliedSpeedKmh;

    @Column
    private Double expectedHeading;

    @Column
    private Double actualHeading;

    @Column(nullable = false)
    private Instant detectedAt;

    @Column
    private Instant resolvedAt;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 100)
    private String resolvedBy;

    @Column(length = 500)
    private String resolution;

    @Column
    private Instant createdAt;

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTenantCode() { return tenantCode; }
    public void setTenantCode(String tenantCode) { this.tenantCode = tenantCode; }

    public String getDiscrepancyType() { return discrepancyType; }
    public void setDiscrepancyType(String discrepancyType) { this.discrepancyType = discrepancyType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getPreviousLatitude() { return previousLatitude; }
    public void setPreviousLatitude(Double previousLatitude) { this.previousLatitude = previousLatitude; }

    public Double getPreviousLongitude() { return previousLongitude; }
    public void setPreviousLongitude(Double previousLongitude) { this.previousLongitude = previousLongitude; }

    public Double getImpliedSpeedKmh() { return impliedSpeedKmh; }
    public void setImpliedSpeedKmh(Double impliedSpeedKmh) { this.impliedSpeedKmh = impliedSpeedKmh; }

    public Double getExpectedHeading() { return expectedHeading; }
    public void setExpectedHeading(Double expectedHeading) { this.expectedHeading = expectedHeading; }

    public Double getActualHeading() { return actualHeading; }
    public void setActualHeading(Double actualHeading) { this.actualHeading = actualHeading; }

    public Instant getDetectedAt() { return detectedAt; }
    public void setDetectedAt(Instant detectedAt) { this.detectedAt = detectedAt; }

    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }

    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
