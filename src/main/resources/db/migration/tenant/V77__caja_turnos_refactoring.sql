-- Separa la caja fisica permanente del turno operativo y conserva todo el historial.

CREATE TEMP TABLE caja_fisica_mapeo ON COMMIT DROP AS
SELECT
    c.id AS turno_legacy_id,
    FIRST_VALUE(c.id) OVER (
        PARTITION BY c.sucursal_id, UPPER(TRIM(c.codigo))
        ORDER BY c.id
    ) AS caja_fisica_id
FROM cajas c;

ALTER TABLE cajas
    ADD COLUMN IF NOT EXISTS moneda VARCHAR(3) NOT NULL DEFAULT 'PEN';

CREATE TABLE caja_turnos (
    id BIGSERIAL PRIMARY KEY,
    numero VARCHAR(30) UNIQUE,
    caja_id BIGINT NOT NULL,
    usuario_id BIGINT,
    moneda VARCHAR(3) NOT NULL DEFAULT 'PEN',
    estado VARCHAR(20) NOT NULL,
    fecha_apertura TIMESTAMPTZ NOT NULL,
    fecha_cierre TIMESTAMPTZ,
    saldo_apertura NUMERIC(18,2) NOT NULL,
    saldo_esperado NUMERIC(18,2) NOT NULL,
    conteo_fisico NUMERIC(18,2),
    diferencia_cierre NUMERIC(18,2),
    numero_ventas INTEGER NOT NULL DEFAULT 0,
    total_ventas NUMERIC(18,2) NOT NULL DEFAULT 0,
    total_efectivo NUMERIC(18,2) NOT NULL DEFAULT 0,
    total_tarjeta NUMERIC(18,2) NOT NULL DEFAULT 0,
    total_billetera_digital NUMERIC(18,2) NOT NULL DEFAULT 0,
    total_transferencia NUMERIC(18,2) NOT NULL DEFAULT 0,
    total_credito NUMERIC(18,2) NOT NULL DEFAULT 0,
    total_ingresos_manuales NUMERIC(18,2) NOT NULL DEFAULT 0,
    total_retiros NUMERIC(18,2) NOT NULL DEFAULT 0,
    total_depositos NUMERIC(18,2) NOT NULL DEFAULT 0,
    total_reembolsos NUMERIC(18,2) NOT NULL DEFAULT 0,
    responsable_apertura_id VARCHAR(80) NOT NULL,
    responsable_apertura_nombre VARCHAR(150) NOT NULL,
    responsable_cierre_id VARCHAR(80),
    responsable_cierre_nombre VARCHAR(150),
    observacion_apertura VARCHAR(500),
    observacion_cierre VARCHAR(500),
    turno_legacy_id BIGINT UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_caja_turno_estado CHECK (estado IN ('ABIERTO', 'CERRADO'))
);

INSERT INTO caja_turnos (
    caja_id,
    usuario_id,
    moneda,
    estado,
    fecha_apertura,
    fecha_cierre,
    saldo_apertura,
    saldo_esperado,
    conteo_fisico,
    diferencia_cierre,
    total_ingresos_manuales,
    total_retiros,
    total_depositos,
    responsable_apertura_id,
    responsable_apertura_nombre,
    responsable_cierre_id,
    responsable_cierre_nombre,
    observacion_apertura,
    observacion_cierre,
    turno_legacy_id,
    created_at,
    updated_at,
    version
)
SELECT
    m.caja_fisica_id,
    CASE
        WHEN c.responsable_apertura_id ~ '^[0-9]+$'
            THEN c.responsable_apertura_id::BIGINT
        ELSE NULL
    END,
    c.moneda,
    CASE WHEN UPPER(c.estado) = 'CERRADA' THEN 'CERRADO' ELSE 'ABIERTO' END,
    c.fecha_apertura,
    c.fecha_cierre,
    c.saldo_capital,
    c.saldo_actual,
    c.saldo_salida,
    c.diferencia_cierre,
    c.total_entradas,
    c.total_salidas,
    c.total_depositos,
    c.responsable_apertura_id,
    c.responsable_apertura_nombre,
    c.responsable_cierre_id,
    c.responsable_cierre_nombre,
    c.observacion_apertura,
    c.observacion_cierre,
    c.id,
    c.created_at,
    c.updated_at,
    c.version
FROM cajas c
JOIN caja_fisica_mapeo m ON m.turno_legacy_id = c.id;

UPDATE caja_turnos
SET numero = 'T-' || LPAD(id::TEXT, 8, '0')
WHERE numero IS NULL;

ALTER TABLE caja_turnos
    ALTER COLUMN numero SET NOT NULL;

ALTER TABLE caja_movimientos
    ADD COLUMN turno_id BIGINT,
    ADD COLUMN origen VARCHAR(30) NOT NULL DEFAULT 'MANUAL',
    ADD COLUMN medio_pago VARCHAR(30) NOT NULL DEFAULT 'EFECTIVO',
    ADD COLUMN afecta_efectivo BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN venta_id BIGINT,
    ADD COLUMN anulado BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN motivo_anulacion VARCHAR(500);

UPDATE caja_movimientos movimiento
SET turno_id = turno.id,
    origen = CASE
        WHEN UPPER(movimiento.descripcion) LIKE '%VENTA%'
          OR UPPER(COALESCE(movimiento.referencia, '')) LIKE 'FAC%'
          OR UPPER(COALESCE(movimiento.referencia, '')) LIKE 'BOL%'
          OR UPPER(COALESCE(movimiento.referencia, '')) LIKE 'TKT%'
            THEN 'VENTA'
        ELSE 'MANUAL'
    END
FROM caja_turnos turno
WHERE turno.turno_legacy_id = movimiento.caja_id;

WITH resumen AS (
    SELECT
        turno_id,
        COUNT(*) FILTER (WHERE origen = 'VENTA')::INTEGER AS numero_ventas,
        COALESCE(SUM(monto) FILTER (WHERE origen = 'VENTA'), 0) AS total_ventas,
        COALESCE(SUM(monto) FILTER (WHERE origen = 'VENTA'), 0) AS total_efectivo,
        COALESCE(SUM(monto) FILTER (
            WHERE origen = 'MANUAL' AND tipo_movimiento = 'ENTRADA'
        ), 0) AS total_ingresos_manuales,
        COALESCE(SUM(monto) FILTER (WHERE tipo_movimiento = 'SALIDA'), 0) AS total_retiros,
        COALESCE(SUM(monto) FILTER (WHERE tipo_movimiento = 'DEPOSITO_CUENTA'), 0) AS total_depositos
    FROM caja_movimientos
    GROUP BY turno_id
)
UPDATE caja_turnos turno
SET numero_ventas = resumen.numero_ventas,
    total_ventas = resumen.total_ventas,
    total_efectivo = resumen.total_efectivo,
    total_ingresos_manuales = resumen.total_ingresos_manuales,
    total_retiros = resumen.total_retiros,
    total_depositos = resumen.total_depositos
FROM resumen
WHERE resumen.turno_id = turno.id;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM caja_movimientos WHERE turno_id IS NULL) THEN
        RAISE EXCEPTION 'No se puede migrar caja: existen movimientos sin un turno historico valido';
    END IF;
END $$;

ALTER TABLE caja_movimientos
    ALTER COLUMN turno_id SET NOT NULL;

ALTER TABLE caja_movimientos
    DROP CONSTRAINT IF EXISTS caja_movimientos_caja_id_fkey;
DROP INDEX IF EXISTS idx_caja_movimientos_caja_fecha;
ALTER TABLE caja_movimientos
    DROP COLUMN caja_id;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = current_schema()
          AND table_name = 'usuario_cajas'
    ) THEN
        ALTER TABLE usuario_cajas DROP CONSTRAINT IF EXISTS uq_usuario_caja;

        UPDATE usuario_cajas asignacion
        SET caja_id = m.caja_fisica_id
        FROM caja_fisica_mapeo m
        WHERE asignacion.caja_id = m.turno_legacy_id;

        DELETE FROM usuario_cajas duplicado
        USING usuario_cajas original
        WHERE duplicado.id > original.id
          AND duplicado.usuario_id = original.usuario_id
          AND duplicado.caja_id = original.caja_id;

        ALTER TABLE usuario_cajas
            ADD CONSTRAINT uq_usuario_caja UNIQUE (usuario_id, caja_id);
    END IF;
END $$;

DELETE FROM cajas caja
WHERE NOT EXISTS (
    SELECT 1
    FROM caja_fisica_mapeo m
    WHERE m.caja_fisica_id = caja.id
);

ALTER TABLE cajas
    DROP COLUMN saldo_capital,
    DROP COLUMN saldo_actual,
    DROP COLUMN saldo_salida,
    DROP COLUMN total_entradas,
    DROP COLUMN total_salidas,
    DROP COLUMN total_depositos,
    DROP COLUMN diferencia_cierre,
    DROP COLUMN responsable_apertura_id,
    DROP COLUMN responsable_apertura_nombre,
    DROP COLUMN responsable_cierre_id,
    DROP COLUMN responsable_cierre_nombre,
    DROP COLUMN fecha_apertura,
    DROP COLUMN fecha_cierre,
    DROP COLUMN observacion_apertura,
    DROP COLUMN observacion_cierre;

UPDATE cajas
SET codigo = UPPER(TRIM(codigo)),
    estado = 'ACTIVA';

DROP INDEX IF EXISTS uq_caja_sucursal_codigo_abierta;
DROP INDEX IF EXISTS idx_cajas_sucursal_estado_fecha;

CREATE UNIQUE INDEX uq_cajas_sucursal_codigo
    ON cajas (sucursal_id, UPPER(TRIM(codigo)));
CREATE INDEX idx_cajas_sucursal_estado
    ON cajas (sucursal_id, estado, nombre);

ALTER TABLE caja_turnos
    ADD CONSTRAINT fk_caja_turnos_caja
        FOREIGN KEY (caja_id) REFERENCES cajas(id);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = current_schema()
          AND table_name = 'usuarios'
    ) THEN
        ALTER TABLE caja_turnos
            ADD CONSTRAINT fk_caja_turnos_usuario
                FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE SET NULL;
    END IF;
END $$;

ALTER TABLE caja_movimientos
    ADD CONSTRAINT fk_caja_movimientos_turno
        FOREIGN KEY (turno_id) REFERENCES caja_turnos(id);

ALTER TABLE ventas
    ADD COLUMN caja_turno_id BIGINT,
    ADD COLUMN forma_pago VARCHAR(20),
    ADD COLUMN metodo_pago VARCHAR(30);

ALTER TABLE ventas
    ADD CONSTRAINT fk_ventas_caja_turno
        FOREIGN KEY (caja_turno_id) REFERENCES caja_turnos(id) ON DELETE SET NULL;

ALTER TABLE caja_movimientos
    ADD CONSTRAINT fk_caja_movimientos_venta
        FOREIGN KEY (venta_id) REFERENCES ventas(id) ON DELETE SET NULL;

CREATE UNIQUE INDEX uq_caja_turno_abierto_por_caja
    ON caja_turnos (caja_id)
    WHERE estado = 'ABIERTO';
CREATE UNIQUE INDEX uq_caja_turno_abierto_por_usuario
    ON caja_turnos (usuario_id)
    WHERE estado = 'ABIERTO' AND usuario_id IS NOT NULL;
CREATE INDEX idx_caja_turnos_fecha
    ON caja_turnos (fecha_apertura DESC);
CREATE INDEX idx_caja_turnos_sucursal_estado
    ON caja_turnos (caja_id, estado, fecha_apertura DESC);
CREATE INDEX idx_caja_movimientos_turno_fecha
    ON caja_movimientos (turno_id, fecha_movimiento DESC);
CREATE INDEX idx_caja_movimientos_origen_medio
    ON caja_movimientos (origen, medio_pago, fecha_movimiento DESC);
CREATE INDEX idx_ventas_caja_turno
    ON ventas (caja_turno_id, fecha_venta DESC);

ALTER TABLE caja_turnos
    DROP COLUMN turno_legacy_id;

INSERT INTO permisos (codigo, nombre, descripcion, modulo, activo, sistema)
VALUES
    ('CAJA_CONFIGURE', 'Configurar cajas', 'Crear cajas fisicas y asignar cajeros autorizados', 'CAJA', TRUE, TRUE)
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT rol.id, permiso.id
FROM roles rol
JOIN permisos permiso ON permiso.codigo = 'CAJA_CONFIGURE'
WHERE rol.codigo IN ('ADMIN', 'ADMIN_EMPRESA', 'ERP_ADMIN')
ON CONFLICT (rol_id, permiso_id) DO NOTHING;
