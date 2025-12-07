package com.utam.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CvEventDto {
    @JsonProperty("cameraId")
    private String cameraId;

    @JsonProperty("cameraName")
    private String cameraName;

    @JsonProperty("activityType")
    private String activityType;

    @JsonProperty("eventUniqueId")
    private String eventUniqueId;

    @JsonProperty("eventType")
    private Integer eventType;

    @JsonProperty("eventTimeStamp")
    private String eventTimeStamp;

    @JsonProperty("stand")
    private String stand;

    // Getters and Setters
    public String getCameraId() { return cameraId; }
    public void setCameraId(String cameraId) { this.cameraId = cameraId; }

    public String getCameraName() { return cameraName; }
    public void setCameraName(String cameraName) { this.cameraName = cameraName; }

    public String getActivityType() { return activityType; }
    public void setActivityType(String activityType) { this.activityType = activityType; }

    public String getEventUniqueId() { return eventUniqueId; }
    public void setEventUniqueId(String eventUniqueId) { this.eventUniqueId = eventUniqueId; }

    public Integer getEventType() { return eventType; }
    public void setEventType(Integer eventType) { this.eventType = eventType; }

    public String getEventTimeStamp() { return eventTimeStamp; }
    public void setEventTimeStamp(String eventTimeStamp) { this.eventTimeStamp = eventTimeStamp; }

    public String getStand() { return stand; }
    public void setStand(String stand) { this.stand = stand; }
}
