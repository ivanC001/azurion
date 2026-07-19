ALTER TABLE roles
    ADD COLUMN IF NOT EXISTS ambito VARCHAR(20) NOT NULL DEFAULT 'ERP';

ALTER TABLE roles
    ADD COLUMN IF NOT EXISTS deprecated BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE roles
    DROP CONSTRAINT IF EXISTS ck_roles_ambito;

ALTER TABLE roles
    ADD CONSTRAINT ck_roles_ambito
        CHECK (ambito IN ('TENANT', 'ERP', 'CRM', 'SHARED', 'MIXED'));

INSERT INTO roles (codigo, nombre, descripcion, activo, sistema, ambito)
VALUES
    ('ERP_ADMIN', 'Administrador ERP', 'Administra la operacion ERP contratada por la empresa', TRUE, TRUE, 'ERP'),
    ('ERP_VENDEDOR', 'Vendedor ERP', 'Gestiona ventas, clientes y cotizaciones ERP', TRUE, TRUE, 'ERP'),
    ('ERP_CAJERO', 'Cajero ERP', 'Opera caja y ventas de mostrador', TRUE, TRUE, 'ERP'),
    ('ERP_ALMACENERO', 'Almacenero ERP', 'Gestiona productos, almacenes y stock', TRUE, TRUE, 'ERP'),
    ('ERP_CONTADOR', 'Contador ERP', 'Consulta ventas y gestiona facturacion y tributacion', TRUE, TRUE, 'ERP'),
    ('CRM_ADMIN', 'Administrador CRM', 'Administra configuracion, equipo y operacion CRM', TRUE, TRUE, 'CRM')
ON CONFLICT (codigo) DO UPDATE
SET nombre = EXCLUDED.nombre,
    descripcion = EXCLUDED.descripcion,
    activo = TRUE,
    sistema = TRUE,
    ambito = EXCLUDED.ambito,
    updated_at = now();

UPDATE roles
SET ambito = CASE
    WHEN codigo IN ('ADMIN', 'ADMIN_EMPRESA') THEN 'TENANT'
    WHEN codigo LIKE 'CRM_%' THEN 'CRM'
    WHEN codigo IN (
        'ERP_ADMIN', 'ERP_VENDEDOR', 'ERP_CAJERO', 'ERP_ALMACENERO', 'ERP_CONTADOR',
        'SUPERVISOR_SUCURSAL', 'CAJERO', 'VENDEDOR', 'ALMACENERO', 'CONTADOR', 'AUDITOR'
    ) THEN 'ERP'
    WHEN EXISTS (
        SELECT 1
        FROM rol_permisos rp
        JOIN permisos p ON p.id = rp.permiso_id
        WHERE rp.rol_id = roles.id AND p.modulo = 'CRM'
    ) AND EXISTS (
        SELECT 1
        FROM rol_permisos rp
        JOIN permisos p ON p.id = rp.permiso_id
        WHERE rp.rol_id = roles.id
          AND p.modulo NOT IN ('CRM', 'CLIENTES', 'COTIZACIONES')
    ) THEN 'MIXED'
    WHEN EXISTS (
        SELECT 1
        FROM rol_permisos rp
        JOIN permisos p ON p.id = rp.permiso_id
        WHERE rp.rol_id = roles.id AND p.modulo = 'CRM'
    ) THEN 'CRM'
    WHEN EXISTS (
        SELECT 1
        FROM rol_permisos rp
        JOIN permisos p ON p.id = rp.permiso_id
        WHERE rp.rol_id = roles.id AND p.modulo IN ('CLIENTES', 'COTIZACIONES')
    ) AND NOT EXISTS (
        SELECT 1
        FROM rol_permisos rp
        JOIN permisos p ON p.id = rp.permiso_id
        WHERE rp.rol_id = roles.id AND p.modulo NOT IN ('CLIENTES', 'COTIZACIONES')
    ) THEN 'SHARED'
    ELSE 'ERP'
END,
updated_at = now();

UPDATE roles
SET deprecated = TRUE,
    updated_at = now()
WHERE codigo IN ('VENDEDOR', 'SUPERVISOR_SUCURSAL', 'AUDITOR');

UPDATE roles
SET deprecated = FALSE,
    updated_at = now()
WHERE codigo IN (
    'ADMIN_EMPRESA', 'ERP_ADMIN', 'ERP_VENDEDOR', 'ERP_CAJERO', 'ERP_ALMACENERO', 'ERP_CONTADOR',
    'CRM_ADMIN', 'CRM_GERENTE', 'CRM_SUPERVISOR', 'CRM_VENDEDOR', 'CRM_MARKETING', 'CRM_CALLCENTER'
);

DELETE FROM rol_permisos rp
USING roles r
WHERE rp.rol_id = r.id
  AND r.codigo = 'ADMIN_EMPRESA';

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permisos p
WHERE r.codigo = 'ADMIN_EMPRESA'
  AND p.activo = TRUE
  AND p.codigo IN (
      'USUARIOS_READ', 'USUARIOS_WRITE',
      'ROLES_READ', 'ROLES_WRITE',
      'SUCURSALES_READ', 'SUCURSALES_WRITE',
      'CONFIGURACION_WRITE', 'EMPRESA_MODULOS_READ', 'AUDITORIA_READ'
  )
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permisos p
WHERE r.codigo = 'ERP_ADMIN'
  AND p.activo = TRUE
  AND p.modulo IN (
      'VENTAS', 'CAJA', 'INVENTORY', 'INVENTARIO', 'PRODUCTOS', 'ALMACENES',
      'COMPRAS', 'CLIENTES', 'COTIZACIONES', 'REPORTES', 'FACTURACION', 'TRIBUTACION'
  )
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permisos p
WHERE r.codigo = 'CRM_ADMIN'
  AND p.activo = TRUE
  AND p.modulo IN ('CRM', 'CLIENTES', 'COTIZACIONES')
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permisos p
WHERE p.activo = TRUE
  AND (
      (r.codigo = 'ERP_VENDEDOR' AND (
          p.modulo IN ('VENTAS', 'CLIENTES', 'COTIZACIONES') OR
          p.codigo IN ('PRODUCTOS_READ', 'INVENTORY_READ')
      )) OR
      (r.codigo = 'ERP_CAJERO' AND (
          p.modulo = 'CAJA' OR
          p.codigo IN ('VENTAS_READ', 'VENTAS_CREATE', 'VENTAS_REPRINT_TICKET', 'CLIENTES_READ', 'PRODUCTOS_READ')
      )) OR
      (r.codigo = 'ERP_ALMACENERO' AND p.modulo IN ('INVENTORY', 'INVENTARIO', 'PRODUCTOS', 'ALMACENES', 'COMPRAS')) OR
      (r.codigo = 'ERP_CONTADOR' AND (
          p.modulo IN ('FACTURACION', 'TRIBUTACION', 'REPORTES') OR
          p.codigo IN ('VENTAS_READ', 'CLIENTES_READ', 'CAJA_READ')
      ))
  )
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

DELETE FROM rol_permisos rp
USING roles r, permisos p
WHERE rp.rol_id = r.id
  AND rp.permiso_id = p.id
  AND p.codigo = 'CRM_WRITE'
  AND r.codigo LIKE 'CRM_%';

INSERT INTO usuario_roles (usuario_id, rol_id)
SELECT DISTINCT ur.usuario_id, product_role.id
FROM usuario_roles ur
JOIN roles tenant_admin ON tenant_admin.id = ur.rol_id AND tenant_admin.codigo = 'ADMIN_EMPRESA'
JOIN roles product_role ON product_role.codigo = 'CRM_ADMIN'
WHERE EXISTS (SELECT 1 FROM permisos p WHERE p.modulo = 'CRM' AND p.activo = TRUE)
ON CONFLICT (usuario_id, rol_id) DO NOTHING;

INSERT INTO usuario_roles (usuario_id, rol_id)
SELECT DISTINCT ur.usuario_id, product_role.id
FROM usuario_roles ur
JOIN roles tenant_admin ON tenant_admin.id = ur.rol_id AND tenant_admin.codigo = 'ADMIN_EMPRESA'
JOIN roles product_role ON product_role.codigo = 'ERP_ADMIN'
WHERE EXISTS (
    SELECT 1
    FROM permisos p
    WHERE p.activo = TRUE
      AND p.modulo IN ('VENTAS', 'CAJA', 'INVENTORY', 'INVENTARIO', 'PRODUCTOS', 'COMPRAS', 'FACTURACION', 'TRIBUTACION')
)
ON CONFLICT (usuario_id, rol_id) DO NOTHING;

CREATE INDEX IF NOT EXISTS idx_roles_ambito_activo ON roles (ambito, activo);
