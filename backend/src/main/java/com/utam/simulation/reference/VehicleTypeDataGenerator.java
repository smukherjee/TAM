package com.utam.simulation.reference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.utam.entity.VehicleType;
import com.utam.repository.VehicleTypeRepository;
import com.utam.simulation.config.SimulationConfig;
import com.utam.simulation.core.BaseDataGenerator;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;

/**
 * Generates vehicle type reference data.
 * Vehicle types define the 15 GSE categories (Fuel, Catering, Baggage, etc.).
 * This generator loads from any tenant's seed file since vehicle types are global.
 */
@Component
public class VehicleTypeDataGenerator extends BaseDataGenerator {

    private final VehicleTypeRepository vehicleTypeRepository;
    private final ObjectMapper objectMapper;

    public VehicleTypeDataGenerator(SimulationConfig config, MeterRegistry meterRegistry,
                                    VehicleTypeRepository vehicleTypeRepository, ObjectMapper objectMapper) {
        super(config, meterRegistry);
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getGeneratorName() {
        return "VehicleTypeDataGenerator";
    }

    @Override
    public String getEntityType() {
        return "VehicleType";
    }

    @PostConstruct
    public void init() {
        log.info("VehicleTypeDataGenerator initialized");
    }

    @Override
    public int generateBatch(String tenantCode, int batchSize) {
        // Vehicle types are global, so only generate once
        if (vehicleTypeRepository.count() > 0) {
            log.debug("Vehicle types already exist, skipping generation");
            return 0;
        }

        try {
            // Load from first available tenant seed file
            JsonNode rootNode = loadSeedData(tenantCode);
            if (rootNode == null) {
                log.warn("No seed data found for tenant: {}", tenantCode);
                return generateDefaultVehicleTypes();
            }

            JsonNode typesNode = rootNode.get("vehicleTypes");
            if (typesNode == null || !typesNode.isArray()) {
                log.warn("No vehicleTypes array in seed data, generating defaults");
                return generateDefaultVehicleTypes();
            }

            int count = 0;
            for (JsonNode typeNode : typesNode) {
                String code = typeNode.get("code").asText();
                
                if (vehicleTypeRepository.existsByCode(code)) {
                    continue;
                }

                VehicleType vehicleType = new VehicleType();
                vehicleType.setCode(code);
                vehicleType.setName(typeNode.get("name").asText());
                vehicleType.setMinSpeed(typeNode.has("minSpeed") ? typeNode.get("minSpeed").asInt() : 5);
                vehicleType.setMaxSpeed(typeNode.has("maxSpeed") ? typeNode.get("maxSpeed").asInt() : 30);
                vehicleType.setDefaultQuantity(typeNode.has("defaultQuantity") ? typeNode.get("defaultQuantity").asInt() : 10);
                vehicleType.setDefaultDepotType(typeNode.has("defaultDepotType") ? typeNode.get("defaultDepotType").asText() : null);
                vehicleType.setIconColor(typeNode.has("iconColor") ? typeNode.get("iconColor").asText() : "#607D8B");

                vehicleTypeRepository.save(vehicleType);
                count++;
            }

            log.info("Generated {} vehicle types", count);
            return count;

        } catch (Exception e) {
            log.error("Error generating vehicle types: {}", e.getMessage(), e);
            return 0;
        }
    }

    private int generateDefaultVehicleTypes() {
        // Generate the 15 GSE types as per FR-056
        Object[][] defaults = {
            {"FUEL", "Fuel Truck", 10, 30, 20, "FUEL", "#FF6B00"},
            {"CATERING", "Catering High-Lift", 5, 25, 15, "CATERING", "#4CAF50"},
            {"BAGGAGE_TUG", "Baggage Tractor", 10, 35, 30, "BAGGAGE", "#2196F3"},
            {"BAGGAGE_CART", "Baggage Cart", 0, 0, 100, "BAGGAGE", "#90CAF9"},
            {"BELT_LOADER", "Belt Loader", 5, 20, 25, "BAGGAGE", "#3F51B5"},
            {"GPU", "Ground Power Unit", 5, 25, 20, "GPU", "#FFC107"},
            {"PUSHBACK", "Pushback Tug", 5, 15, 15, "PUSHBACK", "#9C27B0"},
            {"STAIRS", "Passenger Stairs", 5, 20, 10, "STAIRS", "#607D8B"},
            {"WATER", "Water Service Truck", 10, 30, 8, "WATER", "#00BCD4"},
            {"LAVATORY", "Lavatory Service Truck", 10, 30, 8, "LAVATORY", "#795548"},
            {"DEICING", "De-icing Truck", 10, 25, 5, "MAINTENANCE", "#E91E63"},
            {"ASU", "Air Start Unit", 10, 30, 5, "MAINTENANCE", "#FF5722"},
            {"BUS", "Passenger Bus", 10, 40, 10, "BUS", "#009688"},
            {"CARGO", "Cargo Loader", 5, 20, 10, "CARGO", "#8BC34A"},
            {"AMBULIFT", "Ambulift Vehicle", 5, 20, 3, "MAINTENANCE", "#F44336"}
        };

        int count = 0;
        for (Object[] data : defaults) {
            String code = (String) data[0];
            if (vehicleTypeRepository.existsByCode(code)) {
                continue;
            }

            VehicleType vt = new VehicleType();
            vt.setCode(code);
            vt.setName((String) data[1]);
            vt.setMinSpeed((Integer) data[2]);
            vt.setMaxSpeed((Integer) data[3]);
            vt.setDefaultQuantity((Integer) data[4]);
            vt.setDefaultDepotType((String) data[5]);
            vt.setIconColor((String) data[6]);

            vehicleTypeRepository.save(vt);
            count++;
        }

        log.info("Generated {} default vehicle types", count);
        return count;
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
