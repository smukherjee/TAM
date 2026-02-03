package com.utam.tracking.mapper;

import com.utam.tracking.domain.RestrictedZone;
import com.utam.tracking.dto.RestrictedZoneDTO;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for RestrictedZone entity to DTO conversions.
 * Handles PostGIS Polygon geometry to coordinate array conversions.
 * Feature: 005-asset-tracking-security
 * Task: T056
 */
@Component
public class ZoneMapper {

    private static final int SRID = 4326; // WGS84
    private final GeometryFactory geometryFactory;

    public ZoneMapper() {
        this.geometryFactory = new GeometryFactory(new PrecisionModel(), SRID);
    }

    /**
     * Convert entity to DTO.
     */
    public RestrictedZoneDTO toDTO(RestrictedZone entity) {
        if (entity == null) {
            return null;
        }

        return RestrictedZoneDTO.builder()
                .id(entity.getId())
                .zoneId(entity.getZoneId())
                .zoneName(entity.getZoneName())
                .zoneType(entity.getZoneType())
                .description(entity.getDescription())
                .boundaryCoordinates(polygonToCoordinates(entity.getBoundary()))
                .authorizedAssetCategories(entity.getAuthorizedAssetCategories())
                .isActive(entity.getIsActive())
                .effectiveFrom(entity.getEffectiveFrom())
                .effectiveTo(entity.getEffectiveTo())
                .tenantCode(entity.getTenantCode())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Convert DTO to entity (for create/update operations).
     */
    public RestrictedZone toEntity(RestrictedZoneDTO dto) {
        if (dto == null) {
            return null;
        }

        return RestrictedZone.builder()
                .id(dto.getId())
                .zoneId(dto.getZoneId())
                .zoneName(dto.getZoneName())
                .zoneType(dto.getZoneType())
                .description(dto.getDescription())
                .boundary(coordinatesToPolygon(dto.getBoundaryCoordinates()))
                .authorizedAssetCategories(dto.getAuthorizedAssetCategories())
                .isActive(dto.getIsActive())
                .effectiveFrom(dto.getEffectiveFrom())
                .effectiveTo(dto.getEffectiveTo())
                .tenantCode(dto.getTenantCode())
                .build();
    }

    /**
     * Convert list of entities to DTOs.
     */
    public List<RestrictedZoneDTO> toDTOList(List<RestrictedZone> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Update entity from DTO (partial update).
     */
    public void updateEntityFromDTO(RestrictedZone entity, RestrictedZoneDTO dto) {
        if (dto.getZoneName() != null) {
            entity.setZoneName(dto.getZoneName());
        }
        if (dto.getZoneType() != null) {
            entity.setZoneType(dto.getZoneType());
        }
        if (dto.getDescription() != null) {
            entity.setDescription(dto.getDescription());
        }
        if (dto.getBoundaryCoordinates() != null && !dto.getBoundaryCoordinates().isEmpty()) {
            entity.setBoundary(coordinatesToPolygon(dto.getBoundaryCoordinates()));
        }
        if (dto.getAuthorizedAssetCategories() != null) {
            entity.setAuthorizedAssetCategories(dto.getAuthorizedAssetCategories());
        }
        if (dto.getIsActive() != null) {
            entity.setIsActive(dto.getIsActive());
        }
        if (dto.getEffectiveFrom() != null) {
            entity.setEffectiveFrom(dto.getEffectiveFrom());
        }
        if (dto.getEffectiveTo() != null) {
            entity.setEffectiveTo(dto.getEffectiveTo());
        }
    }

    /**
     * Convert PostGIS Polygon to coordinate list.
     * Returns list of [longitude, latitude] pairs.
     */
    public List<List<Double>> polygonToCoordinates(Polygon polygon) {
        if (polygon == null) {
            return List.of();
        }

        Coordinate[] coords = polygon.getExteriorRing().getCoordinates();
        List<List<Double>> result = new ArrayList<>();

        for (Coordinate coord : coords) {
            result.add(Arrays.asList(coord.x, coord.y)); // [lng, lat]
        }

        return result;
    }

    /**
     * Convert coordinate list to PostGIS Polygon.
     * Expects list of [longitude, latitude] pairs.
     */
    public Polygon coordinatesToPolygon(List<List<Double>> coordinates) {
        if (coordinates == null || coordinates.size() < 4) {
            throw new IllegalArgumentException(
                    "Polygon requires at least 4 coordinates (first and last must be the same)");
        }

        Coordinate[] coords = new Coordinate[coordinates.size()];
        for (int i = 0; i < coordinates.size(); i++) {
            List<Double> point = coordinates.get(i);
            if (point.size() < 2) {
                throw new IllegalArgumentException(
                        "Each coordinate must have at least 2 values [longitude, latitude]");
            }
            coords[i] = new Coordinate(point.get(0), point.get(1)); // x=lng, y=lat
        }

        // Ensure polygon is closed
        if (!coords[0].equals(coords[coords.length - 1])) {
            Coordinate[] closedCoords = new Coordinate[coords.length + 1];
            System.arraycopy(coords, 0, closedCoords, 0, coords.length);
            closedCoords[coords.length] = coords[0];
            coords = closedCoords;
        }

        LinearRing shell = geometryFactory.createLinearRing(coords);
        Polygon polygon = geometryFactory.createPolygon(shell);
        polygon.setSRID(SRID);

        return polygon;
    }

    /**
     * Validate polygon coordinates.
     */
    public boolean isValidPolygon(List<List<Double>> coordinates) {
        if (coordinates == null || coordinates.size() < 4) {
            return false;
        }

        for (List<Double> point : coordinates) {
            if (point.size() < 2) {
                return false;
            }
            double lng = point.get(0);
            double lat = point.get(1);
            if (lng < -180 || lng > 180 || lat < -90 || lat > 90) {
                return false;
            }
        }

        return true;
    }
}
