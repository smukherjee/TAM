package com.utam.simulation.alert;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Alert entity representing a simulation alert.
 * Implements FR-091 to FR-094: Alert generation for delays and anomalies.
 */
@Entity(name = "SimulationAlert")
@Table(name = "simulation_alerts", indexes = {
        @Index(name = "idx_alert_tenant", columnList = "tenantCode"),
        @Index(name = "idx_alert_type", columnList = "alertType"),
        @Index(name = "idx_alert_severity", columnList = "severity"),
        @Index(name = "idx_alert_status", columnList = "status"),
        @Index(name = "idx_alert_created", columnList = "createdAt")
})
public class Alert {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, length = 10)
    private String tenantCode;

    @Column(nullable = false, length = 30)
    private String alertType; // DELAY, GEOFENCE, MAINTENANCE, EQUIPMENT, SLA_BREACH

    @Column(nullable = false, length = 20)
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL

    @Column(nullable = false, length = 20)
    private String status; // NEW, ACKNOWLEDGED, IN_PROGRESS, RESOLVED, DISMISSED

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private Instant acknowledgedAt;

    @Column
    private Instant resolvedAt;

    // Reference to related entity
    @Column(length = 30)
    private String entityType; // VEHICLE, TURNAROUND, DISPATCH, FLIGHT

    @Column
    private UUID entityId;

    @Column(length = 50)
    private String entityReference;

    // Additional context
    @Column
    private Integer delayMinutes;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(length = 100)
    private String assignedTo;

    // Additional fields for enrichment
    @Column(length = 500)
    private String message;

    @Column
    private Double financialImpact;

    @Column(length = 500)
    private String recommendation;

    @Column
    private Instant escalationDeadline;

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(Instant acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    public String getEntityReference() {
        return entityReference;
    }

    public void setEntityReference(String entityReference) {
        this.entityReference = entityReference;
    }

    public Integer getDelayMinutes() {
        return delayMinutes;
    }

    public void setDelayMinutes(Integer delayMinutes) {
        this.delayMinutes = delayMinutes;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Double getFinancialImpact() {
        return financialImpact;
    }

    public void setFinancialImpact(Double financialImpact) {
        this.financialImpact = financialImpact;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public Instant getEscalationDeadline() {
        return escalationDeadline;
    }

    public void setEscalationDeadline(Instant escalationDeadline) {
        this.escalationDeadline = escalationDeadline;
    }

    /**
     * Check if alert is critical.
     */
    public boolean isCritical() {
        return "CRITICAL".equals(severity) || "HIGH".equals(severity);
    }

    /**
     * Check if alert is active (not resolved).
     */
    public boolean isActive() {
        return !List.of("RESOLVED", "DISMISSED").contains(status);
    }

    @Override
    public String toString() {
        return "Alert{" +
                "id=" + id +
                ", alertType='" + alertType + '\'' +
                ", severity='" + severity + '\'' +
                ", status='" + status + '\'' +
                ", title='" + title + '\'' +
                '}';
    }
}
