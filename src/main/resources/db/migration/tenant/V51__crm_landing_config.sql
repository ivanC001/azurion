ALTER TABLE crm_prospectos
    ADD COLUMN IF NOT EXISTS landing_key VARCHAR(120),
    ADD COLUMN IF NOT EXISTS producto_pendiente BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_crm_prospectos_landing_key ON crm_prospectos(landing_key);
CREATE INDEX IF NOT EXISTS idx_crm_prospectos_producto_pendiente ON crm_prospectos(producto_pendiente);

CREATE TABLE IF NOT EXISTS crm_landing_config (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(160) NOT NULL,
    landing_key VARCHAR(120) NOT NULL UNIQUE,
    campania VARCHAR(120),
    canal_ingreso VARCHAR(30) NOT NULL DEFAULT 'LANDING',
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    recibir_leads BOOLEAN NOT NULL DEFAULT TRUE,
    modo_producto VARCHAR(30) NOT NULL DEFAULT 'REQUERIDO',
    crear_seguimiento BOOLEAN NOT NULL DEFAULT TRUE,
    crear_actividad_inicial BOOLEAN NOT NULL DEFAULT TRUE,
    responsable_id VARCHAR(80),
    campos_obligatorios TEXT,
    validar_duplicados_por VARCHAR(40) NOT NULL DEFAULT 'TELEFONO_CORREO',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_crm_landing_config_modo_producto CHECK (modo_producto IN ('REQUERIDO', 'OPCIONAL', 'SIN_CATALOGO')),
    CONSTRAINT chk_crm_landing_config_canal_ingreso CHECK (canal_ingreso IN ('LANDING', 'WEBHOOK', 'WHATSAPP', 'FACEBOOK', 'IMPORTADO'))
);

CREATE TABLE IF NOT EXISTS crm_landing_catalog_items (
    id BIGSERIAL PRIMARY KEY,
    landing_config_id BIGINT NOT NULL REFERENCES crm_landing_config(id) ON DELETE CASCADE,
    catalogo_item_id BIGINT NOT NULL REFERENCES crm_catalogo_items(id),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_crm_landing_catalog_item UNIQUE (landing_config_id, catalogo_item_id)
);

CREATE INDEX IF NOT EXISTS idx_crm_landing_catalog_items_config ON crm_landing_catalog_items(landing_config_id);
CREATE INDEX IF NOT EXISTS idx_crm_landing_catalog_items_catalogo ON crm_landing_catalog_items(catalogo_item_id);
