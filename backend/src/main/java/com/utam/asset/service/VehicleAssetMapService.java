package com.utam.asset.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class VehicleAssetMapService {

    private static final Logger logger = LoggerFactory.getLogger(VehicleAssetMapService.class);

    private final JdbcTemplate jdbcTemplate;

    public VehicleAssetMapService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<AssetInfo> findByVehicle(String vehicleId, String tenantCode) {
        try {
            String sql = """
                    SELECT a.id, a.asset_id AS identifier, a.name, a.category
                    FROM vehicle_asset_map v
                    JOIN assets a ON v.asset_id = a.id
                    WHERE v.vehicle_id = ? AND v.tenant_code = ?
                    LIMIT 1
                    """;

            return jdbcTemplate.query(sql, rs -> {
                if (rs.next()) {
                    return Optional.of(new AssetInfo(
                            UUID.fromString(rs.getString("id")),
                            rs.getString("identifier"),
                            rs.getString("name"),
                            rs.getString("category")
                    ));
                }
                return Optional.empty();
            }, vehicleId, tenantCode);
        } catch (Exception e) {
            logger.error("Failed to fetch mapping for vehicle {} (tenant {}): {}", vehicleId, tenantCode, e.getMessage());
            return Optional.empty();
        }
    }

    public record AssetInfo(UUID id, String identifier, String name, String category) { }
}
