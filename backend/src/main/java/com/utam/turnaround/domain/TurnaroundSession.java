package com.utam.turnaround.domain;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.List;

@Entity
@Table(name = "turnaround_sessions")
public class TurnaroundSession {
    @Id
    private UUID id;

    @Column(name = "tenant_code", nullable = false)
    private String tenantCode;

    @Column(name = "flight_id", nullable = false)
    private String flightId;

    @Column(name = "stand_id", nullable = false)
    private String standId;

    private ZonedDateTime sirt;
    private ZonedDateTime eibt;
    private ZonedDateTime aibt;
    private ZonedDateTime tobt;
    private ZonedDateTime tsat;
    private ZonedDateTime aobt;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at")
    private ZonedDateTime createdAt;

    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL)
    private List<TurnaroundTask> tasks;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL)
    private List<Alert> alerts;

    public TurnaroundSession() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTenantCode() { return tenantCode; }
    public void setTenantCode(String tenantCode) { this.tenantCode = tenantCode; }

    public String getFlightId() { return flightId; }
    public void setFlightId(String flightId) { this.flightId = flightId; }

    public String getStandId() { return standId; }
    public void setStandId(String standId) { this.standId = standId; }

    public ZonedDateTime getSirt() { return sirt; }
    public void setSirt(ZonedDateTime sirt) { this.sirt = sirt; }

    public ZonedDateTime getEibt() { return eibt; }
    public void setEibt(ZonedDateTime eibt) { this.eibt = eibt; }

    public ZonedDateTime getAibt() { return aibt; }
    public void setAibt(ZonedDateTime aibt) { this.aibt = aibt; }

    public ZonedDateTime getTobt() { return tobt; }
    public void setTobt(ZonedDateTime tobt) { this.tobt = tobt; }

    public ZonedDateTime getTsat() { return tsat; }
    public void setTsat(ZonedDateTime tsat) { this.tsat = tsat; }

    public ZonedDateTime getAobt() { return aobt; }
    public void setAobt(ZonedDateTime aobt) { this.aobt = aobt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }

    public ZonedDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(ZonedDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<TurnaroundTask> getTasks() { return tasks; }
    public void setTasks(List<TurnaroundTask> tasks) { this.tasks = tasks; }

    public List<Alert> getAlerts() { return alerts; }
    public void setAlerts(List<Alert> alerts) { this.alerts = alerts; }
}
