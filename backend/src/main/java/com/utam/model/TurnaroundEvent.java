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
    @Column(name = "event_unique_id")
    private String eventUniqueId;

    @Column(name = "camera_id")
    private String cameraId;

    @Column(name = "camera_name")
    private String cameraName;

    @Column(name = "activity_type")
    private String activityType;

    @Column(name = "event_type")
    private Integer eventType;

    @Column(name = "event_time_stamp")
    private LocalDateTime eventTimeStamp;

    @Column(name = "stand")
    private String stand;

    @Column(name = "icao_code")
    @JsonProperty("icao_code")
    private String icaoCode;

    @JsonProperty("creation_timestamp")
    @jakarta.persistence.Transient
    private Long creationTimestamp;

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

    public Long getCreationTimestamp() {
        return creationTimestamp;
    }

    public void setCreationTimestamp(Long creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }
}
