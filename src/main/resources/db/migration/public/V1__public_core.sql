CREATE TABLE IF NOT EXISTS public.planes (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    codigo VARCHAR(40) NOT NULL UNIQUE,
    limite_mensual_bolsa BIGINT NOT NULL DEFAULT 0,
    precio_mensual NUMERIC(18,2) NOT NULL DEFAULT 0,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS public.modulos (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(60) NOT NULL UNIQUE,
    nombre VARCHAR(120) NOT NULL,
    descripcion VARCHAR(400),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS public.empresas (
    id BIGSERIAL PRIMARY KEY,
    ruc VARCHAR(11) NOT NULL UNIQUE,
    razon_social VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(80) NOT NULL UNIQUE,
    schema_name VARCHAR(80) NOT NULL UNIQUE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS public.suscripciones (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES public.empresas(id),
    plan_id BIGINT NOT NULL REFERENCES public.planes(id),
    estado VARCHAR(30) NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS public.empresa_modulos (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES public.empresas(id),
    modulo_id BIGINT NOT NULL REFERENCES public.modulos(id),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_inicio DATE,
    fecha_fin DATE,
    configuracion_extra JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (empresa_id, modulo_id)
);

CREATE TABLE IF NOT EXISTS public.schemas_empresas (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(80) NOT NULL UNIQUE,
    schema_name VARCHAR(80) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS public.usuarios_globales (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    roles VARCHAR(255) NOT NULL,
    empresa_default VARCHAR(80),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS public.auditoria_global (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(80) NOT NULL,
    user_id VARCHAR(80),
    method VARCHAR(10) NOT NULL,
    path VARCHAR(255) NOT NULL,
    status_code INT NOT NULL,
    duration_ms BIGINT NOT NULL,
    message VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

INSERT INTO public.planes (nombre, codigo, limite_mensual_bolsa, precio_mensual, estado)
VALUES ('Plan Inicial', 'BASIC', 1000, 99.00, 'ACTIVO')
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO public.modulos (codigo, nombre, descripcion, estado)
VALUES
  ('SAAS_CORE', 'SaaS Core', 'Modulo administrativo ERP', 'ACTIVO'),
  ('FACTURACION_CORE', 'Facturacion Core', 'Motor electronico SUNAT', 'ACTIVO')
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO public.usuarios_globales (username, password_hash, roles, empresa_default, activo)
VALUES ('platform.admin', '\$2a\$10\$MTNVJ2WKlIqmYk5.tfZ5cefPT3u8z3NYJ.hM4LvDELoFrWyylhNP6', 'ROLE_PLATFORM_ADMIN,ROLE_ADMIN,ROLE_FACTURACION_API,ROLE_SALES', 'public', TRUE)
ON CONFLICT (username) DO NOTHING;
