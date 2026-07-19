ALTER TABLE crm_canal_token_config
    ADD COLUMN IF NOT EXISTS app_secret VARCHAR(1000);

CREATE TABLE IF NOT EXISTS crm_whatsapp_messages (
    id BIGSERIAL PRIMARY KEY,
    prospecto_id BIGINT REFERENCES crm_prospectos(id) ON DELETE SET NULL,
    meta_message_id VARCHAR(255) NOT NULL UNIQUE,
    direccion VARCHAR(15) NOT NULL,
    remitente VARCHAR(80),
    destinatario VARCHAR(80),
    tipo_mensaje VARCHAR(40) NOT NULL,
    contenido TEXT,
    estado VARCHAR(30) NOT NULL,
    mensaje_en TIMESTAMPTZ,
    raw_payload TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_crm_whatsapp_messages_direccion CHECK (direccion IN ('ENTRANTE', 'SALIENTE'))
);

CREATE INDEX IF NOT EXISTS idx_crm_whatsapp_messages_prospecto
    ON crm_whatsapp_messages(prospecto_id, mensaje_en DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_crm_whatsapp_messages_estado
    ON crm_whatsapp_messages(estado);
