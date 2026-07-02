INSERT INTO permisos (codigo, nombre, descripcion, modulo, activo, sistema)
VALUES
    ('CRM_READ', 'Ver CRM', 'Consultar prospectos, oportunidades y actividades CRM', 'CRM', TRUE, TRUE),
    ('CRM_WRITE', 'Gestionar CRM', 'Crear y actualizar prospectos, oportunidades y actividades CRM', 'CRM', TRUE, TRUE),
    ('CRM_ASSIGN', 'Asignar responsables CRM', 'Asignar prospectos y oportunidades a otros usuarios', 'CRM', TRUE, TRUE),
    ('CRM_VIEW_ALL', 'Ver todo el CRM', 'Consultar registros CRM de todos los responsables', 'CRM', TRUE, TRUE),
    ('CRM_CONVERT_CLIENT', 'Convertir prospecto a cliente', 'Crear clientes desde prospectos CRM', 'CRM', TRUE, TRUE),
    ('CRM_CONVERT_SALE', 'Convertir oportunidad a venta', 'Generar cotizaciones y ventas desde oportunidades CRM', 'CRM', TRUE, TRUE),
    ('CRM_REPORTS_READ', 'Ver reportes CRM', 'Consultar dashboard y reportes comerciales CRM', 'CRM', TRUE, TRUE),
    ('CRM_DELETE', 'Eliminar registros CRM', 'Eliminar o descartar registros CRM', 'CRM', TRUE, TRUE)
ON CONFLICT (codigo) DO UPDATE
SET nombre = EXCLUDED.nombre,
    descripcion = EXCLUDED.descripcion,
    modulo = EXCLUDED.modulo,
    activo = TRUE,
    sistema = TRUE,
    updated_at = now();

CREATE TABLE IF NOT EXISTS crm_prospectos (
    id BIGSERIAL PRIMARY KEY,
    tipo_persona VARCHAR(20) NOT NULL,
    tipo_documento VARCHAR(5),
    numero_documento VARCHAR(20),
    nombre VARCHAR(180) NOT NULL,
    razon_social VARCHAR(220),
    nombre_comercial VARCHAR(180),
    telefono VARCHAR(40),
    correo VARCHAR(180),
    direccion VARCHAR(500),
    origen VARCHAR(30) NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'NUEVO',
    responsable_id VARCHAR(80) NOT NULL,
    observacion VARCHAR(1000),
    cliente_id BIGINT REFERENCES clientes(id),
    fecha_conversion TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_crm_prospectos_tipo_persona CHECK (tipo_persona IN ('NATURAL', 'JURIDICA')),
    CONSTRAINT chk_crm_prospectos_origen CHECK (origen IN ('WHATSAPP', 'FACEBOOK', 'WEB', 'REFERIDO', 'LLAMADA', 'VISITA', 'OTRO')),
    CONSTRAINT chk_crm_prospectos_estado CHECK (estado IN ('NUEVO', 'CONTACTADO', 'INTERESADO', 'NO_INTERESADO', 'CONVERTIDO', 'DESCARTADO'))
);

CREATE TABLE IF NOT EXISTS crm_oportunidades (
    id BIGSERIAL PRIMARY KEY,
    prospecto_id BIGINT REFERENCES crm_prospectos(id),
    cliente_id BIGINT REFERENCES clientes(id),
    titulo VARCHAR(220) NOT NULL,
    descripcion VARCHAR(1000),
    monto_estimado NUMERIC(18, 2) NOT NULL DEFAULT 0,
    probabilidad INTEGER NOT NULL DEFAULT 0,
    etapa VARCHAR(30) NOT NULL DEFAULT 'NUEVO',
    fecha_cierre_estimada DATE,
    responsable_id VARCHAR(80) NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'ABIERTA',
    motivo_perdida VARCHAR(500),
    fecha_cierre_real TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_crm_oportunidades_etapa CHECK (etapa IN ('NUEVO', 'CONTACTADO', 'INTERESADO', 'COTIZADO', 'NEGOCIACION', 'GANADO', 'PERDIDO')),
    CONSTRAINT chk_crm_oportunidades_estado CHECK (estado IN ('ABIERTA', 'GANADA', 'PERDIDA')),
    CONSTRAINT chk_crm_oportunidades_probabilidad CHECK (probabilidad BETWEEN 0 AND 100),
    CONSTRAINT chk_crm_oportunidades_monto CHECK (monto_estimado >= 0),
    CONSTRAINT chk_crm_oportunidades_vinculo CHECK (prospecto_id IS NOT NULL OR cliente_id IS NOT NULL)
);

CREATE TABLE IF NOT EXISTS crm_actividades (
    id BIGSERIAL PRIMARY KEY,
    prospecto_id BIGINT REFERENCES crm_prospectos(id),
    oportunidad_id BIGINT REFERENCES crm_oportunidades(id),
    cliente_id BIGINT REFERENCES clientes(id),
    tipo_actividad VARCHAR(30) NOT NULL,
    asunto VARCHAR(220) NOT NULL,
    descripcion VARCHAR(1000),
    fecha_programada TIMESTAMPTZ NOT NULL,
    fecha_realizada TIMESTAMPTZ,
    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    usuario_id VARCHAR(80) NOT NULL,
    resultado VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_crm_actividades_tipo CHECK (tipo_actividad IN ('LLAMADA', 'WHATSAPP', 'CORREO', 'REUNION', 'VISITA', 'TAREA', 'NOTA')),
    CONSTRAINT chk_crm_actividades_estado CHECK (estado IN ('PENDIENTE', 'REALIZADA', 'CANCELADA', 'VENCIDA')),
    CONSTRAINT chk_crm_actividades_vinculo CHECK (prospecto_id IS NOT NULL OR oportunidad_id IS NOT NULL OR cliente_id IS NOT NULL)
);

ALTER TABLE cotizaciones
    ADD COLUMN IF NOT EXISTS crm_oportunidad_id BIGINT REFERENCES crm_oportunidades(id);

CREATE INDEX IF NOT EXISTS idx_crm_prospectos_estado ON crm_prospectos(estado);
CREATE INDEX IF NOT EXISTS idx_crm_prospectos_responsable ON crm_prospectos(responsable_id);
CREATE INDEX IF NOT EXISTS idx_crm_prospectos_cliente ON crm_prospectos(cliente_id);
CREATE INDEX IF NOT EXISTS idx_crm_oportunidades_estado ON crm_oportunidades(estado);
CREATE INDEX IF NOT EXISTS idx_crm_oportunidades_etapa ON crm_oportunidades(etapa);
CREATE INDEX IF NOT EXISTS idx_crm_oportunidades_responsable ON crm_oportunidades(responsable_id);
CREATE INDEX IF NOT EXISTS idx_crm_oportunidades_prospecto ON crm_oportunidades(prospecto_id);
CREATE INDEX IF NOT EXISTS idx_crm_oportunidades_cliente ON crm_oportunidades(cliente_id);
CREATE INDEX IF NOT EXISTS idx_crm_actividades_estado ON crm_actividades(estado);
CREATE INDEX IF NOT EXISTS idx_crm_actividades_usuario ON crm_actividades(usuario_id);
CREATE INDEX IF NOT EXISTS idx_crm_actividades_programada ON crm_actividades(fecha_programada);
CREATE INDEX IF NOT EXISTS idx_cotizaciones_crm_oportunidad ON cotizaciones(crm_oportunidad_id);

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permisos p
WHERE r.codigo = 'ADMIN_EMPRESA'
  AND p.codigo IN ('CRM_READ', 'CRM_WRITE', 'CRM_ASSIGN', 'CRM_VIEW_ALL', 'CRM_CONVERT_CLIENT',
                   'CRM_CONVERT_SALE', 'CRM_REPORTS_READ', 'CRM_DELETE')
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo IN ('CRM_READ', 'CRM_WRITE')
WHERE r.codigo = 'VENDEDOR'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo IN ('CRM_READ', 'CRM_WRITE', 'CRM_ASSIGN', 'CRM_VIEW_ALL', 'CRM_REPORTS_READ')
WHERE r.codigo = 'SUPERVISOR_SUCURSAL'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo IN ('CRM_READ', 'CRM_REPORTS_READ')
WHERE r.codigo = 'AUDITOR'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;
