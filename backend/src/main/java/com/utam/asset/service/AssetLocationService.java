package com.utam.asset.service;

import com.utam.asset.dto.AssetLocationDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

/**
 * Service for querying live asset locations from asset_location_register.
 * Provides filtered views for Universal Asset Map (US5).
 * <p>
 * Feature: 005-asset-tracking-security
 * Task: T028
 */
@Service
public class AssetLocationService {

    private static final Logger logger = LoggerFactory.getLogger(AssetLocationService.class);

    private final JdbcTemplate jdbcTemplate;

    // Category color mapping (from research.md)
    private static final Map<String, String> CATEGORY_COLORS = Map.of(
            "Emergency", "Red",
            "Fueling", "Orange",
            "Cargo", "Blue",
            "Ground Support", "Green",
            "Transport", "Purple",
            "Power", "Yellow",
            "Services", "Teal",
            "Other", "Gray"
    );

    public AssetLocationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Appends "AND column IN (?, ?, ...)" for a comma-separated multi-select filter value
     * (frontend joins selected checkboxes with commas, e.g. "Emergency,Fueling").
     */
    private void appendInClause(StringBuilder sql, String column, String commaSeparated, List<Object> params) {
        List<String> values = Arrays.stream(commaSeparated.split(","))
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .toList();
        if (values.isEmpty()) {
            return;
        }
        sql.append(" AND ").append(column).append(" IN (")
                .append(String.join(",", values.stream().map(v -> "?").toList()))
                .append(")");
        params.addAll(values);
    }

    /**
     * Appends the ground-handler filter. "Untagged" matches assets with no assigned
     * company; any other value matches that company exactly (no more implicit NULL bleed-through).
     */
    private void appendGroundHandlerClause(StringBuilder sql, String groundHandler, List<Object> params) {
        if ("Untagged".equalsIgnoreCase(groundHandler)) {
            sql.append(" AND a.company IS NULL");
        } else {
            sql.append(" AND a.company = ?");
            params.add(groundHandler);
        }
    }

    /**
     * Get all live assets with optional filters.
     * Cached for 5 seconds to reduce database load.
     *
     * @param tenantCode Filter by tenant (required)
     * @param category   Filter by asset category (optional)
     * @param status     Filter by asset status (optional)
     * @param zoneId     Filter by restricted zone UUID (optional)
     * @param groundHandler Filter by owning ground handler; pass "Untagged" to match assets with no company (optional)
     * @param limit      Max results (default 100, max 500)
     * @param offset     Pagination offset
     * @return List of asset locations
     */
    @Cacheable(value = "liveAssets", key = "#tenantCode + '_' + #category + '_' + #status + '_' + #zoneId + '_' + #groundHandler + '_' + #limit + '_' + #offset")
    public List<AssetLocationDTO> getAllLiveAssets(
            String tenantCode,
            String category,
            String status,
            UUID zoneId,
            String groundHandler,
            Integer limit,
            Integer offset) {

        logger.debug("Querying live assets: tenant={}, category={}, status={}, zone={}, groundHandler={}, limit={}, offset={}",
                tenantCode, category, status, zoneId, groundHandler, limit, offset);

        StringBuilder sql = new StringBuilder("""
                SELECT
                    a.id AS asset_id,
                    a.asset_id AS asset_identifier,
                    a.name,
                    a.category,
                    ST_Y(alr.current_location) AS latitude,
                    ST_X(alr.current_location) AS longitude,
                    a.status,
                    rz.zone_name AS current_zone,
                    rz.zone_type AS current_zone_type,
                    CASE WHEN alr.is_in_restricted_zone THEN 'IN_RESTRICTED_ZONE' ELSE 'OUTSIDE_ZONES' END AS zone_status,
                    amt.speed,
                    amt.heading,
                    alr.last_updated AS last_seen,
                    a.tenant_code,
                    a.company,
                    a.qr_id,
                    a.value,
                    a.description,
                    CASE WHEN amt.speed > 0 THEN true ELSE false END AS is_moving,
                    EXISTS(
                        SELECT 1 FROM zone_violations zv
                        WHERE zv.asset_identifier = a.asset_id
                        AND zv.acknowledged = false
                    ) AS has_violation
                FROM assets a
                JOIN asset_location_register alr ON a.asset_id = alr.asset_identifier
                LEFT JOIN LATERAL (
                    SELECT id, zone_name, zone_type
                    FROM restricted_zones
                    WHERE ST_DWithin(alr.current_location, geometry, 50)
                    ORDER BY ST_Distance(alr.current_location, geometry)
                    LIMIT 1
                ) rz ON true
                LEFT JOIN LATERAL (
                    SELECT speed, heading
                    FROM asset_movement_trail
                    WHERE asset_identifier = a.asset_id
                    ORDER BY timestamp DESC
                    LIMIT 1
                ) amt ON true
                WHERE a.tenant_code = ?
                """);

        List<Object> params = new ArrayList<>();
        params.add(tenantCode);

        if (category != null && !category.isEmpty()) {
            appendInClause(sql, "a.category", category, params);
        }

        if (status != null && !status.isEmpty()) {
            appendInClause(sql, "a.status", status, params);
        }

        if (zoneId != null) {
            sql.append(" AND rz.id = ?");
            params.add(zoneId);
        }

        if (groundHandler != null && !groundHandler.isEmpty()) {
            appendGroundHandlerClause(sql, groundHandler, params);
        }

        // Order by last_seen descending (most recent first)
        sql.append(" ORDER BY alr.last_updated DESC");

        // Apply limit and offset
        int queryLimit = (limit != null && limit > 0) ? Math.min(limit, 500) : 100;
        int queryOffset = (offset != null && offset >= 0) ? offset : 0;
        sql.append(" LIMIT ? OFFSET ?");
        params.add(queryLimit);
        params.add(queryOffset);

        return jdbcTemplate.query(sql.toString(), new AssetLocationRowMapper(), params.toArray());
    }

    /**
     * Get total count of live assets (for pagination).
     */
    public long countLiveAssets(String tenantCode, String category, String status, UUID zoneId, String groundHandler) {
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(DISTINCT a.id)
                FROM assets a
                JOIN asset_location_register alr ON a.asset_id = alr.asset_identifier
                LEFT JOIN restricted_zones rz ON ST_DWithin(alr.current_location, rz.geometry, 50)
                WHERE a.tenant_code = ?
                """);

        List<Object> params = new ArrayList<>();
        params.add(tenantCode);

        if (category != null && !category.isEmpty()) {
            appendInClause(sql, "a.category", category, params);
        }

        if (status != null && !status.isEmpty()) {
            appendInClause(sql, "a.status", status, params);
        }

        if (zoneId != null) {
            sql.append(" AND rz.id = ?");
            params.add(zoneId);
        }

        if (groundHandler != null && !groundHandler.isEmpty()) {
            appendGroundHandlerClause(sql, groundHandler, params);
        }

        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count != null ? count : 0L;
    }

    /**
     * Get single live asset by ID.
     *
     * @param assetId Asset UUID
     * @return Asset location or null if not found
     */
    @Cacheable(value = "liveAsset", key = "#assetId")
    public AssetLocationDTO getLiveAssetById(UUID assetId) {
        logger.debug("Querying live asset by ID: {}", assetId);

        String sql = """
                SELECT
                    a.asset_id,
                    a.asset_id AS asset_identifier,
                    a.name,
                    a.category,
                    ST_Y(alr.current_location) AS latitude,
                    ST_X(alr.current_location) AS longitude,
                    a.status,
                    rz.zone_name AS current_zone,
                    rz.zone_type AS current_zone_type,
                    CASE WHEN alr.is_in_restricted_zone THEN 'IN_RESTRICTED_ZONE' ELSE 'OUTSIDE_ZONES' END AS zone_status,
                    amt.speed,
                    amt.heading,
                    alr.last_updated AS last_seen,
                    a.tenant_code,
                    a.company,
                    a.qr_id,
                    a.value,
                    a.description,
                    CASE WHEN amt.speed > 0 THEN true ELSE false END AS is_moving,
                    EXISTS(
                        SELECT 1 FROM zone_violations zv
                        WHERE zv.asset_identifier = a.asset_id
                        AND zv.acknowledged = false
                    ) AS has_violation
                FROM assets a
                JOIN asset_location_register alr ON a.asset_id = alr.asset_identifier
                LEFT JOIN LATERAL (
                    SELECT id, zone_name, zone_type
                    FROM restricted_zones
                    WHERE ST_DWithin(alr.current_location, geometry, 50)
                    ORDER BY ST_Distance(alr.current_location, geometry)
                    LIMIT 1
                ) rz ON true
                LEFT JOIN LATERAL (
                    SELECT speed, heading
                    FROM asset_movement_trail
                    WHERE asset_identifier = a.asset_id
                    ORDER BY timestamp DESC
                    LIMIT 1
                ) amt ON true
                WHERE a.id = ?
                """;

        List<AssetLocationDTO> results = jdbcTemplate.query(sql, new AssetLocationRowMapper(), assetId);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Get assets currently in a specific zone.
     *
     * @param zoneId   Restricted zone UUID
     * @param tenantCode Tenant filter
     * @return List of assets in zone
     */
    public List<AssetLocationDTO> getAssetsInZone(UUID zoneId, String tenantCode) {
        logger.debug("Querying assets in zone: zoneId={}, tenant={}", zoneId, tenantCode);

        String sql = """
                SELECT
                    a.asset_id,
                    a.asset_id AS asset_identifier,
                    a.name,
                    a.category,
                    ST_Y(alr.current_location) AS latitude,
                    ST_X(alr.current_location) AS longitude,
                    a.status,
                    rz.zone_name AS current_zone,
                    rz.zone_type AS current_zone_type,
                    CASE WHEN alr.is_in_restricted_zone THEN 'IN_RESTRICTED_ZONE' ELSE 'OUTSIDE_ZONES' END AS zone_status,
                    amt.speed,
                    amt.heading,
                    alr.last_updated AS last_seen,
                    a.tenant_code,
                    a.company,
                    a.qr_id,
                    a.value,
                    a.description,
                    CASE WHEN amt.speed > 0 THEN true ELSE false END AS is_moving,
                    EXISTS(
                        SELECT 1 FROM zone_violations zv
                        WHERE zv.asset_identifier = a.asset_id
                        AND zv.acknowledged = false
                    ) AS has_violation
                FROM assets a
                JOIN asset_location_register alr ON a.asset_id = alr.asset_identifier
                JOIN restricted_zones rz ON ST_DWithin(alr.current_location, rz.geometry, 50)
                LEFT JOIN LATERAL (
                    SELECT speed, heading
                    FROM asset_movement_trail
                    WHERE asset_identifier = a.asset_id
                    ORDER BY timestamp DESC
                    LIMIT 1
                ) amt ON true
                WHERE rz.id = ? AND a.tenant_code = ?
                ORDER BY alr.last_updated DESC
                """;

        return jdbcTemplate.query(sql, new AssetLocationRowMapper(), zoneId, tenantCode);
    }

    /**
     * Get assets by category.
     *
     * @param category   Asset category
     * @param tenantCode Tenant filter
     * @return List of assets
     */
    public List<AssetLocationDTO> getAssetsByCategory(String category, String tenantCode) {
        return getAllLiveAssets(tenantCode, category, null, null, null, 500, 0);
    }

    /**
     * RowMapper for AssetLocationDTO.
     */
    private static class AssetLocationRowMapper implements RowMapper<AssetLocationDTO> {
        @Override
        public AssetLocationDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
            String category = rs.getString("category");
            String categoryColor = CATEGORY_COLORS.getOrDefault(category, "Gray");

            Timestamp lastSeenTs = rs.getTimestamp("last_seen");
            Instant lastSeen = lastSeenTs != null ? lastSeenTs.toInstant() : null;

            return AssetLocationDTO.builder()
                    .assetId(UUID.fromString(rs.getString("asset_id")))
                    .assetIdentifier(rs.getString("asset_identifier"))
                    .name(rs.getString("name"))
                    .category(category)
                    .latitude(rs.getDouble("latitude"))
                    .longitude(rs.getDouble("longitude"))
                    .status(rs.getString("status"))
                    .currentZone(rs.getString("current_zone"))
                    .currentZoneType(rs.getString("current_zone_type"))
                    .zoneStatus(rs.getString("zone_status"))
                    .speed(rs.getObject("speed") != null ? rs.getDouble("speed") : null)
                    .heading(rs.getObject("heading") != null ? rs.getDouble("heading") : null)
                    .lastSeen(lastSeen)
                    .tenantCode(rs.getString("tenant_code"))
                    .qrId(rs.getString("qr_id"))
                    .value(rs.getObject("value") != null ? rs.getDouble("value") : null)
                    .description(rs.getString("description"))
                    .isMoving(rs.getBoolean("is_moving"))
                    .hasViolation(rs.getBoolean("has_violation"))
                    .categoryColor(categoryColor)
                    .owner(rs.getString("company") != null ? rs.getString("company") : rs.getString("tenant_code"))
                    .build();
        }
    }
}
