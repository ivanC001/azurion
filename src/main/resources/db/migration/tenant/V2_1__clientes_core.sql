-- Clientes es un contrato compartido por CRM, Cotizaciones y Ventas.

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
