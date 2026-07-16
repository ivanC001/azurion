CREATE TABLE IF NOT EXISTS crm_currency_config (
    id BIGSERIAL PRIMARY KEY,
    moneda VARCHAR(3) NOT NULL UNIQUE,
    nombre VARCHAR(80) NOT NULL,
    simbolo VARCHAR(8) NOT NULL,
    tipo_cambio_base NUMERIC(18, 6) NOT NULL DEFAULT 1,
    margen_conversion_porcentaje NUMERIC(8, 4) NOT NULL DEFAULT 0,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_crm_currency_config_moneda CHECK (moneda IN ('USD', 'EUR')),
    CONSTRAINT chk_crm_currency_config_tipo_cambio CHECK (tipo_cambio_base > 0),
    CONSTRAINT chk_crm_currency_config_margen CHECK (margen_conversion_porcentaje >= 0)
);

INSERT INTO crm_currency_config (moneda, nombre, simbolo, tipo_cambio_base, margen_conversion_porcentaje, activo)
VALUES
    ('USD', 'Dolar americano', '$', 3.800000, 0, TRUE),
    ('EUR', 'Euro', '€', 4.100000, 0, TRUE)
ON CONFLICT (moneda) DO NOTHING;
