package com.utam.turnaround.dto;

import java.time.ZonedDateTime;
import java.util.UUID;

public class TaskDetailDTO {
    private UUID id;
    private String taskType;
    private String status;
    private ZonedDateTime plannedStart;
    private ZonedDateTime plannedEnd;
    private ZonedDateTime actualStart;
    private ZonedDateTime actualEnd;

    public TaskDetailDTO() {}

    public TaskDetailDTO(UUID id, String taskType, String status, ZonedDateTime plannedStart, ZonedDateTime plannedEnd, ZonedDateTime actualStart, ZonedDateTime actualEnd) {
        this.id = id;
        this.taskType = taskType;
        this.status = status;
        this.plannedStart = plannedStart;
        this.plannedEnd = plannedEnd;
        this.actualStart = actualStart;
        this.actualEnd = actualEnd;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public ZonedDateTime getPlannedStart() { return plannedStart; }
    public void setPlannedStart(ZonedDateTime plannedStart) { this.plannedStart = plannedStart; }

    public ZonedDateTime getPlannedEnd() { return plannedEnd; }
    public void setPlannedEnd(ZonedDateTime plannedEnd) { this.plannedEnd = plannedEnd; }

    public ZonedDateTime getActualStart() { return actualStart; }
    public void setActualStart(ZonedDateTime actualStart) { this.actualStart = actualStart; }

    public ZonedDateTime getActualEnd() { return actualEnd; }
    public void setActualEnd(ZonedDateTime actualEnd) { this.actualEnd = actualEnd; }
}
