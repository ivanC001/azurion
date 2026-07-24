CREATE TABLE IF NOT EXISTS crm_whatsapp_conversation_notes (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES crm_whatsapp_conversations(id) ON DELETE CASCADE,
    slot SMALLINT NOT NULL,
    contenido TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_crm_whatsapp_note_slot UNIQUE (conversation_id, slot),
    CONSTRAINT chk_crm_whatsapp_note_slot CHECK (slot BETWEEN 1 AND 3),
    CONSTRAINT chk_crm_whatsapp_note_content CHECK (length(btrim(contenido)) BETWEEN 1 AND 4000)
);

INSERT INTO crm_whatsapp_conversation_notes (conversation_id, slot, contenido)
SELECT id, 1, btrim(nota_interna)
FROM crm_whatsapp_conversations
WHERE nota_interna IS NOT NULL
  AND btrim(nota_interna) <> ''
ON CONFLICT (conversation_id, slot) DO NOTHING;

CREATE INDEX IF NOT EXISTS idx_crm_whatsapp_notes_conversation
    ON crm_whatsapp_conversation_notes(conversation_id, slot);
