package com.utam.tracking.mapper;

import com.utam.tracking.domain.MovementDiscrepancy;
import com.utam.tracking.dto.MovementDiscrepancyDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for MovementDiscrepancy entity to DTO conversions.
 * Feature: 005-asset-tracking-security
 * Task: T056
 */
@Component
public class DiscrepancyMapper {

    /**
     * Convert entity to DTO.
     */
    public MovementDiscrepancyDTO toDTO(MovementDiscrepancy entity) {
        if (entity == null) {
            return null;
        }

        return MovementDiscrepancyDTO.builder()
                .id(entity.getId())
                .discrepancyId(entity.getDiscrepancyId())
                .assetId(entity.getAssetId())
                .assetIdentifier(entity.getAssetIdentifier())
                .assetName(entity.getAssetName())
                .assetCategory(entity.getAssetCategory())
                .discrepancyType(entity.getDiscrepancyType() != null ? 
                        entity.getDiscrepancyType().name() : null)
                .expectedLocation(entity.getExpectedLocation())
                .actualLocation(entity.getActualLocation())
                .expectedLatitude(entity.getExpectedLatitude())
                .expectedLongitude(entity.getExpectedLongitude())
                .actualLatitude(entity.getActualLatitude())
                .actualLongitude(entity.getActualLongitude())
                .deviationMeters(entity.getDeviationMeters())
                .expectedStatus(entity.getExpectedStatus())
                .actualStatus(entity.getActualStatus())
                .description(entity.getDescription())
                .severity(entity.getSeverity() != null ? entity.getSeverity().name() : null)
                .timestamp(entity.getTimestamp())
                .acknowledged(entity.getAcknowledged())
                .acknowledgedBy(entity.getAcknowledgedBy())
                .acknowledgedAt(entity.getAcknowledgedAt())
                .resolutionNotes(entity.getResolutionNotes())
                .tenantCode(entity.getTenantCode())
                .build();
    }

    /**
     * Convert DTO to entity (for create operations).
     */
    public MovementDiscrepancy toEntity(MovementDiscrepancyDTO dto) {
        if (dto == null) {
            return null;
        }

        return MovementDiscrepancy.builder()
                .id(dto.getId())
                .discrepancyId(dto.getDiscrepancyId())
                .assetId(dto.getAssetId())
                .assetIdentifier(dto.getAssetIdentifier())
                .assetName(dto.getAssetName())
                .assetCategory(dto.getAssetCategory())
                .discrepancyType(dto.getDiscrepancyType() != null ? 
                        MovementDiscrepancy.DiscrepancyType.valueOf(dto.getDiscrepancyType()) : null)
                .expectedLocation(dto.getExpectedLocation())
                .actualLocation(dto.getActualLocation())
                .expectedLatitude(dto.getExpectedLatitude())
                .expectedLongitude(dto.getExpectedLongitude())
                .actualLatitude(dto.getActualLatitude())
                .actualLongitude(dto.getActualLongitude())
                .deviationMeters(dto.getDeviationMeters())
                .expectedStatus(dto.getExpectedStatus())
                .actualStatus(dto.getActualStatus())
                .description(dto.getDescription())
                .severity(dto.getSeverity() != null ? 
                        MovementDiscrepancy.DiscrepancySeverity.valueOf(dto.getSeverity()) : null)
                .timestamp(dto.getTimestamp())
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
    public List<MovementDiscrepancyDTO> toDTOList(List<MovementDiscrepancy> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Update entity from DTO (partial update for acknowledgment).
     */
    public void updateEntityFromDTO(MovementDiscrepancy entity, MovementDiscrepancyDTO dto) {
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
