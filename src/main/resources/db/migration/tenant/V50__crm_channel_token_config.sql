CREATE TABLE IF NOT EXISTS crm_canal_token_config (
    id BIGSERIAL PRIMARY KEY,
    canal VARCHAR(40) NOT NULL UNIQUE,
    nombre VARCHAR(120) NOT NULL,
    access_token VARCHAR(2000),
    verify_token VARCHAR(300),
    webhook_url VARCHAR(500),
    app_id VARCHAR(180),
    phone_number_id VARCHAR(180),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    metadata_json TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_crm_canal_token_config_canal CHECK (canal IN ('WEB', 'WHATSAPP', 'INSTAGRAM', 'FACEBOOK'))
);

INSERT INTO crm_canal_token_config (canal, nombre, activo)
VALUES
    ('WEB', 'Landing web', TRUE),
    ('WHATSAPP', 'WhatsApp Business', FALSE),
    ('INSTAGRAM', 'Instagram', FALSE),
    ('FACEBOOK', 'Facebook Lead Ads', FALSE)
ON CONFLICT (canal) DO NOTHING;
