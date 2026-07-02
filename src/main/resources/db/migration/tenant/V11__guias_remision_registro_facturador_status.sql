ALTER TABLE guias_remision
    ALTER COLUMN documento_id DROP NOT NULL,
    ALTER COLUMN tipo_gre SET DEFAULT '09';

UPDATE guias_remision
SET tipo_gre = '09'
WHERE tipo_gre IS NULL;

ALTER TABLE guias_remision
    ADD COLUMN IF NOT EXISTS external_id VARCHAR(80),
    ADD COLUMN IF NOT EXISTS sucursal_origen_id BIGINT,
    ADD COLUMN IF NOT EXISTS sucursal_origen_nombre VARCHAR(255),
    ADD COLUMN IF NOT EXISTS sucursal_destino_id BIGINT,
    ADD COLUMN IF NOT EXISTS sucursal_destino_nombre VARCHAR(255),
    ADD COLUMN IF NOT EXISTS fecha_emision DATE,
    ADD COLUMN IF NOT EXISTS fecha_traslado DATE,
    ADD COLUMN IF NOT EXISTS motivo_traslado VARCHAR(120),
    ADD COLUMN IF NOT EXISTS transportista VARCHAR(255),
    ADD COLUMN IF NOT EXISTS observacion VARCHAR(500),
    ADD COLUMN IF NOT EXISTS responsable_id VARCHAR(120),
    ADD COLUMN IF NOT EXISTS responsable_nombre VARCHAR(255),
    ADD COLUMN IF NOT EXISTS items_resumen TEXT,
    ADD COLUMN IF NOT EXISTS facturacion_estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    ADD COLUMN IF NOT EXISTS facturacion_intentos INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS facturador_http_status INT,
    ADD COLUMN IF NOT EXISTS facturador_endpoint VARCHAR(120),
    ADD COLUMN IF NOT EXISTS facturador_tipo_comprobante VARCHAR(30),
    ADD COLUMN IF NOT EXISTS facturador_mensaje VARCHAR(500),
    ADD COLUMN IF NOT EXISTS facturador_sunat_estado VARCHAR(30),
    ADD COLUMN IF NOT EXISTS facturador_documento_id VARCHAR(80),
    ADD COLUMN IF NOT EXISTS facturador_ticket VARCHAR(120),
    ADD COLUMN IF NOT EXISTS facturador_pdf_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS facturador_xml_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS facturador_cdr_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS facturador_respuesta_json TEXT,
    ADD COLUMN IF NOT EXISTS facturacion_actualizado_en TIMESTAMPTZ;

UPDATE guias_remision
SET facturacion_estado = COALESCE(facturacion_estado, 'PENDIENTE'),
    facturacion_intentos = COALESCE(facturacion_intentos, 0),
    facturacion_actualizado_en = COALESCE(facturacion_actualizado_en, now())
WHERE facturacion_estado IS NULL
   OR facturacion_intentos IS NULL
   OR facturacion_actualizado_en IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_guias_remision_external_id
    ON guias_remision (external_id)
    WHERE external_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_guias_remision_fecha_emision_desc ON guias_remision (fecha_emision DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_guias_remision_facturacion_estado ON guias_remision (facturacion_estado);
