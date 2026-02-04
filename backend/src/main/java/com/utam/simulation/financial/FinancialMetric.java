package com.utam.simulation.financial;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Financial metrics entity for delay cost tracking.
 * Implements FR-102 to FR-108: Financial metrics generation.
 */
@Entity
@Table(name = "simulation_financial_metrics", indexes = {
        @Index(name = "idx_financial_tenant", columnList = "tenantCode"),
        @Index(name = "idx_financial_date", columnList = "reportDate"),
        @Index(name = "idx_financial_type", columnList = "metricType"),
        @Index(name = "idx_financial_stand", columnList = "standCode")
})
public class FinancialMetric {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, length = 10)
    private String tenantCode;

    @Column(nullable = false)
    private LocalDate reportDate;

    @Column(nullable = false, length = 30)
    private String metricType; // DELAY_COST, OPERATIONAL_COST, SLA_PENALTY, UTILIZATION

    @Column(length = 10)
    private String standCode;

    @Column(length = 20)
    private String vehicleType;

    @Column(length = 10)
    private String flightNumber;

    // Financial values
    @Column(precision = 12, scale = 2)
    private BigDecimal delayCostUsd;

    @Column(precision = 12, scale = 2)
    private BigDecimal operationalCostUsd;

    @Column(precision = 12, scale = 2)
    private BigDecimal penaltyCostUsd;

    @Column(precision = 12, scale = 2)
    private BigDecimal totalCostUsd;

    // Additional financial metrics for generator compatibility
    @Column(precision = 12, scale = 2)
    private BigDecimal totalDelayCost;

    @Column(precision = 12, scale = 2)
    private BigDecimal preventedDelayCost;

    @Column(precision = 12, scale = 2)
    private BigDecimal capacityGainValue;

    // Operational metrics
    @Column
    private Integer delayMinutes;

    @Column
    private Integer totalDelayMinutes;

    @Column
    private Integer preventedDelayMinutes;

    @Column
    private Integer turnaroundCount;

    @Column
    private Integer turnaroundsCompleted;

    @Column
    private Integer turnaroundsDelayed;

    @Column
    private Integer dispatchCount;

    @Column
    private Integer alertCount;

    @Column
    private Integer alertsGenerated;

    @Column
    private Integer alertsResolved;

    @Column(precision = 5, scale = 2)
    private BigDecimal utilizationPercent;

    @Column(precision = 5, scale = 2)
    private BigDecimal slaCompliancePercent;

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private Instant updatedAt;

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

    public LocalDate getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
    }

    // Alias for metricDate (used by generator)
    public LocalDate getMetricDate() {
        return reportDate;
    }

    public void setMetricDate(LocalDate metricDate) {
        this.reportDate = metricDate;
    }

    public String getMetricType() {
        return metricType;
    }

    public void setMetricType(String metricType) {
        this.metricType = metricType;
    }

    public String getStandCode() {
        return standCode;
    }

    public void setStandCode(String standCode) {
        this.standCode = standCode;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }

    public BigDecimal getDelayCostUsd() {
        return delayCostUsd;
    }

    public void setDelayCostUsd(BigDecimal delayCostUsd) {
        this.delayCostUsd = delayCostUsd;
    }

    public BigDecimal getOperationalCostUsd() {
        return operationalCostUsd;
    }

    public void setOperationalCostUsd(BigDecimal operationalCostUsd) {
        this.operationalCostUsd = operationalCostUsd;
    }

    public BigDecimal getPenaltyCostUsd() {
        return penaltyCostUsd;
    }

    public void setPenaltyCostUsd(BigDecimal penaltyCostUsd) {
        this.penaltyCostUsd = penaltyCostUsd;
    }

    public BigDecimal getTotalCostUsd() {
        return totalCostUsd;
    }

    public void setTotalCostUsd(BigDecimal totalCostUsd) {
        this.totalCostUsd = totalCostUsd;
    }

    public BigDecimal getTotalDelayCost() {
        return totalDelayCost;
    }

    public void setTotalDelayCost(BigDecimal totalDelayCost) {
        this.totalDelayCost = totalDelayCost;
    }

    public BigDecimal getPreventedDelayCost() {
        return preventedDelayCost;
    }

    public void setPreventedDelayCost(BigDecimal preventedDelayCost) {
        this.preventedDelayCost = preventedDelayCost;
    }

    public BigDecimal getCapacityGainValue() {
        return capacityGainValue;
    }

    public void setCapacityGainValue(BigDecimal capacityGainValue) {
        this.capacityGainValue = capacityGainValue;
    }

    public Integer getDelayMinutes() {
        return delayMinutes;
    }

    public void setDelayMinutes(Integer delayMinutes) {
        this.delayMinutes = delayMinutes;
    }

    public Integer getTotalDelayMinutes() {
        return totalDelayMinutes;
    }

    public void setTotalDelayMinutes(Integer totalDelayMinutes) {
        this.totalDelayMinutes = totalDelayMinutes;
    }

    public Integer getPreventedDelayMinutes() {
        return preventedDelayMinutes;
    }

    public void setPreventedDelayMinutes(Integer preventedDelayMinutes) {
        this.preventedDelayMinutes = preventedDelayMinutes;
    }

    public Integer getTurnaroundCount() {
        return turnaroundCount;
    }

    public void setTurnaroundCount(Integer turnaroundCount) {
        this.turnaroundCount = turnaroundCount;
    }

    public Integer getTurnaroundsCompleted() {
        return turnaroundsCompleted;
    }

    public void setTurnaroundsCompleted(Integer turnaroundsCompleted) {
        this.turnaroundsCompleted = turnaroundsCompleted;
    }

    public Integer getTurnaroundsDelayed() {
        return turnaroundsDelayed;
    }

    public void setTurnaroundsDelayed(Integer turnaroundsDelayed) {
        this.turnaroundsDelayed = turnaroundsDelayed;
    }

    public Integer getDispatchCount() {
        return dispatchCount;
    }

    public void setDispatchCount(Integer dispatchCount) {
        this.dispatchCount = dispatchCount;
    }

    public Integer getAlertCount() {
        return alertCount;
    }

    public void setAlertCount(Integer alertCount) {
        this.alertCount = alertCount;
    }

    public Integer getAlertsGenerated() {
        return alertsGenerated;
    }

    public void setAlertsGenerated(Integer alertsGenerated) {
        this.alertsGenerated = alertsGenerated;
    }

    public Integer getAlertsResolved() {
        return alertsResolved;
    }

    public void setAlertsResolved(Integer alertsResolved) {
        this.alertsResolved = alertsResolved;
    }

    public BigDecimal getUtilizationPercent() {
        return utilizationPercent;
    }

    public void setUtilizationPercent(BigDecimal utilizationPercent) {
        this.utilizationPercent = utilizationPercent;
    }

    public BigDecimal getSlaCompliancePercent() {
        return slaCompliancePercent;
    }

    public void setSlaCompliancePercent(BigDecimal slaCompliancePercent) {
        this.slaCompliancePercent = slaCompliancePercent;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Calculate total cost from components.
     */
    public void calculateTotalCost() {
        BigDecimal total = BigDecimal.ZERO;
        if (delayCostUsd != null) total = total.add(delayCostUsd);
        if (operationalCostUsd != null) total = total.add(operationalCostUsd);
        if (penaltyCostUsd != null) total = total.add(penaltyCostUsd);
        this.totalCostUsd = total;
    }

    @Override
    public String toString() {
        return "FinancialMetric{" +
                "id=" + id +
                ", tenantCode='" + tenantCode + '\'' +
                ", reportDate=" + reportDate +
                ", metricType='" + metricType + '\'' +
                ", totalCostUsd=" + totalCostUsd +
                '}';
    }
}
