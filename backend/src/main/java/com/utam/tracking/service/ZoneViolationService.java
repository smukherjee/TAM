package com.utam.tracking.service;

import com.utam.tracking.domain.ZoneViolation;
import com.utam.tracking.dto.ZoneViolationDTO;
import com.utam.tracking.repository.ZoneViolationRepository;
import org.locationtech.jts.geom.Point;
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
 * Service for managing zone violations.
 * Feature: 005-asset-tracking-security
 * Task: T051
 */
@Service
public class ZoneViolationService {

    private static final Logger logger = LoggerFactory.getLogger(ZoneViolationService.class);

    private final ZoneViolationRepository violationRepository;

    public ZoneViolationService(ZoneViolationRepository violationRepository) {
        this.violationRepository = violationRepository;
    }

    /**
     * Get violations with filters and pagination.
     */
    @Transactional(readOnly = true)
    public Page<ZoneViolationDTO> getViolations(
            String tenantCode,
            ZonedDateTime startDate,
            ZonedDateTime endDate,
            String severity,
            Boolean acknowledged,
            Pageable pageable) {

        Page<ZoneViolation> violations;
        
        if (severity != null && !severity.isEmpty()) {
            ZoneViolation.ViolationSeverity severityEnum = 
                ZoneViolation.ViolationSeverity.valueOf(severity.toUpperCase());
            violations = violationRepository.findByTenantCodeAndSeverity(tenantCode, severityEnum, pageable);
        } else if (acknowledged != null) {
            violations = violationRepository.findByAcknowledged(acknowledged, pageable);
        } else if (startDate != null && endDate != null) {
            violations = violationRepository.findByTenantCodeAndTimestampBetween(
                tenantCode, startDate, endDate, pageable);
        } else {
            violations = violationRepository.findByTenantCode(tenantCode, pageable);
        }

        return violations.map(this::toDTO);
    }

    /**
     * Get a single violation by ID.
     */
    @Transactional(readOnly = true)
    public ZoneViolationDTO getViolationById(UUID id) {
        return violationRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("Violation not found: " + id));
    }

    /**
     * Acknowledge a violation.
     */
    @Transactional
    public ZoneViolationDTO acknowledgeViolation(UUID violationId, String userId, String notes) {
        ZoneViolation violation = violationRepository.findById(violationId)
                .orElseThrow(() -> new RuntimeException("Violation not found: " + violationId));

        violation.setAcknowledged(true);
        violation.setAcknowledgedBy(userId);
        violation.setAcknowledgedAt(ZonedDateTime.now());
        violation.setResolutionNotes(notes);

        ZoneViolation saved = violationRepository.save(violation);
        logger.info("Violation {} acknowledged by {}", violationId, userId);
        
        return toDTO(saved);
    }

    /**
     * Get violation statistics for a tenant.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getViolationStatistics(String tenantCode, ZonedDateTime startDate, ZonedDateTime endDate) {
        List<Object[]> stats = violationRepository.getViolationStatsBySeverity(tenantCode, startDate, endDate);
        
        Map<String, Long> bySeverity = stats.stream()
                .collect(Collectors.toMap(
                    row -> row[0].toString(),
                    row -> (Long) row[1]
                ));

        long total = bySeverity.values().stream().mapToLong(Long::longValue).sum();
        long unacknowledged = violationRepository.countByTenantCodeAndAcknowledged(tenantCode, false);

        return Map.of(
            "total", total,
            "unacknowledged", unacknowledged,
            "bySeverity", bySeverity,
            "tenantCode", tenantCode,
            "startDate", startDate,
            "endDate", endDate
        );
    }

    /**
     * Get recent unacknowledged violations for alerts.
     */
    @Transactional(readOnly = true)
    public List<ZoneViolationDTO> getRecentUnacknowledgedViolations(String tenantCode, int limit) {
        return violationRepository.findByTenantCodeAndAcknowledgedOrderByTimestampDesc(tenantCode, false)
                .stream()
                .limit(limit)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Convert entity to DTO.
     */
    private ZoneViolationDTO toDTO(ZoneViolation entity) {
        Point entryLocation = entity.getEntryLocation();

        return ZoneViolationDTO.builder()
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
                .entryLatitude(entryLocation != null ? entryLocation.getY() : null)
                .entryLongitude(entryLocation != null ? entryLocation.getX() : null)
                .severity(entity.getSeverity() != null ? entity.getSeverity().name() : null)
                .timestamp(entity.getTimestamp())
                .durationSeconds(entity.getDurationSeconds() != null ? entity.getDurationSeconds().longValue() : null)
                .acknowledged(entity.getAcknowledged())
                .acknowledgedBy(entity.getAcknowledgedBy())
                .acknowledgedAt(entity.getAcknowledgedAt())
                .resolutionNotes(entity.getResolutionNotes())
                .tenantCode(entity.getTenantCode())
                .build();
    }
}
