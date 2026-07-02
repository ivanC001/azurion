INSERT INTO permisos (codigo, nombre, descripcion, modulo, activo, sistema)
VALUES
    ('COTIZACIONES_READ', 'Ver cotizaciones', 'Consultar cotizaciones comerciales', 'COTIZACIONES', TRUE, TRUE),
    ('COTIZACIONES_CREATE', 'Crear cotizaciones', 'Registrar nuevas cotizaciones', 'COTIZACIONES', TRUE, TRUE),
    ('COTIZACIONES_UPDATE', 'Actualizar cotizaciones', 'Editar y cambiar estado de cotizaciones', 'COTIZACIONES', TRUE, TRUE),
    ('COTIZACIONES_PDF', 'Descargar PDF de cotizacion', 'Generar PDF simple de cotizacion', 'COTIZACIONES', TRUE, TRUE)
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
JOIN permisos p ON p.codigo IN ('COTIZACIONES_READ', 'COTIZACIONES_CREATE', 'COTIZACIONES_UPDATE', 'COTIZACIONES_PDF')
WHERE r.codigo IN ('ADMIN', 'ADMIN_EMPRESA', 'CRM_ADMIN', 'CRM_GERENTE', 'CRM_SUPERVISOR', 'CRM_VENDEDOR')
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo = 'COTIZACIONES_READ'
WHERE r.codigo IN ('CRM_MARKETING', 'CRM_CALLCENTER')
ON CONFLICT (rol_id, permiso_id) DO NOTHING;
