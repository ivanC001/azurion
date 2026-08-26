WITH latest_quote AS (
    SELECT DISTINCT ON (crm_oportunidad_id)
           crm_oportunidad_id,
           UPPER(moneda) AS moneda
    FROM cotizaciones
    WHERE crm_oportunidad_id IS NOT NULL
      AND moneda IS NOT NULL
      AND BTRIM(moneda) <> ''
    ORDER BY crm_oportunidad_id, id DESC
)
UPDATE crm_oportunidades oportunidad
SET moneda = quote.moneda
FROM latest_quote quote
WHERE oportunidad.id = quote.crm_oportunidad_id
  AND oportunidad.moneda IS DISTINCT FROM quote.moneda;
