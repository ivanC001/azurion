ALTER TABLE crm_catalogo_items
    ADD COLUMN IF NOT EXISTS moneda VARCHAR(3);

DO $$
DECLARE
    tenant_currency VARCHAR(3);
BEGIN
    SELECT COALESCE(NULLIF(UPPER(BTRIM(moneda_codigo)), ''), 'PEN')
      INTO tenant_currency
      FROM public.empresas
     WHERE schema_name = current_schema()
     LIMIT 1;

    UPDATE crm_catalogo_items
       SET moneda = COALESCE(tenant_currency, 'PEN')
     WHERE moneda IS NULL OR BTRIM(moneda) = '';
END $$;

ALTER TABLE crm_catalogo_items
    ALTER COLUMN moneda SET DEFAULT 'PEN',
    ALTER COLUMN moneda SET NOT NULL;

ALTER TABLE crm_catalogo_items
    DROP CONSTRAINT IF EXISTS ck_crm_catalogo_items_moneda;

ALTER TABLE crm_catalogo_items
    ADD CONSTRAINT ck_crm_catalogo_items_moneda
        CHECK (moneda ~ '^[A-Z]{3}$');
