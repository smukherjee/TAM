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
    @JsonProperty("LivePlotId")
    @Column(name = "live_plot_id")
    private UUID livePlotId;

    @JsonProperty("Time")
    @Column(name = "time")
    private Instant time;

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

    @JsonProperty("TrackId")
    @Column(name = "track_id")
    private String trackId;

    @JsonProperty("ModeSId")
    @Column(name = "mode_s_id")
    private String modeSId;

    @JsonProperty("FlightLevel")
    @Column(name = "flight_level")
    private Double flightLevel;

    @JsonProperty("ROC")
    @Column(name = "roc")
    private Double roc;

    @JsonProperty("SSR")
    @Column(name = "ssr")
    private String ssr;

    @JsonProperty("SafetyAlert")
    @Column(name = "safety_alert")
    private Boolean safetyAlert;

    @JsonProperty("SystemStatus")
    @Column(name = "system_status")
    private String systemStatus;

    @JsonProperty("Spi")
    @Column(name = "spi")
    private Boolean spi;

    @JsonProperty("UpdateType")
    @Column(name = "update_type")
    private String updateType;

    @JsonProperty("icao_code")
    @Column(name = "icao_code")
    private String icaoCode;

    public Flight() {
    }

    public Flight(UUID livePlotId, Instant time, String callsign, Double latitude, Double longitude, Double speed,
            Double heading, Double altitude, String status, String trackId, String modeSId, Double flightLevel,
            Double roc, String ssr, Boolean safetyAlert, String systemStatus, Boolean spi, String updateType) {
        this.livePlotId = livePlotId;
        this.time = time;
        this.callsign = callsign;
        this.latitude = latitude;
        this.longitude = longitude;
        this.speed = speed;
        this.heading = heading;
        this.altitude = altitude;
        this.status = status;
        this.trackId = trackId;
        this.modeSId = modeSId;
        this.flightLevel = flightLevel;
        this.roc = roc;
        this.ssr = ssr;
        this.safetyAlert = safetyAlert;
        this.systemStatus = systemStatus;
        this.spi = spi;
        this.updateType = updateType;
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

    public String getTrackId() {
        return trackId;
    }

    public void setTrackId(String trackId) {
        this.trackId = trackId;
    }

    public String getModeSId() {
        return modeSId;
    }

    public void setModeSId(String modeSId) {
        this.modeSId = modeSId;
    }

    public Double getFlightLevel() {
        return flightLevel;
    }

    public void setFlightLevel(Double flightLevel) {
        this.flightLevel = flightLevel;
    }

    public Double getRoc() {
        return roc;
    }

    public void setRoc(Double roc) {
        this.roc = roc;
    }

    public String getSsr() {
        return ssr;
    }

    public void setSsr(String ssr) {
        this.ssr = ssr;
    }

    public Boolean getSafetyAlert() {
        return safetyAlert;
    }

    public void setSafetyAlert(Boolean safetyAlert) {
        this.safetyAlert = safetyAlert;
    }

    public String getSystemStatus() {
        return systemStatus;
    }

    public void setSystemStatus(String systemStatus) {
        this.systemStatus = systemStatus;
    }

    public Boolean getSpi() {
        return spi;
    }

    public void setSpi(Boolean spi) {
        this.spi = spi;
    }

    public String getUpdateType() {
        return updateType;
    }

    public void setUpdateType(String updateType) {
        this.updateType = updateType;
    }

    public String getIcaoCode() {
        return icaoCode;
    }

    public void setIcaoCode(String icaoCode) {
        this.icaoCode = icaoCode;
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
                ", trackId='" + trackId + '\'' +
                ", modeSId='" + modeSId + '\'' +
                ", flightLevel=" + flightLevel +
                ", roc=" + roc +
                ", ssr='" + ssr + '\'' +
                ", safetyAlert=" + safetyAlert +
                ", systemStatus='" + systemStatus + '\'' +
                ", spi=" + spi +
                ", updateType='" + updateType + '\'' +
                '}';
    }
}
