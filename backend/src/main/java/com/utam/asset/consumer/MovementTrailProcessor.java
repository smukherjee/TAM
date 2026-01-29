package com.utam.asset.consumer;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.utam.asset.event.AssetPositionEvent;
import com.utam.asset.service.AssetService;
import com.utam.tracking.service.MovementTrailIngestionService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Kafka consumer for asset position events from NiFi pipeline.
 * Processes events from asset-positions-json topic to:
 * 1. Write to asset_movement_trail (for historical tracking)
 * 2. Update asset_location_register (current location)
 * 3. Detect zone violations using PostGIS spatial queries
 * 4. Detect movement discrepancies (status mismatch, unexpected movement)
 * 5. Broadcast WebSocket events to frontend
 * <p>
 * Feature: 005-asset-tracking-security (US1-US4)
 * Tasks: T026b, T026c, T041-T047
 */
@Service
public class MovementTrailProcessor {

    private static final Logger logger = LoggerFactory.getLogger(MovementTrailProcessor.class);
    
    private final JdbcTemplate jdbcTemplate;
    private final AssetService assetService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final MovementTrailIngestionService ingestionService;
    
    // Metrics
    private final Timer latencyTimer;
    private final Counter processedCounter;
    private final Counter errorCounter;
    private final Counter violationCounter;

    public MovementTrailProcessor(
            JdbcTemplate jdbcTemplate,
            AssetService assetService,
            SimpMessagingTemplate messagingTemplate,
            ObjectMapper objectMapper,
            MovementTrailIngestionService ingestionService,
            MeterRegistry registry) {
        this.jdbcTemplate = jdbcTemplate;
        this.assetService = assetService;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
        this.ingestionService = ingestionService;
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        // Initialize metrics
        this.latencyTimer = Timer.builder("asset.tracking.latency.seconds")
                .tag("type", "position_update")
                .description("End-to-end latency for asset position processing")
                .register(registry);
        
        this.processedCounter = Counter.builder("asset.tracking.events.processed")
                .tag("type", "position_update")
                .description("Number of successfully processed position events")
                .register(registry);
        
        this.errorCounter = Counter.builder("asset.tracking.events.failed")
                .tag("type", "position_update")
                .description("Number of failed position events")
                .register(registry);
        
        this.violationCounter = Counter.builder("asset.tracking.violations.detected")
                .tag("type", "zone")
                .description("Number of zone violations detected")
                .register(registry);
    }

    /**
     * Consume asset position events from Kafka.
     * Group ID: asset-tracking-consumer-group (matches 3 Kafka partitions for parallelism)
     * Concurrency: Configured in application.yml
     */
    @KafkaListener(
            topics = "asset-positions-json",
            groupId = "asset-tracking-consumer-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consumePositionEvent(String message) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Parse message (support both single object and array)
            List<AssetPositionEvent> events;
            if (message.trim().startsWith("[")) {
                events = Arrays.asList(objectMapper.readValue(message, AssetPositionEvent[].class));
            } else {
                AssetPositionEvent event = objectMapper.readValue(message, AssetPositionEvent.class);
                events = Collections.singletonList(event);
            }
            
            logger.debug("Received {} asset position events from Kafka", events.size());
            
            for (AssetPositionEvent event : events) {
                processPositionEvent(event);
                processedCounter.increment();
            }
            
            // Record latency
            long latency = System.currentTimeMillis() - startTime;
            latencyTimer.record(Duration.ofMillis(latency));
            
        } catch (Exception e) {
            logger.error("Failed to process asset position event: {}", e.getMessage(), e);
            errorCounter.increment();
        }
    }

    /**
     * Process a single position event.
     * Steps:
     * 1. Match vehicle_id → asset using qr_id (if not already matched)
     * 2. Delegate to ingestion service for zone detection, violation detection, discrepancy detection
     * 3. Broadcast WebSocket event
     */
    private void processPositionEvent(AssetPositionEvent event) {
        try {
            // Validate event data
            if (event.getLatitude() == null || event.getLongitude() == null) {
                logger.warn("Skipping event with null location: vehicleId={}", event.getVehicleId());
                return;
            }
            
            // Step 1: Match vehicle → asset via qr_id (if assetId not provided)
            UUID assetId = event.getAssetId();
            String assetIdentifier = null;
            String assetName = null;
            String assetCategory = null;
            
            if (assetId == null) {
                AssetInfo assetInfo = matchVehicleToAsset(event.getVehicleId(), event.getTenantCode());
                if (assetInfo == null) {
                    logger.debug("No asset matched for vehicle: {} (tenant: {})", 
                            event.getVehicleId(), event.getTenantCode());
                    return;
                }
                assetId = assetInfo.id;
                assetIdentifier = assetInfo.identifier;
                assetName = assetInfo.name;
                assetCategory = assetInfo.category;
            } else {
                // Fetch asset details for enrichment
                AssetInfo assetInfo = getAssetDetails(assetId);
                if (assetInfo != null) {
                    assetIdentifier = assetInfo.identifier;
                    assetName = assetInfo.name;
                    assetCategory = assetInfo.category;
                }
            }
            
            // Step 2: Delegate to ingestion service for processing
            // This handles movement trail writing, zone detection, violation/discrepancy detection, location register update
            ZonedDateTime eventTime = event.getTimestamp() != null 
                ? ZonedDateTime.ofInstant(event.getTimestamp(), ZoneOffset.UTC)
                : ZonedDateTime.now(ZoneOffset.UTC);
                
            ingestionService.processPositionUpdate(
                    assetId,
                    assetIdentifier,
                    assetName,
                    assetCategory,
                    event.getVehicleId(),
                    event.getLatitude(),
                    event.getLongitude(),
                    event.getSpeed(),
                    event.getHeading(),
                    event.getStatus(),
                    eventTime,
                    event.getTenantCode()
            );
            
            // Step 3: Broadcast WebSocket event to frontend
            broadcastPositionUpdate(assetId, event);
            
            logger.debug("Processed position event: assetId={}, vehicle={}, lat={}, lng={}", 
                    assetId, event.getVehicleId(), event.getLatitude(), event.getLongitude());
            
        } catch (Exception e) {
            logger.error("Failed to process position event for vehicle {}: {}", 
                    event.getVehicleId(), e.getMessage(), e);
            throw new RuntimeException("Position event processing failed", e);
        }
    }

    /**
     * Match vehicle_id to asset_id using qr_id.
     * Assumes vehicles.vehicle_id = assets.qr_id for tracking.
     */
    private AssetInfo matchVehicleToAsset(String vehicleId, String tenantCode) {
        try {
            String sql = "SELECT id, identifier, name, category FROM assets WHERE qr_id = ? AND tenant_code = ? LIMIT 1";
            List<AssetInfo> results = jdbcTemplate.query(
                    sql,
                    (rs, rowNum) -> new AssetInfo(
                            UUID.fromString(rs.getString("id")),
                            rs.getString("identifier"),
                            rs.getString("name"),
                            rs.getString("category")
                    ),
                    vehicleId,
                    tenantCode
            );
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            logger.error("Failed to match vehicle to asset: vehicleId={}, tenant={}, error={}", 
                    vehicleId, tenantCode, e.getMessage());
            return null;
        }
    }

    /**
     * Get asset details by ID
     */
    private AssetInfo getAssetDetails(UUID assetId) {
        try {
            String sql = "SELECT identifier, name, category FROM assets WHERE id = ? LIMIT 1";
            List<AssetInfo> results = jdbcTemplate.query(
                    sql,
                    (rs, rowNum) -> new AssetInfo(
                            assetId,
                            rs.getString("identifier"),
                            rs.getString("name"),
                            rs.getString("category")
                    ),
                    assetId
            );
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            logger.error("Failed to get asset details: assetId={}, error={}", 
                    assetId, e.getMessage());
            return null;
        }
    }

    /**
     * Broadcast position update via WebSocket to frontend.
     * Frontend subscribes to /topic/asset-tracking/{tenantCode}
     */
    private void broadcastPositionUpdate(UUID assetId, AssetPositionEvent event) {
        try {
            String destination = "/topic/asset-tracking/" + event.getTenantCode();
            
            // Build simple position update message
            var update = new java.util.HashMap<String, Object>();
            update.put("assetId", assetId.toString());
            update.put("vehicleId", event.getVehicleId());
            update.put("latitude", event.getLatitude());
            update.put("longitude", event.getLongitude());
            update.put("speed", event.getSpeed());
            update.put("heading", event.getHeading());
            update.put("status", event.getStatus());
            update.put("timestamp", event.getTimestamp());
            
            messagingTemplate.convertAndSend(destination, update);
            
        } catch (Exception e) {
            logger.warn("Failed to broadcast WebSocket update for asset {}: {}", 
                    assetId, e.getMessage());
            // Non-critical error, don't fail the whole processing
        }
    }

    /**
     * Helper class for asset information
     */
    private static class AssetInfo {
        final UUID id;
        final String identifier;
        final String name;
        final String category;

        AssetInfo(UUID id, String identifier, String name, String category) {
            this.id = id;
            this.identifier = identifier;
            this.name = name;
            this.category = category;
        }
    }
}
