package com.utam.simulation.asset;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Asset position entity for time-series GPS tracking data.
 * Implements FR-066 to FR-070: Position tracking with timestamp.
 */
@Entity
@Table(name = "simulation_asset_positions", indexes = {
        @Index(name = "idx_position_asset", columnList = "assetId"),
        @Index(name = "idx_position_tenant", columnList = "tenantCode"),
        @Index(name = "idx_position_timestamp", columnList = "timestamp")
})
public class AssetPosition {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false)
    private UUID assetId;

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

    @Column(nullable = false)
    private Double altitude;

    @Column(nullable = false)
    private Double accuracy;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false, length = 20)
    private String source;

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAssetId() {
        return assetId;
    }

    public void setAssetId(UUID assetId) {
        this.assetId = assetId;
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

    public Double getAltitude() {
        return altitude;
    }

    public void setAltitude(Double altitude) {
        this.altitude = altitude;
    }

    public Double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Double accuracy) {
        this.accuracy = accuracy;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    @Override
    public String toString() {
        return "AssetPosition{" +
                "id=" + id +
                ", assetId=" + assetId +
                ", tenantCode='" + tenantCode + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
