package com.utam.simulation.reference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.utam.entity.Stand;
import com.utam.repository.StandRepository;
import com.utam.simulation.config.SimulationConfig;
import com.utam.simulation.core.BaseDataGenerator;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;

/**
 * Generates stand reference data from seed files.
 * Stands are loaded from data/vidp.json and data/ybbn.json.
 */
@Component
public class StandDataGenerator extends BaseDataGenerator {

    private final StandRepository standRepository;
    private final ObjectMapper objectMapper;

    public StandDataGenerator(SimulationConfig config, MeterRegistry meterRegistry,
                              StandRepository standRepository, ObjectMapper objectMapper) {
        super(config, meterRegistry);
        this.standRepository = standRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getGeneratorName() {
        return "StandDataGenerator";
    }

    @Override
    public String getEntityType() {
        return "Stand";
    }

    @PostConstruct
    public void init() {
        // Auto-register with orchestrator is handled externally
        log.info("StandDataGenerator initialized");
    }

    @Override
    public int generateBatch(String tenantCode, int batchSize) {
        // Check if data already exists
        if (standRepository.countByTenantCode(tenantCode) > 0) {
            log.debug("Stands already exist for tenant {}, skipping generation", tenantCode);
            return 0;
        }

        try {
            JsonNode rootNode = loadSeedData(tenantCode);
            if (rootNode == null) {
                log.warn("No seed data found for tenant: {}", tenantCode);
                return 0;
            }

            JsonNode standsNode = rootNode.get("stands");
            if (standsNode == null || !standsNode.isArray()) {
                log.warn("No stands array in seed data for tenant: {}", tenantCode);
                return 0;
            }

            int count = 0;
            for (JsonNode standNode : standsNode) {
                Stand stand = new Stand();
                stand.setTenantCode(tenantCode);
                stand.setStandId(standNode.get("standId").asText());
                stand.setName(standNode.get("name").asText());
                stand.setLatitude(standNode.get("latitude").asDouble());
                stand.setLongitude(standNode.get("longitude").asDouble());
                stand.setTerminalId(standNode.has("terminalId") ? standNode.get("terminalId").asText() : null);
                stand.setStandType(standNode.has("standType") ? standNode.get("standType").asText() : "CONTACT");
                stand.setActive(true);

                standRepository.save(stand);
                count++;
            }

            log.info("Generated {} stands for tenant: {}", count, tenantCode);
            return count;

        } catch (Exception e) {
            log.error("Error generating stands for tenant {}: {}", tenantCode, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public boolean generateSingleUpdate(String tenantCode) {
        // Reference data doesn't need continuous updates
        return false;
    }

    private JsonNode loadSeedData(String tenantCode) {
        String fileName = "data/" + tenantCode.toLowerCase() + ".json";
        try {
            ClassPathResource resource = new ClassPathResource(fileName);
            if (!resource.exists()) {
                log.warn("Seed file not found: {}", fileName);
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
