ALTER TABLE productos
    ALTER COLUMN almacen_id DROP NOT NULL;

ALTER TABLE stock
    DROP CONSTRAINT IF EXISTS chk_stock_cantidad_no_negativa;
ALTER TABLE stock
    ADD CONSTRAINT chk_stock_cantidad_no_negativa
    CHECK (cantidad >= 0) NOT VALID;

ALTER TABLE stock
    DROP CONSTRAINT IF EXISTS chk_stock_reservado_no_negativo;
ALTER TABLE stock
    ADD CONSTRAINT chk_stock_reservado_no_negativo
    CHECK (stock_reservado >= 0) NOT VALID;

ALTER TABLE stock
    DROP CONSTRAINT IF EXISTS chk_stock_minimo_no_negativo;
ALTER TABLE stock
    ADD CONSTRAINT chk_stock_minimo_no_negativo
    CHECK (stock_minimo >= 0) NOT VALID;

ALTER TABLE stock
    DROP CONSTRAINT IF EXISTS chk_stock_maximo_valido;
ALTER TABLE stock
    ADD CONSTRAINT chk_stock_maximo_valido
    CHECK (stock_maximo IS NULL OR stock_maximo >= stock_minimo) NOT VALID;

ALTER TABLE stock_lotes
    DROP CONSTRAINT IF EXISTS chk_stock_lote_no_negativo;
ALTER TABLE stock_lotes
    ADD CONSTRAINT chk_stock_lote_no_negativo
    CHECK (stock_actual >= 0) NOT VALID;

ALTER TABLE lotes
    DROP CONSTRAINT IF EXISTS chk_lote_fechas_validas;
ALTER TABLE lotes
    ADD CONSTRAINT chk_lote_fechas_validas
    CHECK (
        fecha_fabricacion IS NULL
        OR fecha_vencimiento IS NULL
        OR fecha_fabricacion <= fecha_vencimiento
    ) NOT VALID;

CREATE INDEX IF NOT EXISTS idx_stock_almacen_producto
    ON stock (almacen_id, producto_id);
CREATE INDEX IF NOT EXISTS idx_stock_lotes_producto_almacen
    ON stock_lotes (producto_id, almacen_id);
CREATE INDEX IF NOT EXISTS idx_lotes_vencimiento_activo
    ON lotes (fecha_vencimiento)
    WHERE estado = 'ACTIVO';
CREATE INDEX IF NOT EXISTS idx_compras_fecha_ingreso
    ON compras (fecha_ingreso DESC);
