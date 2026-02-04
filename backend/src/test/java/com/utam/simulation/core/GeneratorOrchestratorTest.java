package com.utam.simulation.core;

import com.utam.simulation.config.SimulationConfig;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GeneratorOrchestrator.
 */
class GeneratorOrchestratorTest {

    private SimulationConfig config;
    private MeterRegistry meterRegistry;
    private GeneratorOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        config = createTestConfig();
        meterRegistry = new SimpleMeterRegistry();
        orchestrator = new GeneratorOrchestrator(config, meterRegistry);
    }

    @Test
    @DisplayName("Should register and unregister generators")
    void shouldRegisterAndUnregisterGenerators() {
        TestGenerator generator = new TestGenerator(config, meterRegistry, "Test1");
        
        orchestrator.registerGenerator(generator);
        assertTrue(orchestrator.getRegisteredGenerators().contains("Test1Generator"));
        
        orchestrator.unregisterGenerator("Test1Generator");
        assertFalse(orchestrator.getRegisteredGenerators().contains("Test1Generator"));
    }

    @Test
    @DisplayName("Should start and stop continuous simulation")
    void shouldStartAndStopContinuousSimulation() {
        TestGenerator generator = new TestGenerator(config, meterRegistry, "Test");
        orchestrator.registerGenerator(generator);
        
        orchestrator.startContinuousSimulation();
        assertTrue(orchestrator.getStatus().continuousMode());
        
        orchestrator.stopContinuousSimulation();
        assertFalse(orchestrator.getStatus().continuousMode());
    }

    @Test
    @DisplayName("Should start batch population")
    void shouldStartBatchPopulation() {
        TestGenerator generator = new TestGenerator(config, meterRegistry, "Test");
        orchestrator.registerGenerator(generator);
        
        GeneratorOrchestrator.BatchPopulationResult result = 
                orchestrator.startBatchPopulation("VIDP");
        
        assertTrue(result.success());
        assertFalse(result.recordsGenerated().isEmpty());
    }

    @Test
    @DisplayName("Should report orchestrator status")
    void shouldReportOrchestratorStatus() {
        TestGenerator generator1 = new TestGenerator(config, meterRegistry, "Test1");
        TestGenerator generator2 = new TestGenerator(config, meterRegistry, "Test2");
        
        orchestrator.registerGenerator(generator1);
        orchestrator.registerGenerator(generator2);
        
        GeneratorOrchestrator.OrchestratorStatus status = orchestrator.getStatus();
        
        assertEquals(2, status.totalGenerators());
        assertFalse(status.continuousMode());
        assertFalse(status.batchMode());
    }

    @Test
    @DisplayName("Should start specific generator")
    void shouldStartSpecificGenerator() {
        TestGenerator generator = new TestGenerator(config, meterRegistry, "Test");
        orchestrator.registerGenerator(generator);
        
        assertTrue(orchestrator.startGenerator("TestGenerator"));
        assertTrue(generator.isRunning());
        
        assertTrue(orchestrator.stopGenerator("TestGenerator"));
        assertFalse(generator.isRunning());
    }

    @Test
    @DisplayName("Should return false for non-existent generator")
    void shouldReturnFalseForNonExistentGenerator() {
        assertFalse(orchestrator.startGenerator("NonExistent"));
        assertFalse(orchestrator.stopGenerator("NonExistent"));
    }

    @Test
    @DisplayName("Should check health status")
    void shouldCheckHealthStatus() {
        // Not in continuous mode - should be healthy
        assertTrue(orchestrator.isHealthy());
        
        TestGenerator generator = new TestGenerator(config, meterRegistry, "Test");
        orchestrator.registerGenerator(generator);
        
        orchestrator.startContinuousSimulation();
        // Generator started, should be healthy
        assertTrue(orchestrator.isHealthy());
        
        orchestrator.shutdown();
    }

    @Test
    @DisplayName("Should shutdown gracefully")
    void shouldShutdownGracefully() {
        TestGenerator generator = new TestGenerator(config, meterRegistry, "Test");
        orchestrator.registerGenerator(generator);
        generator.start();
        
        orchestrator.shutdown();
        
        assertFalse(generator.isRunning());
    }

    private SimulationConfig createTestConfig() {
        SimulationConfig config = new SimulationConfig();
        config.setEnabled(true);
        config.setBatchSize(100);
        config.setContinuousEnabled(true);
        
        SimulationConfig.TenantConfig vidp = new SimulationConfig.TenantConfig();
        vidp.setName("Test Airport");
        vidp.setTimezone("Asia/Kolkata");
        vidp.setCenterLatitude(28.5665);
        vidp.setCenterLongitude(77.1031);
        
        config.getTenants().put("VIDP", vidp);
        
        return config;
    }

    /**
     * Test generator implementation.
     */
    private static class TestGenerator extends BaseDataGenerator {
        private final String name;

        public TestGenerator(SimulationConfig config, MeterRegistry meterRegistry, String name) {
            super(config, meterRegistry);
            this.name = name;
        }

        @Override
        public String getGeneratorName() {
            return name + "Generator";
        }

        @Override
        public String getEntityType() {
            return name + "Entity";
        }

        @Override
        public int generateBatch(String tenantCode, int batchSize) {
            return batchSize;
        }

        @Override
        public boolean generateSingleUpdate(String tenantCode) {
            return true;
        }
    }
}
