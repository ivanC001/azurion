-- Repair tenants that have CRM/cotizaciones enabled but missed warehouse linkage on productos.
-- Keep idempotent because some tenants already have the full inventory structure.

CREATE TABLE IF NOT EXISTS almacenes (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(50) NOT NULL UNIQUE,
    nombre VARCHAR(150) NOT NULL,
    direccion VARCHAR(255),
    sucursal_id BIGINT,
    tipo_almacen VARCHAR(30) NOT NULL DEFAULT 'PRINCIPAL',
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

ALTER TABLE almacenes
    ADD COLUMN IF NOT EXISTS sucursal_id BIGINT,
    ADD COLUMN IF NOT EXISTS tipo_almacen VARCHAR(30) NOT NULL DEFAULT 'PRINCIPAL',
    ADD COLUMN IF NOT EXISTS estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    ADD COLUMN IF NOT EXISTS activo BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE productos
    ADD COLUMN IF NOT EXISTS almacen_id BIGINT;

INSERT INTO almacenes (
    codigo,
    nombre,
    direccion,
    sucursal_id,
    tipo_almacen,
    estado,
    activo
)
SELECT
    'ALM-CRM',
    'Almacen CRM',
    'Generado automaticamente para compatibilidad CRM',
    s.id,
    'PRINCIPAL',
    'ACTIVO',
    TRUE
FROM sucursales s
WHERE NOT EXISTS (SELECT 1 FROM almacenes)
ORDER BY s.id
LIMIT 1;

WITH sucursal_default AS (
    SELECT id
    FROM sucursales
    ORDER BY id
    LIMIT 1
)
UPDATE almacenes a
SET sucursal_id = (SELECT id FROM sucursal_default),
    updated_at = now()
WHERE a.sucursal_id IS NULL
  AND EXISTS (SELECT 1 FROM sucursal_default);

WITH almacen_default AS (
    SELECT id
    FROM almacenes
    ORDER BY id
    LIMIT 1
)
UPDATE productos p
SET almacen_id = (SELECT id FROM almacen_default)
WHERE p.almacen_id IS NULL
  AND EXISTS (SELECT 1 FROM almacen_default);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_almacenes_sucursal'
    ) AND EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'almacenes'
          AND column_name = 'sucursal_id'
    ) THEN
        ALTER TABLE almacenes
            ADD CONSTRAINT fk_almacenes_sucursal
            FOREIGN KEY (sucursal_id) REFERENCES sucursales(id);
    END IF;
END $$;

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

CREATE INDEX IF NOT EXISTS idx_productos_almacen_id ON productos(almacen_id);
