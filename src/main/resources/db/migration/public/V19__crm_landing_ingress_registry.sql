CREATE TABLE IF NOT EXISTS public.crm_landing_ingress_registry (
    id BIGSERIAL PRIMARY KEY,
    source_key VARCHAR(120) NOT NULL UNIQUE,
    tenant_id VARCHAR(80) NOT NULL,
    landing_config_id BIGINT NOT NULL,
    relay_secret_encrypted TEXT NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_crm_landing_ingress_tenant_config UNIQUE (tenant_id, landing_config_id)
);

CREATE INDEX IF NOT EXISTS idx_crm_landing_ingress_tenant
    ON public.crm_landing_ingress_registry(tenant_id);

CREATE INDEX IF NOT EXISTS idx_crm_landing_ingress_active
    ON public.crm_landing_ingress_registry(activo, source_key);
