package com.utam.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents a GSE vehicle storage/dispatch location (depot).
 * Examples: Fuel Farm, Baggage Hall, Catering Facility, Maintenance Hangar
 */
@Entity
@Table(name = "depots", indexes = {
        @Index(name = "idx_depot_tenant_type", columnList = "tenant_code, depot_type")
})
public class Depot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_code", nullable = false, length = 10)
    private String tenantCode;

    @Column(name = "depot_type", nullable = false, length = 30)
    private String depotType;  // BAGGAGE, FUEL, CATERING, MAINTENANCE, GPU, PUSHBACK, etc.

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(nullable = false)
    private Integer capacity = 10;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Depot() {
    }

    public Depot(String tenantCode, String depotType, String name, Double latitude, Double longitude, Integer capacity) {
        this.tenantCode = tenantCode;
        this.depotType = depotType;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.capacity = capacity;
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTenantCode() { return tenantCode; }
    public void setTenantCode(String tenantCode) { this.tenantCode = tenantCode; }

    public String getDepotType() { return depotType; }
    public void setDepotType(String depotType) { this.depotType = depotType; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Depot{" +
                "id=" + id +
                ", tenantCode='" + tenantCode + '\'' +
                ", depotType='" + depotType + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
