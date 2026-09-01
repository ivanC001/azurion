-- Opt-out de WhatsApp por prospecto.
--
-- La politica de Meta obliga a honrar la baja: si el cliente pide dejar de recibir
-- mensajes y se le sigue escribiendo, cae la calidad del numero y Meta termina
-- bloqueando los envios. El worker de reenganche descarta cualquier prospecto que
-- tenga whatsapp_optout_en.

ALTER TABLE crm_prospectos
    ADD COLUMN IF NOT EXISTS whatsapp_optout_en TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS whatsapp_optout_motivo VARCHAR(200);

CREATE INDEX IF NOT EXISTS idx_crm_prospectos_whatsapp_optout
    ON crm_prospectos (whatsapp_optout_en)
    WHERE whatsapp_optout_en IS NOT NULL;
