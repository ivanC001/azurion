DO $$
BEGIN
    IF to_regclass('crm_currency_config') IS NOT NULL THEN
        ALTER TABLE crm_currency_config
            DROP CONSTRAINT IF EXISTS chk_crm_currency_config_moneda;
    END IF;
END $$;

ALTER TABLE cotizaciones
    ADD COLUMN IF NOT EXISTS moneda_base VARCHAR(3),
    ADD COLUMN IF NOT EXISTS tipo_cambio_aplicado NUMERIC(18, 6),
    ADD COLUMN IF NOT EXISTS fecha_tipo_cambio TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS subtotal_moneda_base NUMERIC(18, 2),
    ADD COLUMN IF NOT EXISTS total_moneda_base NUMERIC(18, 2);

UPDATE cotizaciones
SET moneda = UPPER(COALESCE(NULLIF(BTRIM(moneda), ''), 'PEN')),
    moneda_base = UPPER(COALESCE(NULLIF(BTRIM(moneda_base), ''), NULLIF(BTRIM(moneda), ''), 'PEN')),
    tipo_cambio_aplicado = COALESCE(tipo_cambio_aplicado, 1),
    fecha_tipo_cambio = COALESCE(fecha_tipo_cambio, created_at, NOW()),
    subtotal_moneda_base = COALESCE(subtotal_moneda_base, subtotal),
    total_moneda_base = COALESCE(total_moneda_base, total);

ALTER TABLE cotizaciones
    ALTER COLUMN moneda_base SET NOT NULL,
    ALTER COLUMN tipo_cambio_aplicado SET NOT NULL,
    ALTER COLUMN fecha_tipo_cambio SET NOT NULL,
    ALTER COLUMN subtotal_moneda_base SET NOT NULL,
    ALTER COLUMN total_moneda_base SET NOT NULL;

ALTER TABLE cotizaciones
    DROP CONSTRAINT IF EXISTS chk_cotizaciones_moneda_iso,
    DROP CONSTRAINT IF EXISTS chk_cotizaciones_moneda_base_iso,
    DROP CONSTRAINT IF EXISTS chk_cotizaciones_tipo_cambio_positivo;

ALTER TABLE cotizaciones
    ADD CONSTRAINT chk_cotizaciones_moneda_iso CHECK (moneda ~ '^[A-Z]{3}$'),
    ADD CONSTRAINT chk_cotizaciones_moneda_base_iso CHECK (moneda_base ~ '^[A-Z]{3}$'),
    ADD CONSTRAINT chk_cotizaciones_tipo_cambio_positivo CHECK (tipo_cambio_aplicado > 0);

DO $$
BEGIN
    IF to_regclass('crm_currency_config') IS NOT NULL THEN
        ALTER TABLE crm_currency_config
            DROP CONSTRAINT IF EXISTS chk_crm_currency_config_moneda_iso;
        ALTER TABLE crm_currency_config
            ADD CONSTRAINT chk_crm_currency_config_moneda_iso CHECK (moneda ~ '^[A-Z]{3}$');
    END IF;
END $$;
