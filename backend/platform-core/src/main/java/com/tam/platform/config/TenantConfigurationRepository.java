package com.tam.platform.config;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface TenantConfigurationRepository extends JpaRepository<TenantConfiguration, Long> {
    
    List<TenantConfiguration> findByTenantIdAndDomainId(String tenantId, String domainId);
    
    Optional<TenantConfiguration> findByTenantIdAndDomainIdAndConfigKey(String tenantId, String domainId, String configKey);
    
    List<TenantConfiguration> findByUpdatedAtAfter(Instant timestamp);
}
