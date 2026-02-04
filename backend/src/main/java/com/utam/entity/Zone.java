package com.utam.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents a zone definition for admin zone management.
 * Different from RestrictedZone which is for security violations.
 * 
 * Supports zone types: APRON, TERMINAL, GATE, CARGO, MAINTENANCE, RESTRICTED
 */
@Entity
@Table(name = "zones", indexes = {
        @Index(name = "idx_zone_tenant_code", columnList = "tenant_code, code", unique = true),
        @Index(name = "idx_zone_tenant_type", columnList = "tenant_code, type")
})
public class Zone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_code", nullable = false, length = 10)
    private String tenantCode;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, length = 30)
    private String type;  // APRON, TERMINAL, GATE, CARGO, MAINTENANCE, RESTRICTED

    @Column(name = "geojson", columnDefinition = "TEXT")
    private String geojson;

    @Column(name = "fill_color", length = 20)
    private String fillColor;

    @Column(name = "stroke_color", length = 20)
    private String strokeColor;

    @Column(name = "fill_opacity")
    private Double fillOpacity;

    @Column(name = "polygon", columnDefinition = "TEXT")
    private String polygon;

    @Column(name = "color", length = 20)
    private String color;

    @Column(name = "opacity")
    private Double opacity;

    @Column(name = "restricted")
    private Boolean restricted = false;

    @Column
    private Boolean active = true;

    @Column(name = "allowed_vehicle_types", columnDefinition = "TEXT")
    private String allowedVehicleTypes;

    @Column(name = "allowed_roles", columnDefinition = "TEXT")
    private String allowedRoles;

    @Column(name = "access_schedule", length = 500)
    private String accessSchedule;

    @Column(name = "alert_on_entry")
    private Boolean alertOnEntry = false;

    @Column(name = "alert_on_exit")
    private Boolean alertOnExit = false;

    @Column(name = "dwell_time_alert_minutes")
    private Integer dwellTimeAlertMinutes;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTenantCode() { return tenantCode; }
    public void setTenantCode(String tenantCode) { this.tenantCode = tenantCode; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getGeojson() { return geojson; }
    public void setGeojson(String geojson) { this.geojson = geojson; }

    public String getFillColor() { return fillColor; }
    public void setFillColor(String fillColor) { this.fillColor = fillColor; }

    public String getStrokeColor() { return strokeColor; }
    public void setStrokeColor(String strokeColor) { this.strokeColor = strokeColor; }

    public Double getFillOpacity() { return fillOpacity; }
    public void setFillOpacity(Double fillOpacity) { this.fillOpacity = fillOpacity; }

    public String getPolygon() { return polygon; }
    public void setPolygon(String polygon) { this.polygon = polygon; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public Double getOpacity() { return opacity; }
    public void setOpacity(Double opacity) { this.opacity = opacity; }

    public Boolean getRestricted() { return restricted; }
    public void setRestricted(Boolean restricted) { this.restricted = restricted; }
    public boolean isRestricted() { return restricted != null && restricted; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public boolean isActive() { return active != null && active; }

    public String getAllowedVehicleTypes() { return allowedVehicleTypes; }
    public void setAllowedVehicleTypes(String allowedVehicleTypes) { this.allowedVehicleTypes = allowedVehicleTypes; }

    public String getAllowedRoles() { return allowedRoles; }
    public void setAllowedRoles(String allowedRoles) { this.allowedRoles = allowedRoles; }

    public String getAccessSchedule() { return accessSchedule; }
    public void setAccessSchedule(String accessSchedule) { this.accessSchedule = accessSchedule; }

    public Boolean getAlertOnEntry() { return alertOnEntry; }
    public void setAlertOnEntry(Boolean alertOnEntry) { this.alertOnEntry = alertOnEntry; }
    public boolean isAlertOnEntry() { return alertOnEntry != null && alertOnEntry; }

    public Boolean getAlertOnExit() { return alertOnExit; }
    public void setAlertOnExit(Boolean alertOnExit) { this.alertOnExit = alertOnExit; }
    public boolean isAlertOnExit() { return alertOnExit != null && alertOnExit; }

    public Integer getDwellTimeAlertMinutes() { return dwellTimeAlertMinutes; }
    public void setDwellTimeAlertMinutes(Integer dwellTimeAlertMinutes) { this.dwellTimeAlertMinutes = dwellTimeAlertMinutes; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
