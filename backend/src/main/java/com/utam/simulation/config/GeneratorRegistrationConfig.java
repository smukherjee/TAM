package com.utam.simulation.config;

import com.utam.simulation.alert.AlertDataGenerator;
import com.utam.simulation.asset.AssetDataGenerator;
import com.utam.simulation.core.BaseDataGenerator;
import com.utam.simulation.core.GeneratorOrchestrator;
import com.utam.simulation.dispatch.DispatchDataGenerator;
import com.utam.simulation.financial.FinancialDataGenerator;
import com.utam.simulation.flight.FlightDataGenerator;
import com.utam.simulation.reference.BoundaryDataGenerator;
import com.utam.simulation.reference.DepotDataGenerator;
import com.utam.simulation.reference.StandDataGenerator;
import com.utam.simulation.reference.VehicleTypeDataGenerator;
import com.utam.simulation.turnaround.TurnaroundDataGenerator;
import com.utam.simulation.vehicle.VehicleDataGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * Configuration class that registers all data generators with the orchestrator.
 * Implements FR-005: Generator registration and coordination.
 */
@Configuration
public class GeneratorRegistrationConfig {

    private static final Logger log = LoggerFactory.getLogger(GeneratorRegistrationConfig.class);

    private final GeneratorOrchestrator orchestrator;
    private final List<BaseDataGenerator> generators;

    public GeneratorRegistrationConfig(
            GeneratorOrchestrator orchestrator,
            // Reference data generators
            StandDataGenerator standDataGenerator,
            DepotDataGenerator depotDataGenerator,
            VehicleTypeDataGenerator vehicleTypeDataGenerator,
            BoundaryDataGenerator boundaryDataGenerator,
            // Core entity generators
            FlightDataGenerator flightDataGenerator,
            VehicleDataGenerator vehicleDataGenerator,
            AssetDataGenerator assetDataGenerator,
            // Operational generators
            TurnaroundDataGenerator turnaroundDataGenerator,
            DispatchDataGenerator dispatchDataGenerator,
            AlertDataGenerator alertDataGenerator,
            // Financial generator
            FinancialDataGenerator financialDataGenerator
    ) {
        this.orchestrator = orchestrator;
        this.generators = List.of(
                // Reference generators (load first)
                standDataGenerator,
                depotDataGenerator,
                vehicleTypeDataGenerator,
                boundaryDataGenerator,
                // Core generators
                flightDataGenerator,
                vehicleDataGenerator,
                assetDataGenerator,
                // Operational generators
                turnaroundDataGenerator,
                dispatchDataGenerator,
                alertDataGenerator,
                // Financial (depends on others)
                financialDataGenerator
        );
    }

    @PostConstruct
    public void registerGenerators() {
        log.info("Registering {} generators with orchestrator...", generators.size());
        
        for (BaseDataGenerator generator : generators) {
            orchestrator.registerGenerator(generator);
            log.debug("Registered generator: {}", generator.getGeneratorName());
        }
        
        log.info("All generators registered successfully");
    }
}
