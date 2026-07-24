CREATE TABLE IF NOT EXISTS public.platform_messages (
    id BIGSERIAL PRIMARY KEY,
    asunto VARCHAR(180) NOT NULL,
    contenido TEXT NOT NULL,
    prioridad VARCHAR(20) NOT NULL DEFAULT 'INFO',
    audiencia VARCHAR(30) NOT NULL,
    tenant_id VARCHAR(80),
    enviado_por_usuario_id BIGINT NOT NULL,
    enviado_por_username VARCHAR(120) NOT NULL,
    publicado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    expira_en TIMESTAMPTZ,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_platform_message_priority
        CHECK (prioridad IN ('INFO', 'WARNING', 'CRITICAL')),
    CONSTRAINT chk_platform_message_audience
        CHECK (audiencia IN ('PLATFORM_ADMINS', 'TENANT_ADMINS', 'TENANT_USERS', 'SELECTED_USERS', 'ALL_USERS')),
    CONSTRAINT chk_platform_message_expiration
        CHECK (expira_en IS NULL OR expira_en > publicado_en)
);

CREATE TABLE IF NOT EXISTS public.platform_message_recipients (
    id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL REFERENCES public.platform_messages(id) ON DELETE CASCADE,
    recipient_scope VARCHAR(20) NOT NULL,
    tenant_id VARCHAR(80) NOT NULL,
    user_id BIGINT NOT NULL,
    username_snapshot VARCHAR(120) NOT NULL,
    display_name_snapshot VARCHAR(180),
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_platform_message_recipient_scope
        CHECK (recipient_scope IN ('PLATFORM', 'TENANT')),
    CONSTRAINT uq_platform_message_recipient
        UNIQUE (message_id, recipient_scope, tenant_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_platform_messages_published
    ON public.platform_messages(publicado_en DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_platform_message_inbox
    ON public.platform_message_recipients(recipient_scope, tenant_id, user_id, read_at, id DESC);

CREATE INDEX IF NOT EXISTS idx_platform_message_recipient_message
    ON public.platform_message_recipients(message_id);
