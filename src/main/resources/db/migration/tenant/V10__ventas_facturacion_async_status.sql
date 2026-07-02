ALTER TABLE ventas
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

UPDATE ventas
SET facturacion_estado = COALESCE(facturacion_estado, 'PENDIENTE'),
    facturacion_intentos = COALESCE(facturacion_intentos, 0),
    facturacion_actualizado_en = COALESCE(facturacion_actualizado_en, now())
WHERE facturacion_estado IS NULL
   OR facturacion_intentos IS NULL
   OR facturacion_actualizado_en IS NULL;

CREATE INDEX IF NOT EXISTS idx_ventas_fecha_venta_desc ON ventas (fecha_venta DESC);
CREATE INDEX IF NOT EXISTS idx_ventas_facturacion_estado ON ventas (facturacion_estado);
