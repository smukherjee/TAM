package com.utam.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents an aircraft parking position (stand/gate) at an airport terminal.
 */
@Entity
@Table(name = "stands", indexes = {
        @Index(name = "idx_stand_tenant_stand", columnList = "tenant_code, stand_id", unique = true),
        @Index(name = "idx_stand_tenant_terminal", columnList = "tenant_code, terminal_id")
})
public class Stand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_code", nullable = false, length = 10)
    private String tenantCode;

    @Column(name = "stand_id", nullable = false, length = 10)
    private String standId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "terminal_id", length = 10)
    private String terminalId;

    @Column(name = "stand_type", length = 20)
    private String standType;  // CONTACT, REMOTE, PUSHBACK

    @Column(name = "apron", length = 50)
    private String apron;  // Apron 1, Apron 2, North Remote, etc.

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Stand() {
    }

    public Stand(String tenantCode, String standId, String name, Double latitude, Double longitude) {
        this.tenantCode = tenantCode;
        this.standId = standId;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.active = true;
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
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTenantCode() { return tenantCode; }
    public void setTenantCode(String tenantCode) { this.tenantCode = tenantCode; }

    public String getStandId() { return standId; }
    public void setStandId(String standId) { this.standId = standId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getTerminalId() { return terminalId; }
    public void setTerminalId(String terminalId) { this.terminalId = terminalId; }

    public String getStandType() { return standType; }
    public void setStandType(String standType) { this.standType = standType; }

    public String getApron() { return apron; }
    public void setApron(String apron) { this.apron = apron; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "Stand{" +
                "id=" + id +
                ", tenantCode='" + tenantCode + '\'' +
                ", standId='" + standId + '\'' +
                ", name='" + name + '\'' +
                ", terminalId='" + terminalId + '\'' +
                '}';
    }
}
