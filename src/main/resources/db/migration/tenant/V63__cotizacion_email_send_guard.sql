ALTER TABLE cotizaciones
    ADD COLUMN IF NOT EXISTS email_send_status VARCHAR(20),
    ADD COLUMN IF NOT EXISTS email_send_token VARCHAR(80),
    ADD COLUMN IF NOT EXISTS email_send_started_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS email_send_error VARCHAR(500);

ALTER TABLE cotizaciones
    DROP CONSTRAINT IF EXISTS chk_cotizaciones_email_send_status;

ALTER TABLE cotizaciones
    ADD CONSTRAINT chk_cotizaciones_email_send_status
        CHECK (email_send_status IS NULL OR email_send_status IN ('SENDING', 'SENT', 'ERROR', 'UNKNOWN'));

CREATE INDEX IF NOT EXISTS idx_cotizaciones_email_send_status
    ON cotizaciones(email_send_status, email_send_started_at);
