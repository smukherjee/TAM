package com.tam.platform.context;

import io.micrometer.context.ThreadLocalAccessor;

/**
 * Accessor to allow Micrometer Context Propagation to capture and restore TenantContext.
 */
public class TenantContextAccessor implements ThreadLocalAccessor<TenantContext> {

    public static final String KEY = "tenantContext";

    @Override
    public Object key() {
        return KEY;
    }

    @Override
    public TenantContext getValue() {
        return TenantContextService.get();
    }

    @Override
    public void setValue(TenantContext value) {
        TenantContextService.set(value);
    }

    @Override
    public void reset() {
        TenantContextService.clear();
    }
}
