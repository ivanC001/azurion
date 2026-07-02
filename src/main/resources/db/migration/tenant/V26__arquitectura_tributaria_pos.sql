CREATE TABLE IF NOT EXISTS configuracion_tributaria_empresa (
    id BIGSERIAL PRIMARY KEY,
    tipo_operacion_default_id VARCHAR(4) NOT NULL,
    tipo_afectacion_default_id VARCHAR(4) NOT NULL,
    tributo_default_id VARCHAR(6) NOT NULL,
    porcentaje_igv_default NUMERIC(5,2) NOT NULL,
    moneda_default VARCHAR(3) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_config_tributaria_porcentaje CHECK (porcentaje_igv_default >= 0 AND porcentaje_igv_default <= 100)
);

INSERT INTO configuracion_tributaria_empresa (
    tipo_operacion_default_id,
    tipo_afectacion_default_id,
    tributo_default_id,
    porcentaje_igv_default,
    moneda_default,
    estado
)
SELECT '0101', '10', '1000', 18.00, 'PEN', 'ACTIVO'
WHERE NOT EXISTS (SELECT 1 FROM configuracion_tributaria_empresa);

ALTER TABLE sucursales
    ADD COLUMN IF NOT EXISTS tipo_operacion_default_id VARCHAR(4),
    ADD COLUMN IF NOT EXISTS tipo_afectacion_default_id VARCHAR(4),
    ADD COLUMN IF NOT EXISTS tributo_default_id VARCHAR(6),
    ADD COLUMN IF NOT EXISTS porcentaje_igv_default NUMERIC(5,2);

ALTER TABLE productos
    ADD COLUMN IF NOT EXISTS tipo_afectacion_igv_id VARCHAR(4),
    ADD COLUMN IF NOT EXISTS tributo_id VARCHAR(6),
    ADD COLUMN IF NOT EXISTS porcentaje_impuesto NUMERIC(5,2),
    ADD COLUMN IF NOT EXISTS usa_configuracion_empresa BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE productos
SET tipo_afectacion_igv_id = CASE WHEN afecto_igv THEN '10' ELSE '20' END,
    tributo_id = CASE WHEN afecto_igv THEN '1000' ELSE '9997' END,
    porcentaje_impuesto = CASE WHEN afecto_igv THEN 18.00 ELSE 0.00 END
WHERE tipo_afectacion_igv_id IS NULL
   OR tributo_id IS NULL
   OR porcentaje_impuesto IS NULL;

CREATE TABLE IF NOT EXISTS venta_detalles (
    id BIGSERIAL PRIMARY KEY,
    venta_id BIGINT NOT NULL REFERENCES ventas(id),
    producto_id BIGINT REFERENCES productos(id),
    sku VARCHAR(80) NOT NULL,
    descripcion VARCHAR(255) NOT NULL,
    cantidad NUMERIC(18,4) NOT NULL,
    precio_unitario NUMERIC(18,2) NOT NULL,
    descuento NUMERIC(18,2) NOT NULL DEFAULT 0,
    tipo_operacion_codigo VARCHAR(4) NOT NULL,
    tipo_afectacion_igv_codigo VARCHAR(4) NOT NULL,
    tributo_codigo VARCHAR(6) NOT NULL,
    porcentaje_igv NUMERIC(5,2) NOT NULL,
    base_imponible NUMERIC(18,2) NOT NULL,
    monto_igv NUMERIC(18,2) NOT NULL,
    total NUMERIC(18,2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_venta_detalle_porcentaje_igv CHECK (porcentaje_igv >= 0 AND porcentaje_igv <= 100)
);

CREATE INDEX IF NOT EXISTS idx_venta_detalles_venta ON venta_detalles(venta_id);
CREATE INDEX IF NOT EXISTS idx_venta_detalles_producto ON venta_detalles(producto_id);

INSERT INTO permisos (codigo, nombre, descripcion, modulo, activo)
VALUES
    ('TRIBUTACION_READ', 'Ver configuracion tributaria', 'Consultar reglas tributarias de empresa, sucursal y producto', 'CONFIGURACION', TRUE),
    ('TRIBUTACION_WRITE', 'Gestionar configuracion tributaria', 'Actualizar reglas tributarias administrativas', 'CONFIGURACION', TRUE)
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo IN ('TRIBUTACION_READ', 'TRIBUTACION_WRITE')
WHERE r.codigo IN ('ADMIN', 'ADMIN_EMPRESA')
ON CONFLICT (rol_id, permiso_id) DO NOTHING;
