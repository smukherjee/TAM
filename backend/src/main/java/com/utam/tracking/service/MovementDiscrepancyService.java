package com.utam.tracking.service;

import com.utam.tracking.domain.MovementDiscrepancy;
import com.utam.tracking.dto.MovementDiscrepancyDTO;
import com.utam.tracking.repository.MovementDiscrepancyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing movement discrepancies.
 * Feature: 005-asset-tracking-security
 * Task: T052
 */
@Service
public class MovementDiscrepancyService {

    private static final Logger logger = LoggerFactory.getLogger(MovementDiscrepancyService.class);

    private final MovementDiscrepancyRepository discrepancyRepository;

    public MovementDiscrepancyService(MovementDiscrepancyRepository discrepancyRepository) {
        this.discrepancyRepository = discrepancyRepository;
    }

    /**
     * Get discrepancies with filters and pagination.
     */
    @Transactional(readOnly = true)
    public Page<MovementDiscrepancyDTO> getDiscrepancies(
            String tenantCode,
            ZonedDateTime startDate,
            ZonedDateTime endDate,
            String discrepancyType,
            Boolean acknowledged,
            Pageable pageable) {

        Page<MovementDiscrepancy> discrepancies;

        if (discrepancyType != null && !discrepancyType.isEmpty()) {
            MovementDiscrepancy.DiscrepancyType type = 
                MovementDiscrepancy.DiscrepancyType.valueOf(discrepancyType.toUpperCase());
            discrepancies = discrepancyRepository.findByDiscrepancyType(type, pageable);
        } else if (acknowledged != null) {
            discrepancies = discrepancyRepository.findByAcknowledged(acknowledged, pageable);
        } else if (startDate != null && endDate != null) {
            discrepancies = discrepancyRepository.findByTenantCodeAndTimestampBetween(
                tenantCode, startDate, endDate, pageable);
        } else {
            discrepancies = discrepancyRepository.findByTenantCode(tenantCode, pageable);
        }

        return discrepancies.map(this::toDTO);
    }

    /**
     * Get a single discrepancy by ID.
     */
    @Transactional(readOnly = true)
    public MovementDiscrepancyDTO getDiscrepancyById(UUID id) {
        return discrepancyRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("Discrepancy not found: " + id));
    }

    /**
     * Acknowledge a discrepancy.
     */
    @Transactional
    public MovementDiscrepancyDTO acknowledgeDiscrepancy(UUID discrepancyId, String userId, String notes) {
        MovementDiscrepancy discrepancy = discrepancyRepository.findById(discrepancyId)
                .orElseThrow(() -> new RuntimeException("Discrepancy not found: " + discrepancyId));

        discrepancy.setAcknowledged(true);
        try {
            discrepancy.setAcknowledgedBy(UUID.fromString(userId));
        } catch (IllegalArgumentException e) {
            // If userId is not a valid UUID, leave acknowledgedBy as null
            logger.warn("Invalid UUID format for userId: {}", userId);
        }
        discrepancy.setAcknowledgedAt(ZonedDateTime.now());
        discrepancy.setResolutionNotes(notes);

        MovementDiscrepancy saved = discrepancyRepository.save(discrepancy);
        logger.info("Discrepancy {} acknowledged by {}", discrepancyId, userId);
        
        return toDTO(saved);
    }

    /**
     * Get discrepancy statistics for a tenant.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDiscrepancyStatistics(String tenantCode, ZonedDateTime startDate, ZonedDateTime endDate) {
        List<Object[]> stats = discrepancyRepository.getDiscrepancyStatsByType(tenantCode, startDate, endDate);
        
        Map<String, Long> byType = stats.stream()
                .collect(Collectors.toMap(
                    row -> row[0].toString(),
                    row -> (Long) row[1]
                ));

        long total = byType.values().stream().mapToLong(Long::longValue).sum();
        long unacknowledged = discrepancyRepository.countByTenantCodeAndAcknowledged(tenantCode, false);

        return Map.of(
            "total", total,
            "unacknowledged", unacknowledged,
            "byType", byType,
            "tenantCode", tenantCode,
            "startDate", startDate,
            "endDate", endDate
        );
    }

    /**
     * Get all discrepancies for export (unpaged).
     * Task: T055a, T055b
     */
    @Transactional(readOnly = true)
    public List<MovementDiscrepancyDTO> getAllDiscrepanciesForExport(
            String tenantCode,
            ZonedDateTime startDate,
            ZonedDateTime endDate,
            String discrepancyType,
            Boolean acknowledged) {

        List<MovementDiscrepancy> discrepancies;

        if (startDate != null && endDate != null) {
            discrepancies = discrepancyRepository.findByTenantCodeAndTimestampBetweenOrderByTimestampDesc(
                tenantCode, startDate, endDate);
        } else {
            discrepancies = discrepancyRepository.findByTenantCodeOrderByTimestampDesc(tenantCode);
        }

        // Apply filters
        return discrepancies.stream()
            .filter(d -> discrepancyType == null || discrepancyType.isEmpty() || 
                        (d.getDiscrepancyType() != null && d.getDiscrepancyType().name().equalsIgnoreCase(discrepancyType)))
            .filter(d -> acknowledged == null || d.getAcknowledged().equals(acknowledged))
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    /**
     * Convert entity to DTO.
     */
    private MovementDiscrepancyDTO toDTO(MovementDiscrepancy entity) {
        return MovementDiscrepancyDTO.builder()
                .id(entity.getId())
                .discrepancyId(entity.getDiscrepancyId())
                .assetId(entity.getAssetId())
                .assetIdentifier(entity.getAssetIdentifier())
                .assetName(entity.getAssetName())
                .assetCategory(entity.getAssetCategory())
                .discrepancyType(entity.getDiscrepancyType() != null ? entity.getDiscrepancyType().name() : null)
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
}
