ALTER TABLE crm_prospectos
    ADD COLUMN IF NOT EXISTS canal_ingreso VARCHAR(30) NOT NULL DEFAULT 'MANUAL',
    ADD COLUMN IF NOT EXISTS campania VARCHAR(120),
    ADD COLUMN IF NOT EXISTS landing_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS mensaje VARCHAR(1500);

ALTER TABLE crm_prospectos
    DROP CONSTRAINT IF EXISTS chk_crm_prospectos_canal_ingreso;

ALTER TABLE crm_prospectos
    ADD CONSTRAINT chk_crm_prospectos_canal_ingreso
        CHECK (canal_ingreso IN ('MANUAL', 'LANDING', 'WEBHOOK', 'WHATSAPP', 'FACEBOOK', 'IMPORTADO'));

CREATE INDEX IF NOT EXISTS idx_crm_prospectos_canal_ingreso ON crm_prospectos(canal_ingreso);
CREATE INDEX IF NOT EXISTS idx_crm_prospectos_campania ON crm_prospectos(campania);
