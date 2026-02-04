package com.utam.asset.consumer;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.utam.asset.event.AssetPositionEvent;
import com.utam.model.Vehicle;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class VehiclePositionBridge {

    private static final Logger logger = LoggerFactory.getLogger(VehiclePositionBridge.class);

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Counter forwardedCounter;
    private final Counter errorCounter;

    public VehiclePositionBridge(ObjectMapper objectMapper,
                                 KafkaTemplate<String, Object> kafkaTemplate,
                                 MeterRegistry registry) {
        // copy mapper so we can tweak deserialization without side effects
        this.objectMapper = objectMapper.copy();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.registerModule(new JavaTimeModule());
        this.kafkaTemplate = kafkaTemplate;
        this.forwardedCounter = Counter.builder("asset.tracking.events.forwarded")
                .tag("type", "vehicle_position")
                .description("Vehicle position events forwarded to asset-positions")
                .register(registry);
        this.errorCounter = Counter.builder("asset.tracking.events.forwarded.failed")
                .tag("type", "vehicle_position")
                .description("Failed vehicle position forwards")
                .register(registry);
    }

    @KafkaListener(
            topics = "vehicle-raw-json",
            groupId = "asset-position-forwarder",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void forwardVehiclePositions(String message) {
        try {
            List<Vehicle> vehicles;
            if (message.trim().startsWith("[")) {
                vehicles = Arrays.asList(objectMapper.readValue(message, Vehicle[].class));
            } else {
                vehicles = Collections.singletonList(objectMapper.readValue(message, Vehicle.class));
            }

            for (Vehicle vehicle : vehicles) {
                if (vehicle.getLatitude() == null || vehicle.getLongitude() == null) {
                    logger.debug("Skipping vehicle without coordinates: {}", vehicle.getVehicleId());
                    continue;
                }

                String tenant = vehicle.getTenantCode() != null ? vehicle.getTenantCode() : "VIDP";

                AssetPositionEvent event = AssetPositionEvent.builder()
                        .vehicleId(vehicle.getVehicleId())
                        .assetId(null)
                        .latitude(vehicle.getLatitude())
                        .longitude(vehicle.getLongitude())
                        .speed(vehicle.getSpeed())
                        .heading(null)
                        .status(vehicle.getStatus())
                        .timestamp(vehicle.getTimestamp() != null ? vehicle.getTimestamp() : Instant.now())
                        .tenantCode(tenant)
                        .build();

                kafkaTemplate.send("asset-positions-json", vehicle.getVehicleId(), event);
                forwardedCounter.increment();
            }
        } catch (Exception e) {
            logger.error("Failed to forward vehicle positions to asset-positions: {}", e.getMessage(), e);
            errorCounter.increment();
        }
    }
}
