CREATE TABLE IF NOT EXISTS cajas (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(50) NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    saldo_capital NUMERIC(18,2) NOT NULL,
    saldo_actual NUMERIC(18,2) NOT NULL,
    saldo_salida NUMERIC(18,2),
    total_entradas NUMERIC(18,2) NOT NULL DEFAULT 0,
    total_salidas NUMERIC(18,2) NOT NULL DEFAULT 0,
    total_depositos NUMERIC(18,2) NOT NULL DEFAULT 0,
    diferencia_cierre NUMERIC(18,2),
    responsable_apertura_id VARCHAR(80) NOT NULL,
    responsable_apertura_nombre VARCHAR(150) NOT NULL,
    responsable_cierre_id VARCHAR(80),
    responsable_cierre_nombre VARCHAR(150),
    fecha_apertura TIMESTAMPTZ NOT NULL,
    fecha_cierre TIMESTAMPTZ,
    observacion_apertura VARCHAR(500),
    observacion_cierre VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_caja_codigo_abierta
    ON cajas (codigo)
    WHERE estado = 'ABIERTA';

CREATE INDEX IF NOT EXISTS idx_cajas_estado_fecha
    ON cajas (estado, fecha_apertura DESC);

CREATE TABLE IF NOT EXISTS caja_movimientos (
    id BIGSERIAL PRIMARY KEY,
    caja_id BIGINT NOT NULL REFERENCES cajas(id),
    tipo_movimiento VARCHAR(30) NOT NULL,
    monto NUMERIC(18,2) NOT NULL,
    saldo_anterior NUMERIC(18,2) NOT NULL,
    saldo_resultante NUMERIC(18,2) NOT NULL,
    descripcion VARCHAR(250) NOT NULL,
    referencia VARCHAR(120),
    cuenta_empresarial VARCHAR(120),
    responsable_id VARCHAR(80) NOT NULL,
    responsable_nombre VARCHAR(150) NOT NULL,
    fecha_movimiento TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_caja_movimientos_caja_fecha
    ON caja_movimientos (caja_id, fecha_movimiento DESC);

INSERT INTO permisos (codigo, nombre, descripcion, modulo, activo)
VALUES
    ('CAJA_READ', 'Ver caja', 'Consultar cajas, cierres y movimientos', 'CAJA', TRUE),
    ('CAJA_WRITE', 'Gestionar caja', 'Abrir, cerrar y registrar movimientos de caja', 'CAJA', TRUE)
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo IN ('CAJA_READ', 'CAJA_WRITE')
WHERE r.codigo = 'ADMIN'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;
