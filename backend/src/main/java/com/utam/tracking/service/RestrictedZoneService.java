package com.utam.tracking.service;

import com.utam.tracking.domain.RestrictedZone;
import com.utam.tracking.dto.RestrictedZoneDTO;
import com.utam.tracking.repository.RestrictedZoneRepository;
import org.locationtech.jts.geom.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing restricted zones.
 * Feature: 005-asset-tracking-security
 * Task: T054
 */
@Service
public class RestrictedZoneService {

    private static final Logger logger = LoggerFactory.getLogger(RestrictedZoneService.class);

    private final RestrictedZoneRepository zoneRepository;
    private final GeometryFactory geometryFactory;

    public RestrictedZoneService(RestrictedZoneRepository zoneRepository) {
        this.zoneRepository = zoneRepository;
        this.geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    }

    /**
     * Get all zones for a tenant.
     */
    @Transactional(readOnly = true)
    public List<RestrictedZoneDTO> getZones(String tenantCode) {
        return zoneRepository.findByTenantCode(tenantCode).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get active zones for a tenant.
     */
    @Transactional(readOnly = true)
    public List<RestrictedZoneDTO> getActiveZones(String tenantCode) {
        return zoneRepository.findByTenantCodeAndIsActive(tenantCode, true).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get a zone by ID.
     */
    @Transactional(readOnly = true)
    public RestrictedZoneDTO getZoneById(UUID id) {
        return zoneRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("Zone not found: " + id));
    }

    /**
     * Create a new restricted zone.
     */
    @Transactional
    public RestrictedZoneDTO createZone(RestrictedZoneDTO zoneDTO) {
        validateZoneDTO(zoneDTO);

        RestrictedZone zone = new RestrictedZone();
        zone.setZoneId(zoneDTO.getZoneId());
        zone.setZoneName(zoneDTO.getZoneName());
        zone.setZoneType(zoneDTO.getZoneType());
        zone.setDescription(zoneDTO.getDescription());
        zone.setBoundary(createPolygon(zoneDTO.getBoundaryCoordinates()));
        zone.setAuthorizedAssetCategories(zoneDTO.getAuthorizedAssetCategories());
        zone.setIsActive(zoneDTO.getIsActive() != null ? zoneDTO.getIsActive() : true);
        zone.setEffectiveFrom(zoneDTO.getEffectiveFrom() != null ? zoneDTO.getEffectiveFrom() : ZonedDateTime.now());
        zone.setEffectiveTo(zoneDTO.getEffectiveTo());
        zone.setTenantCode(zoneDTO.getTenantCode());

        RestrictedZone saved = zoneRepository.save(zone);
        logger.info("Created restricted zone: {} ({})", saved.getZoneName(), saved.getZoneId());
        
        return toDTO(saved);
    }

    /**
     * Update an existing zone.
     */
    @Transactional
    public RestrictedZoneDTO updateZone(UUID zoneId, RestrictedZoneDTO zoneDTO) {
        RestrictedZone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new RuntimeException("Zone not found: " + zoneId));

        if (zoneDTO.getZoneName() != null) {
            zone.setZoneName(zoneDTO.getZoneName());
        }
        if (zoneDTO.getZoneType() != null) {
            zone.setZoneType(zoneDTO.getZoneType());
        }
        if (zoneDTO.getDescription() != null) {
            zone.setDescription(zoneDTO.getDescription());
        }
        if (zoneDTO.getBoundaryCoordinates() != null) {
            zone.setBoundary(createPolygon(zoneDTO.getBoundaryCoordinates()));
        }
        if (zoneDTO.getAuthorizedAssetCategories() != null) {
            zone.setAuthorizedAssetCategories(zoneDTO.getAuthorizedAssetCategories());
        }
        if (zoneDTO.getIsActive() != null) {
            zone.setIsActive(zoneDTO.getIsActive());
        }
        if (zoneDTO.getEffectiveFrom() != null) {
            zone.setEffectiveFrom(zoneDTO.getEffectiveFrom());
        }
        if (zoneDTO.getEffectiveTo() != null) {
            zone.setEffectiveTo(zoneDTO.getEffectiveTo());
        }

        RestrictedZone saved = zoneRepository.save(zone);
        logger.info("Updated restricted zone: {}", saved.getZoneId());
        
        return toDTO(saved);
    }

    /**
     * Delete a zone.
     */
    @Transactional
    public void deleteZone(UUID zoneId) {
        RestrictedZone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new RuntimeException("Zone not found: " + zoneId));
        
        zoneRepository.delete(zone);
        logger.info("Deleted restricted zone: {}", zone.getZoneId());
    }

    /**
     * Validate zone DTO.
     */
    private void validateZoneDTO(RestrictedZoneDTO dto) {
        if (dto.getBoundaryCoordinates() == null || dto.getBoundaryCoordinates().size() < 4) {
            throw new IllegalArgumentException("Boundary must have at least 4 coordinates (closed polygon)");
        }
        
        List<String> validTypes = List.of("PROHIBITED", "RESTRICTED", "CONTROLLED", "MAINTENANCE");
        if (!validTypes.contains(dto.getZoneType().toUpperCase())) {
            throw new IllegalArgumentException("Invalid zone type. Must be one of: " + validTypes);
        }
    }

    /**
     * Create PostGIS Polygon from coordinate list.
     */
    private Polygon createPolygon(List<List<Double>> coordinates) {
        Coordinate[] coords = new Coordinate[coordinates.size()];
        for (int i = 0; i < coordinates.size(); i++) {
            List<Double> point = coordinates.get(i);
            coords[i] = new Coordinate(point.get(0), point.get(1)); // [lng, lat]
        }
        
        // Ensure polygon is closed
        if (!coords[0].equals2D(coords[coords.length - 1])) {
            Coordinate[] closedCoords = new Coordinate[coords.length + 1];
            System.arraycopy(coords, 0, closedCoords, 0, coords.length);
            closedCoords[coords.length] = coords[0];
            coords = closedCoords;
        }

        LinearRing ring = geometryFactory.createLinearRing(coords);
        return geometryFactory.createPolygon(ring);
    }

    /**
     * Convert entity to DTO.
     */
    private RestrictedZoneDTO toDTO(RestrictedZone entity) {
        List<List<Double>> coordinates = null;
        if (entity.getBoundary() != null) {
            Coordinate[] coords = entity.getBoundary().getCoordinates();
            coordinates = new java.util.ArrayList<>();
            for (Coordinate coord : coords) {
                coordinates.add(List.of(coord.x, coord.y)); // [lng, lat]
            }
        }

        return RestrictedZoneDTO.builder()
                .id(entity.getId())
                .zoneId(entity.getZoneId())
                .zoneName(entity.getZoneName())
                .zoneType(entity.getZoneType())
                .description(entity.getDescription())
                .boundaryCoordinates(coordinates)
                .authorizedAssetCategories(entity.getAuthorizedAssetCategories())
                .isActive(entity.getIsActive())
                .effectiveFrom(entity.getEffectiveFrom())
                .effectiveTo(entity.getEffectiveTo())
                .tenantCode(entity.getTenantCode())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
