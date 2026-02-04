package com.utam.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Tracks key timestamps within a turnaround session.
 * Implements FR-052: Track 30+ turnaround sub-processes with milestones.
 */
@Entity
@Table(name = "turnaround_milestones", indexes = {
        @Index(name = "idx_milestone_turnaround", columnList = "turnaroundId"),
        @Index(name = "idx_milestone_type", columnList = "milestoneType"),
        @Index(name = "idx_milestone_sequence", columnList = "turnaroundId, sequenceOrder")
})
public class TurnaroundMilestone {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false)
    private UUID turnaroundId;

    @Column(nullable = false, length = 50)
    private String milestoneType;

    @Column(nullable = false)
    private Instant plannedTime;

    @Column
    private Instant actualTime;

    @Column(nullable = false)
    private Integer sequenceOrder;

    @Column
    private Integer varianceMinutes;

    @Column(length = 200)
    private String notes;

    @Column(nullable = false, length = 10)
    private String tenantCode;

    @Column(nullable = false)
    private Instant createdAt;

    // Milestone type constants
    public static final String CHOCKS_ON = "CHOCKS_ON";
    public static final String DOOR_OPEN = "DOOR_OPEN";
    public static final String JETBRIDGE_CONNECTED = "JETBRIDGE_CONNECTED";
    public static final String PAX_DEBOARD_START = "PAX_DEBOARD_START";
    public static final String PAX_DEBOARD_COMPLETE = "PAX_DEBOARD_COMPLETE";
    public static final String BAGGAGE_UNLOAD_START = "BAGGAGE_UNLOAD_START";
    public static final String BAGGAGE_UNLOAD_COMPLETE = "BAGGAGE_UNLOAD_COMPLETE";
    public static final String CATERING_START = "CATERING_START";
    public static final String CATERING_COMPLETE = "CATERING_COMPLETE";
    public static final String FUEL_START = "FUEL_START";
    public static final String FUEL_COMPLETE = "FUEL_COMPLETE";
    public static final String CLEANING_START = "CLEANING_START";
    public static final String CLEANING_COMPLETE = "CLEANING_COMPLETE";
    public static final String WATER_START = "WATER_START";
    public static final String WATER_COMPLETE = "WATER_COMPLETE";
    public static final String LAVATORY_START = "LAVATORY_START";
    public static final String LAVATORY_COMPLETE = "LAVATORY_COMPLETE";
    public static final String CARGO_LOAD_START = "CARGO_LOAD_START";
    public static final String CARGO_LOAD_COMPLETE = "CARGO_LOAD_COMPLETE";
    public static final String BAGGAGE_LOAD_START = "BAGGAGE_LOAD_START";
    public static final String BAGGAGE_LOAD_COMPLETE = "BAGGAGE_LOAD_COMPLETE";
    public static final String PAX_BOARD_START = "PAX_BOARD_START";
    public static final String PAX_BOARD_COMPLETE = "PAX_BOARD_COMPLETE";
    public static final String DOOR_CLOSE = "DOOR_CLOSE";
    public static final String JETBRIDGE_DISCONNECTED = "JETBRIDGE_DISCONNECTED";
    public static final String PUSHBACK_START = "PUSHBACK_START";
    public static final String CHOCKS_OFF = "CHOCKS_OFF";

    public TurnaroundMilestone() {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
    }

    public TurnaroundMilestone(UUID turnaroundId, String milestoneType, Instant plannedTime, 
                               int sequenceOrder, String tenantCode) {
        this();
        this.turnaroundId = turnaroundId;
        this.milestoneType = milestoneType;
        this.plannedTime = plannedTime;
        this.sequenceOrder = sequenceOrder;
        this.tenantCode = tenantCode;
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTurnaroundId() {
        return turnaroundId;
    }

    public void setTurnaroundId(UUID turnaroundId) {
        this.turnaroundId = turnaroundId;
    }

    public String getMilestoneType() {
        return milestoneType;
    }

    public void setMilestoneType(String milestoneType) {
        this.milestoneType = milestoneType;
    }

    public Instant getPlannedTime() {
        return plannedTime;
    }

    public void setPlannedTime(Instant plannedTime) {
        this.plannedTime = plannedTime;
    }

    public Instant getActualTime() {
        return actualTime;
    }

    public void setActualTime(Instant actualTime) {
        this.actualTime = actualTime;
        if (this.plannedTime != null && actualTime != null) {
            this.varianceMinutes = (int) java.time.Duration.between(this.plannedTime, actualTime).toMinutes();
        }
    }

    public Integer getSequenceOrder() {
        return sequenceOrder;
    }

    public void setSequenceOrder(Integer sequenceOrder) {
        this.sequenceOrder = sequenceOrder;
    }

    public Integer getVarianceMinutes() {
        return varianceMinutes;
    }

    public void setVarianceMinutes(Integer varianceMinutes) {
        this.varianceMinutes = varianceMinutes;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isCompleted() {
        return actualTime != null;
    }

    public boolean isDelayed() {
        return varianceMinutes != null && varianceMinutes > 0;
    }
}
