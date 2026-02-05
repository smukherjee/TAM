package com.utam.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Stores airport perimeter polygons for boundary validation.
 * Used to ensure all ground vehicle and asset positions stay within airport
 * boundaries.
 */
@Entity
@Table(name = "airport_boundaries", indexes = {
        @Index(name = "idx_airport_boundary_tenant", columnList = "tenant_code", unique = true)
})
public class AirportBoundary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_code", nullable = false, unique = true, length = 10)
    private String tenantCode;

    @Column(nullable = false, length = 100)
    private String name;

    /**
     * PostGIS geometry representation of the boundary polygon.
     * Maps to the actual database column 'boundary_polygon'.
     */
    @Column(name = "boundary_polygon", columnDefinition = "geometry(Polygon,4326)")
    private String boundaryPolygon;

    /**
     * Bounding box for quick containment checks.
     */
    @Column(name = "min_latitude")
    private Double minLatitude;

    @Column(name = "max_latitude")
    private Double maxLatitude;

    @Column(name = "min_longitude")
    private Double minLongitude;

    @Column(name = "max_longitude")
    private Double maxLongitude;

    /**
     * Additional properties as JSON.
     */
    @Column(columnDefinition = "TEXT")
    private String properties;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public AirportBoundary() {
    }

    public AirportBoundary(String tenantCode, String name, String boundaryPolygon) {
        this.tenantCode = tenantCode;
        this.name = name;
        this.boundaryPolygon = boundaryPolygon;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
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

    public String getBoundaryPolygon() {
        return boundaryPolygon;
    }

    public void setBoundaryPolygon(String boundaryPolygon) {
        this.boundaryPolygon = boundaryPolygon;
    }

    public Double getMinLatitude() {
        return minLatitude;
    }

    public void setMinLatitude(Double minLatitude) {
        this.minLatitude = minLatitude;
    }

    public Double getMaxLatitude() {
        return maxLatitude;
    }

    public void setMaxLatitude(Double maxLatitude) {
        this.maxLatitude = maxLatitude;
    }

    public Double getMinLongitude() {
        return minLongitude;
    }

    public void setMinLongitude(Double minLongitude) {
        this.minLongitude = minLongitude;
    }

    public Double getMaxLongitude() {
        return maxLongitude;
    }

    public void setMaxLongitude(Double maxLongitude) {
        this.maxLongitude = maxLongitude;
    }

    public String getProperties() {
        return properties;
    }

    public void setProperties(String properties) {
        this.properties = properties;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Quick bounding box check if a point is potentially within the boundary.
     */
    public boolean isWithinBoundingBox(double latitude, double longitude) {
        if (minLatitude == null || maxLatitude == null || minLongitude == null || maxLongitude == null) {
            return true; // No bounding box defined, assume within bounds
        }
        return latitude >= minLatitude && latitude <= maxLatitude
                && longitude >= minLongitude && longitude <= maxLongitude;
    }

    @Override
    public String toString() {
        return "AirportBoundary{" +
                "id=" + id +
                ", tenantCode='" + tenantCode + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
