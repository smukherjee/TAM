package com.utam.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "flights")
public class Flight {
    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "tenant_code", nullable = false)
    private String tenantCode;

    @JsonProperty("FlightNumber")
    @Column(name = "flight_number", nullable = false)
    private String flightNumber;

    @JsonProperty("Time")
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @JsonProperty("CallSign")
    @Column(name = "callsign")
    private String callsign;

    @JsonProperty("Lat")
    @Column(name = "latitude")
    private Double latitude;

    @JsonProperty("Lon")
    @Column(name = "longitude")
    private Double longitude;

    @JsonProperty("Speed")
    @Column(name = "speed")
    private Double speed;

    @JsonProperty("Heading")
    @Column(name = "heading")
    private Double heading;

    @JsonProperty("Altitude")
    @Column(name = "altitude")
    private Double altitude;

    @JsonProperty("Status")
    @Column(name = "status")
    private String status;

    @Column(name = "created_at")
    private Instant createdAt;

    // Extra fields from old model removed to match DB schema
    // If needed, add them to DB schema first

    public Flight() {
    }

    public Flight(UUID id, String tenantCode, String flightNumber, Instant timestamp, String callsign, Double latitude, Double longitude, Double speed,
            Double heading, Double altitude, String status) {
        this.id = id;
        this.tenantCode = tenantCode;
        this.flightNumber = flightNumber;
        this.timestamp = timestamp;
        this.callsign = callsign;
        this.latitude = latitude;
        this.longitude = longitude;
        this.speed = speed;
        this.heading = heading;
        this.altitude = altitude;
        this.status = status;
    }

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

    public String getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getCallsign() {
        return callsign;
    }

    public void setCallsign(String callsign) {
        this.callsign = callsign;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Flight{" +
                "id=" + id +
                ", tenantCode='" + tenantCode + '\'' +
                ", flightNumber='" + flightNumber + '\'' +
                ", timestamp=" + timestamp +
                ", callsign='" + callsign + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", speed=" + speed +
                ", heading=" + heading +
                ", altitude=" + altitude +
                ", status='" + status + '\'' +
                '}';
    }
}
