package com.tam.platform.context;

import java.time.ZoneId;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Immutable record holding tenant and request context information.
 *
 * @param tenantId      The ID of the tenant (e.g., airline code).
 * @param domainId      The ID of the domain (e.g., airport code).
 * @param roles         The set of roles assigned to the user/service.
 * @param correlationId The unique ID for request tracing.
 * @param zoneId        The tenant's time zone.
 * @param locale        The tenant's locale.
 * @param attributes    Custom attributes for the tenant.
 */
public record TenantContext(
        String tenantId,
        String domainId,
        Set<String> roles,
        String correlationId,
        ZoneId zoneId,
        Locale locale,
        Map<String, Object> attributes
) {
    public TenantContext {
        if (roles == null) roles = Collections.emptySet();
        else roles = Collections.unmodifiableSet(roles);
        
        if (attributes == null) attributes = Collections.emptyMap();
        else attributes = Collections.unmodifiableMap(attributes);
        
        if (zoneId == null) zoneId = ZoneId.of("UTC");
        if (locale == null) locale = Locale.ENGLISH;
    }

    public static TenantContext empty() {
        return new TenantContext(null, null, Collections.emptySet(), null, null, null, Collections.emptyMap());
    }
}
