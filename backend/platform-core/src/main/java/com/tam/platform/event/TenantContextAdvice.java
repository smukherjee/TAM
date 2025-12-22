package com.tam.platform.event;

import com.tam.platform.context.TenantContext;
import com.tam.platform.context.TenantContextService;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * AOP Advice to extract TenantContext from Kafka headers and set it in TenantContextService.
 */
public class TenantContextAdvice implements MethodInterceptor {

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        ConsumerRecord<?, ?> record = null;
        for (Object arg : invocation.getArguments()) {
            if (arg instanceof ConsumerRecord) {
                record = (ConsumerRecord<?, ?>) arg;
                break;
            }
        }

        if (record != null) {
            String tenantId = getHeader(record, "X-Tenant-ID");
            String domainId = getHeader(record, "X-Domain-ID");
            String correlationId = getHeader(record, "X-Correlation-ID");

            TenantContext context = new TenantContext(tenantId, domainId, Collections.<String>emptySet(), correlationId, null, null, null);
            TenantContextService.set(context);
        }

        try {
            return invocation.proceed();
        } finally {
            TenantContextService.clear();
        }
    }

    private String getHeader(ConsumerRecord<?, ?> record, String key) {
        Header header = record.headers().lastHeader(key);
        if (header != null) {
            return new String(header.value(), StandardCharsets.UTF_8);
        }
        return null;
    }
}
