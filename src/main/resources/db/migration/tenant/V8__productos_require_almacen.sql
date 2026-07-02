ALTER TABLE productos
    ADD COLUMN IF NOT EXISTS almacen_id BIGINT;

INSERT INTO almacenes (codigo, nombre, direccion, activo)
SELECT 'ALM-PRINCIPAL', 'Almacen Principal', 'Generado por migracion', TRUE
WHERE NOT EXISTS (SELECT 1 FROM almacenes);

WITH almacen_default AS (
    SELECT id
    FROM almacenes
    ORDER BY id
    LIMIT 1
)
UPDATE productos p
SET almacen_id = (SELECT id FROM almacen_default)
WHERE p.almacen_id IS NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_productos_almacen'
    ) THEN
        ALTER TABLE productos
            ADD CONSTRAINT fk_productos_almacen
            FOREIGN KEY (almacen_id) REFERENCES almacenes(id);
    END IF;
END $$;

ALTER TABLE productos
    ALTER COLUMN almacen_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_productos_almacen_id ON productos(almacen_id);
