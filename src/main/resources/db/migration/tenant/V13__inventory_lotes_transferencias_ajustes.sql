CREATE TABLE IF NOT EXISTS categorias (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(400),
    padre_id BIGINT REFERENCES categorias(id),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS marcas (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL UNIQUE,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS unidades_medida (
    id BIGSERIAL PRIMARY KEY,
    codigo_sunat VARCHAR(12) NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    abreviatura VARCHAR(20) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_unidades_medida_codigo UNIQUE (codigo_sunat)
);

INSERT INTO unidades_medida (codigo_sunat, nombre, abreviatura, estado)
VALUES
    ('NIU', 'Unidad', 'und', 'ACTIVO'),
    ('KGM', 'Kilogramo', 'kg', 'ACTIVO'),
    ('LTR', 'Litro', 'l', 'ACTIVO'),
    ('BX', 'Caja', 'cja', 'ACTIVO'),
    ('PK', 'Paquete', 'paq', 'ACTIVO'),
    ('MTR', 'Metro', 'm', 'ACTIVO')
ON CONFLICT (codigo_sunat) DO NOTHING;

ALTER TABLE almacenes
    ADD COLUMN IF NOT EXISTS sucursal_id BIGINT REFERENCES sucursales(id),
    ADD COLUMN IF NOT EXISTS tipo_almacen VARCHAR(30) NOT NULL DEFAULT 'PRINCIPAL',
    ADD COLUMN IF NOT EXISTS permite_venta BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO';

UPDATE almacenes
SET estado = CASE WHEN activo THEN 'ACTIVO' ELSE 'INACTIVO' END
WHERE estado IS NULL OR estado = '';

ALTER TABLE productos
    ADD COLUMN IF NOT EXISTS codigo VARCHAR(80),
    ADD COLUMN IF NOT EXISTS codigo_barras VARCHAR(80),
    ADD COLUMN IF NOT EXISTS descripcion VARCHAR(500),
    ADD COLUMN IF NOT EXISTS categoria_id BIGINT REFERENCES categorias(id),
    ADD COLUMN IF NOT EXISTS marca_id BIGINT REFERENCES marcas(id),
    ADD COLUMN IF NOT EXISTS unidad_medida_id BIGINT REFERENCES unidades_medida(id),
    ADD COLUMN IF NOT EXISTS tipo_producto VARCHAR(30) NOT NULL DEFAULT 'PRODUCTO',
    ADD COLUMN IF NOT EXISTS imagen_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS precio_compra_base NUMERIC(18,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS precio_venta_base NUMERIC(18,2),
    ADD COLUMN IF NOT EXISTS afecto_igv BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS maneja_stock BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS maneja_lotes BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS maneja_vencimiento BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS stock_minimo_global NUMERIC(18,4) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO';

UPDATE productos
SET codigo = COALESCE(codigo, sku),
    precio_venta_base = COALESCE(precio_venta_base, precio),
    estado = CASE WHEN activo THEN 'ACTIVO' ELSE 'INACTIVO' END
WHERE codigo IS NULL
   OR precio_venta_base IS NULL
   OR estado IS NULL
   OR estado = '';

DO $$
DECLARE
    default_unidad_id BIGINT;
BEGIN
    SELECT id INTO default_unidad_id
    FROM unidades_medida
    WHERE codigo_sunat = 'NIU'
    ORDER BY id
    LIMIT 1;

    UPDATE productos
    SET unidad_medida_id = default_unidad_id
    WHERE unidad_medida_id IS NULL;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_productos_codigo_not_null ON productos(codigo) WHERE codigo IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_productos_categoria_id ON productos(categoria_id);
CREATE INDEX IF NOT EXISTS idx_productos_marca_id ON productos(marca_id);
CREATE INDEX IF NOT EXISTS idx_productos_estado ON productos(estado);

ALTER TABLE stock
    ADD COLUMN IF NOT EXISTS stock_reservado NUMERIC(18,4) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS stock_minimo NUMERIC(18,4) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS stock_maximo NUMERIC(18,4),
    ADD COLUMN IF NOT EXISTS ubicacion_fisica VARCHAR(120),
    ADD COLUMN IF NOT EXISTS estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO';

CREATE TABLE IF NOT EXISTS lotes (
    id BIGSERIAL PRIMARY KEY,
    producto_id BIGINT NOT NULL REFERENCES productos(id),
    codigo_lote VARCHAR(120) NOT NULL,
    fecha_fabricacion DATE,
    fecha_ingreso DATE NOT NULL DEFAULT CURRENT_DATE,
    fecha_vencimiento DATE,
    proveedor_id BIGINT,
    costo_unitario NUMERIC(18,6) NOT NULL DEFAULT 0,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_lotes_producto_codigo UNIQUE (producto_id, codigo_lote)
);

CREATE TABLE IF NOT EXISTS stock_lotes (
    id BIGSERIAL PRIMARY KEY,
    lote_id BIGINT NOT NULL REFERENCES lotes(id),
    producto_id BIGINT NOT NULL REFERENCES productos(id),
    almacen_id BIGINT NOT NULL REFERENCES almacenes(id),
    stock_actual NUMERIC(18,4) NOT NULL DEFAULT 0,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_stock_lotes_lote_almacen UNIQUE (lote_id, almacen_id)
);

ALTER TABLE kardex_movimientos
    ADD COLUMN IF NOT EXISTS lote_id BIGINT REFERENCES lotes(id),
    ADD COLUMN IF NOT EXISTS referencia_tipo VARCHAR(40),
    ADD COLUMN IF NOT EXISTS referencia_id BIGINT,
    ADD COLUMN IF NOT EXISTS stock_anterior NUMERIC(18,4),
    ADD COLUMN IF NOT EXISTS stock_nuevo NUMERIC(18,4),
    ADD COLUMN IF NOT EXISTS costo_unitario NUMERIC(18,6),
    ADD COLUMN IF NOT EXISTS costo_total NUMERIC(18,6),
    ADD COLUMN IF NOT EXISTS usuario_id VARCHAR(120),
    ADD COLUMN IF NOT EXISTS observacion VARCHAR(500);

UPDATE kardex_movimientos
SET stock_nuevo = COALESCE(stock_nuevo, saldo_resultante),
    stock_anterior = COALESCE(stock_anterior, saldo_resultante - cantidad)
WHERE stock_nuevo IS NULL
   OR stock_anterior IS NULL;

CREATE TABLE IF NOT EXISTS transferencias_almacen (
    id BIGSERIAL PRIMARY KEY,
    almacen_origen_id BIGINT NOT NULL REFERENCES almacenes(id),
    almacen_destino_id BIGINT NOT NULL REFERENCES almacenes(id),
    usuario_id VARCHAR(120),
    fecha_transferencia TIMESTAMPTZ NOT NULL DEFAULT now(),
    estado VARCHAR(20) NOT NULL DEFAULT 'BORRADOR',
    observacion VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_transferencia_almacenes_distintos CHECK (almacen_origen_id <> almacen_destino_id)
);

CREATE TABLE IF NOT EXISTS detalle_transferencia_almacen (
    id BIGSERIAL PRIMARY KEY,
    transferencia_id BIGINT NOT NULL REFERENCES transferencias_almacen(id) ON DELETE CASCADE,
    producto_id BIGINT NOT NULL REFERENCES productos(id),
    lote_id BIGINT REFERENCES lotes(id),
    cantidad NUMERIC(18,4) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_detalle_transferencia_cantidad CHECK (cantidad > 0)
);

CREATE TABLE IF NOT EXISTS ajustes_inventario (
    id BIGSERIAL PRIMARY KEY,
    almacen_id BIGINT NOT NULL REFERENCES almacenes(id),
    usuario_id VARCHAR(120),
    fecha_ajuste TIMESTAMPTZ NOT NULL DEFAULT now(),
    motivo VARCHAR(150) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'CONFIRMADO',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS detalle_ajuste_inventario (
    id BIGSERIAL PRIMARY KEY,
    ajuste_id BIGINT NOT NULL REFERENCES ajustes_inventario(id) ON DELETE CASCADE,
    producto_id BIGINT NOT NULL REFERENCES productos(id),
    lote_id BIGINT REFERENCES lotes(id),
    cantidad NUMERIC(18,4) NOT NULL,
    tipo_ajuste VARCHAR(20) NOT NULL,
    observacion VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_detalle_ajuste_cantidad CHECK (cantidad > 0)
);

CREATE INDEX IF NOT EXISTS idx_lotes_producto_estado ON lotes(producto_id, estado);
CREATE INDEX IF NOT EXISTS idx_lotes_vencimiento ON lotes(fecha_vencimiento);
CREATE INDEX IF NOT EXISTS idx_stock_lotes_producto_almacen ON stock_lotes(producto_id, almacen_id);
CREATE INDEX IF NOT EXISTS idx_kardex_lote_fecha ON kardex_movimientos(lote_id, fecha_movimiento DESC);
CREATE INDEX IF NOT EXISTS idx_transferencias_estado_fecha ON transferencias_almacen(estado, fecha_transferencia DESC);
