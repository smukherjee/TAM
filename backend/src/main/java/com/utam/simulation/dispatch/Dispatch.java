package com.utam.simulation.dispatch;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Dispatch entity representing a vehicle dispatch assignment.
 * Implements FR-081 to FR-090: Dispatch simulation with assignment/completion.
 */
@Entity
@Table(name = "simulation_dispatches", indexes = {
        @Index(name = "idx_dispatch_tenant", columnList = "tenantCode"),
        @Index(name = "idx_dispatch_vehicle", columnList = "vehicleId"),
        @Index(name = "idx_dispatch_stand", columnList = "standCode"),
        @Index(name = "idx_dispatch_status", columnList = "status"),
        @Index(name = "idx_dispatch_created", columnList = "createdAt")
})
public class Dispatch {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, length = 10)
    private String tenantCode;

    @Column(nullable = false)
    private UUID vehicleId;

    @Column(nullable = false, length = 20)
    private String vehicleType;

    @Column(nullable = false, length = 10)
    private String standCode;

    @Column
    private UUID turnaroundId;

    @Column(nullable = false, length = 20)
    private String status; // PENDING, EN_ROUTE, ARRIVED, SERVICING, COMPLETED, CANCELLED

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private Instant dispatchedAt;

    @Column
    private Instant arrivedAt;

    @Column
    private Instant completedAt;

    @Column
    private Integer estimatedDurationSeconds;

    @Column
    private Integer actualDurationSeconds;

    @Column
    private Integer delaySeconds;

    @Column(length = 20)
    private String priority; // LOW, NORMAL, HIGH, URGENT

    @Column(length = 200)
    private String notes;

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

    public UUID getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(UUID vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getStandCode() {
        return standCode;
    }

    public void setStandCode(String standCode) {
        this.standCode = standCode;
    }

    public UUID getTurnaroundId() {
        return turnaroundId;
    }

    public void setTurnaroundId(UUID turnaroundId) {
        this.turnaroundId = turnaroundId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getDispatchedAt() {
        return dispatchedAt;
    }

    public void setDispatchedAt(Instant dispatchedAt) {
        this.dispatchedAt = dispatchedAt;
    }

    public Instant getArrivedAt() {
        return arrivedAt;
    }

    public void setArrivedAt(Instant arrivedAt) {
        this.arrivedAt = arrivedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Integer getEstimatedDurationSeconds() {
        return estimatedDurationSeconds;
    }

    public void setEstimatedDurationSeconds(Integer estimatedDurationSeconds) {
        this.estimatedDurationSeconds = estimatedDurationSeconds;
    }

    public Integer getActualDurationSeconds() {
        return actualDurationSeconds;
    }

    public void setActualDurationSeconds(Integer actualDurationSeconds) {
        this.actualDurationSeconds = actualDurationSeconds;
    }

    public Integer getDelaySeconds() {
        return delaySeconds;
    }

    public void setDelaySeconds(Integer delaySeconds) {
        this.delaySeconds = delaySeconds;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    /**
     * Check if dispatch is delayed (>5 min over estimated).
     */
    public boolean isDelayed() {
        return delaySeconds != null && delaySeconds > 300;
    }

    @Override
    public String toString() {
        return "Dispatch{" +
                "id=" + id +
                ", vehicleType='" + vehicleType + '\'' +
                ", standCode='" + standCode + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
