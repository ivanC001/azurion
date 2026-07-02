INSERT INTO permisos (codigo, nombre, descripcion, modulo, activo, sistema)
VALUES
    ('CRM_PIPELINE_READ', 'Ver embudo CRM', 'Consultar embudo comercial y tablero Kanban', 'CRM', TRUE, TRUE),
    ('CRM_PIPELINE_WRITE', 'Gestionar embudo CRM', 'Mover oportunidades y administrar etapas del embudo', 'CRM', TRUE, TRUE),
    ('CRM_REPORTS_READ', 'Ver reportes CRM', 'Consultar indicadores y conversiones comerciales CRM', 'CRM', TRUE, TRUE)
ON CONFLICT (codigo) DO UPDATE
SET nombre = EXCLUDED.nombre,
    descripcion = EXCLUDED.descripcion,
    modulo = EXCLUDED.modulo,
    activo = TRUE,
    sistema = TRUE,
    updated_at = now();

ALTER TABLE crm_prospectos
    DROP CONSTRAINT IF EXISTS chk_crm_prospectos_origen;

ALTER TABLE crm_prospectos
    ADD CONSTRAINT chk_crm_prospectos_origen
    CHECK (origen IN ('WHATSAPP', 'FACEBOOK', 'INSTAGRAM', 'WEB', 'REFERIDO', 'LLAMADA', 'VISITA', 'OTRO'));

CREATE TABLE IF NOT EXISTS crm_etapas_pipeline (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(40) NOT NULL UNIQUE,
    nombre VARCHAR(120) NOT NULL,
    orden INTEGER NOT NULL,
    color VARCHAR(20) NOT NULL DEFAULT '#2563eb',
    es_ganado BOOLEAN NOT NULL DEFAULT FALSE,
    es_perdido BOOLEAN NOT NULL DEFAULT FALSE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_crm_etapas_pipeline_orden CHECK (orden > 0),
    CONSTRAINT chk_crm_etapas_pipeline_estado_unico CHECK (NOT (es_ganado AND es_perdido))
);

INSERT INTO crm_etapas_pipeline (codigo, nombre, orden, color, es_ganado, es_perdido, activo)
VALUES
    ('NUEVO', 'Nuevo', 1, '#2563eb', FALSE, FALSE, TRUE),
    ('CONTACTADO', 'Contactado', 2, '#0ea5e9', FALSE, FALSE, TRUE),
    ('INTERESADO', 'Interesado', 3, '#14b8a6', FALSE, FALSE, TRUE),
    ('COTIZADO', 'Cotizado', 4, '#f59e0b', FALSE, FALSE, TRUE),
    ('NEGOCIACION', 'Negociacion', 5, '#8b5cf6', FALSE, FALSE, TRUE),
    ('GANADO', 'Ganado', 6, '#10b981', TRUE, FALSE, TRUE),
    ('PERDIDO', 'Perdido', 7, '#ef4444', FALSE, TRUE, TRUE)
ON CONFLICT (codigo) DO UPDATE
SET nombre = EXCLUDED.nombre,
    orden = EXCLUDED.orden,
    color = EXCLUDED.color,
    es_ganado = EXCLUDED.es_ganado,
    es_perdido = EXCLUDED.es_perdido,
    activo = TRUE,
    updated_at = now();

ALTER TABLE crm_oportunidades
    DROP CONSTRAINT IF EXISTS chk_crm_oportunidades_etapa;

ALTER TABLE crm_oportunidades
    ADD COLUMN IF NOT EXISTS etapa_id BIGINT REFERENCES crm_etapas_pipeline(id),
    ADD COLUMN IF NOT EXISTS fecha_ultima_actualizacion TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS fecha_ganada TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS fecha_perdida TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS monto_real NUMERIC(18, 2);

UPDATE crm_oportunidades o
SET etapa_id = e.id,
    fecha_ultima_actualizacion = COALESCE(o.fecha_ultima_actualizacion, o.updated_at, now()),
    fecha_ganada = CASE WHEN o.estado = 'GANADA' THEN COALESCE(o.fecha_ganada, o.fecha_cierre_real, o.updated_at, now()) ELSE o.fecha_ganada END,
    fecha_perdida = CASE WHEN o.estado = 'PERDIDA' THEN COALESCE(o.fecha_perdida, o.fecha_cierre_real, o.updated_at, now()) ELSE o.fecha_perdida END
FROM crm_etapas_pipeline e
WHERE e.codigo = COALESCE(NULLIF(o.etapa, ''), 'NUEVO')
  AND o.etapa_id IS NULL;

UPDATE crm_oportunidades o
SET etapa_id = e.id
FROM crm_etapas_pipeline e
WHERE e.codigo = 'NUEVO'
  AND o.etapa_id IS NULL;

ALTER TABLE crm_oportunidades
    ALTER COLUMN etapa_id SET NOT NULL,
    ALTER COLUMN fecha_ultima_actualizacion SET DEFAULT now();

CREATE TABLE IF NOT EXISTS crm_oportunidad_historial (
    id BIGSERIAL PRIMARY KEY,
    oportunidad_id BIGINT NOT NULL REFERENCES crm_oportunidades(id) ON DELETE CASCADE,
    etapa_origen_id BIGINT REFERENCES crm_etapas_pipeline(id),
    etapa_destino_id BIGINT NOT NULL REFERENCES crm_etapas_pipeline(id),
    usuario_id VARCHAR(80) NOT NULL,
    observacion VARCHAR(500),
    fecha_cambio TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_crm_etapas_pipeline_orden ON crm_etapas_pipeline(activo, orden);
CREATE INDEX IF NOT EXISTS idx_crm_oportunidades_etapa_id ON crm_oportunidades(etapa_id);
CREATE INDEX IF NOT EXISTS idx_crm_oportunidades_responsable_estado ON crm_oportunidades(responsable_id, estado);
CREATE INDEX IF NOT EXISTS idx_crm_historial_oportunidad_fecha ON crm_oportunidad_historial(oportunidad_id, fecha_cambio DESC);

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permisos p
WHERE r.codigo = 'ADMIN_EMPRESA'
  AND p.codigo IN ('CRM_PIPELINE_READ', 'CRM_PIPELINE_WRITE', 'CRM_REPORTS_READ')
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo IN ('CRM_PIPELINE_READ', 'CRM_PIPELINE_WRITE')
WHERE r.codigo = 'VENDEDOR'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo IN ('CRM_PIPELINE_READ', 'CRM_PIPELINE_WRITE', 'CRM_REPORTS_READ')
WHERE r.codigo = 'SUPERVISOR_SUCURSAL'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo IN ('CRM_PIPELINE_READ', 'CRM_REPORTS_READ')
WHERE r.codigo = 'AUDITOR'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;
