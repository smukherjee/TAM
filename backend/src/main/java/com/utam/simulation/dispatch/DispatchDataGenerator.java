package com.utam.simulation.dispatch;

import com.utam.entity.Stand;
import com.utam.entity.VehicleType;
import com.utam.repository.StandRepository;
import com.utam.repository.VehicleTypeRepository;
import com.utam.simulation.config.SimulationConfig;
import com.utam.simulation.core.BaseDataGenerator;
import com.utam.simulation.vehicle.Vehicle;
import com.utam.simulation.vehicle.SimVehicleRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generates realistic dispatch assignments.
 * Implements FR-081 to FR-090: Dispatch assignment simulation.
 */
@Component
public class DispatchDataGenerator extends BaseDataGenerator {

    private final DispatchRepository dispatchRepository;
    private final SimVehicleRepository vehicleRepository;
    private final StandRepository standRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final Random random = new Random();

    // Active dispatches being simulated
    private final Map<UUID, ActiveDispatch> activeDispatches = new ConcurrentHashMap<>();

    // Cached reference data
    private final Map<String, List<Vehicle>> vehicles = new ConcurrentHashMap<>();
    private final Map<String, List<Stand>> stands = new ConcurrentHashMap<>();
    private final Map<String, List<VehicleType>> vehicleTypes = new ConcurrentHashMap<>();

    private static final List<String> PRIORITIES = List.of("LOW", "NORMAL", "NORMAL", "NORMAL", "HIGH", "URGENT");

    public DispatchDataGenerator(SimulationConfig config, MeterRegistry meterRegistry,
                                 DispatchRepository dispatchRepository,
                                 SimVehicleRepository vehicleRepository,
                                 StandRepository standRepository,
                                 VehicleTypeRepository vehicleTypeRepository) {
        super(config, meterRegistry);
        this.dispatchRepository = dispatchRepository;
        this.vehicleRepository = vehicleRepository;
        this.standRepository = standRepository;
        this.vehicleTypeRepository = vehicleTypeRepository;
    }

    @Override
    public String getGeneratorName() {
        return "DispatchDataGenerator";
    }

    @Override
    public String getEntityType() {
        return "Dispatch";
    }

    @PostConstruct
    public void init() {
        log.info("DispatchDataGenerator initialized");
    }

    public void refreshReferenceData() {
        // VehicleTypes are shared reference data - load once for all tenants
        List<VehicleType> allVehicleTypes = new java.util.ArrayList<>();
        try {
            allVehicleTypes = vehicleTypeRepository.findAll();
        } catch (Exception e) {
            log.warn("Failed to load vehicle types: {}", e.getMessage());
        }
        
        for (String tenantCode : config.getTenants().keySet()) {
            try {
                vehicles.put(tenantCode, vehicleRepository.findByTenantCode(tenantCode));
            } catch (Exception e) {
                log.warn("Failed to load vehicles for tenant {}: {}", tenantCode, e.getMessage());
                vehicles.put(tenantCode, new java.util.ArrayList<>());
            }
            try {
                stands.put(tenantCode, standRepository.findByTenantCode(tenantCode));
            } catch (Exception e) {
                log.warn("Failed to load stands for tenant {}: {}", tenantCode, e.getMessage());
                stands.put(tenantCode, new java.util.ArrayList<>());
            }
            vehicleTypes.put(tenantCode, allVehicleTypes);
        }
    }

    @Override
    public int generateBatch(String tenantCode, int batchSize) {
        // Refresh reference data
        refreshReferenceData();

        SimulationConfig.TenantConfig tenantConfig = config.getTenant(tenantCode);
        if (tenantConfig == null) {
            log.warn("No tenant config for: {}", tenantCode);
            return 0;
        }

        List<Vehicle> tenantVehicles = vehicles.get(tenantCode);
        List<Stand> tenantStands = stands.get(tenantCode);

        if (tenantVehicles == null || tenantVehicles.isEmpty()) {
            log.warn("No vehicles loaded for tenant: {}", tenantCode);
            return 0;
        }
        if (tenantStands == null || tenantStands.isEmpty()) {
            log.warn("No stands loaded for tenant: {}", tenantCode);
            return 0;
        }

        int generated = 0;
        Instant baseTime = Instant.now().minus(4, ChronoUnit.HOURS);

        for (int i = 0; i < batchSize; i++) {
            Dispatch dispatch = createDispatch(tenantCode, tenantVehicles, tenantStands, baseTime, i);
            if (dispatch != null) {
                dispatchRepository.save(dispatch);
                
                // Register if active
                if (!List.of("COMPLETED", "CANCELLED").contains(dispatch.getStatus())) {
                    registerActiveDispatch(dispatch);
                }
                generated++;
            }
        }

        log.info("Generated {} dispatches for tenant {}", generated, tenantCode);
        return generated;
    }

    @Override
    public boolean generateSingleUpdate(String tenantCode) {
        List<UUID> toRemove = new ArrayList<>();

        for (Map.Entry<UUID, ActiveDispatch> entry : activeDispatches.entrySet()) {
            ActiveDispatch active = entry.getValue();
            if (!active.tenantCode.equals(tenantCode)) {
                continue;
            }

            // Progress the dispatch
            progressDispatch(active);

            // Update database
            dispatchRepository.findById(active.dispatchId).ifPresent(dispatch -> {
                dispatch.setStatus(active.status);
                
                if ("ARRIVED".equals(active.status) && dispatch.getArrivedAt() == null) {
                    dispatch.setArrivedAt(Instant.now());
                }
                
                if ("COMPLETED".equals(active.status)) {
                    dispatch.setCompletedAt(Instant.now());
                    if (dispatch.getDispatchedAt() != null) {
                        long seconds = java.time.Duration.between(
                                dispatch.getDispatchedAt(), Instant.now()).getSeconds();
                        dispatch.setActualDurationSeconds((int) seconds);
                        
                        if (dispatch.getEstimatedDurationSeconds() != null) {
                            dispatch.setDelaySeconds((int) seconds - dispatch.getEstimatedDurationSeconds());
                        }
                    }
                }
                
                dispatchRepository.save(dispatch);
            });

            if ("COMPLETED".equals(active.status) || "CANCELLED".equals(active.status)) {
                toRemove.add(entry.getKey());
            }
        }

        toRemove.forEach(activeDispatches::remove);

        // Occasionally create new dispatches
        if (random.nextDouble() < 0.15) { // 15% chance per tick
            refreshReferenceData();
            List<Vehicle> tenantVehicles = vehicles.get(tenantCode);
            List<Stand> tenantStands = stands.get(tenantCode);
            
            if (tenantVehicles != null && !tenantVehicles.isEmpty() &&
                tenantStands != null && !tenantStands.isEmpty()) {
                Dispatch dispatch = createDispatch(tenantCode, tenantVehicles, tenantStands, Instant.now(), 0);
                if (dispatch != null) {
                    dispatch.setStatus("EN_ROUTE");
                    dispatch.setDispatchedAt(Instant.now());
                    dispatchRepository.save(dispatch);
                    registerActiveDispatch(dispatch);
                }
            }
        }

        return true;
    }

    private void progressDispatch(ActiveDispatch active) {
        long secondsSincePhaseStart = java.time.Duration.between(
                active.phaseStartTime, Instant.now()).getSeconds();

        switch (active.status) {
            case "PENDING" -> {
                if (secondsSincePhaseStart >= 30) { // 30s pending
                    active.status = "EN_ROUTE";
                    active.phaseStartTime = Instant.now();
                }
            }
            case "EN_ROUTE" -> {
                if (secondsSincePhaseStart >= active.travelTimeSeconds) {
                    active.status = "ARRIVED";
                    active.phaseStartTime = Instant.now();
                }
            }
            case "ARRIVED" -> {
                if (secondsSincePhaseStart >= 60) { // 60s to start servicing
                    active.status = "SERVICING";
                    active.phaseStartTime = Instant.now();
                }
            }
            case "SERVICING" -> {
                if (secondsSincePhaseStart >= active.serviceTimeSeconds) {
                    active.status = "COMPLETED";
                }
            }
        }
    }

    private Dispatch createDispatch(String tenantCode, List<Vehicle> tenantVehicles,
                                    List<Stand> tenantStands, Instant baseTime, int index) {
        Dispatch dispatch = new Dispatch();
        dispatch.setId(UUID.randomUUID());
        dispatch.setTenantCode(tenantCode);

        // Select random vehicle
        Vehicle vehicle = tenantVehicles.get(random.nextInt(tenantVehicles.size()));
        dispatch.setVehicleId(vehicle.getId());
        dispatch.setVehicleType(vehicle.getVehicleId().split("-")[1]); // Extract type code

        // Select random stand
        Stand stand = tenantStands.get(random.nextInt(tenantStands.size()));
        dispatch.setStandCode(stand.getStandId());

        // Timing
        Instant createdAt = baseTime.plus(index * 10L + random.nextInt(5), ChronoUnit.MINUTES);
        dispatch.setCreatedAt(createdAt);
        dispatch.setDispatchedAt(createdAt.plus(30, ChronoUnit.SECONDS));

        // Duration estimates
        int estimatedSeconds = 120 + random.nextInt(180); // 2-5 minutes
        dispatch.setEstimatedDurationSeconds(estimatedSeconds);

        // Status based on time
        long secondsSinceCreated = java.time.Duration.between(createdAt, Instant.now()).getSeconds();
        if (secondsSinceCreated < 0) {
            dispatch.setStatus("PENDING");
        } else if (secondsSinceCreated < estimatedSeconds) {
            dispatch.setStatus(secondsSinceCreated < 60 ? "EN_ROUTE" : "SERVICING");
        } else {
            dispatch.setStatus("COMPLETED");
            dispatch.setCompletedAt(createdAt.plus(estimatedSeconds + random.nextInt(60), ChronoUnit.SECONDS));
            dispatch.setActualDurationSeconds(estimatedSeconds + random.nextInt(60) - 30);
            dispatch.setDelaySeconds(dispatch.getActualDurationSeconds() - estimatedSeconds);
        }

        dispatch.setPriority(PRIORITIES.get(random.nextInt(PRIORITIES.size())));

        return dispatch;
    }

    private void registerActiveDispatch(Dispatch dispatch) {
        ActiveDispatch active = new ActiveDispatch();
        active.dispatchId = dispatch.getId();
        active.tenantCode = dispatch.getTenantCode();
        active.status = dispatch.getStatus();
        active.phaseStartTime = Instant.now();
        active.travelTimeSeconds = 60 + random.nextInt(120); // 1-3 min travel
        active.serviceTimeSeconds = 180 + random.nextInt(300); // 3-8 min service

        activeDispatches.put(dispatch.getId(), active);
    }

    private static class ActiveDispatch {
        UUID dispatchId;
        String tenantCode;
        String status;
        Instant phaseStartTime;
        int travelTimeSeconds;
        int serviceTimeSeconds;
    }
}
