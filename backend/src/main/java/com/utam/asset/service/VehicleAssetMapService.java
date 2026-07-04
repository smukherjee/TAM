package com.utam.asset.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class VehicleAssetMapService {

    private static final Logger logger = LoggerFactory.getLogger(VehicleAssetMapService.class);

    private final JdbcTemplate jdbcTemplate;

    // Vehicle type to asset category mapping (from
    // create_vehicle_asset_mapping.sql)
    private static final Map<String, String> TYPE_TO_CATEGORY = Map.ofEntries(
            Map.entry("ASU", "Power"),
            Map.entry("Ambulift", "Transport"),
            Map.entry("AMBULIFT", "Transport"),
            Map.entry("Baggage Cart", "Baggage"),
            Map.entry("BAGGAGE", "Baggage"),
            Map.entry("Baggage Loader", "Baggage"),
            Map.entry("Baggage Tug", "Baggage"),
            Map.entry("BAG", "Baggage"),
            Map.entry("Belt Loader", "Cargo"),
            Map.entry("BELT", "Cargo"),
            Map.entry("Bus", "Transport"),
            Map.entry("BUS", "Transport"),
            Map.entry("PAX", "Transport"),
            Map.entry("Cargo Loader", "Cargo"),
            Map.entry("CARGO", "Cargo"),
            Map.entry("Catering Truck", "Catering"),
            Map.entry("CAT", "Catering"),
            Map.entry("CATERING", "Catering"),
            Map.entry("De-icing Truck", "De-icing"),
            Map.entry("DEICE", "De-icing"),
            Map.entry("DEICING", "De-icing"),
            Map.entry("Fuel Truck", "Fueling"),
            Map.entry("FUEL", "Fueling"),
            Map.entry("HYDR", "Fueling"),
            Map.entry("GPU", "Power"),
            Map.entry("Lavatory Truck", "Services"),
            Map.entry("LAV", "Services"),
            Map.entry("LAVATORY", "Services"),
            Map.entry("Pushback", "Ground Support"),
            Map.entry("PB", "Ground Support"),
            Map.entry("PUSHBACK", "Ground Support"),
            Map.entry("TWB", "Ground Support"),
            Map.entry("Stairs", "Passenger"),
            Map.entry("STRS", "Passenger"),
            Map.entry("Tug", "Ground Support"),
            Map.entry("Water Truck", "Services"),
            Map.entry("WATER", "Services"),
            Map.entry("EMERGENCY", "Emergency"));

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
                            rs.getString("category")));
                }
                return Optional.empty();
            }, vehicleId, tenantCode);
        } catch (Exception e) {
            logger.error("Failed to fetch mapping for vehicle {} (tenant {}): {}", vehicleId, tenantCode,
                    e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Get asset category from vehicle type code or name.
     * Mirrors the logic from create_vehicle_asset_mapping.sql
     */
    public String getAssetCategory(String vehicleType) {
        return TYPE_TO_CATEGORY.getOrDefault(vehicleType, "Other");
    }

    /**
     * Create or update asset entry for a vehicle and create the mapping.
     * This ensures every vehicle has a corresponding asset entry.
     * 
     * @param vehicleId   The vehicle identifier (e.g., "VIDP-FT-001")
     * @param vehicleName The human-readable vehicle name
     * @param vehicleType The vehicle type code or name (e.g., "FUEL", "Fuel Truck")
     * @param tenantCode  The tenant code (e.g., "VIDP")
     * @param company     The owning ground handler (nullable = untagged asset)
     * @return The asset info if created/found successfully
     */
    @Transactional
    public Optional<AssetInfo> createOrUpdateAssetForVehicle(String vehicleId, String vehicleName,
            String vehicleType, String tenantCode, String company) {
        try {
            // First check if mapping already exists
            Optional<AssetInfo> existing = findByVehicle(vehicleId, tenantCode);
            if (existing.isPresent()) {
                logger.debug("Asset mapping already exists for vehicle {} (tenant {})", vehicleId, tenantCode);
                return existing;
            }

            String category = getAssetCategory(vehicleType);
            String assetName = (vehicleName != null && !vehicleName.isBlank())
                    ? vehicleName
                    : vehicleType + " - " + vehicleId;
            String description = "Auto-generated from vehicle tracking data. Type: " + vehicleType;

            // Check if asset already exists (without mapping)
            String checkAssetSql = """
                    SELECT id, asset_id, name, category FROM assets
                    WHERE asset_id = ? AND tenant_code = ?
                    """;

            UUID assetUuid = jdbcTemplate.query(checkAssetSql, rs -> {
                if (rs.next()) {
                    return UUID.fromString(rs.getString("id"));
                }
                return null;
            }, vehicleId, tenantCode);

            // Create asset if it doesn't exist
            if (assetUuid == null) {
                assetUuid = UUID.randomUUID();
                String insertAssetSql = """
                        INSERT INTO assets (id, asset_id, name, category, status, tenant_code, company, description, created_at)
                        VALUES (?::uuid, ?, ?, ?, 'Available', ?, ?, ?, now())
                        ON CONFLICT (asset_id, tenant_code) DO UPDATE
                        SET name = EXCLUDED.name, category = EXCLUDED.category, company = EXCLUDED.company, description = EXCLUDED.description
                        """;
                jdbcTemplate.update(insertAssetSql, assetUuid.toString(), vehicleId, assetName,
                        category, tenantCode, company, description);
                logger.debug("Created asset {} for vehicle {} (tenant {})", assetUuid, vehicleId, tenantCode);
            }

            // Create the vehicle-asset mapping
            String insertMapSql = """
                    INSERT INTO vehicle_asset_map (id, tenant_code, vehicle_id, asset_id, created_at)
                    VALUES (?::uuid, ?, ?, ?::uuid, now())
                    ON CONFLICT (tenant_code, vehicle_id) DO NOTHING
                    """;
            jdbcTemplate.update(insertMapSql, UUID.randomUUID().toString(), tenantCode, vehicleId,
                    assetUuid.toString());
            logger.debug("Created vehicle-asset mapping for vehicle {} -> asset {} (tenant {})",
                    vehicleId, assetUuid, tenantCode);

            return Optional.of(new AssetInfo(assetUuid, vehicleId, assetName, category));

        } catch (Exception e) {
            logger.error("Failed to create asset for vehicle {} (tenant {}): {}", vehicleId, tenantCode,
                    e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Sync all vehicles to assets and create mappings.
     * This is useful for bulk operations after vehicle generation.
     * 
     * @param tenantCode The tenant code to sync, or null for all tenants
     * @return Number of mappings created
     */
    @Transactional
    public int syncVehiclesToAssets(String tenantCode) {
        try {
            // First ensure all vehicles have corresponding assets
            // Use DISTINCT ON to handle duplicate vehicle_ids in the vehicles table
            String insertAssetsSql = """
                    INSERT INTO assets (id, asset_id, name, category, status, tenant_code, description, created_at)
                    SELECT
                        uuid_generate_v4() AS id,
                        v.vehicle_id AS asset_id,
                        COALESCE(NULLIF(v.vehicle_name, ''), v.vehicle_type || ' - ' || v.vehicle_id) AS name,
                        CASE v.vehicle_type
                            WHEN 'ASU' THEN 'Power'
                            WHEN 'Ambulift' THEN 'Transport'
                            WHEN 'Baggage Cart' THEN 'Baggage'
                            WHEN 'Baggage Loader' THEN 'Baggage'
                            WHEN 'Baggage Tug' THEN 'Baggage'
                            WHEN 'Belt Loader' THEN 'Cargo'
                            WHEN 'Bus' THEN 'Transport'
                            WHEN 'Cargo Loader' THEN 'Cargo'
                            WHEN 'Catering Truck' THEN 'Catering'
                            WHEN 'De-icing Truck' THEN 'De-icing'
                            WHEN 'Fuel Truck' THEN 'Fueling'
                            WHEN 'GPU' THEN 'Power'
                            WHEN 'Lavatory Truck' THEN 'Services'
                            WHEN 'Pushback' THEN 'Ground Support'
                            WHEN 'Stairs' THEN 'Passenger'
                            WHEN 'Tug' THEN 'Ground Support'
                            WHEN 'Water Truck' THEN 'Services'
                            ELSE 'Other'
                        END AS category,
                        'Available' AS status,
                        v.tenant_code,
                        'Auto-generated from vehicle tracking data. Type: ' || v.vehicle_type AS description,
                        now()
                    FROM (
                        SELECT DISTINCT ON (vehicle_id, tenant_code)
                            vehicle_id, vehicle_name, vehicle_type, tenant_code
                        FROM vehicles
                        ORDER BY vehicle_id, tenant_code, id
                    ) v
                    WHERE NOT EXISTS (
                        SELECT 1 FROM assets a
                        WHERE a.asset_id = v.vehicle_id AND a.tenant_code = v.tenant_code
                    )
                    """ + (tenantCode != null ? " AND v.tenant_code = ?\n" : "") +
                    "ON CONFLICT (asset_id, tenant_code) DO NOTHING";

            int assetsCreated = tenantCode != null
                    ? jdbcTemplate.update(insertAssetsSql, tenantCode)
                    : jdbcTemplate.update(insertAssetsSql);

            logger.info("Created {} new assets from vehicles" + (tenantCode != null ? " for tenant " + tenantCode : ""),
                    assetsCreated);

            // Now create the mappings - use DISTINCT ON to handle duplicate vehicle_ids
            String insertMappingsSql = """
                    INSERT INTO vehicle_asset_map (id, tenant_code, vehicle_id, asset_id, created_at)
                    SELECT
                        uuid_generate_v4(),
                        v.tenant_code,
                        v.vehicle_id,
                        a.id,
                        now()
                    FROM (
                        SELECT DISTINCT ON (vehicle_id, tenant_code)
                            vehicle_id, tenant_code
                        FROM vehicles
                        ORDER BY vehicle_id, tenant_code
                    ) v
                    JOIN assets a ON a.asset_id = v.vehicle_id AND a.tenant_code = v.tenant_code
                    WHERE NOT EXISTS (
                        SELECT 1 FROM vehicle_asset_map vam
                        WHERE vam.vehicle_id = v.vehicle_id AND vam.tenant_code = v.tenant_code
                    )
                    """ + (tenantCode != null ? " AND v.tenant_code = ?\n" : "") +
                    "ON CONFLICT (tenant_code, vehicle_id) DO NOTHING";

            int mappingsCreated = tenantCode != null
                    ? jdbcTemplate.update(insertMappingsSql, tenantCode)
                    : jdbcTemplate.update(insertMappingsSql);

            logger.info(
                    "Created {} new vehicle-asset mappings" + (tenantCode != null ? " for tenant " + tenantCode : ""),
                    mappingsCreated);

            // Ensure live map has current locations for all mapped vehicle-backed assets.
            int registerRowsSynced = syncLocationRegisterFromVehicles(tenantCode);
            logger.info("Upserted {} asset location register rows{}",
                    registerRowsSynced,
                    tenantCode != null ? " for tenant " + tenantCode : "");

            // Update asset statuses based on vehicle tracking link
            updateAssetStatuses(tenantCode);

            // Populate asset movement trail for hotspot/heatmap data
            populateAssetMovementTrail(tenantCode);

            return mappingsCreated;

        } catch (Exception e) {
            logger.error("Failed to sync vehicles to assets: {}", e.getMessage());
            return 0;
        }
    }

    @Transactional
    protected int syncLocationRegisterFromVehicles(String tenantCode) {
        String upsertSql = """
                INSERT INTO asset_location_register (
                    asset_id,
                    asset_identifier,
                    tenant_code,
                    current_latitude,
                    current_longitude,
                    current_location,
                    current_zone,
                    last_movement_at,
                    is_in_restricted_zone,
                    is_authorized_for_zone,
                    last_updated
                )
                SELECT
                    a.id,
                    a.asset_id,
                    v.tenant_code,
                    v.latitude,
                    v.longitude,
                    CASE
                        WHEN v.latitude IS NOT NULL AND v.longitude IS NOT NULL
                            THEN ST_SetSRID(ST_MakePoint(v.longitude, v.latitude), 4326)
                        ELSE NULL
                    END,
                    v.zone,
                    COALESCE(v.timestamp, NOW()),
                    FALSE,
                    TRUE,
                    NOW()
                FROM (
                    SELECT DISTINCT ON (vehicle_id, tenant_code)
                        vehicle_id, tenant_code, latitude, longitude, zone, status, speed, timestamp, id
                    FROM vehicles
                    ORDER BY vehicle_id, tenant_code, timestamp DESC NULLS LAST, id DESC
                ) v
                JOIN assets a
                    ON a.asset_id = v.vehicle_id
                   AND a.tenant_code = v.tenant_code
                WHERE (? IS NULL OR v.tenant_code = ?)
                ON CONFLICT (asset_id) DO UPDATE SET
                    current_latitude = EXCLUDED.current_latitude,
                    current_longitude = EXCLUDED.current_longitude,
                    current_location = EXCLUDED.current_location,
                    current_zone = EXCLUDED.current_zone,
                    last_movement_at = EXCLUDED.last_movement_at,
                    last_updated = NOW()
                """;

        return jdbcTemplate.update(upsertSql, tenantCode, tenantCode);
    }

    /**
     * Update asset statuses based on vehicle tracking link.
     * - Assets linked to vehicles (via vehicle_asset_map) → "In Use" (actively
     * tracked)
     * - Standalone assets (not linked) → Random split between "Available" and
     * "Maintenance"
     * - Location: Standalone assets get random locations
     * 
     * @param tenantCode The tenant code to update, or null for all tenants
     */
    @Transactional
    public void updateAssetStatuses(String tenantCode) {
        try {
            // Step 1: Set all vehicle-linked assets to "In Use"
            String updateInUseSql = """
                    UPDATE assets a
                    SET
                        status = 'In Use',
                        updated_at = NOW()
                    WHERE EXISTS (
                        SELECT 1 FROM vehicle_asset_map vam WHERE vam.asset_id = a.id
                    )
                    """ + (tenantCode != null ? " AND a.tenant_code = ?" : "");

            int inUseCount = tenantCode != null
                    ? jdbcTemplate.update(updateInUseSql, tenantCode)
                    : jdbcTemplate.update(updateInUseSql);

            logger.info("Updated {} assets to 'In Use' status (vehicle-tracked)", inUseCount);

            // Step 2: Update standalone assets with random statuses and locations
            // Using PL/pgSQL DO block with a loop to ensure truly random values per row
            String tenantFilterValue = tenantCode != null ? "'" + tenantCode + "'" : "NULL";
            String updateStandaloneSql = "DO $$ " +
                    "DECLARE " +
                    "    asset_rec RECORD; " +
                    "    locations TEXT[] := ARRAY[" +
                    "        'Terminal 1', 'Terminal 2', 'Terminal 3', " +
                    "        'Apron A', 'Apron B', 'Apron C', " +
                    "        'Cargo Area', 'Fuel Station', 'Service Area', " +
                    "        'Hangar 1', 'Hangar 2', 'Maintenance Bay', " +
                    "        'Gate A1', 'Gate A2', 'Gate B1', 'Gate B2', " +
                    "        'Remote Stand R1', 'Remote Stand R2', 'Remote Stand R3', " +
                    "        'Equipment Yard', 'Ground Support Depot'" +
                    "    ]; " +
                    "    maintenance_descs TEXT[] := ARRAY[" +
                    "        'Scheduled maintenance', " +
                    "        'Oil change due', " +
                    "        'Tire replacement in progress', " +
                    "        'Annual safety inspection', " +
                    "        'Brake service required', " +
                    "        'Engine diagnostic check', " +
                    "        'Mandatory safety inspection', " +
                    "        'Battery replacement pending', " +
                    "        'Filter and fluid change scheduled'" +
                    "    ]; " +
                    "    rand_location_idx INT; " +
                    "    rand_status FLOAT; " +
                    "    new_status TEXT; " +
                    "    new_desc TEXT; " +
                    "    tenant_filter TEXT := " + tenantFilterValue + "; " +
                    "BEGIN " +
                    "    FOR asset_rec IN " +
                    "        SELECT a.id " +
                    "        FROM assets a " +
                    "        LEFT JOIN vehicle_asset_map vam ON a.id = vam.asset_id " +
                    "        WHERE vam.id IS NULL " +
                    "        AND (tenant_filter IS NULL OR a.tenant_code = tenant_filter) " +
                    "    LOOP " +
                    "        rand_status := random(); " +
                    "        IF rand_status < 0.7 THEN " +
                    "            new_status := 'Available'; " +
                    "            new_desc := NULL; " +
                    "        ELSE " +
                    "            new_status := 'Maintenance'; " +
                    "            new_desc := maintenance_descs[1 + floor(random() * array_length(maintenance_descs, 1))::int]; "
                    +
                    "            IF random() > 0.5 THEN " +
                    "                new_desc := new_desc || ' - Due: ' || (CURRENT_DATE + (floor(random() * 14) + 1)::int)::text; "
                    +
                    "            END IF; " +
                    "        END IF; " +
                    "        rand_location_idx := 1 + floor(random() * array_length(locations, 1))::int; " +
                    "        UPDATE assets SET status = new_status, location = locations[rand_location_idx], " +
                    "               description = COALESCE(new_desc, description), updated_at = NOW() " +
                    "        WHERE id = asset_rec.id; " +
                    "    END LOOP; " +
                    "END $$;";

            jdbcTemplate.execute(updateStandaloneSql);

            // Count standalone assets for logging
            String countSql = """
                    SELECT COUNT(*) FROM assets a
                    LEFT JOIN vehicle_asset_map vam ON a.id = vam.asset_id
                    WHERE vam.id IS NULL
                    """ + (tenantCode != null ? " AND a.tenant_code = ?" : "");

            Integer standaloneCount = tenantCode != null
                    ? jdbcTemplate.queryForObject(countSql, Integer.class, tenantCode)
                    : jdbcTemplate.queryForObject(countSql, Integer.class);

            logger.info("Updated {} standalone assets with random statuses (Available/Maintenance)", standaloneCount);

        } catch (Exception e) {
            logger.error("Failed to update asset statuses: {}", e.getMessage());
        }
    }

    /**
     * Populate asset movement trail data for hotspot/heatmap visualization.
     * Creates simulated movement data based on existing assets.
     * 
     * @param tenantCode The tenant code to populate, or null for all tenants
     * @return Number of movement records created
     */
    @Transactional
    public int populateAssetMovementTrail(String tenantCode) {
        try {
            // Get tenant coordinates
            String coordsSql = """
                    DO $$
                    DECLARE
                        tenant_lat DOUBLE PRECISION;
                        tenant_lng DOUBLE PRECISION;
                        v_tenant_code VARCHAR(4) := COALESCE(?, 'VIDP');
                    BEGIN
                        -- Set coordinates based on tenant
                        CASE v_tenant_code
                            WHEN 'VIDP' THEN tenant_lat := 28.5665; tenant_lng := 77.1031;
                            WHEN 'LIRN' THEN tenant_lat := 40.8844; tenant_lng := 14.2908;
                            WHEN 'YBBN' THEN tenant_lat := -27.3842; tenant_lng := 153.1175;
                            ELSE tenant_lat := 28.5665; tenant_lng := 77.1031;
                        END CASE;
                    END $$;
                    """;

            // Insert movement trail data for the tenant's assets
            String insertSql = """
                    INSERT INTO asset_movement_trail (
                        asset_identifier, tenant_code, latitude, longitude,
                        speed, heading, zone, status, timestamp
                    )
                    SELECT
                        a.asset_id,
                        a.tenant_code,
                        CASE a.tenant_code
                            WHEN 'VIDP' THEN 28.5665 + (random() - 0.5) * 0.05
                            WHEN 'LIRN' THEN 40.8844 + (random() - 0.5) * 0.05
                            WHEN 'YBBN' THEN -27.3842 + (random() - 0.5) * 0.05
                            ELSE 28.5665 + (random() - 0.5) * 0.05
                        END,
                        CASE a.tenant_code
                            WHEN 'VIDP' THEN 77.1031 + (random() - 0.5) * 0.05
                            WHEN 'LIRN' THEN 14.2908 + (random() - 0.5) * 0.05
                            WHEN 'YBBN' THEN 153.1175 + (random() - 0.5) * 0.05
                            ELSE 77.1031 + (random() - 0.5) * 0.05
                        END,
                        5 + random() * 35,
                        random() * 360,
                        CASE (floor(random() * 8)::int)
                            WHEN 0 THEN 'Terminal 1'
                            WHEN 1 THEN 'Terminal 2'
                            WHEN 2 THEN 'Cargo Area'
                            WHEN 3 THEN 'Fuel Station'
                            WHEN 4 THEN 'Maintenance Bay'
                            WHEN 5 THEN 'Apron East'
                            WHEN 6 THEN 'Apron West'
                            ELSE 'General Aviation'
                        END,
                        a.status,
                        NOW() - ((gs * 30) || ' minutes')::INTERVAL
                    FROM assets a
                    CROSS JOIN generate_series(0, 335) gs
                    WHERE a.status = 'In Use'
                    """ + (tenantCode != null ? " AND a.tenant_code = ?\n" : "") + """
                    ON CONFLICT DO NOTHING
                    """;

            int recordsCreated = tenantCode != null
                    ? jdbcTemplate.update(insertSql, tenantCode)
                    : jdbcTemplate.update(insertSql);

            logger.info(
                    "Created {} asset movement trail records" + (tenantCode != null ? " for tenant " + tenantCode : ""),
                    recordsCreated);

            return recordsCreated;

        } catch (Exception e) {
            logger.error("Failed to populate asset movement trail: {}", e.getMessage());
            return 0;
        }
    }

    public record AssetInfo(UUID id, String identifier, String name, String category) {
    }
}
