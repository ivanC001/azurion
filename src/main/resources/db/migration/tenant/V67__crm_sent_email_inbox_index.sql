CREATE INDEX IF NOT EXISTS idx_cotizaciones_canal_envio_fecha
    ON cotizaciones(canal_envio, email_send_status, fecha_envio DESC, id DESC)
    WHERE fecha_envio IS NOT NULL AND email_send_status = 'SENT';

CREATE INDEX IF NOT EXISTS idx_crm_whatsapp_conversations_unread
    ON crm_whatsapp_conversations(ultimo_mensaje_en DESC, id DESC)
    WHERE no_leidos > 0;
