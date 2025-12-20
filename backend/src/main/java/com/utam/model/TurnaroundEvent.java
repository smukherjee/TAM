package com.utam.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "turnaround_events")
public class TurnaroundEvent {

    @Id
    private String eventUniqueId;

    private String cameraId;
    private String cameraName;
    private String activityType;
    private Integer eventType;
    private LocalDateTime eventTimeStamp;
    private String stand;

    @Column(name = "icao_code")
    @JsonProperty("icao_code")
    private String icaoCode;

    // Getters and Setters

    public String getIcaoCode() {
        return icaoCode;
    }

    public void setIcaoCode(String icaoCode) {
        this.icaoCode = icaoCode;
    }

    public String getEventUniqueId() {
        return eventUniqueId;
    }

    public void setEventUniqueId(String eventUniqueId) {
        this.eventUniqueId = eventUniqueId;
    }

    public String getCameraId() {
        return cameraId;
    }

    public void setCameraId(String cameraId) {
        this.cameraId = cameraId;
    }

    public String getCameraName() {
        return cameraName;
    }

    public void setCameraName(String cameraName) {
        this.cameraName = cameraName;
    }

    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public Integer getEventType() {
        return eventType;
    }

    public void setEventType(Integer eventType) {
        this.eventType = eventType;
    }

    public LocalDateTime getEventTimeStamp() {
        return eventTimeStamp;
    }

    public void setEventTimeStamp(LocalDateTime eventTimeStamp) {
        this.eventTimeStamp = eventTimeStamp;
    }

    public String getStand() {
        return stand;
    }

    public void setStand(String stand) {
        this.stand = stand;
    }
}
