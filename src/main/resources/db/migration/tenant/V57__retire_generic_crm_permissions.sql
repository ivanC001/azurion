DELETE FROM rol_permisos rp
USING roles r, permisos p
WHERE rp.rol_id = r.id
  AND rp.permiso_id = p.id
  AND r.codigo LIKE 'CRM_%'
  AND p.codigo IN ('CRM_READ', 'CRM_WRITE');

UPDATE roles
SET deprecated = TRUE,
    updated_at = now()
WHERE codigo IN ('VENDEDOR', 'SUPERVISOR_SUCURSAL', 'AUDITOR');

