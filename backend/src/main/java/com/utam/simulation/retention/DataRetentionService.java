package com.utam.simulation.retention;

import com.utam.simulation.config.SimulationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * Service for cleaning up old simulation data based on retention policy.
 * Default retention is 90 days, configurable via simulation.retention-days.
 * 
 * Runs daily at 2:00 AM by default.
 * Cleans up data from:
 * - flights
 * - vehicles
 * - sensor_alerts (vehicle alerts)
 * - turnaround_events
 * - asset_movement_trail
 * - asset_location_register
 */
@Service
public class DataRetentionService {

    private static final Logger log = LoggerFactory.getLogger(DataRetentionService.class);

    private final SimulationConfig config;
    private final JdbcTemplate jdbcTemplate;

    // Tables and their timestamp columns for cleanup
    private static final Map<String, String> RETENTION_TABLES = Map.of(
            "flights", "timestamp",
            "vehicles", "timestamp",
            "sensor_alerts", "alert_time",
            "turnaround_events", "event_time",
            "asset_movement_trail", "timestamp",
            "asset_location_register", "last_updated"
    );

    public DataRetentionService(SimulationConfig config, JdbcTemplate jdbcTemplate) {
        this.config = config;
        this.jdbcTemplate = jdbcTemplate;
        log.info("DataRetentionService initialized with {} days retention", config.getRetentionDays());
    }

    /**
     * Scheduled cleanup job - runs daily at 2:00 AM.
     */
    @Scheduled(cron = "${simulation.retention-cron:0 0 2 * * *}")
    public void scheduledCleanup() {
        log.info("Starting scheduled data retention cleanup");
        RetentionResult result = performCleanup();
        log.info("Data retention cleanup complete: {}", result);
    }

    /**
     * Manually trigger data cleanup.
     */
    @Transactional
    public RetentionResult performCleanup() {
        int retentionDays = config.getRetentionDays();
        Instant cutoffTime = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        
        log.info("Cleaning up data older than {} days (before {})", retentionDays, cutoffTime);

        long totalDeleted = 0;
        java.util.Map<String, Long> deletedByTable = new java.util.HashMap<>();

        for (Map.Entry<String, String> entry : RETENTION_TABLES.entrySet()) {
            String tableName = entry.getKey();
            String timestampColumn = entry.getValue();

            try {
                long deleted = cleanupTable(tableName, timestampColumn, cutoffTime);
                deletedByTable.put(tableName, deleted);
                totalDeleted += deleted;
                
                if (deleted > 0) {
                    log.info("Deleted {} records from {} (older than {})", deleted, tableName, cutoffTime);
                }
            } catch (Exception e) {
                log.error("Error cleaning up table {}: {}", tableName, e.getMessage());
                deletedByTable.put(tableName, -1L); // Indicate error
            }
        }

        return new RetentionResult(
                retentionDays,
                cutoffTime,
                totalDeleted,
                deletedByTable
        );
    }

    /**
     * Clean up a single table.
     */
    private long cleanupTable(String tableName, String timestampColumn, Instant cutoffTime) {
        // Check if table exists first
        String checkSql = "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = ?)";
        Boolean exists = jdbcTemplate.queryForObject(checkSql, Boolean.class, tableName);
        
        if (Boolean.FALSE.equals(exists)) {
            log.debug("Table {} does not exist, skipping", tableName);
            return 0;
        }

        String deleteSql = String.format(
                "DELETE FROM %s WHERE %s < ?",
                tableName, timestampColumn
        );

        return jdbcTemplate.update(deleteSql, java.sql.Timestamp.from(cutoffTime));
    }

    /**
     * Get the current retention configuration.
     */
    public int getRetentionDays() {
        return config.getRetentionDays();
    }

    /**
     * Get the cutoff date for retention.
     */
    public Instant getCutoffDate() {
        return Instant.now().minus(config.getRetentionDays(), ChronoUnit.DAYS);
    }

    /**
     * Result of a retention cleanup operation.
     */
    public record RetentionResult(
            int retentionDays,
            Instant cutoffTime,
            long totalDeleted,
            Map<String, Long> deletedByTable
    ) {}
}
