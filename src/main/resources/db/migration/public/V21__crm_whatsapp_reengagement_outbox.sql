-- Cola de reenganche de WhatsApp.
--
-- Vive en el esquema public, igual que venta_facturacion_outbox, porque un unico
-- @Scheduled tiene que poder sondearla sin contexto de tenant. Por eso prospecto_id
-- no es una FK: la fila referenciada esta en el esquema del tenant que nombra
-- tenant_id, y el worker fija TenantContext antes de resolverla.

CREATE TABLE IF NOT EXISTS public.crm_whatsapp_reengagement_outbox (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(120) NOT NULL,
    prospecto_id BIGINT NOT NULL,
    dedupe_key VARCHAR(180) NOT NULL,
    plantilla_nombre VARCHAR(512) NOT NULL,
    plantilla_idioma VARCHAR(35) NOT NULL,
    parametros_json TEXT,
    scheduled_at TIMESTAMP NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_error VARCHAR(1000),
    lease_owner VARCHAR(120),
    lease_until TIMESTAMP,
    heartbeat_at TIMESTAMP,
    creado_por VARCHAR(120),
    resultado VARCHAR(500),
    processed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_crm_wa_reengagement_dedupe UNIQUE (tenant_id, dedupe_key),
    CONSTRAINT chk_crm_wa_reengagement_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'RETRY', 'SENT', 'SKIPPED', 'FAILED', 'CANCELLED')),
    CONSTRAINT chk_crm_wa_reengagement_attempts CHECK (attempts >= 0)
);

CREATE INDEX IF NOT EXISTS idx_crm_wa_reengagement_pending
    ON public.crm_whatsapp_reengagement_outbox (status, next_attempt_at, id);

CREATE INDEX IF NOT EXISTS idx_crm_wa_reengagement_lease
    ON public.crm_whatsapp_reengagement_outbox (status, lease_until)
    WHERE status = 'PROCESSING';

CREATE INDEX IF NOT EXISTS idx_crm_wa_reengagement_prospecto
    ON public.crm_whatsapp_reengagement_outbox (tenant_id, prospecto_id, status);
