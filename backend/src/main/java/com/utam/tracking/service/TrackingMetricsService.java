package com.utam.tracking.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for tracking monitoring metrics using Micrometer.
 * Exposes Prometheus metrics for the asset tracking module.
 * 
 * Feature: 005-asset-tracking-security
 * Task: T048
 * 
 * Metrics exposed:
 * - utam_tracking_positions_ingested_total: Total positions ingested
 * - utam_tracking_violations_detected_total: Total zone violations detected
 * - utam_tracking_discrepancies_detected_total: Total movement discrepancies detected
 * - utam_tracking_ingestion_latency_seconds: Ingestion processing latency
 * - utam_tracking_active_assets: Current number of active assets
 * - utam_tracking_api_requests_total: API request counts by endpoint
 */
@Service
public class TrackingMetricsService {

    private final Counter positionsIngestedCounter;
    private final Counter violationsDetectedCounter;
    private final Counter discrepanciesDetectedCounter;
    private final Counter violationsCriticalCounter;
    private final Counter violationsHighCounter;
    private final Counter discrepanciesCriticalCounter;
    private final Timer ingestionLatencyTimer;
    private final AtomicLong activeAssetsGauge;
    private final AtomicLong lastIngestionTimestamp;

    public TrackingMetricsService(MeterRegistry meterRegistry) {
        // Position ingestion counter
        this.positionsIngestedCounter = Counter.builder("utam_tracking_positions_ingested")
                .description("Total number of asset positions ingested")
                .tag("module", "tracking")
                .register(meterRegistry);

        // Violation counters by severity
        this.violationsDetectedCounter = Counter.builder("utam_tracking_violations_detected")
                .description("Total number of zone violations detected")
                .tag("module", "tracking")
                .register(meterRegistry);

        this.violationsCriticalCounter = Counter.builder("utam_tracking_violations_by_severity")
                .description("Zone violations by severity level")
                .tag("module", "tracking")
                .tag("severity", "CRITICAL")
                .register(meterRegistry);

        this.violationsHighCounter = Counter.builder("utam_tracking_violations_by_severity")
                .description("Zone violations by severity level")
                .tag("module", "tracking")
                .tag("severity", "HIGH")
                .register(meterRegistry);

        // Discrepancy counters
        this.discrepanciesDetectedCounter = Counter.builder("utam_tracking_discrepancies_detected")
                .description("Total number of movement discrepancies detected")
                .tag("module", "tracking")
                .register(meterRegistry);

        this.discrepanciesCriticalCounter = Counter.builder("utam_tracking_discrepancies_by_severity")
                .description("Movement discrepancies by severity level")
                .tag("module", "tracking")
                .tag("severity", "CRITICAL")
                .register(meterRegistry);

        // Ingestion latency timer
        this.ingestionLatencyTimer = Timer.builder("utam_tracking_ingestion_latency")
                .description("Time taken to process position ingestion")
                .tag("module", "tracking")
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .register(meterRegistry);

        // Active assets gauge
        this.activeAssetsGauge = new AtomicLong(0);
        Gauge.builder("utam_tracking_active_assets", activeAssetsGauge, AtomicLong::get)
                .description("Current number of active (tracked) assets")
                .tag("module", "tracking")
                .register(meterRegistry);

        // Last ingestion timestamp gauge
        this.lastIngestionTimestamp = new AtomicLong(0);
        Gauge.builder("utam_tracking_last_ingestion_timestamp", lastIngestionTimestamp, AtomicLong::get)
                .description("Unix timestamp of last successful ingestion")
                .tag("module", "tracking")
                .register(meterRegistry);

        // Create API endpoint counters
        createApiCounters(meterRegistry);
    }

    private void createApiCounters(MeterRegistry meterRegistry) {
        // These will be incremented by controllers
        Counter.builder("utam_tracking_api_requests")
                .description("API request counts")
                .tag("module", "tracking")
                .tag("endpoint", "violations")
                .tag("method", "GET")
                .register(meterRegistry);

        Counter.builder("utam_tracking_api_requests")
                .description("API request counts")
                .tag("module", "tracking")
                .tag("endpoint", "discrepancies")
                .tag("method", "GET")
                .register(meterRegistry);

        Counter.builder("utam_tracking_api_requests")
                .description("API request counts")
                .tag("module", "tracking")
                .tag("endpoint", "trail")
                .tag("method", "GET")
                .register(meterRegistry);

        Counter.builder("utam_tracking_api_requests")
                .description("API request counts")
                .tag("module", "tracking")
                .tag("endpoint", "heatmap")
                .tag("method", "GET")
                .register(meterRegistry);
    }

    /**
     * Record a position ingested.
     */
    public void recordPositionIngested() {
        positionsIngestedCounter.increment();
        lastIngestionTimestamp.set(System.currentTimeMillis());
    }

    /**
     * Record multiple positions ingested.
     */
    public void recordPositionsIngested(int count) {
        positionsIngestedCounter.increment(count);
        lastIngestionTimestamp.set(System.currentTimeMillis());
    }

    /**
     * Record a zone violation detected.
     */
    public void recordViolationDetected(String severity) {
        violationsDetectedCounter.increment();
        if ("CRITICAL".equalsIgnoreCase(severity)) {
            violationsCriticalCounter.increment();
        } else if ("HIGH".equalsIgnoreCase(severity)) {
            violationsHighCounter.increment();
        }
    }

    /**
     * Record a movement discrepancy detected.
     */
    public void recordDiscrepancyDetected(String severity) {
        discrepanciesDetectedCounter.increment();
        if ("CRITICAL".equalsIgnoreCase(severity)) {
            discrepanciesCriticalCounter.increment();
        }
    }

    /**
     * Record ingestion latency.
     */
    public void recordIngestionLatency(long latencyMs) {
        ingestionLatencyTimer.record(latencyMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Record ingestion latency using Timer.Sample.
     */
    public Timer.Sample startIngestionTimer() {
        return Timer.start();
    }

    /**
     * Stop the timer and record the sample.
     */
    public void stopIngestionTimer(Timer.Sample sample) {
        sample.stop(ingestionLatencyTimer);
    }

    /**
     * Update active assets count.
     */
    public void updateActiveAssetsCount(long count) {
        activeAssetsGauge.set(count);
    }

    /**
     * Increment active assets count.
     */
    public void incrementActiveAssets() {
        activeAssetsGauge.incrementAndGet();
    }

    /**
     * Decrement active assets count.
     */
    public void decrementActiveAssets() {
        activeAssetsGauge.decrementAndGet();
    }

    /**
     * Get current positions ingested count.
     */
    public double getPositionsIngestedCount() {
        return positionsIngestedCounter.count();
    }

    /**
     * Get current violations detected count.
     */
    public double getViolationsDetectedCount() {
        return violationsDetectedCounter.count();
    }

    /**
     * Get current discrepancies detected count.
     */
    public double getDiscrepanciesDetectedCount() {
        return discrepanciesDetectedCounter.count();
    }

    /**
     * Get current active assets count.
     */
    public long getActiveAssetsCount() {
        return activeAssetsGauge.get();
    }
}
