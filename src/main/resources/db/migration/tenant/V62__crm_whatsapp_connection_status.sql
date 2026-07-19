ALTER TABLE crm_canal_token_config
    ADD COLUMN IF NOT EXISTS waba_id VARCHAR(180),
    ADD COLUMN IF NOT EXISTS webhook_verified_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS last_connection_test_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS last_connection_ok BOOLEAN,
    ADD COLUMN IF NOT EXISTS last_connection_message VARCHAR(500),
    ADD COLUMN IF NOT EXISTS waba_subscribed BOOLEAN,
    ADD COLUMN IF NOT EXISTS meta_display_phone_number VARCHAR(80),
    ADD COLUMN IF NOT EXISTS meta_verified_name VARCHAR(180),
    ADD COLUMN IF NOT EXISTS meta_quality_rating VARCHAR(40),
    ADD COLUMN IF NOT EXISTS meta_token_expires_at TIMESTAMPTZ;
