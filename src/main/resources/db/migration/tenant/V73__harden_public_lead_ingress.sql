UPDATE crm_landing_config
SET validar_duplicados_por = 'TELEFONO_CORREO'
WHERE validar_duplicados_por IS NULL
   OR validar_duplicados_por NOT IN ('TELEFONO_CORREO', 'TELEFONO', 'CORREO', 'NINGUNO');

ALTER TABLE crm_landing_config
    DROP CONSTRAINT IF EXISTS chk_crm_landing_duplicate_policy;

ALTER TABLE crm_landing_config
    ADD CONSTRAINT chk_crm_landing_duplicate_policy
    CHECK (validar_duplicados_por IN ('TELEFONO_CORREO', 'TELEFONO', 'CORREO', 'NINGUNO'));

WITH grouped AS (
    SELECT
        MIN(id) AS keeper_id,
        prospecto_id,
        COALESCE(landing_key, '') AS landing_key_key,
        COALESCE(campania, '') AS campania_key,
        COALESCE(catalogo_item_id, 0) AS catalogo_item_key,
        producto_pendiente,
        SUM(GREATEST(contador_envios, 1)) AS total_envios,
        MAX(ultimo_envio_en) AS ultimo_envio
    FROM crm_prospecto_intereses
    GROUP BY
        prospecto_id,
        COALESCE(landing_key, ''),
        COALESCE(campania, ''),
        COALESCE(catalogo_item_id, 0),
        producto_pendiente
),
updated AS (
    UPDATE crm_prospecto_intereses interest
    SET contador_envios = grouped.total_envios,
        ultimo_envio_en = grouped.ultimo_envio,
        updated_at = now()
    FROM grouped
    WHERE interest.id = grouped.keeper_id
    RETURNING interest.id
)
DELETE FROM crm_prospecto_intereses interest
USING grouped
WHERE interest.prospecto_id = grouped.prospecto_id
  AND COALESCE(interest.landing_key, '') = grouped.landing_key_key
  AND COALESCE(interest.campania, '') = grouped.campania_key
  AND COALESCE(interest.catalogo_item_id, 0) = grouped.catalogo_item_key
  AND interest.producto_pendiente = grouped.producto_pendiente
  AND interest.id <> grouped.keeper_id;

DROP INDEX IF EXISTS idx_crm_prospecto_intereses_match;

CREATE UNIQUE INDEX IF NOT EXISTS uk_crm_prospecto_intereses_match
    ON crm_prospecto_intereses (
        prospecto_id,
        COALESCE(landing_key, ''),
        COALESCE(campania, ''),
        COALESCE(catalogo_item_id, 0),
        producto_pendiente
    );
