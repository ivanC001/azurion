CREATE TABLE IF NOT EXISTS usuarios (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nombres VARCHAR(160) NOT NULL,
    email VARCHAR(180),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    ultimo_acceso TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS usuario_roles (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    rol_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_usuario_rol UNIQUE (usuario_id, rol_id)
);

CREATE INDEX IF NOT EXISTS idx_usuario_roles_usuario_id ON usuario_roles(usuario_id);
CREATE INDEX IF NOT EXISTS idx_usuario_roles_rol_id ON usuario_roles(rol_id);

INSERT INTO roles (codigo, nombre, descripcion, activo)
VALUES
    ('CAJERO', 'Cajero', 'Opera caja: apertura, movimientos y cierre', TRUE),
    ('VENDEDOR', 'Vendedor', 'Gestiona ventas y clientes', TRUE),
    ('ALMACENERO', 'Almacenero', 'Gestiona inventario y stock', TRUE)
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo IN ('CAJA_READ', 'CAJA_WRITE')
WHERE r.codigo = 'CAJERO'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo IN ('CLIENTES_READ', 'CLIENTES_WRITE')
WHERE r.codigo = 'VENDEDOR'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo IN ('INVENTORY_READ', 'INVENTORY_WRITE')
WHERE r.codigo = 'ALMACENERO'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

INSERT INTO usuarios (username, password_hash, nombres, email, activo)
VALUES (
    'admin',
    '$2a$10$MTNVJ2WKlIqmYk5.tfZ5cefPT3u8z3NYJ.hM4LvDELoFrWyylhNP6',
    'Administrador Empresa',
    'admin@tenant.local',
    TRUE
)
ON CONFLICT (username) DO NOTHING;

INSERT INTO usuario_roles (usuario_id, rol_id)
SELECT u.id, r.id
FROM usuarios u
JOIN roles r ON r.codigo = 'ADMIN_EMPRESA'
WHERE u.username = 'admin'
ON CONFLICT (usuario_id, rol_id) DO NOTHING;
