INSERT INTO permisos (codigo, nombre, descripcion, modulo, activo, sistema)
VALUES
    ('CRM_CONFIG_MANAGE', 'Configurar CRM', 'Administrar configuracion, etapas y reglas del CRM', 'CRM', TRUE, TRUE),
    ('CRM_CATALOG_MANAGE', 'Gestionar catalogo CRM', 'Crear y publicar ofertas comerciales para CRM y landings', 'CRM', TRUE, TRUE),
    ('CRM_LEADS_READ', 'Ver leads CRM', 'Consultar leads y prospectos captados por CRM', 'CRM', TRUE, TRUE),
    ('CRM_LEADS_WRITE', 'Gestionar leads CRM', 'Crear, editar y calificar leads o prospectos CRM', 'CRM', TRUE, TRUE),
    ('CRM_ACTIVITIES_READ', 'Ver actividades CRM', 'Consultar tareas, llamadas y seguimientos CRM', 'CRM', TRUE, TRUE),
    ('CRM_ACTIVITIES_WRITE', 'Gestionar actividades CRM', 'Registrar y cerrar tareas, llamadas y seguimientos CRM', 'CRM', TRUE, TRUE),
    ('CRM_OPPORTUNITIES_READ', 'Ver oportunidades CRM', 'Consultar oportunidades comerciales CRM', 'CRM', TRUE, TRUE),
    ('CRM_OPPORTUNITIES_WRITE', 'Gestionar oportunidades CRM', 'Crear y editar oportunidades comerciales CRM', 'CRM', TRUE, TRUE),
    ('CRM_OPPORTUNITIES_STAGE', 'Mover oportunidades CRM', 'Avanzar o retroceder oportunidades dentro del embudo', 'CRM', TRUE, TRUE),
    ('CRM_OPPORTUNITIES_CLOSE', 'Cerrar oportunidades CRM', 'Marcar oportunidades como ganadas o perdidas', 'CRM', TRUE, TRUE),
    ('CRM_QUOTES_CREATE', 'Crear cotizaciones CRM', 'Generar cotizaciones desde oportunidades CRM', 'CRM', TRUE, TRUE),
    ('CRM_PROSPECTS_CONVERT', 'Convertir prospectos CRM', 'Convertir prospectos CRM en clientes del tenant', 'CRM', TRUE, TRUE),
    ('CRM_REPORTS_TEAM', 'Ver reportes de equipo CRM', 'Consultar metricas CRM de equipo, vendedores y canales', 'CRM', TRUE, TRUE)
ON CONFLICT (codigo) DO UPDATE
SET nombre = EXCLUDED.nombre,
    descripcion = EXCLUDED.descripcion,
    modulo = EXCLUDED.modulo,
    activo = TRUE,
    sistema = TRUE,
    updated_at = now();

INSERT INTO roles (codigo, nombre, descripcion, activo, sistema, deprecated)
VALUES
    ('CRM_ADMIN', 'Administrador CRM', 'Control total del CRM, configuracion, catalogo, pipeline, equipo y reportes', TRUE, TRUE, FALSE),
    ('CRM_GERENTE', 'Gerente comercial CRM', 'Supervisa estrategia comercial, reportes, cierres y desempeno del equipo', TRUE, TRUE, FALSE),
    ('CRM_SUPERVISOR', 'Supervisor CRM', 'Gestiona leads, asignaciones, oportunidades y seguimiento del equipo comercial', TRUE, TRUE, FALSE),
    ('CRM_VENDEDOR', 'Vendedor CRM', 'Gestiona sus prospectos, actividades, oportunidades y cotizaciones', TRUE, TRUE, FALSE),
    ('CRM_MARKETING', 'Marketing CRM', 'Administra campanas, catalogo comercial, landing y leads entrantes', TRUE, TRUE, FALSE),
    ('CRM_CALLCENTER', 'Call center CRM', 'Atiende leads nuevos, registra contacto inicial y deriva prospectos calificados', TRUE, TRUE, FALSE)
ON CONFLICT (codigo) DO UPDATE
SET nombre = EXCLUDED.nombre,
    descripcion = EXCLUDED.descripcion,
    activo = TRUE,
    sistema = TRUE,
    deprecated = FALSE,
    updated_at = now();

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permisos p
WHERE r.codigo IN ('ADMIN_EMPRESA', 'CRM_ADMIN')
  AND p.modulo = 'CRM'
  AND p.activo = TRUE
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo IN (
    'CRM_READ', 'CRM_WRITE', 'CRM_VIEW_ALL', 'CRM_ASSIGN',
    'CRM_LEADS_READ', 'CRM_LEADS_WRITE',
    'CRM_ACTIVITIES_READ', 'CRM_ACTIVITIES_WRITE',
    'CRM_OPPORTUNITIES_READ', 'CRM_OPPORTUNITIES_WRITE',
    'CRM_OPPORTUNITIES_STAGE', 'CRM_OPPORTUNITIES_CLOSE',
    'CRM_QUOTES_CREATE', 'CRM_PROSPECTS_CONVERT',
    'CRM_PIPELINE_READ', 'CRM_PIPELINE_WRITE',
    'CRM_CONVERT_CLIENT', 'CRM_CONVERT_SALE',
    'CRM_REPORTS_READ', 'CRM_REPORTS_TEAM'
)
WHERE r.codigo = 'CRM_GERENTE'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo IN (
    'CRM_READ', 'CRM_WRITE', 'CRM_VIEW_ALL', 'CRM_ASSIGN',
    'CRM_LEADS_READ', 'CRM_LEADS_WRITE',
    'CRM_ACTIVITIES_READ', 'CRM_ACTIVITIES_WRITE',
    'CRM_OPPORTUNITIES_READ', 'CRM_OPPORTUNITIES_WRITE',
    'CRM_OPPORTUNITIES_STAGE', 'CRM_OPPORTUNITIES_CLOSE',
    'CRM_QUOTES_CREATE', 'CRM_PROSPECTS_CONVERT',
    'CRM_PIPELINE_READ', 'CRM_PIPELINE_WRITE',
    'CRM_REPORTS_READ', 'CRM_REPORTS_TEAM'
)
WHERE r.codigo IN ('CRM_SUPERVISOR', 'SUPERVISOR_SUCURSAL')
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo IN (
    'CRM_READ', 'CRM_WRITE',
    'CRM_LEADS_READ', 'CRM_LEADS_WRITE',
    'CRM_ACTIVITIES_READ', 'CRM_ACTIVITIES_WRITE',
    'CRM_OPPORTUNITIES_READ', 'CRM_OPPORTUNITIES_WRITE',
    'CRM_OPPORTUNITIES_STAGE', 'CRM_OPPORTUNITIES_CLOSE',
    'CRM_QUOTES_CREATE',
    'CRM_PIPELINE_READ', 'CRM_PIPELINE_WRITE'
)
WHERE r.codigo IN ('CRM_VENDEDOR', 'VENDEDOR')
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo IN (
    'CRM_READ', 'CRM_WRITE', 'CRM_VIEW_ALL',
    'CRM_CATALOG_MANAGE',
    'CRM_LEADS_READ', 'CRM_LEADS_WRITE',
    'CRM_ACTIVITIES_READ', 'CRM_ACTIVITIES_WRITE',
    'CRM_OPPORTUNITIES_READ',
    'CRM_PIPELINE_READ',
    'CRM_REPORTS_READ', 'CRM_REPORTS_TEAM'
)
WHERE r.codigo = 'CRM_MARKETING'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo IN (
    'CRM_READ', 'CRM_WRITE',
    'CRM_LEADS_READ', 'CRM_LEADS_WRITE',
    'CRM_ACTIVITIES_READ', 'CRM_ACTIVITIES_WRITE',
    'CRM_OPPORTUNITIES_READ',
    'CRM_PIPELINE_READ'
)
WHERE r.codigo = 'CRM_CALLCENTER'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo IN (
    'CRM_READ', 'CRM_VIEW_ALL',
    'CRM_LEADS_READ', 'CRM_ACTIVITIES_READ', 'CRM_OPPORTUNITIES_READ',
    'CRM_PIPELINE_READ', 'CRM_REPORTS_READ', 'CRM_REPORTS_TEAM'
)
WHERE r.codigo = 'AUDITOR'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;
