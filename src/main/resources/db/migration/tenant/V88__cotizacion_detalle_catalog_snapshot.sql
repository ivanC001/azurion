ALTER TABLE cotizacion_detalles
    ADD COLUMN IF NOT EXISTS crm_catalogo_item_id BIGINT,
    ADD COLUMN IF NOT EXISTS catalogo_tipo_item VARCHAR(30),
    ADD COLUMN IF NOT EXISTS catalogo_nombre VARCHAR(220),
    ADD COLUMN IF NOT EXISTS catalogo_descripcion VARCHAR(1500),
    ADD COLUMN IF NOT EXISTS catalogo_metadata_json TEXT,
    ADD COLUMN IF NOT EXISTS catalogo_moneda VARCHAR(3),
    ADD COLUMN IF NOT EXISTS catalogo_precio_referencial NUMERIC(18, 2);

CREATE INDEX IF NOT EXISTS idx_cotizacion_detalles_catalogo
    ON cotizacion_detalles(crm_catalogo_item_id);
