CREATE TABLE IF NOT EXISTS crm_whatsapp_auto_reply_config (
    id BIGSERIAL PRIMARY KEY,
    activo BOOLEAN NOT NULL DEFAULT FALSE,
    modo VARCHAR(20) NOT NULL DEFAULT 'SIEMPRE',
    mensaje TEXT,
    cooldown_minutos INTEGER NOT NULL DEFAULT 720,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_crm_wa_auto_reply_mode CHECK (modo IN ('SIEMPRE', 'HORARIO')),
    CONSTRAINT ck_crm_wa_auto_reply_cooldown CHECK (cooldown_minutos BETWEEN 1 AND 10080)
);

CREATE TABLE IF NOT EXISTS crm_whatsapp_auto_reply_schedule (
    id BIGSERIAL PRIMARY KEY,
    config_id BIGINT NOT NULL REFERENCES crm_whatsapp_auto_reply_config(id) ON DELETE CASCADE,
    dia_semana SMALLINT NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_crm_wa_auto_reply_day CHECK (dia_semana BETWEEN 1 AND 7),
    CONSTRAINT uq_crm_wa_auto_reply_day UNIQUE (config_id, dia_semana)
);

CREATE TABLE IF NOT EXISTS crm_whatsapp_quick_replies (
    id BIGSERIAL PRIMARY KEY,
    usuario_id VARCHAR(120) NOT NULL,
    slot SMALLINT NOT NULL,
    titulo VARCHAR(80) NOT NULL,
    mensaje TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_crm_wa_quick_reply_slot CHECK (slot BETWEEN 1 AND 3),
    CONSTRAINT uq_crm_wa_quick_reply_user_slot UNIQUE (usuario_id, slot)
);

CREATE INDEX IF NOT EXISTS idx_crm_wa_quick_reply_user
    ON crm_whatsapp_quick_replies (usuario_id, slot);

CREATE TABLE IF NOT EXISTS crm_whatsapp_auto_reply_dispatch (
    id BIGSERIAL PRIMARY KEY,
    incoming_message_id BIGINT NOT NULL REFERENCES crm_whatsapp_messages(id) ON DELETE CASCADE,
    prospecto_id BIGINT NOT NULL REFERENCES crm_prospectos(id) ON DELETE CASCADE,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    detalle VARCHAR(500),
    outgoing_message_id BIGINT REFERENCES crm_whatsapp_messages(id) ON DELETE SET NULL,
    processed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_crm_wa_auto_reply_incoming UNIQUE (incoming_message_id),
    CONSTRAINT ck_crm_wa_auto_reply_dispatch_status
        CHECK (estado IN ('PENDIENTE', 'PROCESANDO', 'ENVIADO', 'OMITIDO', 'ERROR'))
);

CREATE INDEX IF NOT EXISTS idx_crm_wa_auto_reply_dispatch_status
    ON crm_whatsapp_auto_reply_dispatch (estado, created_at);

CREATE INDEX IF NOT EXISTS idx_crm_wa_auto_reply_dispatch_prospect
    ON crm_whatsapp_auto_reply_dispatch (prospecto_id, created_at DESC);
