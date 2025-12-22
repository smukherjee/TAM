package com.utam.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sensor_alerts")
public class VehicleAlert {

    @Id
    @Column(name = "alert_id")
    private UUID alertId;

    @Column(name = "type")
    private String type;

    @Column(name = "entity_id")
    private String entityId;

    @Column(name = "value")
    private Double value;

    @Column(name = "timestamp")
    private Instant timestamp;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "tenant_code")
    private String tenantCode;

    public VehicleAlert() {
    }

    public VehicleAlert(UUID alertId, String type, String entityId, Double value, Instant timestamp, Double latitude, Double longitude) {
        this.alertId = alertId;
        this.type = type;
        this.entityId = entityId;
        this.value = value;
        this.timestamp = timestamp;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public UUID getAlertId() {
        return alertId;
    }

    public void setAlertId(UUID alertId) {
        this.alertId = alertId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
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

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    @Override
    public String toString() {
        return "VehicleAlert{" +
                "alertId=" + alertId +
                ", type='" + type + '\'' +
                ", entityId='" + entityId + '\'' +
                ", value=" + value +
                ", timestamp=" + timestamp +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }
}
