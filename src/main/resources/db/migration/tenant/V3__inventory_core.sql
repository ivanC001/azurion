CREATE TABLE IF NOT EXISTS almacenes (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(50) NOT NULL UNIQUE,
    nombre VARCHAR(150) NOT NULL,
    direccion VARCHAR(255),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS stock (
    id BIGSERIAL PRIMARY KEY,
    producto_id BIGINT NOT NULL REFERENCES productos(id),
    almacen_id BIGINT NOT NULL REFERENCES almacenes(id),
    cantidad NUMERIC(18,4) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_stock_producto_almacen UNIQUE (producto_id, almacen_id)
);

CREATE TABLE IF NOT EXISTS kardex_movimientos (
    id BIGSERIAL PRIMARY KEY,
    producto_id BIGINT NOT NULL REFERENCES productos(id),
    almacen_id BIGINT NOT NULL REFERENCES almacenes(id),
    tipo_movimiento VARCHAR(20) NOT NULL,
    motivo VARCHAR(150) NOT NULL,
    cantidad NUMERIC(18,4) NOT NULL,
    saldo_resultante NUMERIC(18,4) NOT NULL,
    referencia VARCHAR(120),
    fecha_movimiento TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_kardex_producto_fecha ON kardex_movimientos(producto_id, fecha_movimiento DESC);
CREATE INDEX IF NOT EXISTS idx_kardex_almacen_fecha ON kardex_movimientos(almacen_id, fecha_movimiento DESC);
