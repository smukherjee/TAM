package com.tam.platform.context;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Filter to extract tenant context information from HTTP headers.
 */
public class TenantContextFilter extends OncePerRequestFilter {

    public static final String HEADER_TENANT_ID = "X-Tenant-ID";
    public static final String HEADER_DOMAIN_ID = "X-Domain-ID";
    public static final String HEADER_ROLES = "X-Roles";
    public static final String HEADER_CORRELATION_ID = "X-Correlation-ID";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String tenantId = request.getHeader(HEADER_TENANT_ID);
            String domainId = request.getHeader(HEADER_DOMAIN_ID);
            String rolesHeader = request.getHeader(HEADER_ROLES);
            String correlationId = request.getHeader(HEADER_CORRELATION_ID);

            // Generate correlation ID if missing
            if (!StringUtils.hasText(correlationId)) {
                correlationId = UUID.randomUUID().toString();
            }

            Set<String> roles = Collections.emptySet();
            if (StringUtils.hasText(rolesHeader)) {
                roles = new HashSet<>(Arrays.asList(rolesHeader.split(",")));
            }

            TenantContext context = new TenantContext(tenantId, domainId, roles, correlationId, null, null, null);
            TenantContextService.set(context);
            
            // Add correlation ID to response for debugging
            response.setHeader(HEADER_CORRELATION_ID, correlationId);

            filterChain.doFilter(request, response);
        } finally {
            TenantContextService.clear();
        }
    }
}
