package com.utam.turnaround.dto;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public class TurnaroundSessionDetailDTO {
    private UUID id;
    private String flightId;
    private String icaoCode;
    private String standId;
    private String status;
    private ZonedDateTime sirt;
    private ZonedDateTime eibt;
    private ZonedDateTime aibt;
    private ZonedDateTime tobt;
    private ZonedDateTime tsat;
    private ZonedDateTime aobt;
    private Integer delayMinutes;
    private String delayReason;
    private List<TaskDetailDTO> tasks;

    public TurnaroundSessionDetailDTO() {
    }

    public TurnaroundSessionDetailDTO(UUID id, String flightId, String icaoCode, String standId, String status,
            ZonedDateTime sirt, ZonedDateTime eibt, ZonedDateTime aibt,
            ZonedDateTime tobt, ZonedDateTime tsat, ZonedDateTime aobt,
            Integer delayMinutes, String delayReason,
            List<TaskDetailDTO> tasks) {
        this.id = id;
        this.flightId = flightId;
        this.icaoCode = icaoCode;
        this.standId = standId;
        this.status = status;
        this.sirt = sirt;
        this.eibt = eibt;
        this.aibt = aibt;
        this.tobt = tobt;
        this.tsat = tsat;
        this.aobt = aobt;
        this.delayMinutes = delayMinutes;
        this.delayReason = delayReason;
        this.tasks = tasks;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFlightId() {
        return flightId;
    }

    public void setFlightId(String flightId) {
        this.flightId = flightId;
    }

    public String getIcaoCode() {
        return icaoCode;
    }

    public void setIcaoCode(String icaoCode) {
        this.icaoCode = icaoCode;
    }

    public String getStandId() {
        return standId;
    }

    public void setStandId(String standId) {
        this.standId = standId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public ZonedDateTime getSirt() {
        return sirt;
    }

    public void setSirt(ZonedDateTime sirt) {
        this.sirt = sirt;
    }

    public ZonedDateTime getEibt() {
        return eibt;
    }

    public void setEibt(ZonedDateTime eibt) {
        this.eibt = eibt;
    }

    public ZonedDateTime getAibt() {
        return aibt;
    }

    public void setAibt(ZonedDateTime aibt) {
        this.aibt = aibt;
    }

    public ZonedDateTime getTobt() {
        return tobt;
    }

    public void setTobt(ZonedDateTime tobt) {
        this.tobt = tobt;
    }

    public ZonedDateTime getTsat() {
        return tsat;
    }

    public void setTsat(ZonedDateTime tsat) {
        this.tsat = tsat;
    }

    public ZonedDateTime getAobt() {
        return aobt;
    }

    public void setAobt(ZonedDateTime aobt) {
        this.aobt = aobt;
    }

    public Integer getDelayMinutes() {
        return delayMinutes;
    }

    public void setDelayMinutes(Integer delayMinutes) {
        this.delayMinutes = delayMinutes;
    }

    public String getDelayReason() {
        return delayReason;
    }

    public void setDelayReason(String delayReason) {
        this.delayReason = delayReason;
    }

    public List<TaskDetailDTO> getTasks() {
        return tasks;
    }

    public void setTasks(List<TaskDetailDTO> tasks) {
        this.tasks = tasks;
    }
}
