CREATE TABLE cliente_abonos (
    id BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    monto NUMERIC(18, 2) NOT NULL,
    saldo_anterior NUMERIC(18, 2) NOT NULL,
    saldo_resultante NUMERIC(18, 2) NOT NULL,
    observacion VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_cliente_abonos_cliente
        FOREIGN KEY (cliente_id) REFERENCES clientes(id),
    CONSTRAINT chk_cliente_abonos_monto
        CHECK (monto > 0),
    CONSTRAINT chk_cliente_abonos_saldos
        CHECK (saldo_anterior >= 0 AND saldo_resultante >= 0)
);

CREATE INDEX idx_cliente_abonos_cliente_fecha
    ON cliente_abonos (cliente_id, created_at DESC);
