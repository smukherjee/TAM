package com.utam.turnaround.domain;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "turnaround_tasks")
public class TurnaroundTask {
    @Id
    private UUID id;

    @Column(name = "tenant_code", nullable = false)
    private String tenantCode;

    @ManyToOne
    @JoinColumn(name = "session_id")
    private TurnaroundSession session;

    @Column(name = "task_type", nullable = false)
    private String taskType;

    @Column(nullable = false)
    private String status;

    @Column(name = "planned_start")
    private ZonedDateTime plannedStart;

    @Column(name = "planned_end")
    private ZonedDateTime plannedEnd;

    @Column(name = "actual_start")
    private ZonedDateTime actualStart;

    @Column(name = "actual_end")
    private ZonedDateTime actualEnd;

    public TurnaroundTask() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTenantCode() { return tenantCode; }
    public void setTenantCode(String tenantCode) { this.tenantCode = tenantCode; }

    public TurnaroundSession getSession() { return session; }
    public void setSession(TurnaroundSession session) { this.session = session; }

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
