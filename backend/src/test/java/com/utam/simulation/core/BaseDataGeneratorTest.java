package com.utam.simulation.core;

import com.utam.simulation.config.SimulationConfig;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BaseDataGenerator abstract class.
 */
class BaseDataGeneratorTest {

    private SimulationConfig config;
    private MeterRegistry meterRegistry;
    private TestDataGenerator generator;

    @BeforeEach
    void setUp() {
        config = createTestConfig();
        meterRegistry = new SimpleMeterRegistry();
        generator = new TestDataGenerator(config, meterRegistry);
    }

    @Test
    @DisplayName("Should initialize generator with correct name and type")
    void shouldInitializeWithCorrectNameAndType() {
        assertEquals("TestDataGenerator", generator.getGeneratorName());
        assertEquals("TestEntity", generator.getEntityType());
    }

    @Test
    @DisplayName("Should start and stop generator")
    void shouldStartAndStopGenerator() {
        assertFalse(generator.isRunning());
        
        generator.start();
        assertTrue(generator.isRunning());
        
        generator.stop();
        assertFalse(generator.isRunning());
    }

    @Test
    @DisplayName("Should track generation stats")
    void shouldTrackGenerationStats() {
        generator.start();
        
        // Generate batch
        int generated = generator.generateBatchWithMetrics("VIDP", 10);
        
        BaseDataGenerator.GeneratorStats stats = generator.getStats();
        assertEquals(generated, stats.recordsGenerated());
        assertTrue(stats.running());
    }

    @Test
    @DisplayName("Should validate tenant before generation")
    void shouldValidateTenantBeforeGeneration() {
        generator.start();
        
        // Valid tenant should work
        int generated = generator.generateBatchWithMetrics("VIDP", 5);
        assertTrue(generated >= 0);
        
        // Invalid tenant should return 0
        int invalidResult = generator.generateBatchWithMetrics("INVALID", 5);
        assertEquals(0, invalidResult);
    }

    @Test
    @DisplayName("Should not generate when stopped")
    void shouldNotGenerateWhenStopped() {
        // Generator not started
        int generated = generator.generateBatchWithMetrics("VIDP", 10);
        assertEquals(0, generated);
    }

    private SimulationConfig createTestConfig() {
        SimulationConfig config = new SimulationConfig();
        config.setEnabled(true);
        config.setBatchSize(100);
        
        SimulationConfig.TenantConfig vidp = new SimulationConfig.TenantConfig();
        vidp.setName("Test Airport");
        vidp.setTimezone("Asia/Kolkata");
        vidp.setCenterLatitude(28.5665);
        vidp.setCenterLongitude(77.1031);
        vidp.setFlightsPerHour(20);
        vidp.setGroundVehicleCount(100);
        
        config.getTenants().put("VIDP", vidp);
        
        return config;
    }

    /**
     * Test implementation of BaseDataGenerator for testing purposes.
     */
    @SuppressWarnings("unused") // generateCount reserved for test tracking
    private static class TestDataGenerator extends BaseDataGenerator {
        private int generateCount = 0;

        public TestDataGenerator(SimulationConfig config, MeterRegistry meterRegistry) {
            super(config, meterRegistry);
        }

        @Override
        public String getGeneratorName() {
            return "TestDataGenerator";
        }

        @Override
        public String getEntityType() {
            return "TestEntity";
        }

        @Override
        public int generateBatch(String tenantCode, int batchSize) {
            generateCount += batchSize;
            return batchSize;
        }

        @Override
        public boolean generateSingleUpdate(String tenantCode) {
            generateCount++;
            return true;
        }
    }
}
