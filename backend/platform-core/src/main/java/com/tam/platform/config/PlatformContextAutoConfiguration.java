package com.tam.platform.config;

import com.tam.platform.context.TenantContextFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

@AutoConfiguration
public class PlatformContextAutoConfiguration {

    @Bean
    @ConditionalOnWebApplication
    public FilterRegistrationBean<TenantContextFilter> tenantContextFilter() {
        FilterRegistrationBean<TenantContextFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TenantContextFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 100); // Run early, but after tracing filter
        registration.addUrlPatterns("/*");
        return registration;
    }
}
