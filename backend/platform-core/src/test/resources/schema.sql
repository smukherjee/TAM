CREATE TABLE IF NOT EXISTS tenant_configurations (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(255),
    domain_id VARCHAR(255),
    config_key VARCHAR(255),
    config_value TEXT,
    value_type VARCHAR(50),
    updated_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_tenant_config UNIQUE (tenant_id, domain_id, config_key)
);
