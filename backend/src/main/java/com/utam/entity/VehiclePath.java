package com.utam.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Admin-defined paths for vehicle simulation.
 * Implements FR-032 to FR-039: Admin path drawing interface.
 */
@Entity
@Table(name = "vehicle_paths", indexes = {
        @Index(name = "idx_path_tenant", columnList = "tenant_code"),
        @Index(name = "idx_path_vehicle_type", columnList = "vehicle_type_code"),
        @Index(name = "idx_path_active", columnList = "tenant_code, active")
})
public class VehiclePath {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_code", nullable = false, length = 10)
    private String tenantCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "vehicle_type_code", length = 20)
    private String vehicleTypeCode;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String waypoints; // JSON array of coordinates

    @Column(columnDefinition = "TEXT")
    private String schedule; // JSON: {"startTime": "06:00", "endTime": "23:00", "interval": 300}

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false)
    private Boolean loop = false;

    @Column(name = "total_distance_km")
    private Double totalDistanceKm;

    @Column(name = "estimated_duration_seconds")
    private Integer estimatedDurationSeconds;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    public VehiclePath() {
        this.createdAt = Instant.now();
        this.active = true;
        this.loop = false;
    }

    public VehiclePath(String tenantCode, String name, String vehicleTypeCode) {
        this();
        this.tenantCode = tenantCode;
        this.name = name;
        this.vehicleTypeCode = vehicleTypeCode;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVehicleTypeCode() {
        return vehicleTypeCode;
    }

    public void setVehicleTypeCode(String vehicleTypeCode) {
        this.vehicleTypeCode = vehicleTypeCode;
    }

    public String getWaypoints() {
        return waypoints;
    }

    public void setWaypoints(String waypoints) {
        this.waypoints = waypoints;
        this.updatedAt = Instant.now();
    }

    public String getSchedule() {
        return schedule;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
        this.updatedAt = Instant.now();
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
        this.updatedAt = Instant.now();
    }

    public Boolean getLoop() {
        return loop;
    }

    public void setLoop(Boolean loop) {
        this.loop = loop;
    }

    public Double getTotalDistanceKm() {
        return totalDistanceKm;
    }

    public void setTotalDistanceKm(Double totalDistanceKm) {
        this.totalDistanceKm = totalDistanceKm;
    }

    public Integer getEstimatedDurationSeconds() {
        return estimatedDurationSeconds;
    }

    public void setEstimatedDurationSeconds(Integer estimatedDurationSeconds) {
        this.estimatedDurationSeconds = estimatedDurationSeconds;
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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
