package com.tam.platform.event;

/**
 * Interface for publishing domain events.
 */
public interface EventBus {

    /**
     * Publishes an event to the specified topic.
     * The implementation should wrap the payload in a DomainEvent and inject the current TenantContext.
     *
     * @param topic     The topic to publish to.
     * @param eventType The type of the event.
     * @param payload   The business data to publish.
     */
    void publish(String topic, String eventType, Object payload);
}
