-- Nucleo de ventas. No debe formar parte de un tenant que contrata solo CRM.

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
