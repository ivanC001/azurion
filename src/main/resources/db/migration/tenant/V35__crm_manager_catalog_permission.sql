INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo IN ('CRM_CATALOG_MANAGE')
WHERE r.codigo = 'CRM_GERENTE'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;
