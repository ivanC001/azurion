-- Catalogo de productos compartido por Inventario, Ventas y Cotizaciones.
-- Se mantiene separado de ventas, stock y facturacion para que CRM no cree
-- estructuras operativas que no forman parte de su contrato.

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
