package com.utam.simulation.tenant;

import com.utam.simulation.config.SimulationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-local context holder for tenant information.
 * Provides tenant isolation for multi-threaded operations.
 * Implements FR-028 to FR-031: Tenant data isolation.
 */
@Component
public class TenantContext {

    private static final Logger log = LoggerFactory.getLogger(TenantContext.class);

    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();
    private static final ConcurrentMap<String, TenantState> tenantStates = new ConcurrentHashMap<>();

    private final SimulationConfig config;

    public TenantContext(SimulationConfig config) {
        this.config = config;
    }

    @PostConstruct
    public void init() {
        // Initialize state for all configured tenants
        for (String tenantCode : config.getTenants().keySet()) {
            tenantStates.put(tenantCode, new TenantState(tenantCode));
        }
        log.info("TenantContext initialized with {} tenants", tenantStates.size());
    }

    /**
     * Set the current tenant for this thread.
     */
    public static void setCurrentTenant(String tenantCode) {
        currentTenant.set(tenantCode);
    }

    /**
     * Get the current tenant for this thread.
     */
    public static String getCurrentTenant() {
        return currentTenant.get();
    }

    /**
     * Clear the current tenant for this thread.
     */
    public static void clear() {
        currentTenant.remove();
    }

    /**
     * Execute a runnable with a specific tenant context.
     */
    public static void executeWithTenant(String tenantCode, Runnable task) {
        String previous = currentTenant.get();
        try {
            currentTenant.set(tenantCode);
            task.run();
        } finally {
            if (previous != null) {
                currentTenant.set(previous);
            } else {
                currentTenant.remove();
            }
        }
    }

    /**
     * Get tenant state.
     */
    public TenantState getTenantState(String tenantCode) {
        return tenantStates.computeIfAbsent(tenantCode, TenantState::new);
    }

    /**
     * Get current tenant's state.
     */
    public TenantState getCurrentTenantState() {
        String tenant = getCurrentTenant();
        if (tenant == null) {
            throw new IllegalStateException("No tenant context set");
        }
        return getTenantState(tenant);
    }

    /**
     * Tenant state holder.
     */
    public static class TenantState {
        private final String tenantCode;
        private volatile boolean simulationActive = false;
        private volatile long lastUpdateTimestamp = 0;
        private volatile long recordsGenerated = 0;

        public TenantState(String tenantCode) {
            this.tenantCode = tenantCode;
        }

        public String getTenantCode() {
            return tenantCode;
        }

        public boolean isSimulationActive() {
            return simulationActive;
        }

        public void setSimulationActive(boolean active) {
            this.simulationActive = active;
        }

        public long getLastUpdateTimestamp() {
            return lastUpdateTimestamp;
        }

        public void setLastUpdateTimestamp(long timestamp) {
            this.lastUpdateTimestamp = timestamp;
        }

        public long getRecordsGenerated() {
            return recordsGenerated;
        }

        public void incrementRecordsGenerated(int count) {
            this.recordsGenerated += count;
        }
    }
}
