ALTER TABLE crm_whatsapp_messages
    ADD COLUMN IF NOT EXISTS leido_en TIMESTAMPTZ;

CREATE TABLE IF NOT EXISTS crm_whatsapp_conversations (
    id BIGSERIAL PRIMARY KEY,
    prospecto_id BIGINT NOT NULL UNIQUE REFERENCES crm_prospectos(id) ON DELETE CASCADE,
    estado VARCHAR(20) NOT NULL DEFAULT 'ABIERTA',
    responsable_id VARCHAR(80),
    no_leidos INTEGER NOT NULL DEFAULT 0,
    ultimo_mensaje TEXT,
    ultima_direccion VARCHAR(15),
    ultimo_mensaje_en TIMESTAMPTZ,
    nota_interna TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_crm_whatsapp_conversations_estado CHECK (estado IN ('ABIERTA', 'RESUELTA', 'ARCHIVADA')),
    CONSTRAINT chk_crm_whatsapp_conversations_no_leidos CHECK (no_leidos >= 0),
    CONSTRAINT chk_crm_whatsapp_conversations_direccion CHECK (
        ultima_direccion IS NULL OR ultima_direccion IN ('ENTRANTE', 'SALIENTE')
    )
);

INSERT INTO crm_whatsapp_conversations (
    prospecto_id,
    estado,
    responsable_id,
    no_leidos,
    ultimo_mensaje,
    ultima_direccion,
    ultimo_mensaje_en
)
SELECT
    latest.prospecto_id,
    'ABIERTA',
    NULLIF(p.responsable_id, 'crm-whatsapp'),
    (
        SELECT count(*)::INTEGER
        FROM crm_whatsapp_messages unread
        WHERE unread.prospecto_id = latest.prospecto_id
          AND unread.direccion = 'ENTRANTE'
          AND unread.leido_en IS NULL
    ),
    latest.contenido,
    latest.direccion,
    latest.mensaje_en
FROM (
    SELECT DISTINCT ON (m.prospecto_id)
        m.prospecto_id,
        m.contenido,
        m.direccion,
        m.mensaje_en,
        m.id
    FROM crm_whatsapp_messages m
    WHERE m.prospecto_id IS NOT NULL
    ORDER BY m.prospecto_id, m.mensaje_en DESC NULLS LAST, m.id DESC
) latest
JOIN crm_prospectos p ON p.id = latest.prospecto_id
ON CONFLICT (prospecto_id) DO NOTHING;

CREATE INDEX IF NOT EXISTS idx_crm_whatsapp_conversations_latest
    ON crm_whatsapp_conversations(ultimo_mensaje_en DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_crm_whatsapp_conversations_responsable
    ON crm_whatsapp_conversations(responsable_id, estado);

CREATE INDEX IF NOT EXISTS idx_crm_whatsapp_messages_unread
    ON crm_whatsapp_messages(prospecto_id, direccion, leido_en);
