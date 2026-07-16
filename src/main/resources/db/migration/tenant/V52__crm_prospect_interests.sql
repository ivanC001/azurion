CREATE TABLE IF NOT EXISTS crm_prospecto_intereses (
    id BIGSERIAL PRIMARY KEY,
    prospecto_id BIGINT NOT NULL REFERENCES crm_prospectos(id) ON DELETE CASCADE,
    landing_key VARCHAR(120),
    campania VARCHAR(120),
    canal_ingreso VARCHAR(30) NOT NULL DEFAULT 'LANDING',
    catalogo_item_id BIGINT REFERENCES crm_catalogo_items(id),
    producto_pendiente BOOLEAN NOT NULL DEFAULT FALSE,
    tipo_interes VARCHAR(30) NOT NULL DEFAULT 'PRODUCTO',
    interes_principal VARCHAR(220),
    interes_detalle VARCHAR(1500),
    mensaje VARCHAR(1500),
    presupuesto_estimado NUMERIC(18,2),
    fecha_interes DATE,
    landing_url VARCHAR(500),
    metadata_json TEXT,
    contador_envios INTEGER NOT NULL DEFAULT 1,
    ultimo_envio_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_crm_prospecto_intereses_canal CHECK (canal_ingreso IN ('MANUAL', 'LANDING', 'WEBHOOK', 'WHATSAPP', 'FACEBOOK', 'IMPORTADO')),
    CONSTRAINT chk_crm_prospecto_intereses_tipo CHECK (tipo_interes IN (
        'PRODUCTO', 'SERVICIO', 'VEHICULO', 'INMUEBLE', 'PROYECTO', 'CURSO',
        'SEGURO', 'SOFTWARE', 'MARKETING', 'CLINICA', 'JURIDICO', 'TURISMO',
        'MAQUINARIA', 'FINANCIERO', 'EDUCACION', 'HOSPITALIDAD', 'MANUFACTURA',
        'TELECOMUNICACION', 'ENERGIA', 'AGRICULTURA', 'CONSULTORIA', 'OTRO'
    ))
);

CREATE INDEX IF NOT EXISTS idx_crm_prospecto_intereses_prospecto ON crm_prospecto_intereses(prospecto_id);
CREATE INDEX IF NOT EXISTS idx_crm_prospecto_intereses_landing ON crm_prospecto_intereses(landing_key);
CREATE INDEX IF NOT EXISTS idx_crm_prospecto_intereses_campania ON crm_prospecto_intereses(campania);
CREATE INDEX IF NOT EXISTS idx_crm_prospecto_intereses_catalogo ON crm_prospecto_intereses(catalogo_item_id);
CREATE INDEX IF NOT EXISTS idx_crm_prospecto_intereses_ultimo ON crm_prospecto_intereses(ultimo_envio_en DESC);
CREATE INDEX IF NOT EXISTS idx_crm_prospecto_intereses_match ON crm_prospecto_intereses(
    prospecto_id,
    COALESCE(landing_key, ''),
    COALESCE(campania, ''),
    COALESCE(catalogo_item_id, 0),
    producto_pendiente
);
