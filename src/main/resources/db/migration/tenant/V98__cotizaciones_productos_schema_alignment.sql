-- Alinea la tabla productos con el mapeo JPA en tenants sin INVENTARIO.
--
-- V2 crea la tabla productos y forma parte del plan de COTIZACIONES, pero las
-- dos columnas que le anaden despues viven solo en el plan de INVENTARIO:
--
--   V8__productos_require_almacen.sql          -> almacen_id
--   V85__professional_tax_and_margin_model.sql -> precio_venta_modo
--
-- La entidad Producto mapea ambas, y CotizacionRepository la arrastra en cada
-- lectura con @EntityGraph(..., "detalles.producto", ...). En un tenant solo-CRM
-- eso hace que todo LEFT JOIN sobre productos falle con
-- "column p1_0.almacen_id does not exist": las cotizaciones se guardaban, pero
-- listarlas, abrirlas o generar su PDF era imposible.
--
-- Idempotente: en un tenant que si tiene INVENTARIO las columnas ya existen y
-- esta migracion no hace nada.

DO $$
BEGIN
    IF to_regclass('productos') IS NULL THEN
        RETURN;
    END IF;

    -- Nullable a proposito: un tenant sin INVENTARIO no tiene almacenes, asi
    -- que no se puede exigir NOT NULL como hace V8.
    ALTER TABLE productos
        ADD COLUMN IF NOT EXISTS almacen_id BIGINT;

    ALTER TABLE productos
        ADD COLUMN IF NOT EXISTS precio_venta_modo VARCHAR(24) NOT NULL DEFAULT 'INCLUYE_IGV';

    -- La FK solo tiene sentido si el tenant tiene almacenes.
    --
    -- El filtro por esquema es imprescindible: pg_constraint.conname es unico
    -- por tabla, no por base de datos. Sin el, en un despliegue multi-schema la
    -- comprobacion encuentra la constraint de otro tenant y omite crearla en el
    -- actual. V8 y V48 arrastran ese defecto.
    IF to_regclass('almacenes') IS NOT NULL
       AND NOT EXISTS (
           SELECT 1
             FROM pg_constraint c
             JOIN pg_class t ON t.oid = c.conrelid
             JOIN pg_namespace n ON n.oid = t.relnamespace
            WHERE c.conname = 'fk_productos_almacen'
              AND t.relname = 'productos'
              AND n.nspname = current_schema()
       )
    THEN
        ALTER TABLE productos
            ADD CONSTRAINT fk_productos_almacen
            FOREIGN KEY (almacen_id) REFERENCES almacenes(id);
    END IF;

    CREATE INDEX IF NOT EXISTS idx_productos_almacen_id ON productos(almacen_id);
END $$;
