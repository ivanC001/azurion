ALTER TABLE cotizaciones
    ADD COLUMN IF NOT EXISTS whatsapp_send_status VARCHAR(20),
    ADD COLUMN IF NOT EXISTS whatsapp_send_token VARCHAR(80),
    ADD COLUMN IF NOT EXISTS whatsapp_send_started_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS whatsapp_send_error VARCHAR(500),
    ADD COLUMN IF NOT EXISTS whatsapp_message_id VARCHAR(255);

ALTER TABLE cotizaciones
    DROP CONSTRAINT IF EXISTS chk_cotizaciones_whatsapp_send_status;

ALTER TABLE cotizaciones
    ADD CONSTRAINT chk_cotizaciones_whatsapp_send_status
        CHECK (
            whatsapp_send_status IS NULL
            OR whatsapp_send_status IN ('SENDING', 'SENT', 'ERROR', 'UNKNOWN')
        );

CREATE INDEX IF NOT EXISTS idx_cotizaciones_whatsapp_send_status
    ON cotizaciones(whatsapp_send_status, whatsapp_send_started_at);
