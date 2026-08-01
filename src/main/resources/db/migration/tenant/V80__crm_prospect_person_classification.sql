ALTER TABLE crm_prospectos
    DROP CONSTRAINT IF EXISTS chk_crm_prospectos_tipo_persona;

ALTER TABLE crm_prospectos
    ALTER COLUMN tipo_persona SET DEFAULT 'SIN_DEFINIR';

UPDATE crm_prospectos
SET tipo_persona = 'SIN_DEFINIR',
    updated_at = now()
WHERE tipo_persona = 'NATURAL'
  AND NULLIF(BTRIM(COALESCE(tipo_documento, '')), '') IS NULL
  AND NULLIF(BTRIM(COALESCE(numero_documento, '')), '') IS NULL
  AND canal_ingreso IN ('LANDING', 'WEBHOOK', 'WHATSAPP', 'FACEBOOK', 'IMPORTADO');

ALTER TABLE crm_prospectos
    ADD CONSTRAINT chk_crm_prospectos_tipo_persona
        CHECK (tipo_persona IN ('SIN_DEFINIR', 'NATURAL', 'JURIDICA'));
