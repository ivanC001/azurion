CREATE TABLE IF NOT EXISTS crm_negociaciones (
    id BIGSERIAL PRIMARY KEY,
    oportunidad_id BIGINT NOT NULL REFERENCES crm_oportunidades(id) ON DELETE CASCADE,
    cotizacion_id BIGINT REFERENCES cotizaciones(id) ON DELETE SET NULL,
    estado VARCHAR(40) NOT NULL DEFAULT 'AJUSTE_SOLICITADO',
    solicitud_cliente VARCHAR(80) NOT NULL DEFAULT 'MEJOR_PRECIO',
    precio_original NUMERIC(18,2) NOT NULL DEFAULT 0,
    descuento NUMERIC(18,2) NOT NULL DEFAULT 0,
    precio_final NUMERIC(18,2) NOT NULL DEFAULT 0,
    forma_pago VARCHAR(80),
    cuotas INTEGER NOT NULL DEFAULT 1,
    fecha_inicio DATE,
    fecha_entrega DATE,
    observacion TEXT,
    resultado VARCHAR(40) NOT NULL DEFAULT 'PENDIENTE',
    usuario_id VARCHAR(120),
    usuario_nombre VARCHAR(160),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_crm_negociaciones_estado CHECK (estado IN ('PENDIENTE','AJUSTE_SOLICITADO','PROPUESTA_ENVIADA','CLIENTE_CONFORME','RECHAZADA','GANADA')),
    CONSTRAINT chk_crm_negociaciones_resultado CHECK (resultado IN ('PENDIENTE','ACEPTA','RECHAZA','AJUSTE')),
    CONSTRAINT chk_crm_negociaciones_montos CHECK (precio_original >= 0 AND descuento >= 0 AND precio_final >= 0),
    CONSTRAINT chk_crm_negociaciones_cuotas CHECK (cuotas >= 1)
);

CREATE INDEX IF NOT EXISTS idx_crm_negociaciones_oportunidad ON crm_negociaciones(oportunidad_id);
CREATE INDEX IF NOT EXISTS idx_crm_negociaciones_cotizacion ON crm_negociaciones(cotizacion_id);
CREATE INDEX IF NOT EXISTS idx_crm_negociaciones_estado ON crm_negociaciones(estado);
