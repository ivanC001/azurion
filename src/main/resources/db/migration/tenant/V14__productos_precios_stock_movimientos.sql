ALTER TABLE productos
    ADD COLUMN IF NOT EXISTS costo_promedio NUMERIC(18,6) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS stock_minimo NUMERIC(18,4) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS foto VARCHAR(500);

UPDATE productos
SET costo_promedio = COALESCE(NULLIF(costo_promedio, 0), precio_compra_base, 0),
    stock_minimo = COALESCE(NULLIF(stock_minimo, 0), stock_minimo_global, 0),
    foto = COALESCE(foto, imagen_url)
WHERE costo_promedio = 0
   OR stock_minimo = 0
   OR foto IS NULL;

ALTER TABLE kardex_movimientos
    ADD COLUMN IF NOT EXISTS precio_compra NUMERIC(18,6),
    ADD COLUMN IF NOT EXISTS precio_venta NUMERIC(18,2);

UPDATE kardex_movimientos
SET precio_compra = COALESCE(precio_compra, costo_unitario)
WHERE precio_compra IS NULL
  AND costo_unitario IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_productos_codigo ON productos(codigo);
CREATE INDEX IF NOT EXISTS idx_productos_codigo_barras ON productos(codigo_barras);
