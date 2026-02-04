package com.utam.simulation.reference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.utam.entity.Depot;
import com.utam.repository.DepotRepository;
import com.utam.simulation.config.SimulationConfig;
import com.utam.simulation.core.BaseDataGenerator;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;

/**
 * Generates depot reference data from seed files.
 * Depots are GSE storage/dispatch locations (Fuel Farm, Baggage Hall, etc.).
 */
@Component
public class DepotDataGenerator extends BaseDataGenerator {

    private final DepotRepository depotRepository;
    private final ObjectMapper objectMapper;

    public DepotDataGenerator(SimulationConfig config, MeterRegistry meterRegistry,
                              DepotRepository depotRepository, ObjectMapper objectMapper) {
        super(config, meterRegistry);
        this.depotRepository = depotRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getGeneratorName() {
        return "DepotDataGenerator";
    }

    @Override
    public String getEntityType() {
        return "Depot";
    }

    @PostConstruct
    public void init() {
        log.info("DepotDataGenerator initialized");
    }

    @Override
    public int generateBatch(String tenantCode, int batchSize) {
        if (depotRepository.countByTenantCode(tenantCode) > 0) {
            log.debug("Depots already exist for tenant {}, skipping generation", tenantCode);
            return 0;
        }

        try {
            JsonNode rootNode = loadSeedData(tenantCode);
            if (rootNode == null) {
                log.warn("No seed data found for tenant: {}", tenantCode);
                return 0;
            }

            JsonNode depotsNode = rootNode.get("depots");
            if (depotsNode == null || !depotsNode.isArray()) {
                log.warn("No depots array in seed data for tenant: {}", tenantCode);
                return 0;
            }

            int count = 0;
            for (JsonNode depotNode : depotsNode) {
                Depot depot = new Depot();
                depot.setTenantCode(tenantCode);
                depot.setDepotType(depotNode.get("depotType").asText());
                depot.setName(depotNode.get("name").asText());
                depot.setLatitude(depotNode.get("latitude").asDouble());
                depot.setLongitude(depotNode.get("longitude").asDouble());
                depot.setCapacity(depotNode.has("capacity") ? depotNode.get("capacity").asInt() : 10);
                depot.setActive(true);

                depotRepository.save(depot);
                count++;
            }

            log.info("Generated {} depots for tenant: {}", count, tenantCode);
            return count;

        } catch (Exception e) {
            log.error("Error generating depots for tenant {}: {}", tenantCode, e.getMessage(), e);
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
