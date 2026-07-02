ALTER TABLE crm_catalogo_items
    ADD COLUMN IF NOT EXISTS public_token VARCHAR(80),
    ADD COLUMN IF NOT EXISTS public_enabled BOOLEAN DEFAULT TRUE NOT NULL,
    ADD COLUMN IF NOT EXISTS landing_slug VARCHAR(140);

UPDATE crm_catalogo_items
SET public_token = substr(md5(random()::text || clock_timestamp()::text || id::text), 1, 32)
WHERE public_token IS NULL OR public_token = '';

ALTER TABLE crm_catalogo_items
    ALTER COLUMN public_token SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_crm_catalogo_public_token ON crm_catalogo_items(public_token);
CREATE INDEX IF NOT EXISTS idx_crm_catalogo_public_lookup ON crm_catalogo_items(id, public_token, estado, public_enabled);
CREATE INDEX IF NOT EXISTS idx_crm_catalogo_landing_slug ON crm_catalogo_items(landing_slug);
