package com.utam.asset.service;

import com.utam.asset.dto.HeatmapDataDTO;
import com.utam.asset.dto.HotspotDetailDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for querying heatmap data from materialized views.
 * Provides activity density, violation density, and dwell time heatmaps.
 * <p>
 * Feature: 005-asset-tracking-security (US6)
 * Task: T033
 */
@Service
public class HeatmapService {

    private static final Logger logger = LoggerFactory.getLogger(HeatmapService.class);

    private final JdbcTemplate jdbcTemplate;

    // Grid size mapping (from research.md)
    private static final Map<String, Double> GRID_SIZES = Map.of(
            "10m", 0.0001,
            "25m", 0.00025,
            "50m", 0.0005,
            "100m", 0.001
    );

    public HeatmapService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Get activity density heatmap.
     * Queries asset_activity_heatmap materialized view.
     *
     * @param tenantCode Tenant filter
     * @param startDate  Start date (ISO 8601)
     * @param endDate    End date (ISO 8601)
     * @param gridSize   Grid resolution (10m, 25m, 50m, 100m)
     * @return List of heatmap cells with normalized intensity
     */
    @Cacheable(value = "activityHeatmap", key = "#tenantCode + '_' + #startDate + '_' + #endDate + '_' + #gridSize")
    public List<HeatmapDataDTO> getActivityHeatmap(
            String tenantCode,
            String startDate,
            String endDate,
            String gridSize) {

        logger.info("Querying activity heatmap: tenant={}, start={}, end={}, grid={}",
                tenantCode, startDate, endDate, gridSize);

        Double gridResolution = GRID_SIZES.getOrDefault(gridSize, 0.0001);

        // Query materialized view with optional resampling
        String sql = """
                SELECT
                    ST_Y(snapped_location) AS latitude,
                    ST_X(snapped_location) AS longitude,
                    SUM(activity_count) AS activity_count,
                    SUM(unique_assets) AS unique_assets,
                    AVG(avg_speed) AS avg_speed,
                    MAX(max_speed) AS max_speed,
                    MIN(first_activity) AS first_activity,
                    MAX(last_activity) AS last_activity
                FROM (
                    SELECT
                        ST_SnapToGrid(grid_location, ?) AS snapped_location,
                        activity_count,
                        unique_assets,
                        avg_speed,
                        max_speed,
                        first_activity,
                        last_activity
                    FROM asset_activity_heatmap
                    WHERE tenant_code = ?
                    AND time_bucket >= ?::timestamp
                    AND time_bucket <= ?::timestamp
                ) subquery
                GROUP BY snapped_location
                HAVING SUM(activity_count) > 0
                ORDER BY SUM(activity_count) DESC
                """;

        List<HeatmapDataDTO> rawData = jdbcTemplate.query(
                sql,
                this::mapActivityHeatmapRow,
                gridResolution, tenantCode, startDate, endDate
        );

        // Normalize intensity values (percentile-based)
        return normalizeIntensity(rawData);
    }

    /**
     * Get violation density heatmap.
     * Queries violation_heatmap materialized view.
     *
     * @param tenantCode Tenant filter
     * @param startDate  Start date (ISO 8601)
     * @param endDate    End date (ISO 8601)
     * @param gridSize   Grid resolution
     * @return List of heatmap cells
     */
    @Cacheable(value = "violationHeatmap", key = "#tenantCode + '_' + #startDate + '_' + #endDate + '_' + #gridSize")
    public List<HeatmapDataDTO> getViolationHeatmap(
            String tenantCode,
            String startDate,
            String endDate,
            String gridSize) {

        logger.info("Querying violation heatmap: tenant={}, start={}, end={}, grid={}",
                tenantCode, startDate, endDate, gridSize);

        Double gridResolution = GRID_SIZES.getOrDefault(gridSize, 0.0001);

        String sql = """
                SELECT
                    ST_Y(snapped_location) AS latitude,
                    ST_X(snapped_location) AS longitude,
                    SUM(violation_count) AS violation_count,
                    SUM(unique_violators) AS unique_assets,
                    AVG(avg_speed) AS avg_speed,
                    MAX(max_speed) AS max_speed,
                    MIN(first_violation) AS first_activity,
                    MAX(last_violation) AS last_activity
                FROM (
                    SELECT
                        ST_SnapToGrid(grid_location, ?) AS snapped_location,
                        violation_count,
                        unique_violators,
                        avg_speed,
                        max_speed,
                        first_violation,
                        last_violation
                    FROM asset_violation_heatmap
                    WHERE tenant_code = ?
                    AND time_bucket >= ?::timestamp
                    AND time_bucket <= ?::timestamp
                ) subquery
                GROUP BY snapped_location
                HAVING SUM(violation_count) > 0
                ORDER BY SUM(violation_count) DESC
                """;

        List<HeatmapDataDTO> rawData = jdbcTemplate.query(
                sql,
                this::mapViolationHeatmapRow,
                gridResolution, tenantCode, startDate, endDate
        );

        return normalizeIntensity(rawData);
    }

    /**
     * Get dwell time heatmap (future enhancement - placeholder).
     * Would require additional materialized view for dwell time calculation.
     */
    public List<HeatmapDataDTO> getDwellHeatmap(
            String tenantCode,
            String startDate,
            String endDate,
            String gridSize) {

        logger.warn("Dwell heatmap not yet implemented - returning empty list");
        return Collections.emptyList();
    }

    /**
     * Get hotspot details for a clicked grid cell.
     * Provides drill-down information for modal/popup.
     *
     * @param latitude  Grid cell latitude
     * @param longitude Grid cell longitude
     * @param mode      Heatmap mode (activity, violations, dwell)
     * @param tenantCode Tenant filter
     * @param startDate Start date
     * @param endDate   End date
     * @param gridSize  Grid resolution
     * @return Hotspot details
     */
    public HotspotDetailDTO getHotspotDetails(
            Double latitude,
            Double longitude,
            String mode,
            String tenantCode,
            String startDate,
            String endDate,
            String gridSize) {

        logger.info("Querying hotspot details: lat={}, lng={}, mode={}, tenant={}",
                latitude, longitude, mode, tenantCode);

        Double gridResolution = GRID_SIZES.getOrDefault(gridSize, 0.0001);

        if ("activity".equalsIgnoreCase(mode)) {
            return getActivityHotspotDetails(latitude, longitude, tenantCode, startDate, endDate, gridResolution);
        } else if ("violations".equalsIgnoreCase(mode)) {
            return getViolationHotspotDetails(latitude, longitude, tenantCode, startDate, endDate, gridResolution);
        } else {
            return HotspotDetailDTO.builder()
                    .mode(mode)
                    .location(HotspotDetailDTO.Location.builder()
                            .latitude(latitude)
                            .longitude(longitude)
                            .gridSize(gridResolution)
                            .build())
                    .build();
        }
    }

    /**
     * Get activity hotspot details.
     */
    private HotspotDetailDTO getActivityHotspotDetails(
            Double latitude,
            Double longitude,
            String tenantCode,
            String startDate,
            String endDate,
            Double gridResolution) {

        String sql = """
                SELECT
                    SUM(activity_count) AS activity_count,
                    SUM(unique_assets) AS unique_assets,
                    ARRAY_AGG(DISTINCT asset_identifier) FILTER (WHERE asset_identifier IS NOT NULL) AS assets,
                    EXTRACT(HOUR FROM time_bucket)::integer AS hour,
                    SUM(activity_count) AS hour_count
                FROM asset_movement_trail amt
                WHERE tenant_code = ?
                AND ST_DWithin(location, ST_SetSRID(ST_MakePoint(?, ?), 4326), ?)
                AND timestamp >= ?::timestamp
                AND timestamp <= ?::timestamp
                GROUP BY EXTRACT(HOUR FROM time_bucket)
                ORDER BY hour
                """;

        // Calculate grid tolerance (half grid size in meters, approx)
        double tolerance = gridResolution * 111000 / 2; // degrees to meters

        Map<Integer, Long> timeDistribution = new HashMap<>();
        List<String> assets = new ArrayList<>();
        long totalActivity = 0;

        jdbcTemplate.query(
                sql,
                rs -> {
                    int hour = rs.getInt("hour");
                    long count = rs.getLong("hour_count");
                    timeDistribution.put(hour, count);

                    // Get assets from first row only
                    if (assets.isEmpty()) {
                        Object[] assetsArray = (Object[]) rs.getArray("assets").getArray();
                        for (Object asset : assetsArray) {
                            if (asset != null) {
                                assets.add(asset.toString());
                            }
                        }
                    }
                },
                tenantCode, longitude, latitude, tolerance, startDate, endDate
        );

        // Sum total activity
        totalActivity = timeDistribution.values().stream().mapToLong(Long::longValue).sum();

        return HotspotDetailDTO.builder()
                .location(HotspotDetailDTO.Location.builder()
                        .latitude(latitude)
                        .longitude(longitude)
                        .gridSize(gridResolution)
                        .build())
                .mode("activity")
                .activityCount(totalActivity)
                .uniqueAssets(assets.size())
                .assets(assets)
                .timeDistribution(timeDistribution)
                .dateRange(HotspotDetailDTO.DateRange.builder()
                        .startDate(startDate)
                        .endDate(endDate)
                        .build())
                .build();
    }

    /**
     * Get violation hotspot details.
     */
    private HotspotDetailDTO getViolationHotspotDetails(
            Double latitude,
            Double longitude,
            String tenantCode,
            String startDate,
            String endDate,
            Double gridResolution) {

        String sql = """
                SELECT
                    SUM(violation_count) AS violation_count,
                    SUM(critical_count) AS critical_count,
                    SUM(high_count) AS high_count,
                    SUM(medium_count) AS medium_count,
                    SUM(low_count) AS low_count,
                    ARRAY_AGG(DISTINCT violating_assets) AS assets_array
                FROM violation_heatmap
                WHERE tenant_code = ?
                AND ST_DWithin(grid_location, ST_SetSRID(ST_MakePoint(?, ?), 4326), ?)
                AND time_bucket >= ?::timestamp
                AND time_bucket <= ?::timestamp
                """;

        double tolerance = gridResolution * 111000 / 2;

        HotspotDetailDTO.ViolationDetails[] violationDetails = new HotspotDetailDTO.ViolationDetails[1];
        List<String> allAssets = new ArrayList<>();

        jdbcTemplate.query(
                sql,
                rs -> {
                    violationDetails[0] = HotspotDetailDTO.ViolationDetails.builder()
                            .totalViolations(rs.getLong("violation_count"))
                            .criticalCount(rs.getLong("critical_count"))
                            .highCount(rs.getLong("high_count"))
                            .mediumCount(rs.getLong("medium_count"))
                            .lowCount(rs.getLong("low_count"))
                            .build();

                    // Extract unique assets from array
                    Object[] assetsArray = (Object[]) rs.getArray("assets_array").getArray();
                    for (Object assetListObj : assetsArray) {
                        if (assetListObj != null) {
                            String[] assetList = (String[]) ((java.sql.Array) assetListObj).getArray();
                            allAssets.addAll(Arrays.asList(assetList));
                        }
                    }
                },
                tenantCode, longitude, latitude, tolerance, startDate, endDate
        );

        List<String> uniqueAssets = allAssets.stream().distinct().collect(Collectors.toList());

        return HotspotDetailDTO.builder()
                .location(HotspotDetailDTO.Location.builder()
                        .latitude(latitude)
                        .longitude(longitude)
                        .gridSize(gridResolution)
                        .build())
                .mode("violations")
                .activityCount(violationDetails[0] != null ? violationDetails[0].getTotalViolations() : 0L)
                .uniqueAssets(uniqueAssets.size())
                .assets(uniqueAssets)
                .violations(violationDetails[0])
                .dateRange(HotspotDetailDTO.DateRange.builder()
                        .startDate(startDate)
                        .endDate(endDate)
                        .build())
                .build();
    }

    /**
     * Normalize intensity values to 0-1 scale using percentile-based approach.
     */
    private List<HeatmapDataDTO> normalizeIntensity(List<HeatmapDataDTO> data) {
        if (data.isEmpty()) {
            return data;
        }

        // Extract activity counts
        List<Long> counts = data.stream()
                .map(d -> d.getMetadata().getActivityCount())
                .sorted()
                .collect(Collectors.toList());

        // Calculate 95th percentile (to avoid outlier skewing)
        int p95Index = (int) (counts.size() * 0.95);
        Long maxValue = counts.get(Math.min(p95Index, counts.size() - 1));

        if (maxValue == 0) {
            maxValue = 1L; // Avoid division by zero
        }

        // Normalize
        Long finalMaxValue = maxValue;
        data.forEach(cell -> {
            double normalized = Math.min(1.0, cell.getMetadata().getActivityCount().doubleValue() / finalMaxValue);
            cell.setIntensity(normalized);
        });

        return data;
    }

    /**
     * Row mapper for activity heatmap.
     */
    private HeatmapDataDTO mapActivityHeatmapRow(ResultSet rs, int rowNum) throws SQLException {
        return HeatmapDataDTO.builder()
                .latitude(rs.getDouble("latitude"))
                .longitude(rs.getDouble("longitude"))
                .metadata(HeatmapDataDTO.HeatmapMetadata.builder()
                        .activityCount(rs.getLong("activity_count"))
                        .uniqueAssets(rs.getInt("unique_assets"))
                        .avgSpeed(rs.getObject("avg_speed") != null ? rs.getDouble("avg_speed") : null)
                        .maxSpeed(rs.getObject("max_speed") != null ? rs.getDouble("max_speed") : null)
                        .firstActivity(rs.getTimestamp("first_activity") != null ?
                                rs.getTimestamp("first_activity").toInstant().toString() : null)
                        .lastActivity(rs.getTimestamp("last_activity") != null ?
                                rs.getTimestamp("last_activity").toInstant().toString() : null)
                        .build())
                .build();
    }

    /**
     * Row mapper for violation heatmap.
     */
    private HeatmapDataDTO mapViolationHeatmapRow(ResultSet rs, int rowNum) throws SQLException {
        return HeatmapDataDTO.builder()
                .latitude(rs.getDouble("latitude"))
                .longitude(rs.getDouble("longitude"))
                .metadata(HeatmapDataDTO.HeatmapMetadata.builder()
                        .activityCount(rs.getLong("violation_count"))
                        .uniqueAssets(rs.getInt("unique_assets"))
                        .avgSpeed(rs.getDouble("avg_speed"))
                        .maxSpeed(rs.getDouble("max_speed"))
                        .firstActivity(rs.getTimestamp("first_activity") != null ?
                                rs.getTimestamp("first_activity").toInstant().toString() : null)
                        .lastActivity(rs.getTimestamp("last_activity") != null ?
                                rs.getTimestamp("last_activity").toInstant().toString() : null)
                        .build())
                .build();
    }
}
