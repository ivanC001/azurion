ALTER TABLE IF EXISTS ventas
    ADD COLUMN IF NOT EXISTS client_operation_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS request_hash VARCHAR(64);

ALTER TABLE IF EXISTS compras
    ADD COLUMN IF NOT EXISTS client_operation_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS request_hash VARCHAR(64);

ALTER TABLE IF EXISTS caja_movimientos
    ADD COLUMN IF NOT EXISTS client_operation_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS request_hash VARCHAR(64);

ALTER TABLE IF EXISTS cliente_abonos
    ADD COLUMN IF NOT EXISTS client_operation_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS request_hash VARCHAR(64);

ALTER TABLE IF EXISTS notas_fiscales
    ADD COLUMN IF NOT EXISTS client_operation_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS request_hash VARCHAR(64);

ALTER TABLE IF EXISTS guias_remision
    ADD COLUMN IF NOT EXISTS client_operation_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS request_hash VARCHAR(64);

DO $$
BEGIN
    IF to_regclass(current_schema() || '.ventas') IS NOT NULL THEN
        EXECUTE 'CREATE UNIQUE INDEX IF NOT EXISTS uq_ventas_client_operation
            ON ventas (client_operation_id) WHERE client_operation_id IS NOT NULL';
    END IF;
    IF to_regclass(current_schema() || '.compras') IS NOT NULL THEN
        EXECUTE 'CREATE UNIQUE INDEX IF NOT EXISTS uq_compras_client_operation
            ON compras (client_operation_id) WHERE client_operation_id IS NOT NULL';
    END IF;
    IF to_regclass(current_schema() || '.caja_movimientos') IS NOT NULL THEN
        EXECUTE 'CREATE UNIQUE INDEX IF NOT EXISTS uq_caja_movimientos_client_operation
            ON caja_movimientos (client_operation_id) WHERE client_operation_id IS NOT NULL';
    END IF;
    IF to_regclass(current_schema() || '.cliente_abonos') IS NOT NULL THEN
        EXECUTE 'CREATE UNIQUE INDEX IF NOT EXISTS uq_cliente_abonos_client_operation
            ON cliente_abonos (client_operation_id) WHERE client_operation_id IS NOT NULL';
    END IF;
    IF to_regclass(current_schema() || '.notas_fiscales') IS NOT NULL THEN
        EXECUTE 'CREATE UNIQUE INDEX IF NOT EXISTS uq_notas_fiscales_client_operation
            ON notas_fiscales (client_operation_id) WHERE client_operation_id IS NOT NULL';
    END IF;
    IF to_regclass(current_schema() || '.guias_remision') IS NOT NULL THEN
        EXECUTE 'CREATE UNIQUE INDEX IF NOT EXISTS uq_guias_remision_client_operation
            ON guias_remision (client_operation_id) WHERE client_operation_id IS NOT NULL';
    END IF;
END $$;
