package com.tam.platform.event;

import com.tam.platform.context.TenantContext;
import java.io.Serializable;
import java.time.Instant;

/**
 * Standard envelope for all business events.
 *
 * @param eventId   Unique ID of the event.
 * @param eventType Type of the event (e.g., "FlightUpdated").
 * @param timestamp When the event occurred.
 * @param context   The tenant context in which the event occurred.
 * @param payload   The actual business data.
 * @param <T>       The type of the payload.
 */
public record DomainEvent<T>(
        String eventId,
        String eventType,
        Instant timestamp,
        TenantContext context,
        T payload
) implements Serializable {
}
