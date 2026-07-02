INSERT INTO roles (codigo, nombre, descripcion, activo)
VALUES ('ADMIN_EMPRESA', 'Administrador de Empresa', 'Rol administrativo del tenant para gestion de accesos', TRUE)
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT admin_empresa.id, rp.permiso_id
FROM roles admin_base
JOIN rol_permisos rp ON rp.rol_id = admin_base.id
JOIN roles admin_empresa ON admin_empresa.codigo = 'ADMIN_EMPRESA'
WHERE admin_base.codigo = 'ADMIN'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;
