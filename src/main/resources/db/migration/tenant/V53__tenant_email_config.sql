CREATE TABLE IF NOT EXISTS tenant_email_config (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(120) NOT NULL UNIQUE,
    nombre_remitente VARCHAR(160) NOT NULL,
    correo_remitente VARCHAR(180) NOT NULL,
    reply_to VARCHAR(180),
    smtp_host VARCHAR(180) NOT NULL,
    smtp_port INTEGER NOT NULL,
    smtp_security VARCHAR(10) NOT NULL,
    smtp_username VARCHAR(180) NOT NULL,
    smtp_password_encrypted VARCHAR(3000) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT FALSE,
    verificado BOOLEAN NOT NULL DEFAULT FALSE,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    fecha_verificacion TIMESTAMP,
    ultimo_error VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_tenant_email_config_security CHECK (smtp_security IN ('NONE', 'SSL', 'TLS')),
    CONSTRAINT chk_tenant_email_config_estado CHECK (estado IN ('PENDIENTE', 'VERIFICADO', 'ERROR', 'INACTIVO')),
    CONSTRAINT chk_tenant_email_config_port CHECK (smtp_port BETWEEN 1 AND 65535)
);

CREATE INDEX IF NOT EXISTS idx_tenant_email_config_tenant ON tenant_email_config(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenant_email_config_estado ON tenant_email_config(estado);
