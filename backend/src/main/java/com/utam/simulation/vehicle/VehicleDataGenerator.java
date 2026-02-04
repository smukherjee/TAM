package com.utam.simulation.vehicle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.utam.entity.Depot;
import com.utam.entity.Stand;
import com.utam.entity.VehicleType;
import com.utam.repository.DepotRepository;
import com.utam.repository.StandRepository;
import com.utam.repository.VehicleTypeRepository;
import com.utam.simulation.config.SimulationConfig;
import com.utam.simulation.core.BaseDataGenerator;
import com.utam.simulation.security.BoundaryValidator;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generates realistic GSE vehicle data for 15 vehicle types.
 * Implements FR-040 to FR-055: GSE fleet generation with all 15 types.
 * 
 * Data Flow: Generator -> NiFi (vehicle-ingest) -> Kafka (vehicle-raw-json) -> VehicleService -> WebSocket -> Frontend
 */
@Component
public class VehicleDataGenerator extends BaseDataGenerator {

    private final SimVehicleRepository vehicleRepository;
    private final VehiclePositionRepository vehiclePositionRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final StandRepository standRepository;
    private final DepotRepository depotRepository;
    private final RestTemplate restTemplate;
    
    @SuppressWarnings("unused") // Reserved for JSON serialization of complex vehicle data
    private final ObjectMapper objectMapper;
    private final Random random = new Random();

    // NiFi ingestion URL for vehicle data - same as MockTelitGenerator
    @Value("${simulation.vehicle-url}")
    private String nifiUrl;

    // T077: Optional boundary validator for position clamping
    @Autowired(required = false)
    private BoundaryValidator boundaryValidator;

    // Active vehicles being simulated
    private final Map<String, ActiveVehicle> activeVehicles = new ConcurrentHashMap<>();

    // Cached reference data
    private final Map<String, List<VehicleType>> vehicleTypes = new ConcurrentHashMap<>();
    private final Map<String, List<Stand>> stands = new ConcurrentHashMap<>();
    private final Map<String, List<Depot>> depots = new ConcurrentHashMap<>();

    // GSE vehicle naming prefixes
    private static final Map<String, String> VEHICLE_PREFIXES = Map.ofEntries(
            Map.entry("FUEL", "FT"),
            Map.entry("CATERING", "CT"),
            Map.entry("BAGGAGE_TUG", "BT"),
            Map.entry("BAGGAGE_CART", "BC"),
            Map.entry("BELT_LOADER", "BL"),
            Map.entry("GPU", "GP"),
            Map.entry("PUSHBACK", "PB"),
            Map.entry("STAIRS", "ST"),
            Map.entry("WATER", "WT"),
            Map.entry("LAVATORY", "LV"),
            Map.entry("DEICING", "DI"),
            Map.entry("ASU", "AS"),
            Map.entry("BUS", "BU"),
            Map.entry("CARGO", "CG"),
            Map.entry("AMBULIFT", "AM")
    );

    public VehicleDataGenerator(SimulationConfig config, MeterRegistry meterRegistry,
                                SimVehicleRepository vehicleRepository,
                                VehiclePositionRepository vehiclePositionRepository,
                                VehicleTypeRepository vehicleTypeRepository,
                                StandRepository standRepository,
                                DepotRepository depotRepository,
                                ObjectMapper objectMapper,
                                RestTemplate restTemplate) {
        super(config, meterRegistry);
        this.vehicleRepository = vehicleRepository;
        this.vehiclePositionRepository = vehiclePositionRepository;
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.standRepository = standRepository;
        this.depotRepository = depotRepository;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    @Override
    public String getGeneratorName() {
        return "VehicleDataGenerator";
    }

    @Override
    public String getEntityType() {
        return "Vehicle";
    }

    @PostConstruct
    public void init() {
        try {
            loadReferenceData();
            log.info("VehicleDataGenerator initialized with {} tenant configurations", vehicleTypes.size());
        } catch (Exception e) {
            log.warn("VehicleDataGenerator failed to load reference data: {}. Generator will use defaults.", e.getMessage());
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
            try {
                depots.put(tenantCode, depotRepository.findByTenantCode(tenantCode));
            } catch (Exception e) {
                log.warn("Failed to load depots for tenant {}: {}", tenantCode, e.getMessage());
                depots.put(tenantCode, new java.util.ArrayList<>());
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

        List<VehicleType> types = vehicleTypes.get(tenantCode);
        if (types == null || types.isEmpty()) {
            log.warn("No vehicle types loaded for tenant: {}", tenantCode);
            return 0;
        }

        int generated = 0;
        SimulationConfig.FleetConfig fleetConfig = config.getFleet();

        // Distribute vehicles across types according to fleet config
        int vehiclesPerType = Math.max(1, batchSize / types.size());

        for (VehicleType type : types) {
            int count = getFleetSizeForType(fleetConfig, type.getCode(), vehiclesPerType);
            
            for (int i = 0; i < count && generated < batchSize; i++) {
                Vehicle vehicle = createVehicle(tenantCode, tenantConfig, type, i + 1);
                if (vehicle != null) {
                    vehicleRepository.save(vehicle);
                    registerActiveVehicle(vehicle);
                    generated++;
                }
            }
        }

        log.info("Generated {} vehicles for tenant {} across {} types", 
                generated, tenantCode, types.size());
        return generated;
    }

    private int getFleetSizeForType(SimulationConfig.FleetConfig fleetConfig, 
                                    String typeCode, int defaultCount) {
        return switch (typeCode) {
            case "FUEL" -> fleetConfig.getFuelTrucks();
            case "CATERING" -> fleetConfig.getCateringTrucks();
            case "BAGGAGE_TUG" -> fleetConfig.getBaggageTugs();
            case "BAGGAGE_CART" -> fleetConfig.getBaggageCarts();
            case "BELT_LOADER" -> fleetConfig.getBeltLoaders();
            case "GPU" -> fleetConfig.getGpus();
            case "PUSHBACK" -> fleetConfig.getPushbackTractors();
            case "STAIRS" -> fleetConfig.getStairs();
            case "WATER" -> fleetConfig.getWaterTrucks();
            case "LAVATORY" -> fleetConfig.getLavatoryTrucks();
            case "DEICING" -> fleetConfig.getDeicingTrucks();
            case "ASU" -> fleetConfig.getAsus();
            case "BUS" -> fleetConfig.getBuses();
            case "CARGO" -> fleetConfig.getCargoLoaders();
            case "AMBULIFT" -> fleetConfig.getAmbulifts();
            default -> defaultCount;
        };
    }

    @Override
    public boolean generateSingleUpdate(String tenantCode) {
        List<com.utam.model.Vehicle> vehicleUpdates = new ArrayList<>();

        // Update positions of active vehicles
        for (Map.Entry<String, ActiveVehicle> entry : activeVehicles.entrySet()) {
            ActiveVehicle vehicle = entry.getValue();
            if (!vehicle.tenantCode.equals(tenantCode)) {
                continue;
            }

            // Update vehicle based on current state
            updateVehiclePosition(vehicle);

            // Save position update to simulation DB
            Vehicle vehicleEntity = vehicleRepository.findById(vehicle.vehicleId).orElse(null);
            if (vehicleEntity != null) {
                vehicleEntity.setLatitude(vehicle.latitude);
                vehicleEntity.setLongitude(vehicle.longitude);
                vehicleEntity.setSpeed(vehicle.speed);
                vehicleEntity.setHeading(vehicle.heading);
                vehicleEntity.setStatus(vehicle.status);
                vehicleEntity.setLastUpdated(Instant.now());
                vehicleRepository.save(vehicleEntity);

                // Create model.Vehicle for NiFi pipeline (WebSocket broadcast)
                com.utam.model.Vehicle nifiVehicle = new com.utam.model.Vehicle();
                nifiVehicle.setId(vehicleEntity.getId());
                nifiVehicle.setTenantCode(vehicle.tenantCode);
                nifiVehicle.setVehicleId(vehicleEntity.getVehicleId());
                nifiVehicle.setVehicleName(vehicleEntity.getVehicleName());
                nifiVehicle.setLatitude(vehicle.latitude);
                nifiVehicle.setLongitude(vehicle.longitude);
                nifiVehicle.setSpeed(vehicle.speed);
                nifiVehicle.setStatus(vehicle.status);
                nifiVehicle.setTimestamp(Instant.now());
                vehicleUpdates.add(nifiVehicle);
            }
        }

        // Send batched updates to NiFi for WebSocket broadcast
        if (!vehicleUpdates.isEmpty()) {
            try {
                restTemplate.postForObject(nifiUrl, vehicleUpdates, String.class);
            } catch (Exception e) {
                // Log at reduced frequency to avoid log spam
                if (random.nextDouble() < 0.01) {
                    log.error("Failed to send vehicle batch update to NiFi: {}", e.getMessage());
                }
            }
        }

        return true;
    }
    private void updateVehiclePosition(ActiveVehicle vehicle) {
        switch (vehicle.status) {
            case "IDLE" -> {
                // Occasionally dispatch to a stand
                if (random.nextDouble() < 0.05) { // 5% chance per tick
                    vehicle.status = "DISPATCHED";
                    vehicle.targetLat = selectRandomStand(vehicle.tenantCode).getLatitude();
                    vehicle.targetLon = selectRandomStand(vehicle.tenantCode).getLongitude();
                }
            }
            case "DISPATCHED", "EN_ROUTE" -> {
                // Move toward target
                moveTowardTarget(vehicle);
                if (isNearTarget(vehicle)) {
                    vehicle.status = "SERVICING";
                    vehicle.serviceStartTime = Instant.now();
                }
            }
            case "SERVICING" -> {
                // Check if service time is complete
                long serviceMinutes = java.time.Duration.between(vehicle.serviceStartTime, Instant.now()).toMinutes();
                if (serviceMinutes >= vehicle.serviceDuration) {
                    vehicle.status = "RETURNING";
                    // Return to depot
                    Depot depot = selectRandomDepot(vehicle.tenantCode);
                    vehicle.targetLat = depot.getLatitude();
                    vehicle.targetLon = depot.getLongitude();
                }
            }
            case "RETURNING" -> {
                moveTowardTarget(vehicle);
                if (isNearTarget(vehicle)) {
                    vehicle.status = "IDLE";
                    vehicle.speed = 0;
                }
            }
        }
    }

    private void moveTowardTarget(ActiveVehicle vehicle) {
        double dx = vehicle.targetLon - vehicle.longitude;
        double dy = vehicle.targetLat - vehicle.latitude;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > 0.0001) { // Still needs to move
            vehicle.heading = Math.toDegrees(Math.atan2(dx, dy));
            if (vehicle.heading < 0) vehicle.heading += 360;

            // Move at configured speed (converted from km/h to degrees/tick)
            double speedDegrees = vehicle.maxSpeed / 111000.0 / 3600.0; // Very rough conversion
            double newLat = vehicle.latitude + Math.cos(Math.toRadians(vehicle.heading)) * speedDegrees;
            double newLon = vehicle.longitude + Math.sin(Math.toRadians(vehicle.heading)) * speedDegrees;
            
            // T077: Boundary validation - clamp position if outside boundary
            if (boundaryValidator != null) {
                BoundaryValidator.ClampedPosition clamped = boundaryValidator.clampToBoundary(
                    vehicle.tenantCode, newLat, newLon);
                vehicle.latitude = clamped.latitude();
                vehicle.longitude = clamped.longitude();
                
                if (clamped.wasClamped()) {
                    // Position was clamped - adjust heading to stay within bounds
                    log.trace("Vehicle {} position clamped to boundary", vehicle.vehicleId);
                }
            } else {
                vehicle.latitude = newLat;
                vehicle.longitude = newLon;
            }
            
            vehicle.speed = vehicle.maxSpeed;
        }
    }

    private boolean isNearTarget(ActiveVehicle vehicle) {
        double dx = vehicle.targetLon - vehicle.longitude;
        double dy = vehicle.targetLat - vehicle.latitude;
        return Math.sqrt(dx * dx + dy * dy) < 0.0001;
    }

    private Stand selectRandomStand(String tenantCode) {
        List<Stand> tenantStands = stands.get(tenantCode);
        if (tenantStands == null || tenantStands.isEmpty()) {
            return null;
        }
        return tenantStands.get(random.nextInt(tenantStands.size()));
    }

    private Depot selectRandomDepot(String tenantCode) {
        List<Depot> tenantDepots = depots.get(tenantCode);
        if (tenantDepots == null || tenantDepots.isEmpty()) {
            return null;
        }
        return tenantDepots.get(random.nextInt(tenantDepots.size()));
    }

    private Vehicle createVehicle(String tenantCode, SimulationConfig.TenantConfig tenantConfig,
                                  VehicleType type, int index) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(UUID.randomUUID());
        vehicle.setTenantCode(tenantCode);

        // Generate vehicle ID
        String prefix = VEHICLE_PREFIXES.getOrDefault(type.getCode(), "GS");
        vehicle.setVehicleId(String.format("%s-%s-%03d", tenantCode, prefix, index));
        vehicle.setVehicleName(type.getName() + " " + index);
        // Convert Long ID to UUID using deterministic mapping for type reference
        vehicle.setVehicleTypeId(new UUID(0L, type.getId()));

        // Initial position at a depot
        Depot depot = selectRandomDepot(tenantCode);
        if (depot != null) {
            vehicle.setLatitude(depot.getLatitude() + (random.nextDouble() - 0.5) * 0.0001);
            vehicle.setLongitude(depot.getLongitude() + (random.nextDouble() - 0.5) * 0.0001);
        } else {
            vehicle.setLatitude(tenantConfig.getCenterLatitude() + (random.nextDouble() - 0.5) * 0.01);
            vehicle.setLongitude(tenantConfig.getCenterLongitude() + (random.nextDouble() - 0.5) * 0.01);
        }

        vehicle.setSpeed(0.0);
        vehicle.setHeading(random.nextDouble() * 360);
        vehicle.setStatus("IDLE");
        vehicle.setLastUpdated(Instant.now());
        vehicle.setCreatedAt(Instant.now());

        return vehicle;
    }

    private void registerActiveVehicle(Vehicle vehicle) {
        ActiveVehicle active = new ActiveVehicle();
        active.vehicleId = vehicle.getId();
        active.tenantCode = vehicle.getTenantCode();
        active.latitude = vehicle.getLatitude();
        active.longitude = vehicle.getLongitude();
        active.speed = vehicle.getSpeed();
        active.heading = vehicle.getHeading();
        active.status = vehicle.getStatus();
        active.maxSpeed = 30.0; // Default 30 km/h for GSE
        active.serviceDuration = 5 + random.nextInt(15); // 5-20 minutes

        activeVehicles.put(vehicle.getId().toString(), active);
    }

    /**
     * T070: Lightweight position update for real-time tracking.
     * Called at high frequency (every 500ms) by GeneratorOrchestrator.
     * Only updates positions for active (moving) vehicles, not idle ones.
     * This is more efficient than generateSingleUpdate for continuous simulation.
     */
    public void generatePositionUpdate(String tenantCode) {
        if (!isEnabled() || activeVehicles.isEmpty()) {
            return;
        }

        List<VehiclePosition> positionUpdates = new ArrayList<>();
        Instant now = Instant.now();

        // Process only vehicles for the specified tenant
        activeVehicles.values().stream()
            .filter(v -> v.tenantCode.equals(tenantCode))
            .filter(v -> !v.status.equals("IDLE")) // Only update moving vehicles
            .forEach(vehicle -> {
                // Update vehicle position based on status
                updateVehicleMovement(vehicle);

                // Create position record for persistence
                VehiclePosition position = new VehiclePosition();
                position.setId(UUID.randomUUID());
                position.setVehicleId(vehicle.vehicleId);
                position.setTenantCode(vehicle.tenantCode);
                position.setLatitude(vehicle.latitude);
                position.setLongitude(vehicle.longitude);
                position.setSpeed(vehicle.speed);
                position.setHeading(vehicle.heading);
                position.setStatus(vehicle.status);
                position.setRecordedAt(now);

                positionUpdates.add(position);
            });

        // Batch persist positions if we have any
        if (!positionUpdates.isEmpty()) {
            try {
                vehiclePositionRepository.saveAll(positionUpdates);
                log.trace("Updated {} vehicle positions for tenant {}", 
                         positionUpdates.size(), tenantCode);
            } catch (Exception e) {
                log.error("Failed to persist vehicle positions for tenant {}: {}", 
                         tenantCode, e.getMessage());
            }

            // Also send to NiFi for WebSocket broadcast to frontend
            sendToNifi(tenantCode, positionUpdates);
        }
    }

    /**
     * Send vehicle position updates to NiFi for WebSocket broadcast.
     * Converts VehiclePosition to model.Vehicle format expected by VehicleService consumer.
     */
    private void sendToNifi(String tenantCode, List<VehiclePosition> positions) {
        List<com.utam.model.Vehicle> nifiVehicles = new ArrayList<>();
        
        for (VehiclePosition pos : positions) {
            // Look up the vehicle entity for additional data
            Vehicle vehicleEntity = vehicleRepository.findById(pos.getVehicleId()).orElse(null);
            
            com.utam.model.Vehicle nifiVehicle = new com.utam.model.Vehicle();
            nifiVehicle.setId(pos.getId());
            nifiVehicle.setTenantCode(tenantCode);
            nifiVehicle.setVehicleId(vehicleEntity != null ? vehicleEntity.getVehicleId() : pos.getVehicleId().toString());
            nifiVehicle.setVehicleName(vehicleEntity != null ? vehicleEntity.getVehicleName() : null);
            nifiVehicle.setLatitude(pos.getLatitude());
            nifiVehicle.setLongitude(pos.getLongitude());
            nifiVehicle.setSpeed(pos.getSpeed());
            nifiVehicle.setStatus(pos.getStatus());
            nifiVehicle.setTimestamp(pos.getRecordedAt());
            nifiVehicles.add(nifiVehicle);
        }

        try {
            restTemplate.postForObject(nifiUrl, nifiVehicles, String.class);
        } catch (Exception e) {
            // Log at reduced frequency to avoid log spam
            if (random.nextDouble() < 0.01) {
                log.error("Failed to send position updates to NiFi: {}", e.getMessage());
            }
        }
    }

    /**
     * Internal movement update - refactored from updateVehiclePosition for reuse.
     * Updates the vehicle's position based on its current status and target.
     */
    private void updateVehicleMovement(ActiveVehicle vehicle) {
        switch (vehicle.status) {
            case "DISPATCHED", "EN_ROUTE" -> {
                moveTowardTarget(vehicle);
                if (isNearTarget(vehicle)) {
                    vehicle.status = "SERVICING";
                    vehicle.serviceStartTime = Instant.now();
                }
            }
            case "SERVICING" -> {
                // Service in progress - check if complete
                long serviceMinutes = java.time.Duration.between(
                    vehicle.serviceStartTime, Instant.now()).toMinutes();
                if (serviceMinutes >= vehicle.serviceDuration) {
                    vehicle.status = "RETURNING";
                    Depot depot = selectRandomDepot(vehicle.tenantCode);
                    if (depot != null) {
                        vehicle.targetLat = depot.getLatitude();
                        vehicle.targetLon = depot.getLongitude();
                    }
                }
            }
            case "RETURNING" -> {
                moveTowardTarget(vehicle);
                if (isNearTarget(vehicle)) {
                    vehicle.status = "IDLE";
                    vehicle.speed = 0;
                }
            }
        }
    }

    /**
     * Get count of active (non-idle) vehicles for a tenant.
     * Used for monitoring and metrics.
     */
    public long getActiveVehicleCount(String tenantCode) {
        return activeVehicles.values().stream()
            .filter(v -> v.tenantCode.equals(tenantCode))
            .filter(v -> !v.status.equals("IDLE"))
            .count();
    }

    /**
     * Get all active vehicles for a tenant (for real-time display).
     * Returns lightweight DTOs for API responses.
     */
    public List<VehiclePositionDTO> getActiveVehiclePositions(String tenantCode) {
        return activeVehicles.values().stream()
            .filter(v -> v.tenantCode.equals(tenantCode))
            .map(this::toPositionDTO)
            .toList();
    }

    private VehiclePositionDTO toPositionDTO(ActiveVehicle vehicle) {
        return VehiclePositionDTO.builder()
            .vehicleId(vehicle.vehicleId.toString())
            .latitude(vehicle.latitude)
            .longitude(vehicle.longitude)
            .speed(vehicle.speed)
            .heading(vehicle.heading)
            .status(vehicle.status)
            .build();
    }

    private static class ActiveVehicle {
        UUID vehicleId;
        String tenantCode;
        double latitude;
        double longitude;
        double speed;
        double heading;
        String status;
        double targetLat;
        double targetLon;
        double maxSpeed;
        int serviceDuration;
        Instant serviceStartTime;
    }
}
