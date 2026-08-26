ALTER TABLE crm_whatsapp_messages
    ADD COLUMN IF NOT EXISTS enviado_por_usuario_id VARCHAR(120),
    ADD COLUMN IF NOT EXISTS enviado_por_nombre VARCHAR(160),
    ADD COLUMN IF NOT EXISTS error_codigo VARCHAR(80),
    ADD COLUMN IF NOT EXISTS error_detalle VARCHAR(500);

ALTER TABLE crm_whatsapp_conversations
    ADD COLUMN IF NOT EXISTS ultimo_entrante_en TIMESTAMPTZ;

UPDATE crm_whatsapp_conversations conversation
SET ultimo_entrante_en = latest.mensaje_en
FROM (
    SELECT DISTINCT ON (prospecto_id)
        prospecto_id,
        mensaje_en
    FROM crm_whatsapp_messages
    WHERE prospecto_id IS NOT NULL
      AND direccion = 'ENTRANTE'
    ORDER BY prospecto_id, mensaje_en DESC NULLS LAST, id DESC
) latest
WHERE conversation.prospecto_id = latest.prospecto_id
  AND conversation.ultimo_entrante_en IS NULL;

CREATE INDEX IF NOT EXISTS idx_crm_whatsapp_messages_sender_audit
    ON crm_whatsapp_messages(enviado_por_usuario_id, mensaje_en DESC)
    WHERE direccion = 'SALIENTE';

CREATE INDEX IF NOT EXISTS idx_crm_whatsapp_conversations_inbound_window
    ON crm_whatsapp_conversations(ultimo_entrante_en DESC)
    WHERE ultimo_entrante_en IS NOT NULL;
