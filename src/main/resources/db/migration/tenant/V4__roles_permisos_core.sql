CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(80) NOT NULL UNIQUE,
    nombre VARCHAR(120) NOT NULL,
    descripcion VARCHAR(400),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS permisos (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(120) NOT NULL UNIQUE,
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(400),
    modulo VARCHAR(80) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS rol_permisos (
    id BIGSERIAL PRIMARY KEY,
    rol_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permiso_id BIGINT NOT NULL REFERENCES permisos(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_rol_permiso UNIQUE (rol_id, permiso_id)
);

CREATE INDEX IF NOT EXISTS idx_rol_permisos_rol_id ON rol_permisos(rol_id);
CREATE INDEX IF NOT EXISTS idx_rol_permisos_permiso_id ON rol_permisos(permiso_id);

INSERT INTO permisos (codigo, nombre, descripcion, modulo, activo)
VALUES
    ('EMPRESAS_READ', 'Ver empresas', 'Consultar empresas registradas', 'SAAS_CORE', TRUE),
    ('EMPRESAS_WRITE', 'Gestionar empresas', 'Crear y actualizar empresas', 'SAAS_CORE', TRUE),
    ('INVENTORY_READ', 'Ver inventario', 'Consultar productos, stock y kardex', 'INVENTORY', TRUE),
    ('INVENTORY_WRITE', 'Gestionar inventario', 'Crear productos, almacenes y movimientos', 'INVENTORY', TRUE),
    ('CLIENTES_READ', 'Ver clientes', 'Consultar clientes', 'CLIENTES', TRUE),
    ('CLIENTES_WRITE', 'Gestionar clientes', 'Crear y editar clientes', 'CLIENTES', TRUE),
    ('ROLES_READ', 'Ver roles', 'Consultar roles y permisos', 'SEGURIDAD', TRUE),
    ('ROLES_WRITE', 'Gestionar roles', 'Crear roles y asignar permisos', 'SEGURIDAD', TRUE)
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO roles (codigo, nombre, descripcion, activo)
VALUES ('ADMIN', 'Administrador', 'Rol administrativo con control operativo del tenant', TRUE)
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo IN (
    'EMPRESAS_READ', 'EMPRESAS_WRITE',
    'INVENTORY_READ', 'INVENTORY_WRITE',
    'CLIENTES_READ', 'CLIENTES_WRITE',
    'ROLES_READ', 'ROLES_WRITE'
)
WHERE r.codigo = 'ADMIN'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;
