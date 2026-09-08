-- Identidad de WhatsApp basada en usuario (WhatsApp usernames).
--
-- Meta dejo de enviar el telefono del remitente en los webhooks de las cuentas ya
-- migradas: el mensaje llega con "from_user_id" y el contacto con "user_id" mas
-- "profile.username", sin "from" ni "wa_id". Como el CRM identificaba al prospecto
-- solo por telefono, esos mensajes se rechazaban con CRM_WHATSAPP_TELEFONO_INVALIDO
-- y nunca entraban al inbox.
--
-- meta_user_id guarda ese identificador opaco (formato "PE.920886250645840") para
-- poder reconocer al mismo contacto entre mensajes y para poder responderle, ya que
-- es el unico dato direccionable cuando no hay telefono.

ALTER TABLE crm_prospectos
    ADD COLUMN IF NOT EXISTS meta_user_id VARCHAR(80),
    ADD COLUMN IF NOT EXISTS whatsapp_username VARCHAR(120);

CREATE UNIQUE INDEX IF NOT EXISTS idx_crm_prospectos_meta_user_id
    ON crm_prospectos (meta_user_id)
    WHERE meta_user_id IS NOT NULL;
