ALTER TABLE crm_oportunidades
    DROP CONSTRAINT IF EXISTS chk_crm_oportunidades_tipo;

CREATE TABLE IF NOT EXISTS crm_catalogo_items (
    id BIGSERIAL PRIMARY KEY,
    tipo_item VARCHAR(30) NOT NULL,
    nombre VARCHAR(220) NOT NULL,
    descripcion VARCHAR(1500),
    precio_referencial NUMERIC(18, 2) DEFAULT 0 NOT NULL,
    estado VARCHAR(30) DEFAULT 'ACTIVO' NOT NULL,
    metadata_json TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_crm_catalogo_tipo CHECK (
        tipo_item IN (
            'PRODUCTO', 'SERVICIO', 'VEHICULO', 'INMUEBLE', 'PROYECTO', 'CURSO',
            'SEGURO', 'SOFTWARE', 'MARKETING', 'CLINICA', 'JURIDICO', 'TURISMO',
            'MAQUINARIA', 'FINANCIERO', 'EDUCACION', 'HOSPITALIDAD', 'MANUFACTURA',
            'TELECOMUNICACION', 'ENERGIA', 'AGRICULTURA', 'CONSULTORIA', 'OTRO'
        )
    ),
    CONSTRAINT chk_crm_catalogo_estado CHECK (estado IN ('ACTIVO', 'INACTIVO', 'ARCHIVADO'))
);

ALTER TABLE crm_prospectos
    ADD COLUMN IF NOT EXISTS tipo_interes VARCHAR(30) DEFAULT 'PRODUCTO' NOT NULL,
    ADD COLUMN IF NOT EXISTS interes_principal VARCHAR(220),
    ADD COLUMN IF NOT EXISTS interes_detalle VARCHAR(1500),
    ADD COLUMN IF NOT EXISTS presupuesto_estimado NUMERIC(18, 2),
    ADD COLUMN IF NOT EXISTS fecha_interes DATE,
    ADD COLUMN IF NOT EXISTS metadata_json TEXT,
    ADD COLUMN IF NOT EXISTS catalogo_item_id BIGINT REFERENCES crm_catalogo_items(id);

ALTER TABLE crm_oportunidades
    ADD COLUMN IF NOT EXISTS catalogo_item_id BIGINT REFERENCES crm_catalogo_items(id);

ALTER TABLE crm_prospectos
    DROP CONSTRAINT IF EXISTS chk_crm_prospectos_tipo_interes;

ALTER TABLE crm_prospectos
    ADD CONSTRAINT chk_crm_prospectos_tipo_interes CHECK (
        tipo_interes IN (
            'PRODUCTO', 'SERVICIO', 'VEHICULO', 'INMUEBLE', 'PROYECTO', 'CURSO',
            'SEGURO', 'SOFTWARE', 'MARKETING', 'CLINICA', 'JURIDICO', 'TURISMO',
            'MAQUINARIA', 'FINANCIERO', 'EDUCACION', 'HOSPITALIDAD', 'MANUFACTURA',
            'TELECOMUNICACION', 'ENERGIA', 'AGRICULTURA', 'CONSULTORIA', 'OTRO'
        )
    );

ALTER TABLE crm_oportunidades
    ADD CONSTRAINT chk_crm_oportunidades_tipo CHECK (
        tipo_oportunidad IN (
            'PRODUCTO', 'SERVICIO', 'VEHICULO', 'INMUEBLE', 'PROYECTO', 'CURSO',
            'SEGURO', 'SOFTWARE', 'MARKETING', 'CLINICA', 'JURIDICO', 'TURISMO',
            'MAQUINARIA', 'FINANCIERO', 'EDUCACION', 'HOSPITALIDAD', 'MANUFACTURA',
            'TELECOMUNICACION', 'ENERGIA', 'AGRICULTURA', 'CONSULTORIA', 'OTRO'
        )
    );

CREATE INDEX IF NOT EXISTS idx_crm_catalogo_tipo_estado ON crm_catalogo_items(tipo_item, estado);
CREATE INDEX IF NOT EXISTS idx_crm_prospectos_tipo_interes ON crm_prospectos(tipo_interes);
CREATE INDEX IF NOT EXISTS idx_crm_prospectos_catalogo_item ON crm_prospectos(catalogo_item_id);
CREATE INDEX IF NOT EXISTS idx_crm_oportunidades_catalogo_item ON crm_oportunidades(catalogo_item_id);
