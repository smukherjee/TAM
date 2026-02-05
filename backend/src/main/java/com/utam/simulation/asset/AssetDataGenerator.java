package com.utam.simulation.asset;

import com.utam.entity.Stand;
import com.utam.entity.VehicleType;
import com.utam.repository.StandRepository;
import com.utam.repository.VehicleTypeRepository;
import com.utam.simulation.config.SimulationConfig;
import com.utam.simulation.core.BaseDataGenerator;
import com.utam.simulation.security.BoundaryValidator;
// New imports for integration
import com.utam.asset.repository.AssetRepository;
import com.utam.tracking.repository.AssetLocationRegisterRepository;
import com.utam.tracking.domain.AssetLocationRegister;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generates realistic asset tracking data for GSE equipment.
 * Implements FR-060 to FR-070: Asset tracking with positions and status updates.
 * 
 * Data Flow: Generator -> NiFi (vehicle-ingest) -> Kafka (vehicle-raw-json) -> VehicleService -> WebSocket -> Frontend
 */
@Component
public class AssetDataGenerator extends BaseDataGenerator {

    private final SimAssetRepository simAssetRepository;
    private final AssetPositionRepository positionRepository;
    // New repositories
    private final AssetRepository assetRepository;
    private final AssetLocationRegisterRepository locationRegisterRepository;
    
    private final VehicleTypeRepository vehicleTypeRepository;
    private final StandRepository standRepository;
    private final RestTemplate restTemplate;
    private final Random random = new Random();
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    // Use same property as MockTelitGenerator for proper NiFi integration
    @Value("${simulation.vehicle-url}")
    private String nifiUrl;


    // T078: Optional boundary validator for position clamping
    @Autowired(required = false)
    private BoundaryValidator boundaryValidator;

    // Active assets being tracked
    private final Map<String, ActiveAsset> activeAssets = new ConcurrentHashMap<>();

    // Cached reference data
    private final Map<String, List<VehicleType>> vehicleTypes = new ConcurrentHashMap<>();
    private final Map<String, List<Stand>> stands = new ConcurrentHashMap<>();

    public AssetDataGenerator(SimulationConfig config, MeterRegistry meterRegistry,
                              SimAssetRepository simAssetRepository,
                              AssetPositionRepository positionRepository,
                              AssetRepository assetRepository,
                              AssetLocationRegisterRepository locationRegisterRepository,
                              VehicleTypeRepository vehicleTypeRepository,
                              StandRepository standRepository,
                              RestTemplate restTemplate) {
        super(config, meterRegistry);
        this.simAssetRepository = simAssetRepository;
        this.positionRepository = positionRepository;
        this.assetRepository = assetRepository;
        this.locationRegisterRepository = locationRegisterRepository;
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.standRepository = standRepository;
        this.restTemplate = restTemplate;
    }

    @Override
    public String getGeneratorName() {
        return "AssetDataGenerator";
    }

    @Override
    public String getEntityType() {
        return "Asset";
    }

    @PostConstruct
    public void init() {
        try {
            loadReferenceData();
            log.info("AssetDataGenerator initialized with {} tenant configurations", vehicleTypes.size());
        } catch (Exception e) {
            log.warn("AssetDataGenerator failed to load reference data: {}. Generator will use defaults.", e.getMessage());
        }
    }

    private void loadReferenceData() {
        // VehicleTypes are shared reference data - load once for all tenants
        List<VehicleType> allVehicleTypes = new java.util.ArrayList<>();
        try {
            allVehicleTypes = vehicleTypeRepository.findAll();
        } catch (Exception e) {
            log.warn("Failed to load vehicle types: {}", e.getMessage());
        }
        
        for (String tenantCode : config.getTenants().keySet()) {
            vehicleTypes.put(tenantCode, allVehicleTypes);
            try {
                stands.put(tenantCode, standRepository.findByTenantCode(tenantCode));
            } catch (Exception e) {
                log.warn("Failed to load stands for tenant {}: {}", tenantCode, e.getMessage());
                stands.put(tenantCode, new java.util.ArrayList<>());
            }
        }
    }

    @Override
    public int generateBatch(String tenantCode, int batchSize) {
        SimulationConfig.TenantConfig tenantConfig = config.getTenant(tenantCode);
        if (tenantConfig == null) {
            log.warn("No tenant config for: {}", tenantCode);
            return 0;
        }

        // Reload reference data if empty
        List<VehicleType> types = vehicleTypes.get(tenantCode);
        if (types == null || types.isEmpty()) {
            log.info("Reloading reference data for AssetDataGenerator...");
            loadReferenceData();
            types = vehicleTypes.get(tenantCode);
        }

        if (types == null || types.isEmpty()) {
            log.warn("No vehicle types loaded for tenant: {}", tenantCode);
            return 0;
        }

        int generated = 0;
        int skipped = 0;
        int assetsPerType = Math.max(1, batchSize / types.size());

        for (VehicleType type : types) {
            for (int i = 0; i < assetsPerType && generated < batchSize; i++) {
                Asset asset = createAsset(tenantCode, tenantConfig, type, i + 1);
                if (asset != null) {
                    // Check if asset already exists to avoid duplicate key errors
                    if (simAssetRepository.existsByAssetId(asset.getAssetId())) {
                        skipped++;
                        continue;
                    }
                    simAssetRepository.save(asset);
                    
                    // Sync to production tables
                    saveProductionAsset(asset);

                    // Create initial position record
                    AssetPosition position = createInitialPosition(asset);
                    positionRepository.save(position);
                    
                    // Update user-facing location register
                    updateLocationRegister(asset.getId(), asset.getAssetId(), tenantCode, 
                            asset.getLatitude(), asset.getLongitude(), position.getSpeed(), position.getHeading());
                    
                    registerActiveAsset(asset);
                    generated++;
                }
            }
        }

        if (skipped > 0) {
            log.info("Skipped {} existing assets for tenant {}", skipped, tenantCode);
        }
        log.info("Generated {} assets for tenant {} across {} types", 
                generated, tenantCode, types.size());
        return generated;
    }

    @Override
    public boolean generateSingleUpdate(String tenantCode) {
        List<Map<String, String>> batchPayload = new ArrayList<>();
        
        for (Map.Entry<String, ActiveAsset> entry : activeAssets.entrySet()) {
            ActiveAsset asset = entry.getValue();
            if (!asset.tenantCode.equals(tenantCode)) {
                continue;
            }

            // Update asset based on current state
            updateAssetPosition(asset);

            // Create NiFi payload for this asset
            Map<String, String> payload = new HashMap<>();
            payload.put("vehicle_no", asset.identifier);
            payload.put("vehicletype", asset.type != null ? asset.type : "GSE");
            payload.put("latitude", String.valueOf(asset.latitude));
            payload.put("longitude", String.valueOf(asset.longitude));
            payload.put("speed", String.valueOf(asset.speed));
            payload.put("status", "ACTIVE");
            // Send both keys to support different consumers/mappings
            payload.put("tenantCode", asset.tenantCode); 
            payload.put("tenant_code", asset.tenantCode);
            
            batchPayload.add(payload);
        }

        // Send batched updates to NiFi
        if (!batchPayload.isEmpty()) {
            try {
                restTemplate.postForObject(nifiUrl, batchPayload, String.class);
            } catch (Exception e) {
                 if (random.nextDouble() < 0.01) {
                    log.error("Failed to send asset batch update to NiFi: {}", e.getMessage());
                 }
            }
        }

        return true;
    }

    // Removed individual sendToNiFi method as it's now handled in batch


    private void updateAssetPosition(ActiveAsset asset) {
        switch (asset.state) {
            case STATIONARY -> {
                // Occasionally start moving
                if (random.nextDouble() < 0.02) { // 2% chance
                    asset.state = AssetState.MOVING;
                    Stand target = selectRandomStand(asset.tenantCode);
                    if (target != null) {
                        asset.targetLat = target.getLatitude();
                        asset.targetLon = target.getLongitude();
                    }
                }
            }
            case MOVING -> {
                moveTowardTarget(asset);
                if (isNearTarget(asset)) {
                    asset.state = AssetState.SERVICING;
                    asset.serviceStart = Instant.now();
                    asset.speed = 0;
                }
            }
            case SERVICING -> {
                long minutes = java.time.Duration.between(asset.serviceStart, Instant.now()).toMinutes();
                if (minutes >= asset.serviceDuration) {
                    asset.state = AssetState.STATIONARY;
                }
            }
        }

        // Add some GPS jitter to stationary assets
        if (asset.state == AssetState.STATIONARY || asset.state == AssetState.SERVICING) {
            asset.latitude += (random.nextDouble() - 0.5) * 0.00001;
            asset.longitude += (random.nextDouble() - 0.5) * 0.00001;
        }
    }

    private void moveTowardTarget(ActiveAsset asset) {
        double dx = asset.targetLon - asset.longitude;
        double dy = asset.targetLat - asset.latitude;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > 0.00005) {
            asset.heading = Math.toDegrees(Math.atan2(dx, dy));
            if (asset.heading < 0) asset.heading += 360;

            double speedDegrees = asset.maxSpeed / 111000.0 / 3600.0;
            double newLat = asset.latitude + Math.cos(Math.toRadians(asset.heading)) * speedDegrees;
            double newLon = asset.longitude + Math.sin(Math.toRadians(asset.heading)) * speedDegrees;
            
            // T078: Boundary validation - clamp position if outside boundary
            if (boundaryValidator != null) {
                BoundaryValidator.ClampedPosition clamped = boundaryValidator.clampToBoundary(
                    asset.tenantCode, newLat, newLon);
                asset.latitude = clamped.latitude();
                asset.longitude = clamped.longitude();
            } else {
                asset.latitude = newLat;
                asset.longitude = newLon;
            }
            
            asset.speed = asset.maxSpeed * (0.8 + random.nextDouble() * 0.4);
        }
    }

    private boolean isNearTarget(ActiveAsset asset) {
        double dx = asset.targetLon - asset.longitude;
        double dy = asset.targetLat - asset.latitude;
        return Math.sqrt(dx * dx + dy * dy) < 0.00005;
    }

    private Stand selectRandomStand(String tenantCode) {
        List<Stand> tenantStands = stands.get(tenantCode);
        if (tenantStands == null || tenantStands.isEmpty()) {
            return null;
        }
        return tenantStands.get(random.nextInt(tenantStands.size()));
    }

    private Asset createAsset(String tenantCode, SimulationConfig.TenantConfig tenantConfig,
                              VehicleType type, int index) {
        Asset asset = new Asset();
        asset.setId(UUID.randomUUID());
        asset.setTenantCode(tenantCode);

        // Generate asset ID
        asset.setAssetId(String.format("AST-%s-%s-%03d", tenantCode, type.getCode(), index));
        asset.setAssetName(type.getName() + " Asset " + index);
        asset.setAssetType(type.getCode());

        // Initial position near center
        asset.setLatitude(tenantConfig.getCenterLatitude() + (random.nextDouble() - 0.5) * 0.01);
        asset.setLongitude(tenantConfig.getCenterLongitude() + (random.nextDouble() - 0.5) * 0.01);

        asset.setStatus("ACTIVE");
        asset.setLastSeen(Instant.now());
        asset.setCreatedAt(Instant.now());

        return asset;
    }

    private AssetPosition createInitialPosition(Asset asset) {
        AssetPosition position = new AssetPosition();
        position.setId(UUID.randomUUID());
        position.setAssetId(asset.getId());
        position.setTenantCode(asset.getTenantCode());
        position.setLatitude(asset.getLatitude());
        position.setLongitude(asset.getLongitude());
        position.setSpeed(0.0);
        position.setHeading(random.nextDouble() * 360);
        position.setAltitude(0.0);
        position.setAccuracy(3.0);
        position.setTimestamp(Instant.now());
        position.setSource("GPS");
        return position;
    }

    private void registerActiveAsset(Asset asset) {
        ActiveAsset active = new ActiveAsset();
        active.assetId = asset.getId();
        active.identifier = asset.getAssetId();
        active.tenantCode = asset.getTenantCode();
        active.type = asset.getAssetType(); // Store type
        active.latitude = asset.getLatitude();
        active.longitude = asset.getLongitude();
        active.speed = 0;
        active.heading = random.nextDouble() * 360;
        active.state = AssetState.STATIONARY;
        active.maxSpeed = 25.0 + random.nextDouble() * 10;
        active.serviceDuration = 5 + random.nextInt(20);

        activeAssets.put(asset.getId().toString(), active);
    }

    private enum AssetState {
        STATIONARY, MOVING, SERVICING
    }

    private static class ActiveAsset {
        UUID assetId;
        String identifier;
        String tenantCode;
        String type; // Added type field
        double latitude;
        double longitude;
        double speed;
        double heading;
        double targetLat;
        double targetLon;
        AssetState state;
        double maxSpeed;
        int serviceDuration;
        Instant serviceStart;
    }

    // --- Sync Methods ---

    private void saveProductionAsset(Asset simAsset) {
        try {
            // Check if exists
            // Since this runs in batch, we might be re-seeding. 
            // We use simAsset.getId() which is randomized in createAsset, so these are new assets.
            
            com.utam.asset.domain.Asset prodAsset = new com.utam.asset.domain.Asset();
            prodAsset.setId(simAsset.getId());
            prodAsset.setAssetId(simAsset.getAssetId());
            prodAsset.setName(simAsset.getAssetName());
            // Use type as category for now. Could maintain a mapping.
            prodAsset.setCategory(mapTypeToCategory(simAsset.getAssetType()));
            prodAsset.setTenantCode(simAsset.getTenantCode());
            prodAsset.setStatus("Available"); // Default matching simulation
            prodAsset.setDescription("Simulated Asset " + simAsset.getAssetType());
            prodAsset.setCreatedAt(java.time.ZonedDateTime.now());
            prodAsset.setUpdatedAt(java.time.ZonedDateTime.now());
            
            assetRepository.save(prodAsset);
        } catch (Exception e) {
            log.error("Failed to sync production asset {}: {}", simAsset.getAssetId(), e.getMessage());
        }
    }
    
    private String mapTypeToCategory(String type) {
        // Simple mapping based on known types
        if (type.contains("FUEL")) return "Fueling";
        if (type.contains("CATERING")) return "Services";
        if (type.contains("BAGGAGE")) return "Ground Support";
        if (type.contains("BUS")) return "Transport";
        return "Other";
    }

    private void updateLocationRegister(UUID assetId, String identifier, String tenantCode, 
                                        Double lat, Double lon, Double speed, Double heading) {
        try {
            AssetLocationRegister reg = locationRegisterRepository.findById(assetId)
                .orElse(AssetLocationRegister.builder()
                    .assetId(assetId)
                    .assetIdentifier(identifier)
                    .tenantCode(tenantCode)
                    .build());
            
            if (reg.getAssetIdentifier() == null && identifier != null) {
                reg.setAssetIdentifier(identifier);
            }
            
            reg.setCurrentLatitude(lat);
            reg.setCurrentLongitude(lon);
            reg.setSpeed(speed);
            reg.setHeading(heading);
            
            if (lat != null && lon != null) {
                reg.setCurrentLocation(geometryFactory.createPoint(new Coordinate(lon, lat)));
            }
            reg.setLastUpdated(java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC));
            // Default zone info
            if (reg.getCurrentZone() == null) {
                reg.setCurrentZone("UNKNOWN");
                reg.setCurrentZoneName("Unknown");
                reg.setIsInRestrictedZone(false);
            }
            
            locationRegisterRepository.save(reg);
        } catch (Exception e) {
            log.error("Failed to update location register for {}: {}", assetId, e.getMessage());
        }
    }
}
