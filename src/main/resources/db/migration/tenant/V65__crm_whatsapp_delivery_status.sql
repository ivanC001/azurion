ALTER TABLE crm_canal_token_config
    ADD COLUMN IF NOT EXISTS last_webhook_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS last_inbound_message_at TIMESTAMPTZ;
