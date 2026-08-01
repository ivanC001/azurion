CREATE TABLE IF NOT EXISTS inventory_operation_requests (
    id BIGSERIAL PRIMARY KEY,
    operation_key VARCHAR(100) NOT NULL,
    operation_type VARCHAR(30) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    kardex_movimiento_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_inventory_operation_request_key UNIQUE (operation_key),
    CONSTRAINT fk_inventory_operation_request_kardex
        FOREIGN KEY (kardex_movimiento_id) REFERENCES kardex_movimientos(id)
);

CREATE INDEX IF NOT EXISTS idx_inventory_operation_request_kardex
    ON inventory_operation_requests (kardex_movimiento_id);
