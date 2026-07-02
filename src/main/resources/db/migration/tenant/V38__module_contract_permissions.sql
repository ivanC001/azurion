INSERT INTO permisos (codigo, nombre, descripcion, modulo, activo, sistema)
VALUES
    ('MODULOS_READ', 'Ver modulos globales', 'Consultar catalogo global de modulos contratables', 'SAAS_CORE', TRUE, TRUE),
    ('MODULOS_WRITE', 'Gestionar modulos globales', 'Crear y actualizar modulos globales contratables', 'SAAS_CORE', TRUE, TRUE),
    ('PLANES_READ', 'Ver planes globales', 'Consultar catalogo global de planes comerciales', 'SAAS_CORE', TRUE, TRUE),
    ('PLANES_WRITE', 'Gestionar planes globales', 'Crear y actualizar planes comerciales y sus modulos', 'SAAS_CORE', TRUE, TRUE),
    ('EMPRESA_MODULOS_READ', 'Ver modulos contratados', 'Consultar modulos contratados por la empresa actual', 'SAAS_CORE', TRUE, TRUE),
    ('EMPRESA_MODULOS_WRITE', 'Gestionar modulos contratados', 'Modificar modulos contratados por una empresa', 'SAAS_CORE', TRUE, TRUE)
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
JOIN permisos p ON p.codigo = 'EMPRESA_MODULOS_READ'
WHERE r.codigo = 'ADMIN_EMPRESA'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;
