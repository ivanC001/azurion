CREATE TABLE IF NOT EXISTS crm_lead_assignment_config (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    automatico BOOLEAN NOT NULL DEFAULT FALSE,
    estrategia VARCHAR(30) NOT NULL DEFAULT 'MENOR_CARGA',
    responsable_ids TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_crm_lead_assignment_strategy CHECK (estrategia IN ('MENOR_CARGA'))
);

INSERT INTO crm_lead_assignment_config (codigo, automatico, estrategia, responsable_ids)
VALUES ('DEFAULT', FALSE, 'MENOR_CARGA', NULL)
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT rol.id, permiso.id
FROM roles rol
JOIN permisos permiso ON permiso.codigo = 'CRM_DELETE'
WHERE rol.codigo IN ('CRM_ADMIN', 'ADMIN_EMPRESA')
ON CONFLICT (rol_id, permiso_id) DO NOTHING;
