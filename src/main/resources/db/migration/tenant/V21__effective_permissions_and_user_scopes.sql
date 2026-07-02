ALTER TABLE roles
    ADD COLUMN IF NOT EXISTS deprecated BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE roles
SET deprecated = TRUE
WHERE codigo IN ('ADMIN', 'SALES');

INSERT INTO permisos (codigo, nombre, descripcion, modulo, activo, sistema)
VALUES
    ('VENTAS_READ', 'Ver ventas', 'Consultar ventas del tenant', 'VENTAS', TRUE, TRUE),
    ('VENTAS_CREATE', 'Registrar ventas', 'Registrar nuevas ventas', 'VENTAS', TRUE, TRUE),
    ('VENTAS_CANCEL', 'Anular ventas', 'Anular ventas registradas', 'VENTAS', TRUE, TRUE),
    ('VENTAS_REPRINT_TICKET', 'Reimprimir tickets', 'Reimprimir comprobantes de venta', 'VENTAS', TRUE, TRUE),
    ('VENTAS_APPLY_DISCOUNT', 'Aplicar descuentos', 'Aplicar descuentos en ventas', 'VENTAS', TRUE, TRUE),
    ('VENTAS_OVERRIDE_PRICE', 'Modificar precio de venta', 'Modificar precios durante la venta', 'VENTAS', TRUE, TRUE),
    ('VENTAS_CREDIT', 'Vender a credito', 'Registrar ventas a credito', 'VENTAS', TRUE, TRUE),
    ('VENTAS_VIEW_MARGIN', 'Ver margen de venta', 'Consultar costos y margenes de venta', 'VENTAS', TRUE, TRUE),
    ('CAJA_OPEN', 'Abrir caja', 'Abrir una caja asignada', 'CAJA', TRUE, TRUE),
    ('CAJA_CLOSE', 'Cerrar caja', 'Cerrar una caja asignada', 'CAJA', TRUE, TRUE),
    ('CAJA_MOVEMENT_CREATE', 'Registrar movimiento de caja', 'Registrar ingresos y egresos de caja', 'CAJA', TRUE, TRUE),
    ('CAJA_WITHDRAW', 'Retirar de caja', 'Registrar retiros de caja', 'CAJA', TRUE, TRUE),
    ('CAJA_DEPOSIT', 'Depositar desde caja', 'Registrar depositos de caja', 'CAJA', TRUE, TRUE),
    ('CAJA_VIEW_OTHERS', 'Ver cajas de otros usuarios', 'Consultar cajas operadas por otros usuarios', 'CAJA', TRUE, TRUE),
    ('CAJA_REOPEN', 'Reabrir caja', 'Reabrir una caja cerrada', 'CAJA', TRUE, TRUE),
    ('PRODUCTOS_READ', 'Ver productos', 'Consultar productos', 'INVENTORY', TRUE, TRUE),
    ('PRODUCTOS_WRITE', 'Gestionar productos', 'Crear y editar productos', 'INVENTORY', TRUE, TRUE),
    ('INVENTORY_ENTRY', 'Registrar entradas', 'Registrar entradas de inventario', 'INVENTORY', TRUE, TRUE),
    ('INVENTORY_EXIT', 'Registrar salidas', 'Registrar salidas de inventario', 'INVENTORY', TRUE, TRUE),
    ('INVENTORY_ADJUST', 'Ajustar inventario', 'Registrar ajustes de inventario', 'INVENTORY', TRUE, TRUE),
    ('INVENTORY_TRANSFER', 'Transferir inventario', 'Transferir stock entre almacenes', 'INVENTORY', TRUE, TRUE),
    ('INVENTORY_VIEW_COST', 'Ver costos', 'Consultar costos de inventario', 'INVENTORY', TRUE, TRUE),
    ('INVENTORY_MANAGE_LOTS', 'Gestionar lotes', 'Gestionar lotes y vencimientos', 'INVENTORY', TRUE, TRUE),
    ('FACTURACION_READ', 'Ver facturacion', 'Consultar comprobantes electronicos', 'FACTURACION', TRUE, TRUE),
    ('FACTURACION_EMIT', 'Emitir comprobantes', 'Emitir comprobantes electronicos', 'FACTURACION', TRUE, TRUE),
    ('FACTURACION_RETRY', 'Reintentar facturacion', 'Reintentar el envio al facturador', 'FACTURACION', TRUE, TRUE),
    ('FACTURACION_DOWNLOAD_FILES', 'Descargar archivos SUNAT', 'Descargar PDF, XML y CDR', 'FACTURACION', TRUE, TRUE),
    ('NOTA_CREDITO_CREATE', 'Emitir nota de credito', 'Registrar notas de credito', 'FACTURACION', TRUE, TRUE),
    ('NOTA_DEBITO_CREATE', 'Emitir nota de debito', 'Registrar notas de debito', 'FACTURACION', TRUE, TRUE),
    ('GUIA_REMISION_CREATE', 'Emitir guia de remision', 'Registrar guias de remision', 'FACTURACION', TRUE, TRUE),
    ('CLIENTES_VIEW_DEBT', 'Ver deuda de clientes', 'Consultar deuda y credito disponible', 'CLIENTES', TRUE, TRUE),
    ('CLIENTES_CHANGE_CREDIT_LIMIT', 'Cambiar limite de credito', 'Modificar el limite de credito del cliente', 'CLIENTES', TRUE, TRUE),
    ('CLIENTES_REGISTER_PAYMENT', 'Registrar pago de cliente', 'Registrar abonos de deuda', 'CLIENTES', TRUE, TRUE),
    ('CLIENTES_AUTHORIZE_CREDIT', 'Autorizar credito', 'Autorizar ventas a credito', 'CLIENTES', TRUE, TRUE),
    ('USUARIOS_READ', 'Ver usuarios', 'Consultar usuarios del tenant', 'SEGURIDAD', TRUE, TRUE),
    ('USUARIOS_WRITE', 'Gestionar usuarios', 'Crear y editar usuarios del tenant', 'SEGURIDAD', TRUE, TRUE),
    ('REPORTES_READ', 'Ver reportes', 'Consultar y exportar reportes', 'REPORTES', TRUE, TRUE),
    ('AUDITORIA_READ', 'Ver auditoria', 'Consultar trazabilidad y auditoria', 'AUDITORIA', TRUE, TRUE),
    ('CONFIGURACION_WRITE', 'Gestionar configuracion', 'Modificar configuracion empresarial', 'CONFIGURACION', TRUE, TRUE)
ON CONFLICT (codigo) DO UPDATE
SET activo = TRUE, sistema = TRUE, updated_at = now();

UPDATE permisos
SET sistema = TRUE
WHERE codigo IN ('CAJA_READ', 'INVENTORY_READ', 'CLIENTES_READ', 'CLIENTES_WRITE',
                 'ROLES_READ', 'ROLES_WRITE', 'SUCURSALES_READ', 'SUCURSALES_WRITE');

INSERT INTO roles (codigo, nombre, descripcion, activo, sistema, deprecated)
VALUES
    ('ADMIN_EMPRESA', 'Administrador de empresa', 'Administra el tenant y todos sus recursos', TRUE, TRUE, FALSE),
    ('SUPERVISOR_SUCURSAL', 'Supervisor de sucursal', 'Supervisa operaciones de una o mas sucursales', TRUE, TRUE, FALSE),
    ('CAJERO', 'Cajero', 'Opera ventas y cajas asignadas', TRUE, TRUE, FALSE),
    ('VENDEDOR', 'Vendedor', 'Registra ventas y gestiona clientes', TRUE, TRUE, FALSE),
    ('ALMACENERO', 'Almacenero', 'Gestiona inventario y almacenes asignados', TRUE, TRUE, FALSE),
    ('CONTADOR', 'Contador', 'Gestiona facturacion electronica y reportes', TRUE, TRUE, FALSE),
    ('AUDITOR', 'Auditor', 'Consulta operaciones y auditoria sin modificar datos', TRUE, TRUE, FALSE)
ON CONFLICT (codigo) DO UPDATE
SET sistema = TRUE, activo = TRUE, deprecated = FALSE, updated_at = now();

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permisos p
WHERE r.codigo = 'ADMIN_EMPRESA' AND p.activo = TRUE
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r
JOIN permisos p ON p.codigo IN (
    'VENTAS_READ', 'VENTAS_CREATE', 'VENTAS_REPRINT_TICKET',
    'CAJA_READ', 'CAJA_OPEN', 'CAJA_CLOSE', 'CAJA_MOVEMENT_CREATE', 'CAJA_DEPOSIT',
    'CLIENTES_READ', 'PRODUCTOS_READ', 'INVENTORY_READ'
)
WHERE r.codigo = 'CAJERO'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r
JOIN permisos p ON p.codigo IN (
    'VENTAS_READ', 'VENTAS_CREATE', 'VENTAS_APPLY_DISCOUNT', 'VENTAS_CREDIT',
    'CLIENTES_READ', 'CLIENTES_WRITE', 'CLIENTES_VIEW_DEBT',
    'PRODUCTOS_READ', 'INVENTORY_READ'
)
WHERE r.codigo = 'VENDEDOR'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r
JOIN permisos p ON p.codigo IN (
    'PRODUCTOS_READ', 'INVENTORY_READ', 'INVENTORY_ENTRY', 'INVENTORY_EXIT',
    'INVENTORY_ADJUST', 'INVENTORY_TRANSFER', 'INVENTORY_VIEW_COST', 'INVENTORY_MANAGE_LOTS'
)
WHERE r.codigo = 'ALMACENERO'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r
JOIN permisos p ON p.codigo IN (
    'VENTAS_READ', 'VENTAS_CANCEL', 'VENTAS_REPRINT_TICKET', 'VENTAS_APPLY_DISCOUNT',
    'VENTAS_VIEW_MARGIN', 'CAJA_READ', 'CAJA_VIEW_OTHERS', 'CLIENTES_READ',
    'CLIENTES_VIEW_DEBT', 'PRODUCTOS_READ', 'INVENTORY_READ', 'REPORTES_READ',
    'SUCURSALES_READ'
)
WHERE r.codigo = 'SUPERVISOR_SUCURSAL'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r
JOIN permisos p ON p.codigo IN (
    'VENTAS_READ', 'CLIENTES_READ', 'CLIENTES_VIEW_DEBT', 'REPORTES_READ',
    'FACTURACION_READ', 'FACTURACION_EMIT', 'FACTURACION_RETRY',
    'FACTURACION_DOWNLOAD_FILES', 'NOTA_CREDITO_CREATE', 'NOTA_DEBITO_CREATE',
    'GUIA_REMISION_CREATE'
)
WHERE r.codigo = 'CONTADOR'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r
JOIN permisos p ON p.codigo IN (
    'VENTAS_READ', 'CAJA_READ', 'PRODUCTOS_READ', 'INVENTORY_READ',
    'CLIENTES_READ', 'CLIENTES_VIEW_DEBT', 'FACTURACION_READ',
    'FACTURACION_DOWNLOAD_FILES', 'USUARIOS_READ', 'ROLES_READ',
    'SUCURSALES_READ', 'REPORTES_READ', 'AUDITORIA_READ'
)
WHERE r.codigo = 'AUDITOR'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = current_schema()
          AND table_name = 'sucursales'
    ) THEN
        EXECUTE 'CREATE TABLE IF NOT EXISTS usuario_sucursales (
            id BIGSERIAL PRIMARY KEY,
            usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
            sucursal_id BIGINT NOT NULL REFERENCES sucursales(id) ON DELETE CASCADE,
            created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
            updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
            CONSTRAINT uq_usuario_sucursal UNIQUE (usuario_id, sucursal_id)
        )';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = current_schema()
          AND table_name = 'almacenes'
    ) THEN
        EXECUTE 'CREATE TABLE IF NOT EXISTS usuario_almacenes (
            id BIGSERIAL PRIMARY KEY,
            usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
            almacen_id BIGINT NOT NULL REFERENCES almacenes(id) ON DELETE CASCADE,
            created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
            updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
            CONSTRAINT uq_usuario_almacen UNIQUE (usuario_id, almacen_id)
        )';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = current_schema()
          AND table_name = 'cajas'
    ) THEN
        EXECUTE 'CREATE TABLE IF NOT EXISTS usuario_cajas (
            id BIGSERIAL PRIMARY KEY,
            usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
            caja_id BIGINT NOT NULL REFERENCES cajas(id) ON DELETE CASCADE,
            created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
            updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
            CONSTRAINT uq_usuario_caja UNIQUE (usuario_id, caja_id)
        )';
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS usuario_permisos_especiales (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    permiso_id BIGINT NOT NULL REFERENCES permisos(id) ON DELETE CASCADE,
    tipo VARCHAR(10) NOT NULL CHECK (tipo IN ('GRANT', 'DENY')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_usuario_permiso_especial UNIQUE (usuario_id, permiso_id)
);

CREATE INDEX IF NOT EXISTS idx_usuario_permisos_especiales_usuario ON usuario_permisos_especiales(usuario_id);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = current_schema()
          AND table_name = 'usuario_sucursales'
    ) THEN
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_usuario_sucursales_usuario ON usuario_sucursales(usuario_id)';
        INSERT INTO usuario_sucursales (usuario_id, sucursal_id)
        SELECT u.id, s.id FROM usuarios u CROSS JOIN sucursales s
        ON CONFLICT (usuario_id, sucursal_id) DO NOTHING;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = current_schema()
          AND table_name = 'usuario_almacenes'
    ) THEN
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_usuario_almacenes_usuario ON usuario_almacenes(usuario_id)';
        INSERT INTO usuario_almacenes (usuario_id, almacen_id)
        SELECT u.id, a.id FROM usuarios u CROSS JOIN almacenes a
        ON CONFLICT (usuario_id, almacen_id) DO NOTHING;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = current_schema()
          AND table_name = 'usuario_cajas'
    ) THEN
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_usuario_cajas_usuario ON usuario_cajas(usuario_id)';
        INSERT INTO usuario_cajas (usuario_id, caja_id)
        SELECT u.id, c.id FROM usuarios u CROSS JOIN cajas c
        ON CONFLICT (usuario_id, caja_id) DO NOTHING;
    END IF;
END $$;
