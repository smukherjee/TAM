package com.utam.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.utam.admin.dto.ZoneDTO;
import com.utam.entity.Zone;
import com.utam.repository.ZoneRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for zone management with GeoJSON import/export.
 * Implements FR-040 to FR-048: Admin zone management.
 */
@Service
@Transactional
public class ZoneService {

    private static final Logger log = LoggerFactory.getLogger(ZoneService.class);

    private final ZoneRepository zoneRepository;
    private final ObjectMapper objectMapper;

    public ZoneService(ZoneRepository zoneRepository, ObjectMapper objectMapper) {
        this.zoneRepository = zoneRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Get all zones for a tenant.
     */
    @Transactional(readOnly = true)
    public List<ZoneDTO> getZonesForTenant(String tenantCode) {
        return zoneRepository.findByTenantCode(tenantCode).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get active zones for a tenant.
     */
    @Transactional(readOnly = true)
    public List<ZoneDTO> getActiveZonesForTenant(String tenantCode) {
        return zoneRepository.findByTenantCodeAndActiveTrue(tenantCode).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get zones by type.
     */
    @Transactional(readOnly = true)
    public List<ZoneDTO> getZonesByType(String tenantCode, String type) {
        return zoneRepository.findByTenantCodeAndType(tenantCode, type).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get a single zone by ID.
     */
    @Transactional(readOnly = true)
    public Optional<ZoneDTO> getZone(Long id) {
        return zoneRepository.findById(id).map(this::toDTO);
    }

    /**
     * Get zone by code.
     */
    @Transactional(readOnly = true)
    public Optional<ZoneDTO> getZoneByCode(String tenantCode, String code) {
        return zoneRepository.findByTenantCodeAndCode(tenantCode, code).map(this::toDTO);
    }

    /**
     * Create a new zone.
     */
    public ZoneDTO createZone(ZoneDTO dto) {
        validateZone(dto);

        Zone zone = fromDTO(dto);
        zone.setCreatedAt(LocalDateTime.now());
        zone.setUpdatedAt(LocalDateTime.now());

        Zone saved = zoneRepository.save(zone);
        log.info("Created zone: {} for tenant {}", saved.getCode(), saved.getTenantCode());

        return toDTO(saved);
    }

    /**
     * Update an existing zone.
     */
    public ZoneDTO updateZone(Long id, ZoneDTO dto) {
        Zone existing = zoneRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Zone not found: " + id));

        validateZone(dto);

        // Update fields
        existing.setName(dto.getName());
        existing.setCode(dto.getCode());
        existing.setType(dto.getType());
        existing.setDescription(dto.getDescription());
        existing.setPolygon(coordinatesToPolygonString(dto.getCoordinates()));
        existing.setColor(dto.getColor());
        existing.setOpacity(dto.getOpacity());
        existing.setRestricted(dto.getRestricted() != null && dto.getRestricted());
        existing.setActive(dto.getActive() != null && dto.getActive());
        existing.setAllowedVehicleTypes(listToJson(dto.getAllowedVehicleTypes()));
        existing.setAllowedRoles(listToJson(dto.getAllowedRoles()));
        existing.setAccessSchedule(dto.getAccessSchedule());
        existing.setAlertOnEntry(dto.getAlertOnEntry() != null && dto.getAlertOnEntry());
        existing.setAlertOnExit(dto.getAlertOnExit() != null && dto.getAlertOnExit());
        existing.setDwellTimeAlertMinutes(dto.getDwellTimeAlertMinutes());
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(dto.getUpdatedBy());

        Zone saved = zoneRepository.save(existing);
        log.info("Updated zone: {} for tenant {}", saved.getCode(), saved.getTenantCode());

        return toDTO(saved);
    }

    /**
     * Delete a zone.
     */
    public void deleteZone(Long id) {
        Zone zone = zoneRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Zone not found: " + id));

        zoneRepository.delete(zone);
        log.info("Deleted zone: {} for tenant {}", zone.getCode(), zone.getTenantCode());
    }

    /**
     * Activate a zone.
     */
    public ZoneDTO setZoneActive(Long id, boolean active) {
        Zone zone = zoneRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Zone not found: " + id));

        zone.setActive(active);
        zone.setUpdatedAt(LocalDateTime.now());

        Zone saved = zoneRepository.save(zone);
        log.info("{} zone: {}", active ? "Activated" : "Deactivated", zone.getCode());

        return toDTO(saved);
    }

    /**
     * Export zones as GeoJSON FeatureCollection.
     */
    @Transactional(readOnly = true)
    public String exportToGeoJson(String tenantCode) {
        List<ZoneDTO> zones = getZonesForTenant(tenantCode);

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"type\":\"FeatureCollection\",");
        json.append("\"features\":[");

        StringJoiner features = new StringJoiner(",");
        for (ZoneDTO zone : zones) {
            features.add(zone.toGeoJson());
        }
        json.append(features.toString());

        json.append("]");
        json.append("}");

        return json.toString();
    }

    /**
     * Import zones from GeoJSON FeatureCollection.
     * Returns list of imported zone DTOs.
     */
    public List<ZoneDTO> importFromGeoJson(String tenantCode, String geoJson) {
        List<ZoneDTO> imported = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(geoJson);

            if (!root.has("type")) {
                throw new IllegalArgumentException("Invalid GeoJSON: missing type");
            }

            String type = root.get("type").asText();

            if ("FeatureCollection".equals(type)) {
                JsonNode features = root.get("features");
                if (features != null && features.isArray()) {
                    for (JsonNode feature : features) {
                        ZoneDTO zone = parseFeature(tenantCode, feature);
                        if (zone != null) {
                            imported.add(createZone(zone));
                        }
                    }
                }
            } else if ("Feature".equals(type)) {
                ZoneDTO zone = parseFeature(tenantCode, root);
                if (zone != null) {
                    imported.add(createZone(zone));
                }
            } else {
                throw new IllegalArgumentException("Unsupported GeoJSON type: " + type);
            }

            log.info("Imported {} zones for tenant {}", imported.size(), tenantCode);

        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid GeoJSON format: " + e.getMessage(), e);
        }

        return imported;
    }

    /**
     * Parse a GeoJSON Feature into ZoneDTO.
     */
    private ZoneDTO parseFeature(String tenantCode, JsonNode feature) {
        if (!feature.has("geometry") || !feature.has("properties")) {
            return null;
        }

        JsonNode geometry = feature.get("geometry");
        JsonNode properties = feature.get("properties");

        if (!"Polygon".equals(geometry.get("type").asText())) {
            log.warn("Skipping non-polygon geometry: {}", geometry.get("type").asText());
            return null;
        }

        // Parse coordinates
        JsonNode coordsArray = geometry.get("coordinates");
        if (coordsArray == null || !coordsArray.isArray() || coordsArray.isEmpty()) {
            return null;
        }

        // GeoJSON polygon has array of rings, first is exterior
        JsonNode exteriorRing = coordsArray.get(0);
        List<List<Double>> coordinates = new ArrayList<>();
        for (JsonNode point : exteriorRing) {
            if (point.isArray() && point.size() >= 2) {
                coordinates.add(Arrays.asList(point.get(0).asDouble(), point.get(1).asDouble()));
            }
        }

        // Build DTO
        ZoneDTO dto = new ZoneDTO();
        dto.setTenantCode(tenantCode);
        dto.setCoordinates(coordinates);
        dto.setActive(true);

        // Parse properties
        if (properties.has("name")) {
            dto.setName(properties.get("name").asText());
        } else {
            dto.setName("Imported Zone " + System.currentTimeMillis());
        }

        if (properties.has("code")) {
            dto.setCode(properties.get("code").asText());
        } else {
            dto.setCode("ZONE_" + System.currentTimeMillis());
        }

        if (properties.has("type")) {
            dto.setType(properties.get("type").asText());
        } else {
            dto.setType(ZoneDTO.TYPE_CUSTOM);
        }

        if (properties.has("description")) {
            dto.setDescription(properties.get("description").asText());
        }

        if (properties.has("color")) {
            dto.setColor(properties.get("color").asText());
        }

        if (properties.has("opacity")) {
            dto.setOpacity(properties.get("opacity").asDouble());
        }

        if (properties.has("restricted")) {
            dto.setRestricted(properties.get("restricted").asBoolean());
        }

        return dto;
    }

    /**
     * Validate zone data.
     */
    private void validateZone(ZoneDTO dto) {
        List<String> errors = new ArrayList<>();

        if (dto.getTenantCode() == null || dto.getTenantCode().isBlank()) {
            errors.add("Tenant code is required");
        }

        if (dto.getName() == null || dto.getName().isBlank()) {
            errors.add("Zone name is required");
        }

        if (dto.getCode() == null || dto.getCode().isBlank()) {
            errors.add("Zone code is required");
        }

        if (dto.getCoordinates() == null || dto.getCoordinates().size() < 3) {
            errors.add("Zone must have at least 3 coordinate points");
        }

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Validation failed: " + String.join(", ", errors));
        }
    }

    /**
     * Convert Zone entity to DTO.
     */
    private ZoneDTO toDTO(Zone zone) {
        ZoneDTO dto = new ZoneDTO();
        dto.setId(zone.getId());
        dto.setTenantCode(zone.getTenantCode());
        dto.setName(zone.getName());
        dto.setCode(zone.getCode());
        dto.setType(zone.getType());
        dto.setDescription(zone.getDescription());
        dto.setCoordinates(polygonStringToCoordinates(zone.getPolygon()));
        dto.setColor(zone.getColor());
        dto.setOpacity(zone.getOpacity());
        dto.setRestricted(zone.isRestricted());
        dto.setActive(zone.isActive());
        dto.setAllowedVehicleTypes(jsonToList(zone.getAllowedVehicleTypes()));
        dto.setAllowedRoles(jsonToList(zone.getAllowedRoles()));
        dto.setAccessSchedule(zone.getAccessSchedule());
        dto.setAlertOnEntry(zone.isAlertOnEntry());
        dto.setAlertOnExit(zone.isAlertOnExit());
        dto.setDwellTimeAlertMinutes(zone.getDwellTimeAlertMinutes());
        dto.setCreatedAt(zone.getCreatedAt());
        dto.setUpdatedAt(zone.getUpdatedAt());
        dto.setCreatedBy(zone.getCreatedBy());
        dto.setUpdatedBy(zone.getUpdatedBy());
        return dto;
    }

    /**
     * Convert DTO to Zone entity.
     */
    private Zone fromDTO(ZoneDTO dto) {
        Zone zone = new Zone();
        zone.setTenantCode(dto.getTenantCode());
        zone.setName(dto.getName());
        zone.setCode(dto.getCode());
        zone.setType(dto.getType() != null ? dto.getType() : ZoneDTO.TYPE_CUSTOM);
        zone.setDescription(dto.getDescription());
        zone.setPolygon(coordinatesToPolygonString(dto.getCoordinates()));
        zone.setColor(dto.getColor());
        zone.setOpacity(dto.getOpacity());
        zone.setRestricted(dto.getRestricted() != null && dto.getRestricted());
        zone.setActive(dto.getActive() != null && dto.getActive());
        zone.setAllowedVehicleTypes(listToJson(dto.getAllowedVehicleTypes()));
        zone.setAllowedRoles(listToJson(dto.getAllowedRoles()));
        zone.setAccessSchedule(dto.getAccessSchedule());
        zone.setAlertOnEntry(dto.getAlertOnEntry() != null && dto.getAlertOnEntry());
        zone.setAlertOnExit(dto.getAlertOnExit() != null && dto.getAlertOnExit());
        zone.setDwellTimeAlertMinutes(dto.getDwellTimeAlertMinutes());
        zone.setCreatedBy(dto.getCreatedBy());
        zone.setUpdatedBy(dto.getUpdatedBy());
        return zone;
    }

    /**
     * Convert coordinates list to polygon string storage format.
     */
    private String coordinatesToPolygonString(List<List<Double>> coordinates) {
        if (coordinates == null || coordinates.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(coordinates);
        } catch (JsonProcessingException e) {
            log.error("Error serializing coordinates", e);
            return "[]";
        }
    }

    /**
     * Parse polygon string to coordinates list.
     */
    private List<List<Double>> polygonStringToCoordinates(String polygon) {
        if (polygon == null || polygon.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(polygon,
                    objectMapper.getTypeFactory().constructCollectionType(List.class,
                            objectMapper.getTypeFactory().constructCollectionType(List.class, Double.class)));
        } catch (JsonProcessingException e) {
            log.error("Error parsing polygon: {}", polygon, e);
            return new ArrayList<>();
        }
    }

    /**
     * Convert list to JSON string.
     */
    private String listToJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * Parse JSON string to list.
     */
    private List<String> jsonToList(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    /**
     * Check if a point is within any restricted zone.
     */
    @Transactional(readOnly = true)
    public List<ZoneDTO> findRestrictedZonesContainingPoint(String tenantCode, double lng, double lat) {
        return getActiveZonesForTenant(tenantCode).stream()
                .filter(z -> Boolean.TRUE.equals(z.getRestricted()))
                .filter(z -> z.containsPoint(lng, lat))
                .collect(Collectors.toList());
    }
}
