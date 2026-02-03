package com.utam.tracking.mapper;

import com.utam.tracking.domain.ZoneViolation;
import com.utam.tracking.dto.ZoneViolationDTO;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for ZoneViolation entity to DTO conversions.
 * Feature: 005-asset-tracking-security
 * Task: T056
 */
@Component
public class ZoneViolationMapper {

    /**
     * Convert entity to DTO.
     */
    public ZoneViolationDTO toDTO(ZoneViolation entity) {
        if (entity == null) {
            return null;
        }

        ZoneViolationDTO.ZoneViolationDTOBuilder builder = ZoneViolationDTO.builder()
                .id(entity.getId())
                .violationId(entity.getViolationId())
                .assetId(entity.getAssetId())
                .assetIdentifier(entity.getAssetIdentifier())
                .assetName(entity.getAssetName())
                .assetCategory(entity.getAssetCategory())
                .restrictedZoneId(entity.getRestrictedZoneId())
                .zoneName(entity.getZoneName())
                .zoneType(entity.getZoneType())
                .violationType(entity.getViolationType())
                .severity(entity.getSeverity() != null ? entity.getSeverity().name() : null)
                .timestamp(entity.getTimestamp())
                .durationSeconds(entity.getDurationSeconds() != null ? entity.getDurationSeconds().longValue() : null)
                .acknowledged(entity.getAcknowledged())
                .acknowledgedBy(entity.getAcknowledgedBy())
                .acknowledgedAt(entity.getAcknowledgedAt())
                .resolutionNotes(entity.getResolutionNotes())
                .tenantCode(entity.getTenantCode());

        // Extract coordinates from Point geometry
        Point entryLocation = entity.getEntryLocation();
        if (entryLocation != null) {
            builder.entryLatitude(entryLocation.getY());
            builder.entryLongitude(entryLocation.getX());
        }

        return builder.build();
    }

    /**
     * Convert DTO to entity (for create/update operations).
     */
    public ZoneViolation toEntity(ZoneViolationDTO dto) {
        if (dto == null) {
            return null;
        }

        return ZoneViolation.builder()
                .id(dto.getId())
                .violationId(dto.getViolationId())
                .assetId(dto.getAssetId())
                .assetIdentifier(dto.getAssetIdentifier())
                .assetName(dto.getAssetName())
                .assetCategory(dto.getAssetCategory())
                .restrictedZoneId(dto.getRestrictedZoneId())
                .zoneName(dto.getZoneName())
                .zoneType(dto.getZoneType())
                .violationType(dto.getViolationType())
                .severity(dto.getSeverity() != null ? 
                        ZoneViolation.ViolationSeverity.valueOf(dto.getSeverity()) : null)
                .timestamp(dto.getTimestamp())
                .durationSeconds(dto.getDurationSeconds() != null ? 
                        dto.getDurationSeconds().intValue() : null)
                .acknowledged(dto.getAcknowledged())
                .acknowledgedBy(dto.getAcknowledgedBy())
                .acknowledgedAt(dto.getAcknowledgedAt())
                .resolutionNotes(dto.getResolutionNotes())
                .tenantCode(dto.getTenantCode())
                .build();
    }

    /**
     * Convert list of entities to DTOs.
     */
    public List<ZoneViolationDTO> toDTOList(List<ZoneViolation> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Update entity from DTO (partial update support).
     */
    public void updateEntityFromDTO(ZoneViolation entity, ZoneViolationDTO dto) {
        if (dto.getAcknowledged() != null) {
            entity.setAcknowledged(dto.getAcknowledged());
        }
        if (dto.getAcknowledgedBy() != null) {
            entity.setAcknowledgedBy(dto.getAcknowledgedBy());
        }
        if (dto.getAcknowledgedAt() != null) {
            entity.setAcknowledgedAt(dto.getAcknowledgedAt());
        }
        if (dto.getResolutionNotes() != null) {
            entity.setResolutionNotes(dto.getResolutionNotes());
        }
    }
}
