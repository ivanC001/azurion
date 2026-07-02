CREATE TABLE IF NOT EXISTS public.roles_globales (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(80) NOT NULL UNIQUE,
    nombre VARCHAR(120) NOT NULL,
    descripcion VARCHAR(400),
    scope VARCHAR(40) NOT NULL DEFAULT 'GLOBAL',
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS public.usuario_global_roles (
    id BIGSERIAL PRIMARY KEY,
    usuario_global_id BIGINT NOT NULL REFERENCES public.usuarios_globales(id) ON DELETE CASCADE,
    rol_codigo VARCHAR(80) NOT NULL REFERENCES public.roles_globales(codigo),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_usuario_global_rol UNIQUE (usuario_global_id, rol_codigo)
);

CREATE TABLE IF NOT EXISTS public.usuario_tenant_roles (
    id BIGSERIAL PRIMARY KEY,
    usuario_global_id BIGINT NOT NULL REFERENCES public.usuarios_globales(id) ON DELETE CASCADE,
    tenant_id VARCHAR(80) NOT NULL,
    rol_codigo VARCHAR(80) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    asignado_por_usuario_id BIGINT REFERENCES public.usuarios_globales(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_usuario_tenant_rol UNIQUE (usuario_global_id, tenant_id, rol_codigo)
);

CREATE INDEX IF NOT EXISTS idx_usuario_tenant_roles_user_tenant
    ON public.usuario_tenant_roles(usuario_global_id, tenant_id);

INSERT INTO public.roles_globales (codigo, nombre, descripcion, scope, activo)
VALUES
    ('ADMIN_GENERAL', 'Administrador General', 'Controla plataforma y asigna administradores de empresa', 'GLOBAL', TRUE),
    ('PLATFORM_ADMIN', 'Platform Admin', 'Alias legacy de administrador de plataforma', 'GLOBAL', TRUE),
    ('ADMIN', 'Administrador', 'Rol administrativo global legacy', 'GLOBAL', TRUE),
    ('FACTURACION_API', 'Facturacion API', 'Permite integraciones de facturacion', 'GLOBAL', TRUE),
    ('SALES', 'Ventas', 'Permisos comerciales base', 'TENANT', TRUE),
    ('ADMIN_EMPRESA', 'Administrador de Empresa', 'Administra usuarios y roles dentro de su tenant', 'TENANT', TRUE)
ON CONFLICT (codigo) DO NOTHING;

UPDATE public.usuarios_globales
SET roles = CONCAT(roles, ',ROLE_ADMIN_GENERAL')
WHERE username = 'platform.admin'
  AND POSITION('ROLE_ADMIN_GENERAL' IN roles) = 0;

INSERT INTO public.usuario_global_roles (usuario_global_id, rol_codigo, activo)
SELECT u.id, rg.codigo,
       TRUE
FROM public.usuarios_globales u,
     UNNEST(STRING_TO_ARRAY(u.roles, ',')) AS value
JOIN public.roles_globales rg
    ON rg.codigo = UPPER(REGEXP_REPLACE(TRIM(value), '^ROLE_', ''))
WHERE TRIM(value) <> ''
ON CONFLICT (usuario_global_id, rol_codigo) DO NOTHING;
