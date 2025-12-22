package com.utam.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vehicles")
@Data
public class Vehicle {
    @Id
    private UUID id;

    @Column(name = "tenant_code", nullable = false)
    private String tenantCode;

    @JsonProperty("gpsactualtime")
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @JsonProperty("vehicle_no")
    @Column(name = "vehicle_id", nullable = false)
    private String vehicleId;

    @JsonProperty("vehicletype")
    @Column(name = "vehicle_type")
    private String vehicleType;

    @JsonProperty("latitude")
    @Column(name = "latitude")
    private Double latitude;

    @JsonProperty("longitude")
    @Column(name = "longitude")
    private Double longitude;

    @JsonProperty("speed")
    @Column(name = "speed")
    private Double speed;

    @JsonProperty("status")
    @Column(name = "status")
    private String status;

    @JsonProperty("vehicle_name")
    @Column(name = "vehicle_name")
    private String vehicleName;

    @Column(name = "created_at")
    private Instant createdAt;
}
