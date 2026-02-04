package com.utam.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Links vehicles to turnaround tasks with timing information.
 * Implements FR-059 to FR-063: Vehicle-to-flight correlation.
 */
@Entity
@Table(name = "vehicle_assignments", indexes = {
        @Index(name = "idx_assignment_turnaround", columnList = "turnaroundId"),
        @Index(name = "idx_assignment_vehicle", columnList = "vehicleId"),
        @Index(name = "idx_assignment_status", columnList = "status"),
        @Index(name = "idx_assignment_tenant", columnList = "tenantCode")
})
public class VehicleAssignment {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false)
    private UUID turnaroundId;

    @Column(nullable = false)
    private UUID vehicleId;

    @Column(nullable = false, length = 50)
    private String taskType; // From TurnaroundTaskType

    @Column(nullable = false)
    private Instant dispatchTime;

    @Column
    private Instant arrivalTime;

    @Column
    private Instant startTime;

    @Column
    private Instant endTime;

    @Column
    private Instant departureTime;

    @Column(nullable = false, length = 20)
    private String status;

    @Column
    private Double distanceKm;

    @Column
    private Integer travelTimeSeconds;

    @Column(nullable = false, length = 10)
    private String tenantCode;

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private Instant updatedAt;

    // Status constants
    public static final String STATUS_DISPATCHED = "DISPATCHED";
    public static final String STATUS_EN_ROUTE = "EN_ROUTE";
    public static final String STATUS_ARRIVED = "ARRIVED";
    public static final String STATUS_WORKING = "WORKING";
    public static final String STATUS_COMPLETE = "COMPLETE";
    public static final String STATUS_CANCELLED = "CANCELLED";

    public VehicleAssignment() {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
        this.status = STATUS_DISPATCHED;
    }

    public VehicleAssignment(UUID turnaroundId, UUID vehicleId, String taskType, String tenantCode) {
        this();
        this.turnaroundId = turnaroundId;
        this.vehicleId = vehicleId;
        this.taskType = taskType;
        this.tenantCode = tenantCode;
        this.dispatchTime = Instant.now();
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTurnaroundId() {
        return turnaroundId;
    }

    public void setTurnaroundId(UUID turnaroundId) {
        this.turnaroundId = turnaroundId;
    }

    public UUID getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(UUID vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public Instant getDispatchTime() {
        return dispatchTime;
    }

    public void setDispatchTime(Instant dispatchTime) {
        this.dispatchTime = dispatchTime;
    }

    public Instant getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(Instant arrivalTime) {
        this.arrivalTime = arrivalTime;
        this.updatedAt = Instant.now();
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
        this.updatedAt = Instant.now();
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
        this.updatedAt = Instant.now();
    }

    public Instant getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(Instant departureTime) {
        this.departureTime = departureTime;
        this.updatedAt = Instant.now();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public Integer getTravelTimeSeconds() {
        return travelTimeSeconds;
    }

    public void setTravelTimeSeconds(Integer travelTimeSeconds) {
        this.travelTimeSeconds = travelTimeSeconds;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Convenience methods
    public void markArrived() {
        this.arrivalTime = Instant.now();
        this.status = STATUS_ARRIVED;
        if (dispatchTime != null) {
            this.travelTimeSeconds = (int) java.time.Duration.between(dispatchTime, arrivalTime).toSeconds();
        }
    }

    public void startWork() {
        this.startTime = Instant.now();
        this.status = STATUS_WORKING;
    }

    public void completeWork() {
        this.endTime = Instant.now();
        this.status = STATUS_COMPLETE;
    }

    public void depart() {
        this.departureTime = Instant.now();
    }

    public boolean isComplete() {
        return STATUS_COMPLETE.equals(status) || STATUS_CANCELLED.equals(status);
    }
}
