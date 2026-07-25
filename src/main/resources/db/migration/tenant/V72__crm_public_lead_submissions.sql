CREATE TABLE IF NOT EXISTS crm_public_lead_submissions (
    id BIGSERIAL PRIMARY KEY,
    receipt_id VARCHAR(64) NOT NULL UNIQUE,
    idempotency_hash VARCHAR(64) UNIQUE,
    source_key VARCHAR(120),
    source_type VARCHAR(20) NOT NULL,
    prospecto_id BIGINT REFERENCES crm_prospectos(id) ON DELETE SET NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'RECEIVED',
    received_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_crm_public_submission_source CHECK (source_type IN ('BROWSER', 'SERVER', 'LEGACY')),
    CONSTRAINT chk_crm_public_submission_status CHECK (estado IN ('RECEIVED', 'PROCESSED', 'REJECTED'))
);

CREATE INDEX IF NOT EXISTS idx_crm_public_submission_source
    ON crm_public_lead_submissions(source_key, received_at DESC);

CREATE INDEX IF NOT EXISTS idx_crm_public_submission_prospect
    ON crm_public_lead_submissions(prospecto_id, received_at DESC);

CREATE INDEX IF NOT EXISTS idx_crm_prospectos_phone_normalized
    ON crm_prospectos ((regexp_replace(coalesce(telefono, ''), '[^0-9]', '', 'g')));

CREATE INDEX IF NOT EXISTS idx_crm_prospectos_email_normalized
    ON crm_prospectos ((lower(trim(coalesce(correo, '')))));
