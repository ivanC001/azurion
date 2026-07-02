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

ALTER TABLE sucursales
    ADD COLUMN IF NOT EXISTS ubigeo_codigo VARCHAR(6),
    ADD COLUMN IF NOT EXISTS departamento VARCHAR(120),
    ADD COLUMN IF NOT EXISTS provincia VARCHAR(120),
    ADD COLUMN IF NOT EXISTS distrito VARCHAR(160),
    ADD COLUMN IF NOT EXISTS igv_porcentaje NUMERIC(5,2) NOT NULL DEFAULT 18.00,
    ADD COLUMN IF NOT EXISTS tipo_operacion_default_id VARCHAR(4),
    ADD COLUMN IF NOT EXISTS tipo_afectacion_default_id VARCHAR(4),
    ADD COLUMN IF NOT EXISTS tributo_default_id VARCHAR(6),
    ADD COLUMN IF NOT EXISTS porcentaje_igv_default NUMERIC(5,2);

INSERT INTO sucursales (
    codigo,
    nombre,
    direccion,
    activo,
    ubigeo_codigo,
    departamento,
    provincia,
    distrito,
    igv_porcentaje,
    tipo_operacion_default_id,
    tipo_afectacion_default_id,
    tributo_default_id,
    porcentaje_igv_default
)
SELECT
    'SUC-PRINCIPAL',
    'Sucursal Principal',
    'Generada automaticamente para CRM',
    TRUE,
    '150101',
    'LIMA',
    'LIMA',
    'LIMA',
    18.00,
    '0101',
    '10',
    '1000',
    18.00
WHERE NOT EXISTS (SELECT 1 FROM sucursales);

UPDATE sucursales
SET ubigeo_codigo = COALESCE(ubigeo_codigo, '150101'),
    departamento = COALESCE(departamento, 'LIMA'),
    provincia = COALESCE(provincia, 'LIMA'),
    distrito = COALESCE(distrito, 'LIMA'),
    igv_porcentaje = COALESCE(igv_porcentaje, 18.00),
    tipo_operacion_default_id = COALESCE(tipo_operacion_default_id, '0101'),
    tipo_afectacion_default_id = COALESCE(tipo_afectacion_default_id, CASE WHEN COALESCE(igv_porcentaje, 18.00) = 0 THEN '20' ELSE '10' END),
    tributo_default_id = COALESCE(tributo_default_id, CASE WHEN COALESCE(igv_porcentaje, 18.00) = 0 THEN '9997' ELSE '1000' END),
    porcentaje_igv_default = COALESCE(porcentaje_igv_default, igv_porcentaje, 18.00),
    activo = COALESCE(activo, TRUE),
    updated_at = now()
WHERE ubigeo_codigo IS NULL
   OR departamento IS NULL
   OR provincia IS NULL
   OR distrito IS NULL
   OR igv_porcentaje IS NULL
   OR tipo_operacion_default_id IS NULL
   OR tipo_afectacion_default_id IS NULL
   OR tributo_default_id IS NULL
   OR porcentaje_igv_default IS NULL
   OR activo IS NULL;

ALTER TABLE sucursales
    ALTER COLUMN ubigeo_codigo SET NOT NULL,
    ALTER COLUMN departamento SET NOT NULL,
    ALTER COLUMN provincia SET NOT NULL,
    ALTER COLUMN distrito SET NOT NULL,
    ALTER COLUMN igv_porcentaje SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_sucursales_ubigeo_codigo ON sucursales(ubigeo_codigo);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = current_schema()
          AND table_name = 'usuarios'
    ) THEN
        CREATE TABLE IF NOT EXISTS usuario_sucursales (
            id BIGSERIAL PRIMARY KEY,
            usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
            sucursal_id BIGINT NOT NULL REFERENCES sucursales(id) ON DELETE CASCADE,
            created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
            updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
            CONSTRAINT uq_usuario_sucursal UNIQUE (usuario_id, sucursal_id)
        );

        CREATE INDEX IF NOT EXISTS idx_usuario_sucursales_usuario ON usuario_sucursales(usuario_id);

        INSERT INTO usuario_sucursales (usuario_id, sucursal_id)
        SELECT u.id, s.id
        FROM usuarios u
        CROSS JOIN sucursales s
        WHERE s.activo = TRUE
        ON CONFLICT (usuario_id, sucursal_id) DO NOTHING;
    END IF;
END $$;

INSERT INTO permisos (codigo, nombre, descripcion, modulo, activo, sistema)
VALUES
    ('SUCURSALES_READ', 'Ver sucursales', 'Consultar sucursales del tenant', 'CONFIGURACION', TRUE, TRUE),
    ('SUCURSALES_WRITE', 'Gestionar sucursales', 'Crear y actualizar sucursales del tenant', 'CONFIGURACION', TRUE, TRUE)
ON CONFLICT (codigo) DO UPDATE
SET nombre = EXCLUDED.nombre,
    descripcion = EXCLUDED.descripcion,
    modulo = EXCLUDED.modulo,
    activo = TRUE,
    sistema = TRUE,
    updated_at = now();

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo IN ('SUCURSALES_READ', 'SUCURSALES_WRITE')
WHERE r.codigo IN ('ADMIN', 'ADMIN_EMPRESA', 'CRM_ADMIN', 'CRM_GERENTE')
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo = 'SUCURSALES_READ'
WHERE r.codigo IN ('CRM_SUPERVISOR', 'CRM_VENDEDOR', 'CRM_MARKETING', 'CRM_CALLCENTER')
ON CONFLICT (rol_id, permiso_id) DO NOTHING;
