ALTER TABLE crm_prospectos
    ADD COLUMN IF NOT EXISTS presupuesto_moneda VARCHAR(3);

ALTER TABLE crm_oportunidades
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

    UPDATE crm_prospectos prospecto
       SET presupuesto_moneda = COALESCE(
               (SELECT item.moneda
                  FROM crm_catalogo_items item
                 WHERE item.id = prospecto.catalogo_item_id),
               tenant_currency,
               'PEN'
           )
     WHERE presupuesto_moneda IS NULL OR BTRIM(presupuesto_moneda) = '';

    UPDATE crm_oportunidades oportunidad
       SET moneda = COALESCE(
               (SELECT item.moneda
                  FROM crm_catalogo_items item
                 WHERE item.id = oportunidad.catalogo_item_id),
               (SELECT prospecto.presupuesto_moneda
                  FROM crm_prospectos prospecto
                 WHERE prospecto.id = oportunidad.prospecto_id),
               tenant_currency,
               'PEN'
           )
     WHERE moneda IS NULL OR BTRIM(moneda) = '';
END $$;

ALTER TABLE crm_prospectos
    ALTER COLUMN presupuesto_moneda SET DEFAULT 'PEN',
    ALTER COLUMN presupuesto_moneda SET NOT NULL;

ALTER TABLE crm_oportunidades
    ALTER COLUMN moneda SET DEFAULT 'PEN',
    ALTER COLUMN moneda SET NOT NULL;

ALTER TABLE crm_prospectos
    DROP CONSTRAINT IF EXISTS ck_crm_prospectos_presupuesto_moneda;

ALTER TABLE crm_prospectos
    ADD CONSTRAINT ck_crm_prospectos_presupuesto_moneda
        CHECK (presupuesto_moneda ~ '^[A-Z]{3}$');

ALTER TABLE crm_oportunidades
    DROP CONSTRAINT IF EXISTS ck_crm_oportunidades_moneda;

ALTER TABLE crm_oportunidades
    ADD CONSTRAINT ck_crm_oportunidades_moneda
        CHECK (moneda ~ '^[A-Z]{3}$');
