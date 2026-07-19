CREATE TABLE IF NOT EXISTS crm_oportunidad_recursos (
    id BIGSERIAL PRIMARY KEY,
    oportunidad_id BIGINT NOT NULL REFERENCES crm_oportunidades(id) ON DELETE CASCADE,
    tipo VARCHAR(30) NOT NULL,
    external_key VARCHAR(180),
    data_json TEXT NOT NULL DEFAULT '{}',
    archivo_nombre VARCHAR(255),
    archivo_path VARCHAR(700),
    archivo_mime_type VARCHAR(120),
    archivo_size BIGINT,
    created_by VARCHAR(160) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_crm_oportunidad_recursos_tipo
        CHECK (tipo IN ('REQUISITO', 'PAGO', 'DOCUMENTO', 'CIERRE')),
    CONSTRAINT chk_crm_oportunidad_recursos_archivo_size
        CHECK (archivo_size IS NULL OR archivo_size BETWEEN 1 AND 8388608)
);

CREATE INDEX IF NOT EXISTS idx_crm_oportunidad_recursos_oportunidad
    ON crm_oportunidad_recursos(oportunidad_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_crm_oportunidad_recursos_tipo
    ON crm_oportunidad_recursos(tipo, created_at DESC);
CREATE UNIQUE INDEX IF NOT EXISTS uk_crm_oportunidad_recursos_external_key
    ON crm_oportunidad_recursos(oportunidad_id, tipo, external_key)
    WHERE external_key IS NOT NULL;
