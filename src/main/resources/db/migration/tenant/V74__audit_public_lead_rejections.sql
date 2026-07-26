ALTER TABLE crm_public_lead_submissions
    ADD COLUMN IF NOT EXISTS completed_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS error_code VARCHAR(80),
    ADD COLUMN IF NOT EXISTS error_message VARCHAR(500);

CREATE INDEX IF NOT EXISTS idx_crm_public_submission_status
    ON crm_public_lead_submissions(estado, received_at DESC);
