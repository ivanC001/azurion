INSERT INTO permisos (codigo, nombre, descripcion, modulo, activo, sistema)
VALUES
    ('COTIZACIONES_READ', 'Ver cotizaciones', 'Consultar cotizaciones comerciales', 'COTIZACIONES', TRUE, TRUE),
    ('COTIZACIONES_CREATE', 'Crear cotizaciones', 'Registrar nuevas cotizaciones', 'COTIZACIONES', TRUE, TRUE),
    ('COTIZACIONES_UPDATE', 'Actualizar cotizaciones', 'Editar y cambiar estado de cotizaciones', 'COTIZACIONES', TRUE, TRUE),
    ('COTIZACIONES_CONVERT_SALE', 'Convertir cotizacion en venta', 'Convertir una cotizacion aceptada en venta', 'COTIZACIONES', TRUE, TRUE),
    ('COTIZACIONES_PDF', 'Descargar PDF de cotizacion', 'Generar PDF simple de cotizacion', 'COTIZACIONES', TRUE, TRUE),
    ('COMPRAS_READ', 'Ver compras', 'Consultar compras e ingresos de productos', 'COMPRAS', TRUE, TRUE),
    ('COMPRAS_CREATE', 'Crear compras', 'Registrar compras e ingresos por comprobante', 'COMPRAS', TRUE, TRUE),
    ('COMPRAS_CANCEL', 'Anular compras', 'Anular compras registradas', 'COMPRAS', TRUE, TRUE),
    ('PROVEEDORES_READ', 'Ver proveedores', 'Consultar proveedores', 'PROVEEDORES', TRUE, TRUE),
    ('PROVEEDORES_WRITE', 'Gestionar proveedores', 'Crear y editar proveedores', 'PROVEEDORES', TRUE, TRUE)
ON CONFLICT (codigo) DO UPDATE
SET nombre = EXCLUDED.nombre,
    descripcion = EXCLUDED.descripcion,
    modulo = EXCLUDED.modulo,
    activo = TRUE,
    sistema = TRUE,
    updated_at = now();

CREATE TABLE IF NOT EXISTS cotizaciones (
    id BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT REFERENCES clientes(id),
    usuario_id VARCHAR(80) NOT NULL,
    usuario_nombre VARCHAR(150) NOT NULL,
    sucursal_id BIGINT NOT NULL REFERENCES sucursales(id),
    fecha_emision DATE NOT NULL,
    fecha_vencimiento DATE,
    moneda VARCHAR(3) NOT NULL DEFAULT 'PEN',
    subtotal NUMERIC(18, 2) NOT NULL DEFAULT 0,
    total NUMERIC(18, 2) NOT NULL DEFAULT 0,
    estado VARCHAR(20) NOT NULL DEFAULT 'BORRADOR',
    observacion VARCHAR(500),
    venta_id BIGINT,
    convertida_en TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_cotizaciones_estado CHECK (estado IN ('BORRADOR', 'ENVIADA', 'ACEPTADA', 'RECHAZADA', 'VENCIDA', 'CONVERTIDA')),
    CONSTRAINT chk_cotizaciones_total_no_negativo CHECK (total >= 0),
    CONSTRAINT chk_cotizaciones_subtotal_no_negativo CHECK (subtotal >= 0)
);

CREATE TABLE IF NOT EXISTS cotizacion_detalles (
    id BIGSERIAL PRIMARY KEY,
    cotizacion_id BIGINT NOT NULL REFERENCES cotizaciones(id) ON DELETE CASCADE,
    producto_id BIGINT NOT NULL REFERENCES productos(id),
    descripcion VARCHAR(500),
    cantidad NUMERIC(18, 4) NOT NULL,
    precio_unitario NUMERIC(18, 2) NOT NULL,
    descuento NUMERIC(18, 2) NOT NULL DEFAULT 0,
    total NUMERIC(18, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_cotizacion_detalles_cantidad CHECK (cantidad > 0),
    CONSTRAINT chk_cotizacion_detalles_precio CHECK (precio_unitario >= 0),
    CONSTRAINT chk_cotizacion_detalles_total CHECK (total >= 0)
);

CREATE INDEX IF NOT EXISTS idx_cotizaciones_fecha ON cotizaciones(fecha_emision DESC);
CREATE INDEX IF NOT EXISTS idx_cotizaciones_estado ON cotizaciones(estado);
CREATE INDEX IF NOT EXISTS idx_cotizaciones_cliente ON cotizaciones(cliente_id);
CREATE INDEX IF NOT EXISTS idx_cotizaciones_sucursal ON cotizaciones(sucursal_id);
CREATE INDEX IF NOT EXISTS idx_cotizacion_detalles_cotizacion ON cotizacion_detalles(cotizacion_id);
CREATE INDEX IF NOT EXISTS idx_cotizacion_detalles_producto ON cotizacion_detalles(producto_id);

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permisos p
WHERE r.codigo = 'ADMIN_EMPRESA'
  AND p.codigo IN ('COTIZACIONES_READ', 'COTIZACIONES_CREATE', 'COTIZACIONES_UPDATE', 'COTIZACIONES_CONVERT_SALE',
                   'COTIZACIONES_PDF', 'COMPRAS_READ', 'COMPRAS_CREATE', 'COMPRAS_CANCEL',
                   'PROVEEDORES_READ', 'PROVEEDORES_WRITE')
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo IN ('COTIZACIONES_READ', 'COTIZACIONES_CREATE', 'COTIZACIONES_UPDATE',
                                'COTIZACIONES_CONVERT_SALE', 'COTIZACIONES_PDF')
WHERE r.codigo = 'VENDEDOR'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo IN ('COTIZACIONES_READ', 'COMPRAS_READ', 'PROVEEDORES_READ')
WHERE r.codigo IN ('SUPERVISOR_SUCURSAL', 'AUDITOR')
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo IN ('COMPRAS_READ', 'COMPRAS_CREATE', 'PROVEEDORES_READ', 'PROVEEDORES_WRITE')
WHERE r.codigo = 'ALMACENERO'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;
