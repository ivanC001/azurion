CREATE TABLE IF NOT EXISTS producto_sku_counter (
    id SMALLINT PRIMARY KEY,
    ultimo_valor BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_producto_sku_counter_singleton CHECK (id = 1),
    CONSTRAINT chk_producto_sku_counter_non_negative CHECK (ultimo_valor >= 0)
);

INSERT INTO producto_sku_counter (id, ultimo_valor)
VALUES (
    1,
    COALESCE((
        SELECT MAX(SUBSTRING(sku FROM 5)::BIGINT)
        FROM productos
        WHERE sku ~ '^PRD-[0-9]+$'
          AND LENGTH(SUBSTRING(sku FROM 5)) <= 12
    ), 0)
)
ON CONFLICT (id) DO UPDATE
SET ultimo_valor = GREATEST(
    producto_sku_counter.ultimo_valor,
    EXCLUDED.ultimo_valor
);

UPDATE productos
SET codigo_barras = NULLIF(BTRIM(codigo_barras), '')
WHERE codigo_barras IS DISTINCT FROM NULLIF(BTRIM(codigo_barras), '');

WITH duplicados AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY LOWER(codigo_barras)
               ORDER BY id
           ) AS posicion
    FROM productos
    WHERE codigo_barras IS NOT NULL
)
UPDATE productos producto
SET codigo_barras = NULL
FROM duplicados
WHERE producto.id = duplicados.id
  AND duplicados.posicion > 1;

CREATE UNIQUE INDEX IF NOT EXISTS uq_productos_codigo_barras_normalizado
    ON productos (LOWER(codigo_barras))
    WHERE codigo_barras IS NOT NULL;

ALTER TABLE productos
    DROP CONSTRAINT IF EXISTS chk_productos_precio_no_negativo;

ALTER TABLE productos
    ADD CONSTRAINT chk_productos_precio_no_negativo
    CHECK (precio >= 0);

ALTER TABLE productos
    DROP CONSTRAINT IF EXISTS chk_productos_costos_no_negativos;

ALTER TABLE productos
    ADD CONSTRAINT chk_productos_costos_no_negativos
    CHECK (
        precio_compra_base >= 0
        AND costo_promedio >= 0
        AND (precio_venta_base IS NULL OR precio_venta_base >= 0)
    );
