package com.utam.simulation.vehicle;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Vehicle entity representing a GSE vehicle in the simulation.
 * Implements FR-040 to FR-055: GSE vehicle modeling for all 15 types.
 */
@Entity(name = "SimulationVehicle")
@Table(name = "simulation_vehicles", indexes = {
        @Index(name = "idx_vehicle_tenant", columnList = "tenantCode"),
        @Index(name = "idx_vehicle_status", columnList = "status"),
        @Index(name = "idx_vehicle_type", columnList = "vehicleTypeId"),
        @Index(name = "idx_vehicle_updated", columnList = "lastUpdated")
})
public class Vehicle {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, length = 10)
    private String tenantCode;

    @Column(nullable = false, unique = true, length = 20)
    private String vehicleId;

    @Column(nullable = false, length = 100)
    private String vehicleName;

    @Column(nullable = false)
    private UUID vehicleTypeId;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private Double speed;

    @Column(nullable = false)
    private Double heading;

    @Column(nullable = false, length = 20)
    private String status;

    @Column
    private UUID currentAssignmentId;

    @Column(length = 20)
    private String currentStandCode;

    @Column
    private Instant lastUpdated;

    @Column
    private Instant createdAt;

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

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getVehicleName() {
        return vehicleName;
    }

    public void setVehicleName(String vehicleName) {
        this.vehicleName = vehicleName;
    }

    public UUID getVehicleTypeId() {
        return vehicleTypeId;
    }

    public void setVehicleTypeId(UUID vehicleTypeId) {
        this.vehicleTypeId = vehicleTypeId;
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

    public Double getSpeed() {
        return speed;
    }

    public void setSpeed(Double speed) {
        this.speed = speed;
    }

    public Double getHeading() {
        return heading;
    }

    public void setHeading(Double heading) {
        this.heading = heading;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public UUID getCurrentAssignmentId() {
        return currentAssignmentId;
    }

    public void setCurrentAssignmentId(UUID currentAssignmentId) {
        this.currentAssignmentId = currentAssignmentId;
    }

    public String getCurrentStandCode() {
        return currentStandCode;
    }

    public void setCurrentStandCode(String currentStandCode) {
        this.currentStandCode = currentStandCode;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Vehicle{" +
                "id=" + id +
                ", vehicleId='" + vehicleId + '\'' +
                ", tenantCode='" + tenantCode + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
