package com.utam.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "flights")
public class Flight {
    @Id
    private UUID livePlotId;
    private Instant time;
    private String callsign;
    private Double latitude;
    private Double longitude;
    private Double speed;
    private Double heading;
    private Double altitude;
    private String status;

    public Flight() {
    }

    public Flight(UUID livePlotId, Instant time, String callsign, Double latitude, Double longitude, Double speed, Double heading, Double altitude, String status) {
        this.livePlotId = livePlotId;
        this.time = time;
        this.callsign = callsign;
        this.latitude = latitude;
        this.longitude = longitude;
        this.speed = speed;
        this.heading = heading;
        this.altitude = altitude;
        this.status = status;
    }

    public UUID getLivePlotId() {
        return livePlotId;
    }

    public void setLivePlotId(UUID livePlotId) {
        this.livePlotId = livePlotId;
    }

    public Instant getTime() {
        return time;
    }

    public void setTime(Instant time) {
        this.time = time;
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

    @Override
    public String toString() {
        return "Flight{" +
                "livePlotId=" + livePlotId +
                ", time=" + time +
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
