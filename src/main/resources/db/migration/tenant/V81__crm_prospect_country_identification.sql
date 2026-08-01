ALTER TABLE crm_prospectos
    ADD COLUMN IF NOT EXISTS pais_codigo VARCHAR(2);

ALTER TABLE crm_prospectos
    ALTER COLUMN tipo_documento TYPE VARCHAR(30),
    ALTER COLUMN numero_documento TYPE VARCHAR(30);

UPDATE crm_prospectos
SET pais_codigo = 'PE'
WHERE pais_codigo IS NULL
  AND tipo_documento IN ('1', '6', 'DNI', 'RUC');

ALTER TABLE crm_prospectos
    DROP CONSTRAINT IF EXISTS chk_crm_prospectos_pais_codigo;

ALTER TABLE crm_prospectos
    ADD CONSTRAINT chk_crm_prospectos_pais_codigo
        CHECK (pais_codigo IS NULL OR pais_codigo ~ '^[A-Z]{2}$');
