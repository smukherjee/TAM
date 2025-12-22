package com.tam.platform.config;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "tenant_configurations", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tenant_id", "domain_id", "config_key"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenantConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "domain_id")
    private String domainId;

    @Column(name = "config_key")
    private String configKey;

    @Column(name = "config_value")
    private String configValue;

    @Column(name = "value_type")
    private String valueType;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
