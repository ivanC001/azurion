ALTER TABLE crm_whatsapp_messages
    ADD COLUMN IF NOT EXISTS plantilla_nombre VARCHAR(512),
    ADD COLUMN IF NOT EXISTS plantilla_idioma VARCHAR(35),
    ADD COLUMN IF NOT EXISTS plantilla_parametros_json TEXT;
