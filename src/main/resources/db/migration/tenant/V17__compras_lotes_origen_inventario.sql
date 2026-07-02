CREATE TABLE IF NOT EXISTS compras (
    id BIGSERIAL PRIMARY KEY,
    proveedor_id BIGINT,
    proveedor_documento VARCHAR(20),
    proveedor_nombre VARCHAR(255),
    tipo_comprobante VARCHAR(20) NOT NULL,
    serie VARCHAR(20),
    correlativo VARCHAR(30),
    numero_comprobante VARCHAR(60) NOT NULL,
    fecha_emision DATE NOT NULL,
    fecha_ingreso TIMESTAMPTZ NOT NULL DEFAULT now(),
    almacen_id BIGINT NOT NULL REFERENCES almacenes(id),
    total NUMERIC(18,2) NOT NULL DEFAULT 0,
    estado VARCHAR(20) NOT NULL DEFAULT 'REGISTRADA',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_compras_tipo_comprobante CHECK (tipo_comprobante IN ('FACTURA', 'BOLETA', 'TICKET', 'OTRO')),
    CONSTRAINT chk_compras_total_no_negativo CHECK (total >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_compras_proveedor_comprobante
    ON compras (
        COALESCE(proveedor_documento, proveedor_id::text, 'SIN_PROVEEDOR'),
        tipo_comprobante,
        numero_comprobante
    );

CREATE TABLE IF NOT EXISTS compra_detalles (
    id BIGSERIAL PRIMARY KEY,
    compra_id BIGINT NOT NULL REFERENCES compras(id) ON DELETE CASCADE,
    producto_id BIGINT NOT NULL REFERENCES productos(id),
    cantidad NUMERIC(18,4) NOT NULL,
    costo_unitario NUMERIC(18,6) NOT NULL,
    precio_venta NUMERIC(18,2),
    total NUMERIC(18,2) NOT NULL,
    codigo_lote VARCHAR(120),
    fecha_fabricacion DATE,
    fecha_vencimiento DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_compra_detalles_cantidad CHECK (cantidad > 0),
    CONSTRAINT chk_compra_detalles_costo CHECK (costo_unitario > 0),
    CONSTRAINT chk_compra_detalles_total CHECK (total >= 0)
);

ALTER TABLE lotes
    ADD COLUMN IF NOT EXISTS compra_detalle_id BIGINT REFERENCES compra_detalles(id),
    ADD COLUMN IF NOT EXISTS cantidad_inicial NUMERIC(18,4) NOT NULL DEFAULT 0;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_stock_lotes_actual_no_negativo'
    ) THEN
        ALTER TABLE stock_lotes
            ADD CONSTRAINT chk_stock_lotes_actual_no_negativo CHECK (stock_actual >= 0);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_stock_cantidad_no_negativa'
    ) THEN
        ALTER TABLE stock
            ADD CONSTRAINT chk_stock_cantidad_no_negativa CHECK (cantidad >= 0);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_compras_fecha_ingreso ON compras(fecha_ingreso DESC);
CREATE INDEX IF NOT EXISTS idx_compras_numero_comprobante ON compras(numero_comprobante);
CREATE INDEX IF NOT EXISTS idx_compra_detalles_compra ON compra_detalles(compra_id);
CREATE INDEX IF NOT EXISTS idx_compra_detalles_producto ON compra_detalles(producto_id);
CREATE INDEX IF NOT EXISTS idx_lotes_compra_detalle ON lotes(compra_detalle_id);
