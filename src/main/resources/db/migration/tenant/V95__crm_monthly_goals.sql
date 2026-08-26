INSERT INTO permisos (codigo, nombre, descripcion, modulo, activo, sistema)
VALUES
    ('CRM_GOALS_READ', 'Ver metas CRM', 'Consultar metas y avance comercial del equipo o asesor', 'CRM', TRUE, TRUE),
    ('CRM_GOALS_MANAGE', 'Gestionar metas CRM', 'Crear, modificar y eliminar metas comerciales mensuales', 'CRM', TRUE, TRUE)
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
JOIN permisos p ON p.codigo IN ('CRM_GOALS_READ', 'CRM_GOALS_MANAGE')
WHERE r.codigo IN ('ADMIN_EMPRESA', 'CRM_ADMIN', 'CRM_GERENTE', 'CRM_SUPERVISOR')
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo = 'CRM_GOALS_READ'
WHERE r.codigo IN ('CRM_VENDEDOR', 'CRM_MARKETING', 'CRM_CALLCENTER')
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

CREATE TABLE IF NOT EXISTS crm_metas (
    id BIGSERIAL PRIMARY KEY,
    anio INTEGER NOT NULL,
    mes SMALLINT NOT NULL,
    alcance VARCHAR(20) NOT NULL,
    responsable_id VARCHAR(80),
    moneda VARCHAR(3) NOT NULL,
    meta_ingresos NUMERIC(18, 2) NOT NULL DEFAULT 0,
    meta_oportunidades_ganadas INTEGER NOT NULL DEFAULT 0,
    meta_prospectos_nuevos INTEGER NOT NULL DEFAULT 0,
    meta_actividades_realizadas INTEGER NOT NULL DEFAULT 0,
    meta_conversion NUMERIC(5, 2) NOT NULL DEFAULT 0,
    created_by VARCHAR(80) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_crm_metas_anio CHECK (anio BETWEEN 2020 AND 2100),
    CONSTRAINT ck_crm_metas_mes CHECK (mes BETWEEN 1 AND 12),
    CONSTRAINT ck_crm_metas_alcance CHECK (alcance IN ('EQUIPO', 'ASESOR')),
    CONSTRAINT ck_crm_metas_responsable CHECK (
        (alcance = 'EQUIPO' AND responsable_id IS NULL)
        OR (alcance = 'ASESOR' AND responsable_id IS NOT NULL)
    ),
    CONSTRAINT ck_crm_metas_valores CHECK (
        meta_ingresos >= 0
        AND meta_oportunidades_ganadas >= 0
        AND meta_prospectos_nuevos >= 0
        AND meta_actividades_realizadas >= 0
        AND meta_conversion BETWEEN 0 AND 100
    )
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_crm_metas_periodo_alcance_responsable
    ON crm_metas (anio, mes, alcance, COALESCE(responsable_id, '__EQUIPO__'));

CREATE INDEX IF NOT EXISTS idx_crm_metas_periodo
    ON crm_metas (anio, mes);

CREATE INDEX IF NOT EXISTS idx_crm_metas_responsable_periodo
    ON crm_metas (responsable_id, anio, mes)
    WHERE responsable_id IS NOT NULL;
