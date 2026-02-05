package com.utam.simulation.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * Service for managing simulation data - clearing and refreshing.
 * Provides tenant-aware data operations for the admin UI.
 */
@Service
public class SimulationDataService {

    private static final Logger log = LoggerFactory.getLogger(SimulationDataService.class);

    private final JdbcTemplate jdbcTemplate;

    // Tables that contain simulation data (in order for FK constraints)
    private static final List<String> SIMULATION_TABLES = Arrays.asList(
            "simulation_violations",
            "simulation_alerts",
            "simulation_dispatches",
            "simulation_discrepancies",
            "simulation_financial_metrics",
            "simulation_turnarounds",
            "simulation_asset_positions",
            "simulation_assets",
            "simulation_vehicles"
    );

    // Core tables that may have simulation-generated data
    private static final List<String> CORE_DATA_TABLES = Arrays.asList(
            "zone_violations",
            "asset_movement_trail",
            "asset_location_register"
    );

    // Materialized views to refresh
    private static final List<String> MATERIALIZED_VIEWS = Arrays.asList(
            "asset_activity_heatmap",
            "violation_heatmap"
    );

    public SimulationDataService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Clear simulation data for a specific tenant.
     * 
     * @param tenantCode The ICAO code of the tenant (e.g., VIDP, LIRN, YBBN)
     * @return Total number of rows deleted
     */
    @Transactional
    public int clearDataForTenant(String tenantCode) {
        log.info("Clearing simulation data for tenant: {}", tenantCode);
        int totalDeleted = 0;

        // Clear simulation tables
        for (String table : SIMULATION_TABLES) {
            try {
                int deleted = clearTableForTenant(table, tenantCode);
                totalDeleted += deleted;
                log.debug("Deleted {} rows from {} for tenant {}", deleted, table, tenantCode);
            } catch (Exception e) {
                log.warn("Could not clear table {} for tenant {}: {}", table, tenantCode, e.getMessage());
            }
        }

        // Clear core data tables
        for (String table : CORE_DATA_TABLES) {
            try {
                int deleted = clearTableForTenant(table, tenantCode);
                totalDeleted += deleted;
                log.debug("Deleted {} rows from {} for tenant {}", deleted, table, tenantCode);
            } catch (Exception e) {
                log.warn("Could not clear table {} for tenant {}: {}", table, tenantCode, e.getMessage());
            }
        }

        log.info("Cleared {} total rows for tenant {}", totalDeleted, tenantCode);
        return totalDeleted;
    }

    /**
     * Clear all simulation data across all tenants.
     * 
     * @return Total number of rows deleted
     */
    @Transactional
    public int clearAllData() {
        log.info("Clearing all simulation data");
        int totalDeleted = 0;

        // Clear simulation tables
        for (String table : SIMULATION_TABLES) {
            try {
                int deleted = clearTable(table);
                totalDeleted += deleted;
                log.debug("Deleted {} rows from {}", deleted, table);
            } catch (Exception e) {
                log.warn("Could not clear table {}: {}", table, e.getMessage());
            }
        }

        // Clear core data tables
        for (String table : CORE_DATA_TABLES) {
            try {
                int deleted = clearTable(table);
                totalDeleted += deleted;
                log.debug("Deleted {} rows from {}", deleted, table);
            } catch (Exception e) {
                log.warn("Could not clear table {}: {}", table, e.getMessage());
            }
        }

        log.info("Cleared {} total rows from all tables", totalDeleted);
        return totalDeleted;
    }

    /**
     * Refresh all materialized views.
     * Should be called after data generation to update heatmaps.
     */
    public void refreshMaterializedViews() {
        log.info("Refreshing {} materialized views", MATERIALIZED_VIEWS.size());

        for (String view : MATERIALIZED_VIEWS) {
            try {
                long startTime = System.currentTimeMillis();
                jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY " + view);
                long duration = System.currentTimeMillis() - startTime;
                log.info("Refreshed materialized view {} in {}ms", view, duration);
            } catch (Exception e) {
                // Try non-concurrent refresh if concurrent fails
                try {
                    log.warn("Concurrent refresh failed for {}, trying non-concurrent: {}", view, e.getMessage());
                    jdbcTemplate.execute("REFRESH MATERIALIZED VIEW " + view);
                    log.info("Refreshed materialized view {} (non-concurrent)", view);
                } catch (Exception e2) {
                    log.error("Failed to refresh materialized view {}: {}", view, e2.getMessage());
                }
            }
        }
    }

    /**
     * Clear a specific table for a tenant.
     */
    private int clearTableForTenant(String tableName, String tenantCode) {
        String sql = String.format("DELETE FROM %s WHERE tenant_code = ?", tableName);
        return jdbcTemplate.update(sql, tenantCode);
    }

    /**
     * Clear all data from a table.
     */
    private int clearTable(String tableName) {
        String sql = String.format("DELETE FROM %s", tableName);
        return jdbcTemplate.update(sql);
    }

    /**
     * Get counts of simulation data per tenant.
     * 
     * @return Map of tenant code to record count
     */
    public java.util.Map<String, Integer> getDataCountsByTenant() {
        java.util.Map<String, Integer> counts = new java.util.HashMap<>();
        
        String sql = """
            SELECT tenant_code, COUNT(*) as count 
            FROM simulation_vehicles 
            GROUP BY tenant_code
            """;
        
        try {
            jdbcTemplate.query(sql, rs -> {
                counts.put(rs.getString("tenant_code"), rs.getInt("count"));
            });
        } catch (Exception e) {
            log.warn("Could not get data counts: {}", e.getMessage());
        }
        
        return counts;
    }
}
