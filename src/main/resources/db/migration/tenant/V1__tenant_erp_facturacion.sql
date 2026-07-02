CREATE TABLE IF NOT EXISTS productos (
    id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(80) NOT NULL UNIQUE,
    nombre VARCHAR(255) NOT NULL,
    precio NUMERIC(18,2) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS clientes (
    id BIGSERIAL PRIMARY KEY,
    tipo_documento VARCHAR(2) NOT NULL,
    numero_documento VARCHAR(20) NOT NULL,
    nombre VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (tipo_documento, numero_documento)
);

CREATE TABLE IF NOT EXISTS ventas (
    id BIGSERIAL PRIMARY KEY,
    external_id VARCHAR(80) NOT NULL UNIQUE,
    cliente_documento VARCHAR(20) NOT NULL,
    cliente_nombre VARCHAR(255) NOT NULL,
    moneda VARCHAR(3) NOT NULL,
    total NUMERIC(18,2) NOT NULL,
    fecha_venta TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS documento (
    id BIGSERIAL PRIMARY KEY,
    external_correlation_id VARCHAR(120) NOT NULL UNIQUE,
    tipo_comprobante VARCHAR(3) NOT NULL,
    serie VARCHAR(10) NOT NULL,
    numero BIGINT NOT NULL,
    moneda VARCHAR(3) NOT NULL,
    cliente_tipo_doc VARCHAR(2) NOT NULL,
    cliente_numero_doc VARCHAR(20) NOT NULL,
    cliente_nombre VARCHAR(255) NOT NULL,
    total NUMERIC(18,2) NOT NULL,
    estado_interno VARCHAR(30) NOT NULL,
    fecha_emision TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS documento_detalles (
    id BIGSERIAL PRIMARY KEY,
    documento_id BIGINT NOT NULL REFERENCES documento(id),
    item INT NOT NULL,
    descripcion VARCHAR(255) NOT NULL,
    cantidad NUMERIC(18,4) NOT NULL,
    valor_unitario NUMERIC(18,6) NOT NULL,
    tipo_afectacion VARCHAR(4) NOT NULL,
    total NUMERIC(18,2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS documento_tributos (
    id BIGSERIAL PRIMARY KEY,
    documento_id BIGINT NOT NULL REFERENCES documento(id),
    tributo_id VARCHAR(6) NOT NULL,
    base_imponible NUMERIC(18,2) NOT NULL,
    monto NUMERIC(18,2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS documento_sunat (
    id BIGSERIAL PRIMARY KEY,
    documento_id BIGINT NOT NULL UNIQUE REFERENCES documento(id),
    estado_sunat VARCHAR(30) NOT NULL,
    ticket VARCHAR(80),
    codigo_respuesta_sunat VARCHAR(10),
    mensaje_sunat VARCHAR(500),
    fecha_envio TIMESTAMPTZ,
    fecha_respuesta TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS documento_referencias (
    id BIGSERIAL PRIMARY KEY,
    documento_id BIGINT NOT NULL REFERENCES documento(id),
    tipo_relacion VARCHAR(4) NOT NULL,
    documento_referenciado_id VARCHAR(120) NOT NULL,
    motivo_codigo VARCHAR(6),
    motivo_descripcion VARCHAR(255),
    fecha_emision_ref TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS documento_cuotas (
    id BIGSERIAL PRIMARY KEY,
    documento_id BIGINT NOT NULL REFERENCES documento(id),
    numero_cuota INT NOT NULL,
    fecha_vencimiento DATE NOT NULL,
    monto NUMERIC(18,2) NOT NULL,
    moneda VARCHAR(3) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS guias_remision (
    id BIGSERIAL PRIMARY KEY,
    documento_id BIGINT NOT NULL REFERENCES documento(id),
    tipo_gre VARCHAR(4) NOT NULL,
    motivo_traslado_codigo VARCHAR(4),
    direccion_partida VARCHAR(255),
    direccion_llegada VARCHAR(255),
    fecha_inicio_traslado DATE,
    estado_traslado VARCHAR(20),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS resumen_diarios (
    id BIGSERIAL PRIMARY KEY,
    fecha_documentos DATE NOT NULL,
    codigo_resumen VARCHAR(80) NOT NULL UNIQUE,
    estado VARCHAR(20) NOT NULL,
    ticket_sunat VARCHAR(80),
    mensaje_sunat VARCHAR(500),
    fecha_envio TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS catalogos_sunat (
    id BIGSERIAL PRIMARY KEY,
    catalogo VARCHAR(40) NOT NULL,
    codigo VARCHAR(20) NOT NULL,
    descripcion VARCHAR(500) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (catalogo, codigo)
);
