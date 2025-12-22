package com.tam.platform.event;

import com.tam.platform.context.TenantContext;
import com.tam.platform.context.TenantContextService;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory implementation of EventBus for testing and local development.
 */
@Slf4j
public class InMemoryEventBus implements EventBus {

    private final List<DomainEvent<?>> publishedEvents = new CopyOnWriteArrayList<>();

    @Override
    public void publish(String topic, String eventType, Object payload) {
        TenantContext context = TenantContextService.get();
        if (context == null) {
            context = TenantContext.empty();
        }

        DomainEvent<Object> event = new DomainEvent<>(
                UUID.randomUUID().toString(),
                eventType,
                Instant.now(),
                context,
                payload
        );

        publishedEvents.add(event);
        log.info("InMemoryEventBus: Published event {} to topic {}", event, topic);
    }

    public List<DomainEvent<?>> getPublishedEvents() {
        return Collections.unmodifiableList(publishedEvents);
    }

    public void clear() {
        publishedEvents.clear();
    }
}
