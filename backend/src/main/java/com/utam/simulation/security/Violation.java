package com.utam.simulation.security;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * T075: Violation entity for security and compliance tracking.
 * Records unauthorized zone entries, boundary breaches, and policy violations.
 */
@Entity
@Table(name = "simulation_violations", indexes = {
        @Index(name = "idx_violation_tenant", columnList = "tenantCode"),
        @Index(name = "idx_violation_type", columnList = "violationType"),
        @Index(name = "idx_violation_status", columnList = "status"),
        @Index(name = "idx_violation_detected", columnList = "detectedAt"),
        @Index(name = "idx_violation_entity", columnList = "entityId"),
        @Index(name = "idx_violation_zone", columnList = "zoneId")
})
public class Violation {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, length = 10)
    private String tenantCode;

    @Column(nullable = false, length = 50)
    private String violationType;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, length = 20)
    private String severity;

    @Column
    private Integer priorityScore;

    @Column(nullable = false, length = 100)
    private String entityId;

    @Column(nullable = false, length = 20)
    private String entityType;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column
    private UUID zoneId;

    @Column(length = 100)
    private String zoneName;

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

    public String getViolationType() { return violationType; }
    public void setViolationType(String violationType) { this.violationType = violationType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public Integer getPriorityScore() { return priorityScore; }
    public void setPriorityScore(Integer priorityScore) { this.priorityScore = priorityScore; }

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public UUID getZoneId() { return zoneId; }
    public void setZoneId(UUID zoneId) { this.zoneId = zoneId; }

    public String getZoneName() { return zoneName; }
    public void setZoneName(String zoneName) { this.zoneName = zoneName; }

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
