package com.utam.simulation.turnaround;

import jakarta.persistence.*;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Turnaround entity representing an aircraft turnaround operation.
 * Implements FR-071 to FR-080: Turnaround simulation with phases.
 */
@Entity
@Table(name = "simulation_turnarounds", indexes = {
        @Index(name = "idx_turnaround_tenant", columnList = "tenantCode"),
        @Index(name = "idx_turnaround_stand", columnList = "standCode"),
        @Index(name = "idx_turnaround_flight", columnList = "flightNumber"),
        @Index(name = "idx_turnaround_status", columnList = "status"),
        @Index(name = "idx_turnaround_start", columnList = "startTime")
})
public class Turnaround {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, length = 10)
    private String tenantCode;

    @Column(nullable = false, length = 10)
    private String flightNumber;

    @Column(nullable = false, length = 10)
    private String standCode;

    @Column(nullable = false, length = 10)
    private String aircraftType;

    @Column(nullable = false)
    private Instant startTime;

    @Column
    private Instant endTime;

    @Column(nullable = false)
    private Integer plannedDurationMinutes;

    @Column
    private Integer actualDurationMinutes;

    @Column(nullable = false, length = 20)
    private String status; // SCHEDULED, IN_PROGRESS, COMPLETED, DELAYED

    @Column(nullable = false, length = 20)
    private String currentPhase; // ARRIVAL, DEBOARDING, SERVICING, BOARDING, DEPARTURE

    @Column
    private Integer delayMinutes;

    @Column(length = 200)
    private String delayReason;

    @Column
    private Instant lastUpdated;

    // Phase timestamps
    @Column
    private Instant arrivalTime;

    @Column
    private Instant deboardingStartTime;

    @Column
    private Instant servicingStartTime;

    @Column
    private Instant boardingStartTime;

    @Column
    private Instant departureTime;

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }

    public String getStandCode() {
        return standCode;
    }

    public void setStandCode(String standCode) {
        this.standCode = standCode;
    }

    public String getAircraftType() {
        return aircraftType;
    }

    public void setAircraftType(String aircraftType) {
        this.aircraftType = aircraftType;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public Integer getPlannedDurationMinutes() {
        return plannedDurationMinutes;
    }

    public void setPlannedDurationMinutes(Integer plannedDurationMinutes) {
        this.plannedDurationMinutes = plannedDurationMinutes;
    }

    public Integer getActualDurationMinutes() {
        return actualDurationMinutes;
    }

    public void setActualDurationMinutes(Integer actualDurationMinutes) {
        this.actualDurationMinutes = actualDurationMinutes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCurrentPhase() {
        return currentPhase;
    }

    public void setCurrentPhase(String currentPhase) {
        this.currentPhase = currentPhase;
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

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Instant getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(Instant arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public Instant getDeboardingStartTime() {
        return deboardingStartTime;
    }

    public void setDeboardingStartTime(Instant deboardingStartTime) {
        this.deboardingStartTime = deboardingStartTime;
    }

    public Instant getServicingStartTime() {
        return servicingStartTime;
    }

    public void setServicingStartTime(Instant servicingStartTime) {
        this.servicingStartTime = servicingStartTime;
    }

    public Instant getBoardingStartTime() {
        return boardingStartTime;
    }

    public void setBoardingStartTime(Instant boardingStartTime) {
        this.boardingStartTime = boardingStartTime;
    }

    public Instant getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(Instant departureTime) {
        this.departureTime = departureTime;
    }

    /**
     * Calculate current delay based on actual vs planned progress.
     */
    public Duration calculateCurrentDelay() {
        if (endTime != null && startTime != null) {
            long actualMinutes = Duration.between(startTime, endTime).toMinutes();
            return Duration.ofMinutes(actualMinutes - plannedDurationMinutes);
        }
        return Duration.ZERO;
    }

    /**
     * Check if turnaround is delayed (>5 min over planned).
     */
    public boolean isDelayed() {
        return delayMinutes != null && delayMinutes > 5;
    }

    @Override
    public String toString() {
        return "Turnaround{" +
                "id=" + id +
                ", flightNumber='" + flightNumber + '\'' +
                ", standCode='" + standCode + '\'' +
                ", status='" + status + '\'' +
                ", currentPhase='" + currentPhase + '\'' +
                '}';
    }
}
