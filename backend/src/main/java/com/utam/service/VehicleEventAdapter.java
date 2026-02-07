package com.utam.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.utam.model.Vehicle;
import com.utam.model.dto.TajSatsVehicleDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class VehicleEventAdapter {

    private static final Logger logger = LoggerFactory.getLogger(VehicleEventAdapter.class);
    private static final DateTimeFormatter TAJSATS_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss",
            Locale.ENGLISH);

    private final VehicleService vehicleService;
    private final ObjectMapper objectMapper;
    private final MinioService minioService;

    public VehicleEventAdapter(VehicleService vehicleService, ObjectMapper objectMapper, MinioService minioService) {
        this.vehicleService = vehicleService;
        this.objectMapper = objectMapper;
        this.minioService = minioService;
    }

    @KafkaListener(topics = "vehicle-raw-json", groupId = "utam-group")
    public void consumeVehicleEvent(String message) {
        try {
            logger.debug("Received raw vehicle message: {}", message);

            // Archive raw message first
            archiveMessage(message);

            List<Vehicle> vehicles = new ArrayList<>();
            JsonNode rootNode = objectMapper.readTree(message);

            if (rootNode.isArray()) {
                for (JsonNode node : rootNode) {
                    processSingleNode(node, vehicles);
                }
            } else {
                processSingleNode(rootNode, vehicles);
            }

            if (!vehicles.isEmpty()) {
                logger.info("Processed {} normalized vehicles from adapter", vehicles.size());
                vehicleService.processVehicles(vehicles);
            }

        } catch (Exception e) {
            logger.error("Error processing vehicle message in adapter: {}", message, e);
        }
    }

    private void processSingleNode(JsonNode node, List<Vehicle> vehicles) {
        try {
            // Check if it's TajSats format (look for specific fields)
            if (node.has("vehicle_No") || node.has("vehicle_Name")) {
                TajSatsVehicleDto dto = objectMapper.treeToValue(node, TajSatsVehicleDto.class);
                vehicles.add(mapTajSatsToVehicle(dto));
            } else {
                // Assume standard Vehicle format
                Vehicle vehicle = objectMapper.treeToValue(node, Vehicle.class);
                vehicles.add(vehicle);
            }
        } catch (Exception e) {
            logger.error("Failed to parse vehicle node: {}", node, e);
        }
    }

    private Vehicle mapTajSatsToVehicle(TajSatsVehicleDto dto) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(UUID.randomUUID());
        // Map vehicle_No to vehicleId
        vehicle.setVehicleId(dto.getVehicleNo());
        vehicle.setVehicleType(dto.getVehicleType());

        // Parse numeric fields
        vehicle.setLatitude(parseDouble(dto.getLatitude()));
        vehicle.setLongitude(parseDouble(dto.getLongitude()));
        vehicle.setSpeed(parseDouble(dto.getSpeed()));

        vehicle.setStatus(dto.getStatus());
        vehicle.setVehicleName(dto.getVehicleName());

        // Handle timestamps
        if (dto.getDatetime() != null) {
            try {
                LocalDateTime ldt = LocalDateTime.parse(dto.getDatetime(), TAJSATS_DATE_FORMATTER);
                vehicle.setTimestamp(ldt.atZone(ZoneId.systemDefault()).toInstant());
            } catch (Exception e) {
                logger.warn("Failed to parse datetime '{}' for vehicle {}", dto.getDatetime(), dto.getVehicleNo());
                vehicle.setTimestamp(Instant.now());
            }
        } else {
            vehicle.setTimestamp(Instant.now());
        }

        // Set default tenant if not present (TajSats usually doesn't send it, assume
        // VIDP or infer)
        if (vehicle.getTenantCode() == null) {
            vehicle.setTenantCode("VIDP");
        }

        return vehicle;
    }

    private Double parseDouble(String value) {
        if (value == null || value.isEmpty())
            return 0.0;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private void archiveMessage(String message) {
        try {
            // Extract tenant code if possible, default to VIDP
            String tenant = "VIDP";
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd/HH"));
            String filename = "archives/raw/" + tenant + "/" + timestamp + "/vehicle_" + UUID.randomUUID() + ".json";
            minioService.uploadJson(filename, message);
        } catch (Exception e) {
            logger.error("Failed to archive vehicle message", e);
        }
    }
}
