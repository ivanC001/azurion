CREATE TABLE IF NOT EXISTS notas_fiscales (
    id BIGSERIAL PRIMARY KEY,
    external_id VARCHAR(80) NOT NULL UNIQUE,
    tipo_documento VARCHAR(2) NOT NULL,
    tipo_nota VARCHAR(20) NOT NULL,
    venta_id BIGINT NOT NULL,
    venta_external_id VARCHAR(80) NOT NULL,
    venta_tipo_documento VARCHAR(3),
    venta_numero_documento VARCHAR(40),
    cliente_documento VARCHAR(20) NOT NULL,
    cliente_nombre VARCHAR(255) NOT NULL,
    moneda VARCHAR(3) NOT NULL,
    monto NUMERIC(18,2) NOT NULL,
    fecha_emision DATE NOT NULL,
    motivo_codigo VARCHAR(6) NOT NULL,
    motivo_descripcion VARCHAR(255) NOT NULL,
    responsable_id VARCHAR(120) NOT NULL,
    responsable_nombre VARCHAR(255) NOT NULL,
    facturacion_estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    facturacion_intentos INT NOT NULL DEFAULT 0,
    facturador_http_status INT,
    facturador_endpoint VARCHAR(120),
    facturador_tipo_comprobante VARCHAR(30),
    facturador_mensaje VARCHAR(500),
    facturador_sunat_estado VARCHAR(30),
    facturador_documento_id VARCHAR(80),
    facturador_ticket VARCHAR(120),
    facturador_pdf_url VARCHAR(500),
    facturador_xml_url VARCHAR(500),
    facturador_cdr_url VARCHAR(500),
    facturador_respuesta_json TEXT,
    facturacion_actualizado_en TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_notas_fiscales_tipo_documento ON notas_fiscales(tipo_documento);
CREATE INDEX IF NOT EXISTS idx_notas_fiscales_venta_id ON notas_fiscales(venta_id);
CREATE INDEX IF NOT EXISTS idx_notas_fiscales_fecha_emision ON notas_fiscales(fecha_emision DESC);
CREATE INDEX IF NOT EXISTS idx_notas_fiscales_facturacion_estado ON notas_fiscales(facturacion_estado);
