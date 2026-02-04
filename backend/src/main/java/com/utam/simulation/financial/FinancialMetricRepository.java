package com.utam.simulation.financial;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for FinancialMetric entity.
 */
@Repository
public interface FinancialMetricRepository extends JpaRepository<FinancialMetric, UUID> {

    List<FinancialMetric> findByTenantCode(String tenantCode);

    List<FinancialMetric> findByTenantCodeAndReportDate(String tenantCode, LocalDate date);

    // Alias for findByTenantCodeAndReportDate (used by generator as metricDate)
    default Optional<FinancialMetric> findByTenantCodeAndMetricDate(String tenantCode, LocalDate date) {
        List<FinancialMetric> results = findByTenantCodeAndReportDate(tenantCode, date);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    // Alias for date range query (used by generator as metricDate)
    default List<FinancialMetric> findByTenantCodeAndMetricDateBetween(String tenantCode, LocalDate startDate, LocalDate endDate) {
        return findByTenantCodeAndDateRange(tenantCode, startDate, endDate);
    }

    List<FinancialMetric> findByTenantCodeAndMetricType(String tenantCode, String metricType);

    @Query("SELECT f FROM FinancialMetric f WHERE f.tenantCode = :tenantCode AND f.reportDate BETWEEN :startDate AND :endDate ORDER BY f.reportDate")
    List<FinancialMetric> findByTenantCodeAndDateRange(String tenantCode, LocalDate startDate, LocalDate endDate);

    @Query("SELECT SUM(f.totalCostUsd) FROM FinancialMetric f WHERE f.tenantCode = :tenantCode AND f.reportDate = :date")
    BigDecimal getTotalCostForDate(String tenantCode, LocalDate date);

    @Query("SELECT SUM(f.delayCostUsd) FROM FinancialMetric f WHERE f.tenantCode = :tenantCode AND f.reportDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalDelayCostForPeriod(String tenantCode, LocalDate startDate, LocalDate endDate);

    @Query("SELECT SUM(f.delayMinutes) FROM FinancialMetric f WHERE f.tenantCode = :tenantCode AND f.reportDate = :date")
    Integer getTotalDelayMinutesForDate(String tenantCode, LocalDate date);

    @Query("SELECT AVG(f.utilizationPercent) FROM FinancialMetric f WHERE f.tenantCode = :tenantCode AND f.reportDate BETWEEN :startDate AND :endDate")
    BigDecimal getAverageUtilizationForPeriod(String tenantCode, LocalDate startDate, LocalDate endDate);

    @Query("SELECT AVG(f.slaCompliancePercent) FROM FinancialMetric f WHERE f.tenantCode = :tenantCode AND f.reportDate BETWEEN :startDate AND :endDate")
    BigDecimal getAverageSlaComplianceForPeriod(String tenantCode, LocalDate startDate, LocalDate endDate);

    @Query("SELECT COUNT(f) FROM FinancialMetric f WHERE f.tenantCode = :tenantCode")
    long countByTenantCode(String tenantCode);

    void deleteByTenantCode(String tenantCode);

    void deleteByReportDateBefore(LocalDate date);
}
