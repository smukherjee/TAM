package com.utam.simulation.reference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.utam.entity.AirportBoundary;
import com.utam.repository.AirportBoundaryRepository;
import com.utam.simulation.config.SimulationConfig;
import com.utam.simulation.core.BaseDataGenerator;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;

/**
 * Generates airport boundary data from seed files.
 * Boundaries are GeoJSON polygons defining airport perimeters for FR-028/FR-029.
 */
@Component
public class BoundaryDataGenerator extends BaseDataGenerator {

    private final AirportBoundaryRepository boundaryRepository;
    private final ObjectMapper objectMapper;

    public BoundaryDataGenerator(SimulationConfig config, MeterRegistry meterRegistry,
                                 AirportBoundaryRepository boundaryRepository, ObjectMapper objectMapper) {
        super(config, meterRegistry);
        this.boundaryRepository = boundaryRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getGeneratorName() {
        return "BoundaryDataGenerator";
    }

    @Override
    public String getEntityType() {
        return "AirportBoundary";
    }

    @PostConstruct
    public void init() {
        log.info("BoundaryDataGenerator initialized");
    }

    @Override
    public int generateBatch(String tenantCode, int batchSize) {
        if (boundaryRepository.existsByTenantCode(tenantCode)) {
            log.debug("Airport boundary already exists for tenant {}, skipping", tenantCode);
            return 0;
        }

        try {
            JsonNode rootNode = loadSeedData(tenantCode);
            if (rootNode == null) {
                log.warn("No seed data found for tenant: {}", tenantCode);
                return 0;
            }

            JsonNode boundaryNode = rootNode.get("boundary");
            if (boundaryNode == null) {
                log.warn("No boundary in seed data for tenant: {}", tenantCode);
                return 0;
            }

            String airportName = rootNode.has("airportName") 
                    ? rootNode.get("airportName").asText() 
                    : tenantCode + " Airport Boundary";

            AirportBoundary boundary = new AirportBoundary();
            boundary.setTenantCode(tenantCode);
            boundary.setName(airportName);
            boundary.setBoundaryGeoJson(objectMapper.writeValueAsString(boundaryNode));

            // Extract bounding box from coordinates
            JsonNode coordinates = boundaryNode.get("coordinates");
            if (coordinates != null && coordinates.isArray() && coordinates.size() > 0) {
                JsonNode ring = coordinates.get(0);
                if (ring != null && ring.isArray()) {
                    double minLat = Double.MAX_VALUE, maxLat = Double.MIN_VALUE;
                    double minLon = Double.MAX_VALUE, maxLon = Double.MIN_VALUE;

                    for (JsonNode point : ring) {
                        if (point.isArray() && point.size() >= 2) {
                            double lon = point.get(0).asDouble();
                            double lat = point.get(1).asDouble();
                            minLat = Math.min(minLat, lat);
                            maxLat = Math.max(maxLat, lat);
                            minLon = Math.min(minLon, lon);
                            maxLon = Math.max(maxLon, lon);
                        }
                    }

                    boundary.setMinLatitude(minLat);
                    boundary.setMaxLatitude(maxLat);
                    boundary.setMinLongitude(minLon);
                    boundary.setMaxLongitude(maxLon);
                }
            }

            boundaryRepository.save(boundary);
            log.info("Generated airport boundary for tenant: {}", tenantCode);
            return 1;

        } catch (Exception e) {
            log.error("Error generating boundary for tenant {}: {}", tenantCode, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public boolean generateSingleUpdate(String tenantCode) {
        return false; // Reference data doesn't need continuous updates
    }

    private JsonNode loadSeedData(String tenantCode) {
        String fileName = "data/" + tenantCode.toLowerCase() + ".json";
        try {
            ClassPathResource resource = new ClassPathResource(fileName);
            if (!resource.exists()) {
                return null;
            }
            try (InputStream is = resource.getInputStream()) {
                return objectMapper.readTree(is);
            }
        } catch (Exception e) {
            log.error("Error loading seed data from {}: {}", fileName, e.getMessage());
            return null;
        }
    }
}
