package com.utam.tracking.service;

import com.utam.tracking.dto.MovementDiscrepancyDTO;
import com.utam.tracking.dto.ZoneViolationDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service for broadcasting tracking events via WebSocket.
 * Feature: 005-asset-tracking-security
 * Task: T068
 */
@Service
public class TrackingWebSocketService {

    private static final Logger logger = LoggerFactory.getLogger(TrackingWebSocketService.class);

    private final SimpMessagingTemplate messagingTemplate;

    public TrackingWebSocketService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Broadcast a zone violation event.
     */
    public void broadcastViolation(ZoneViolationDTO violation) {
        String destination = "/topic/violations/" + violation.getTenantCode();
        
        Map<String, Object> payload = Map.of(
            "type", "ZONE_VIOLATION",
            "violation", violation,
            "severity", violation.getSeverity(),
            "assetId", violation.getAssetId().toString(),
            "assetIdentifier", violation.getAssetIdentifier(),
            "zoneName", violation.getZoneName(),
            "timestamp", violation.getTimestamp().toString()
        );

        try {
            messagingTemplate.convertAndSend(destination, payload);
            logger.debug("Broadcast violation to {}: assetId={}, zone={}", 
                destination, violation.getAssetIdentifier(), violation.getZoneName());
        } catch (Exception e) {
            logger.error("Failed to broadcast violation: {}", e.getMessage());
        }
    }

    /**
     * Broadcast a movement discrepancy event.
     */
    public void broadcastDiscrepancy(MovementDiscrepancyDTO discrepancy) {
        String destination = "/topic/discrepancies/" + discrepancy.getTenantCode();
        
        Map<String, Object> payload = Map.of(
            "type", "MOVEMENT_DISCREPANCY",
            "discrepancy", discrepancy,
            "discrepancyType", discrepancy.getDiscrepancyType(),
            "severity", discrepancy.getSeverity(),
            "assetId", discrepancy.getAssetId().toString(),
            "assetIdentifier", discrepancy.getAssetIdentifier(),
            "timestamp", discrepancy.getTimestamp().toString()
        );

        try {
            messagingTemplate.convertAndSend(destination, payload);
            logger.debug("Broadcast discrepancy to {}: assetId={}, type={}", 
                destination, discrepancy.getAssetIdentifier(), discrepancy.getDiscrepancyType());
        } catch (Exception e) {
            logger.error("Failed to broadcast discrepancy: {}", e.getMessage());
        }
    }

    /**
     * Broadcast violation acknowledged event.
     */
    public void broadcastViolationAcknowledged(ZoneViolationDTO violation) {
        String destination = "/topic/violations/" + violation.getTenantCode();
        
        Map<String, Object> payload = Map.of(
            "type", "VIOLATION_ACKNOWLEDGED",
            "violationId", violation.getId().toString(),
            "acknowledgedBy", violation.getAcknowledgedBy(),
            "acknowledgedAt", violation.getAcknowledgedAt().toString()
        );

        try {
            messagingTemplate.convertAndSend(destination, payload);
            logger.debug("Broadcast violation acknowledged: {}", violation.getId());
        } catch (Exception e) {
            logger.error("Failed to broadcast violation acknowledged: {}", e.getMessage());
        }
    }

    /**
     * Broadcast discrepancy acknowledged event.
     */
    public void broadcastDiscrepancyAcknowledged(MovementDiscrepancyDTO discrepancy) {
        String destination = "/topic/discrepancies/" + discrepancy.getTenantCode();
        
        Map<String, Object> payload = Map.of(
            "type", "DISCREPANCY_ACKNOWLEDGED",
            "discrepancyId", discrepancy.getId().toString(),
            "acknowledgedBy", discrepancy.getAcknowledgedBy(),
            "acknowledgedAt", discrepancy.getAcknowledgedAt().toString()
        );

        try {
            messagingTemplate.convertAndSend(destination, payload);
            logger.debug("Broadcast discrepancy acknowledged: {}", discrepancy.getId());
        } catch (Exception e) {
            logger.error("Failed to broadcast discrepancy acknowledged: {}", e.getMessage());
        }
    }

    /**
     * Broadcast critical alert for immediate attention.
     */
    public void broadcastCriticalAlert(String tenantCode, String alertType, Map<String, Object> data) {
        String destination = "/topic/alerts/" + tenantCode;
        
        Map<String, Object> payload = Map.of(
            "type", "CRITICAL_ALERT",
            "alertType", alertType,
            "data", data,
            "timestamp", java.time.ZonedDateTime.now().toString()
        );

        try {
            messagingTemplate.convertAndSend(destination, payload);
            logger.warn("Broadcast critical alert to {}: type={}", destination, alertType);
        } catch (Exception e) {
            logger.error("Failed to broadcast critical alert: {}", e.getMessage());
        }
    }
}
