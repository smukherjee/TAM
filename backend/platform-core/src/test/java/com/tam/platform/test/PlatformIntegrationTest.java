package com.tam.platform.test;

import com.tam.platform.cache.CacheService;
import com.tam.platform.config.PlatformServicesAutoConfiguration;
import com.tam.platform.context.TenantContext;
import com.tam.platform.context.TenantContextService;
import com.tam.platform.event.EventBus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = {PlatformServicesAutoConfiguration.class, PlatformIntegrationTest.AsyncConfig.class})
@EnableAutoConfiguration
class PlatformIntegrationTest extends TestContainerSupport {

    @Autowired
    private AsyncService asyncService;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private EventBus eventBus;

    @Test
    void testContextPropagation() throws ExecutionException, InterruptedException {
        TenantContext context = new TenantContext("T1", "D1", Collections.<String>emptySet(), "C1", null, null, null);
        TenantContextService.set(context);

        CompletableFuture<String> result = asyncService.getTenantId();
        assertEquals("T1", result.get());
        
        TenantContextService.clear();
    }

    @Test
    void testCacheOperations() {
        cacheService.put("test-key", "test-value", Duration.ofMinutes(1));
        String value = cacheService.get("test-key", String.class).orElse(null);
        assertEquals("test-value", value);
    }

    @TestConfiguration
    @EnableAsync
    static class AsyncConfig {
        @Bean
        public AsyncService asyncService() {
            return new AsyncService();
        }
    }

    static class AsyncService {
        @Async
        public CompletableFuture<String> getTenantId() {
            TenantContext ctx = TenantContextService.get();
            return CompletableFuture.completedFuture(ctx != null ? ctx.tenantId() : null);
        }
    }
}
