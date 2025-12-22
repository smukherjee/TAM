package com.tam.platform.context;

import org.springframework.core.task.TaskDecorator;

public class ContextAwareTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        TenantContext context = TenantContextService.get();
        return () -> {
            try {
                TenantContextService.set(context);
                runnable.run();
            } finally {
                TenantContextService.clear();
            }
        };
    }
}
