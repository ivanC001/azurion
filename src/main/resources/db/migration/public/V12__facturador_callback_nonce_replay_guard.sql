CREATE TABLE IF NOT EXISTS public.facturador_callback_nonces (
    id BIGSERIAL PRIMARY KEY,
    nonce_key_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_facturador_callback_nonces_expiry
    ON public.facturador_callback_nonces(expires_at);
