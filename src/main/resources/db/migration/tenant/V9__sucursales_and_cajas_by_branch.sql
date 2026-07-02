CREATE TABLE IF NOT EXISTS sucursales (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(50) NOT NULL UNIQUE,
    nombre VARCHAR(150) NOT NULL,
    direccion VARCHAR(255),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

INSERT INTO sucursales (codigo, nombre, direccion, activo)
SELECT 'SUC-PRINCIPAL', 'Sucursal Principal', 'Generado por migracion', TRUE
WHERE NOT EXISTS (SELECT 1 FROM sucursales);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = current_schema()
          AND table_name = 'cajas'
    ) THEN
        ALTER TABLE cajas
            ADD COLUMN IF NOT EXISTS sucursal_id BIGINT;

        WITH sucursal_default AS (
            SELECT id FROM sucursales ORDER BY id LIMIT 1
        )
        UPDATE cajas c
        SET sucursal_id = (SELECT id FROM sucursal_default)
        WHERE c.sucursal_id IS NULL;

        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint WHERE conname = 'fk_cajas_sucursal'
        ) THEN
            ALTER TABLE cajas
                ADD CONSTRAINT fk_cajas_sucursal
                FOREIGN KEY (sucursal_id) REFERENCES sucursales(id);
        END IF;

        ALTER TABLE cajas
            ALTER COLUMN sucursal_id SET NOT NULL;

        DROP INDEX IF EXISTS uq_caja_codigo_abierta;

        EXECUTE 'CREATE UNIQUE INDEX IF NOT EXISTS uq_caja_sucursal_codigo_abierta
            ON cajas (sucursal_id, codigo)
            WHERE estado = ''ABIERTA''';

        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_cajas_sucursal_estado_fecha
            ON cajas (sucursal_id, estado, fecha_apertura DESC)';
    END IF;
END $$;

INSERT INTO permisos (codigo, nombre, descripcion, modulo, activo)
VALUES
    ('SUCURSALES_READ', 'Ver sucursales', 'Consultar sucursales del tenant', 'CONFIGURACION', TRUE),
    ('SUCURSALES_WRITE', 'Gestionar sucursales', 'Crear y actualizar sucursales del tenant', 'CONFIGURACION', TRUE)
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo IN ('SUCURSALES_READ', 'SUCURSALES_WRITE')
WHERE r.codigo IN ('ADMIN', 'ADMIN_EMPRESA')
ON CONFLICT (rol_id, permiso_id) DO NOTHING;
