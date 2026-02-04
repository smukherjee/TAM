package com.utam.simulation.vehicle;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * VehiclePosition entity for time-series position tracking.
 * T070: Stores position history for movement trail visualization.
 * Uses TimescaleDB hypertable for efficient time-series storage.
 */
@Entity
@Table(name = "simulation_vehicle_positions", indexes = {
        @Index(name = "idx_vpos_vehicle", columnList = "vehicleId"),
        @Index(name = "idx_vpos_tenant", columnList = "tenantCode"),
        @Index(name = "idx_vpos_recorded", columnList = "recordedAt"),
        @Index(name = "idx_vpos_vehicle_time", columnList = "vehicleId,recordedAt")
})
public class VehiclePosition {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false)
    private UUID vehicleId;

    @Column(nullable = false, length = 10)
    private String tenantCode;

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

    @Column(nullable = false)
    private Instant recordedAt;

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(UUID vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
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

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }
}
