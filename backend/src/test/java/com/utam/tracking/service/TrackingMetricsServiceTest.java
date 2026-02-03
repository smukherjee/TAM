package com.utam.tracking.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TrackingMetricsService.
 * Feature: 005-asset-tracking-security
 * Task: T124
 */
@DisplayName("TrackingMetricsService Tests")
class TrackingMetricsServiceTest {

    private TrackingMetricsService metricsService;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metricsService = new TrackingMetricsService(meterRegistry);
    }

    @Test
    @DisplayName("Should record positions ingested")
    void recordPositionIngested_shouldIncrementCounter() {
        // Act
        metricsService.recordPositionIngested();
        metricsService.recordPositionIngested();
        metricsService.recordPositionIngested();

        // Assert
        assertEquals(3.0, metricsService.getPositionsIngestedCount());
    }

    @Test
    @DisplayName("Should record multiple positions ingested at once")
    void recordPositionsIngested_shouldIncrementByCount() {
        // Act
        metricsService.recordPositionsIngested(10);
        metricsService.recordPositionsIngested(5);

        // Assert
        assertEquals(15.0, metricsService.getPositionsIngestedCount());
    }

    @Test
    @DisplayName("Should record zone violations with severity")
    void recordViolationDetected_shouldIncrementCounters() {
        // Act
        metricsService.recordViolationDetected("CRITICAL");
        metricsService.recordViolationDetected("CRITICAL");
        metricsService.recordViolationDetected("HIGH");
        metricsService.recordViolationDetected("MEDIUM");

        // Assert
        assertEquals(4.0, metricsService.getViolationsDetectedCount());
    }

    @Test
    @DisplayName("Should record discrepancies with severity")
    void recordDiscrepancyDetected_shouldIncrementCounters() {
        // Act
        metricsService.recordDiscrepancyDetected("CRITICAL");
        metricsService.recordDiscrepancyDetected("HIGH");
        metricsService.recordDiscrepancyDetected("MEDIUM");

        // Assert
        assertEquals(3.0, metricsService.getDiscrepanciesDetectedCount());
    }

    @Test
    @DisplayName("Should record ingestion latency")
    void recordIngestionLatency_shouldRecordToTimer() {
        // Act
        metricsService.recordIngestionLatency(100);
        metricsService.recordIngestionLatency(200);
        metricsService.recordIngestionLatency(150);

        // Assert - timer should have 3 recordings
        Timer timer = meterRegistry.find("utam_tracking_ingestion_latency").timer();
        assertNotNull(timer);
        assertEquals(3, timer.count());
    }

    @Test
    @DisplayName("Should use timer sample correctly")
    void startAndStopIngestionTimer_shouldRecordDuration() throws InterruptedException {
        // Act
        Timer.Sample sample = metricsService.startIngestionTimer();
        Thread.sleep(10); // Small delay
        metricsService.stopIngestionTimer(sample);

        // Assert
        Timer timer = meterRegistry.find("utam_tracking_ingestion_latency").timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
        assertTrue(timer.totalTime(TimeUnit.MILLISECONDS) >= 10);
    }

    @Test
    @DisplayName("Should update active assets gauge")
    void updateActiveAssetsCount_shouldSetGaugeValue() {
        // Act
        metricsService.updateActiveAssetsCount(100);

        // Assert
        assertEquals(100, metricsService.getActiveAssetsCount());
    }

    @Test
    @DisplayName("Should increment active assets")
    void incrementActiveAssets_shouldIncreaseCount() {
        // Arrange
        metricsService.updateActiveAssetsCount(50);

        // Act
        metricsService.incrementActiveAssets();
        metricsService.incrementActiveAssets();

        // Assert
        assertEquals(52, metricsService.getActiveAssetsCount());
    }

    @Test
    @DisplayName("Should decrement active assets")
    void decrementActiveAssets_shouldDecreaseCount() {
        // Arrange
        metricsService.updateActiveAssetsCount(50);

        // Act
        metricsService.decrementActiveAssets();

        // Assert
        assertEquals(49, metricsService.getActiveAssetsCount());
    }

    @Test
    @DisplayName("Should initialize all counters at zero")
    void initialState_shouldHaveZeroCounters() {
        // Assert - fresh instance
        assertEquals(0.0, metricsService.getPositionsIngestedCount());
        assertEquals(0.0, metricsService.getViolationsDetectedCount());
        assertEquals(0.0, metricsService.getDiscrepanciesDetectedCount());
        assertEquals(0, metricsService.getActiveAssetsCount());
    }

    @Test
    @DisplayName("Should handle case-insensitive severity")
    void recordViolationDetected_shouldHandleCaseInsensitiveSeverity() {
        // Act
        metricsService.recordViolationDetected("critical");
        metricsService.recordViolationDetected("CRITICAL");
        metricsService.recordViolationDetected("Critical");

        // Assert - all should count toward violations
        assertEquals(3.0, metricsService.getViolationsDetectedCount());
    }

    @Test
    @DisplayName("Should register all expected metrics")
    void constructor_shouldRegisterAllMetrics() {
        // Assert - check all metrics are registered
        assertNotNull(meterRegistry.find("utam_tracking_positions_ingested").counter());
        assertNotNull(meterRegistry.find("utam_tracking_violations_detected").counter());
        assertNotNull(meterRegistry.find("utam_tracking_discrepancies_detected").counter());
        assertNotNull(meterRegistry.find("utam_tracking_ingestion_latency").timer());
        assertNotNull(meterRegistry.find("utam_tracking_active_assets").gauge());
    }
}
