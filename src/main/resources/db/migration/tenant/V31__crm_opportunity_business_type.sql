ALTER TABLE crm_oportunidades
    ADD COLUMN IF NOT EXISTS tipo_oportunidad VARCHAR(30) NOT NULL DEFAULT 'PRODUCTO';

ALTER TABLE crm_oportunidades
    DROP CONSTRAINT IF EXISTS chk_crm_oportunidades_tipo;

ALTER TABLE crm_oportunidades
    ADD CONSTRAINT chk_crm_oportunidades_tipo CHECK (
        tipo_oportunidad IN ('PRODUCTO', 'SERVICIO', 'VEHICULO', 'INMUEBLE', 'PROYECTO', 'CURSO')
    );

CREATE INDEX IF NOT EXISTS idx_crm_oportunidades_tipo ON crm_oportunidades(tipo_oportunidad);
