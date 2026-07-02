ALTER TABLE clientes
    ADD COLUMN IF NOT EXISTS direccion VARCHAR(500),
    ADD COLUMN IF NOT EXISTS ubigeo VARCHAR(6),
    ADD COLUMN IF NOT EXISTS telefono VARCHAR(30),
    ADD COLUMN IF NOT EXISTS limite_credito NUMERIC(18,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS saldo_deuda NUMERIC(18,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS dias_credito INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS activo BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE clientes
    DROP CONSTRAINT IF EXISTS chk_clientes_limite_credito,
    DROP CONSTRAINT IF EXISTS chk_clientes_saldo_deuda,
    DROP CONSTRAINT IF EXISTS chk_clientes_dias_credito;

ALTER TABLE clientes
    ADD CONSTRAINT chk_clientes_limite_credito CHECK (limite_credito >= 0),
    ADD CONSTRAINT chk_clientes_saldo_deuda CHECK (saldo_deuda >= 0),
    ADD CONSTRAINT chk_clientes_dias_credito CHECK (dias_credito >= 0);

CREATE INDEX IF NOT EXISTS idx_clientes_saldo_deuda ON clientes (saldo_deuda DESC);
CREATE INDEX IF NOT EXISTS idx_clientes_activo ON clientes (activo);
