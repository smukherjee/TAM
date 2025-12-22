package com.utam.turnaround.dto;

import java.util.UUID;
import java.util.List;

public class TurnaroundSessionSummaryDTO {
    private UUID id;
    private String flightId;
    private String standId;
    private String status;
    private List<TaskSummaryDTO> tasks;

    public TurnaroundSessionSummaryDTO() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getFlightId() { return flightId; }
    public void setFlightId(String flightId) { this.flightId = flightId; }

    public String getStandId() { return standId; }
    public void setStandId(String standId) { this.standId = standId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<TaskSummaryDTO> getTasks() { return tasks; }
    public void setTasks(List<TaskSummaryDTO> tasks) { this.tasks = tasks; }
}
