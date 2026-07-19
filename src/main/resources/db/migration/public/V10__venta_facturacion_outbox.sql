CREATE TABLE IF NOT EXISTS public.venta_facturacion_outbox (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(120) NOT NULL,
    tenant_ruc VARCHAR(20) NOT NULL,
    venta_id BIGINT NOT NULL,
    external_id VARCHAR(180) NOT NULL,
    endpoint VARCHAR(180) NOT NULL,
    tipo_comprobante VARCHAR(40) NOT NULL,
    payload_json TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_error VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_venta_facturacion_outbox_external UNIQUE (tenant_id, external_id),
    CONSTRAINT chk_venta_facturacion_outbox_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'RETRY', 'COMPLETED', 'FAILED')),
    CONSTRAINT chk_venta_facturacion_outbox_attempts CHECK (attempts >= 0)
);

CREATE INDEX IF NOT EXISTS idx_venta_facturacion_outbox_pending
    ON public.venta_facturacion_outbox(status, next_attempt_at, id);
