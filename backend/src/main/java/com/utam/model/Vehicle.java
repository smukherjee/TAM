package com.utam.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    private String vehicleNo;
    private String type;
    private Double latitude;
    private Double longitude;
    private Double speed;
    private Double altitude;
    private String status;
    private LocalDateTime timestamp;

    public Vehicle() {
    }

    public Vehicle(String vehicleNo, String type, Double latitude, Double longitude, Double speed, Double altitude, String status, LocalDateTime timestamp) {
        this.vehicleNo = vehicleNo;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
        this.speed = speed;
        this.altitude = altitude;
        this.status = status;
        this.timestamp = timestamp;
    }

    public String getVehicleNo() {
        return vehicleNo;
    }

    public void setVehicleNo(String vehicleNo) {
        this.vehicleNo = vehicleNo;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Vehicle{" +
                "vehicleNo='" + vehicleNo + '\'' +
                ", type='" + type + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", speed=" + speed +
                ", altitude=" + altitude +
                ", status='" + status + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
