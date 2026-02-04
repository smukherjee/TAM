package com.utam.simulation.asset;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Asset entity representing a trackable GSE equipment.
 * Implements FR-060 to FR-065: Asset modeling for GPS tracking.
 */
@Entity(name = "SimulationAsset")
@Table(name = "simulation_assets", indexes = {
        @Index(name = "idx_asset_tenant", columnList = "tenantCode"),
        @Index(name = "idx_asset_type", columnList = "assetType"),
        @Index(name = "idx_asset_status", columnList = "status"),
        @Index(name = "idx_asset_last_seen", columnList = "lastSeen")
})
public class Asset {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, length = 10)
    private String tenantCode;

    @Column(nullable = false, unique = true, length = 30)
    private String assetId;

    @Column(nullable = false, length = 100)
    private String assetName;

    @Column(nullable = false, length = 20)
    private String assetType;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false, length = 20)
    private String status;

    @Column
    private Instant lastSeen;

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

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Instant lastSeen) {
        this.lastSeen = lastSeen;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Asset{" +
                "id=" + id +
                ", assetId='" + assetId + '\'' +
                ", tenantCode='" + tenantCode + '\'' +
                ", assetType='" + assetType + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
