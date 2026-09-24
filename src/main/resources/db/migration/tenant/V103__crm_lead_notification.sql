-- Aviso por correo a los asesores cuando entra un lead o un mensaje nuevo.
-- Usa el SMTP ya configurado por el tenant (tenant_email_config).
CREATE TABLE IF NOT EXISTS crm_lead_notification_config (
    id BIGSERIAL PRIMARY KEY,
    activo BOOLEAN NOT NULL DEFAULT FALSE,
    notificar_lead_nuevo BOOLEAN NOT NULL DEFAULT TRUE,
    notificar_mensaje_nuevo BOOLEAN NOT NULL DEFAULT TRUE,
    notificar_responsable BOOLEAN NOT NULL DEFAULT TRUE,
    correos_copia VARCHAR(1000),
    cooldown_minutos INTEGER NOT NULL DEFAULT 30,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_crm_lead_notif_cooldown CHECK (cooldown_minutos BETWEEN 0 AND 10080)
);

-- Un registro por aviso: sirve de auditoria y de freno anti duplicados.
CREATE TABLE IF NOT EXISTS crm_lead_notification_dispatch (
    id BIGSERIAL PRIMARY KEY,
    prospecto_id BIGINT NOT NULL REFERENCES crm_prospectos(id) ON DELETE CASCADE,
    tipo VARCHAR(20) NOT NULL,
    destinatarios VARCHAR(1000) NOT NULL,
    asunto VARCHAR(300) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    detalle VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_crm_lead_notif_tipo CHECK (tipo IN ('LEAD_NUEVO', 'MENSAJE_NUEVO')),
    CONSTRAINT ck_crm_lead_notif_estado CHECK (estado IN ('PENDIENTE', 'ENVIADO', 'ERROR', 'OMITIDO'))
);

CREATE INDEX IF NOT EXISTS idx_crm_lead_notif_prospecto
    ON crm_lead_notification_dispatch (prospecto_id, tipo, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_crm_lead_notif_estado
    ON crm_lead_notification_dispatch (estado, created_at DESC);

INSERT INTO crm_lead_notification_config (activo, notificar_lead_nuevo, notificar_mensaje_nuevo, notificar_responsable, cooldown_minutos)
SELECT FALSE, TRUE, TRUE, TRUE, 30
WHERE NOT EXISTS (SELECT 1 FROM crm_lead_notification_config);
