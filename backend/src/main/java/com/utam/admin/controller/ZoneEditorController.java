package com.utam.admin.controller;

import com.utam.admin.dto.ZoneDTO;
import com.utam.admin.service.ZoneService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * REST controller for zone management with GeoJSON import/export.
 * Implements FR-040 to FR-048: Admin zone management.
 */
@RestController
@RequestMapping("/api/admin/zones")
public class ZoneEditorController {

    private static final Logger log = LoggerFactory.getLogger(ZoneEditorController.class);

    private final ZoneService zoneService;

    public ZoneEditorController(ZoneService zoneService) {
        this.zoneService = zoneService;
    }

    /**
     * Get all zones for a tenant.
     */
    @GetMapping
    public ResponseEntity<List<ZoneDTO>> getZones(
            @RequestParam String tenantCode,
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(required = false) String type) {

        List<ZoneDTO> zones;

        if (type != null && !type.isBlank()) {
            zones = zoneService.getZonesByType(tenantCode, type);
        } else if (Boolean.TRUE.equals(activeOnly)) {
            zones = zoneService.getActiveZonesForTenant(tenantCode);
        } else {
            zones = zoneService.getZonesForTenant(tenantCode);
        }

        return ResponseEntity.ok(zones);
    }

    /**
     * Get a single zone by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ZoneDTO> getZone(@PathVariable Long id) {
        return zoneService.getZone(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get zone by code.
     */
    @GetMapping("/by-code")
    public ResponseEntity<ZoneDTO> getZoneByCode(
            @RequestParam String tenantCode,
            @RequestParam String code) {

        return zoneService.getZoneByCode(tenantCode, code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a new zone.
     */
    @PostMapping
    public ResponseEntity<?> createZone(@RequestBody ZoneDTO dto) {
        try {
            ZoneDTO created = zoneService.createZone(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.warn("Zone creation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Validation failed",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Update an existing zone.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateZone(@PathVariable Long id, @RequestBody ZoneDTO dto) {
        try {
            ZoneDTO updated = zoneService.updateZone(id, dto);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("Zone update failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Validation failed",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Delete a zone.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteZone(@PathVariable Long id) {
        try {
            zoneService.deleteZone(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Activate a zone.
     */
    @PostMapping("/{id}/activate")
    public ResponseEntity<ZoneDTO> activateZone(@PathVariable Long id) {
        try {
            ZoneDTO zone = zoneService.setZoneActive(id, true);
            return ResponseEntity.ok(zone);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Deactivate a zone.
     */
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<ZoneDTO> deactivateZone(@PathVariable Long id) {
        try {
            ZoneDTO zone = zoneService.setZoneActive(id, false);
            return ResponseEntity.ok(zone);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Export all zones as GeoJSON file download.
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportGeoJson(@RequestParam String tenantCode) {
        String geoJson = zoneService.exportToGeoJson(tenantCode);

        byte[] bytes = geoJson.getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentDispositionFormData("attachment", tenantCode + "_zones.geojson");
        headers.setContentLength(bytes.length);

        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    /**
     * Export zones as inline GeoJSON (for API consumption).
     */
    @GetMapping("/geojson")
    public ResponseEntity<String> getGeoJson(@RequestParam String tenantCode) {
        String geoJson = zoneService.exportToGeoJson(tenantCode);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(geoJson);
    }

    /**
     * Import zones from GeoJSON file upload.
     */
    @PostMapping("/import")
    public ResponseEntity<?> importGeoJson(
            @RequestParam String tenantCode,
            @RequestParam("file") MultipartFile file) {

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Empty file",
                        "message", "Please upload a GeoJSON file"
                ));
            }

            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            List<ZoneDTO> imported = zoneService.importFromGeoJson(tenantCode, content);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "imported", imported.size(),
                    "zones", imported
            ));

        } catch (IllegalArgumentException e) {
            log.warn("GeoJSON import failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Import failed",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("GeoJSON import error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Import failed",
                    "message", "Failed to process GeoJSON file"
            ));
        }
    }

    /**
     * Import zones from GeoJSON string (for API usage).
     */
    @PostMapping("/import/json")
    public ResponseEntity<?> importGeoJsonString(
            @RequestParam String tenantCode,
            @RequestBody String geoJson) {

        try {
            List<ZoneDTO> imported = zoneService.importFromGeoJson(tenantCode, geoJson);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "imported", imported.size(),
                    "zones", imported
            ));

        } catch (IllegalArgumentException e) {
            log.warn("GeoJSON import failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Import failed",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Check if a point is within any restricted zone.
     */
    @GetMapping("/check-point")
    public ResponseEntity<?> checkPointInZones(
            @RequestParam String tenantCode,
            @RequestParam double lng,
            @RequestParam double lat) {

        List<ZoneDTO> restrictedZones = zoneService.findRestrictedZonesContainingPoint(tenantCode, lng, lat);

        return ResponseEntity.ok(Map.of(
                "point", Map.of("lng", lng, "lat", lat),
                "inRestrictedZone", !restrictedZones.isEmpty(),
                "restrictedZones", restrictedZones
        ));
    }

    /**
     * Get zone types.
     */
    @GetMapping("/types")
    public ResponseEntity<List<Map<String, String>>> getZoneTypes() {
        List<Map<String, String>> types = List.of(
                Map.of("code", ZoneDTO.TYPE_RESTRICTED, "name", "Restricted Area", "color", "#ef4444"),
                Map.of("code", ZoneDTO.TYPE_OPERATIONAL, "name", "Operational Area", "color", "#3b82f6"),
                Map.of("code", ZoneDTO.TYPE_PARKING, "name", "Parking Area", "color", "#22c55e"),
                Map.of("code", ZoneDTO.TYPE_TAXIWAY, "name", "Taxiway", "color", "#f59e0b"),
                Map.of("code", ZoneDTO.TYPE_RUNWAY, "name", "Runway", "color", "#8b5cf6"),
                Map.of("code", ZoneDTO.TYPE_TERMINAL, "name", "Terminal", "color", "#06b6d4"),
                Map.of("code", ZoneDTO.TYPE_CARGO, "name", "Cargo Area", "color", "#84cc16"),
                Map.of("code", ZoneDTO.TYPE_MAINTENANCE, "name", "Maintenance", "color", "#f97316"),
                Map.of("code", ZoneDTO.TYPE_SECURITY, "name", "Security Zone", "color", "#dc2626"),
                Map.of("code", ZoneDTO.TYPE_CUSTOM, "name", "Custom Zone", "color", "#6b7280")
        );
        return ResponseEntity.ok(types);
    }
}
