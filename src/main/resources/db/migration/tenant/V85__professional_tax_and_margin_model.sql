DO $$
BEGIN
    IF to_regclass('productos') IS NOT NULL THEN
        ALTER TABLE productos
            ADD COLUMN IF NOT EXISTS precio_venta_modo VARCHAR(24) NOT NULL DEFAULT 'INCLUYE_IGV';

        UPDATE productos
        SET precio_venta_modo = 'INCLUYE_IGV'
        WHERE precio_venta_modo IS NULL OR precio_venta_modo <> 'INCLUYE_IGV';

        ALTER TABLE productos DROP CONSTRAINT IF EXISTS chk_productos_precio_venta_modo;
        ALTER TABLE productos
            ADD CONSTRAINT chk_productos_precio_venta_modo
            CHECK (precio_venta_modo = 'INCLUYE_IGV') NOT VALID;
    END IF;
END $$;

DO $$
BEGIN
    IF to_regclass('compras') IS NOT NULL THEN
        ALTER TABLE compras
            ADD COLUMN IF NOT EXISTS subtotal_neto NUMERIC(18,2) NOT NULL DEFAULT 0,
            ADD COLUMN IF NOT EXISTS monto_igv NUMERIC(18,2) NOT NULL DEFAULT 0,
            ADD COLUMN IF NOT EXISTS credito_fiscal_aplicable BOOLEAN NOT NULL DEFAULT FALSE,
            ADD COLUMN IF NOT EXISTS total_costo_inventariable NUMERIC(18,2) NOT NULL DEFAULT 0,
            ADD COLUMN IF NOT EXISTS tratamiento_igv VARCHAR(32) NOT NULL DEFAULT 'HISTORICO_SIN_DESGLOSE';

        UPDATE compras
        SET subtotal_neto = total,
            monto_igv = 0,
            credito_fiscal_aplicable = FALSE,
            total_costo_inventariable = total,
            tratamiento_igv = 'HISTORICO_SIN_DESGLOSE'
        WHERE tratamiento_igv = 'HISTORICO_SIN_DESGLOSE';

        ALTER TABLE compras DROP CONSTRAINT IF EXISTS chk_compras_importes_tributarios;
        ALTER TABLE compras
            ADD CONSTRAINT chk_compras_importes_tributarios
            CHECK (
                subtotal_neto >= 0
                AND monto_igv >= 0
                AND total_costo_inventariable >= 0
                AND total = subtotal_neto + monto_igv
            ) NOT VALID;

        CREATE INDEX IF NOT EXISTS idx_compras_fecha_fiscal
            ON compras (fecha_emision, estado);
    END IF;
END $$;

DO $$
BEGIN
    IF to_regclass('compra_detalles') IS NOT NULL THEN
        ALTER TABLE compra_detalles
            ADD COLUMN IF NOT EXISTS costo_neto_unitario NUMERIC(18,6),
            ADD COLUMN IF NOT EXISTS porcentaje_igv NUMERIC(5,2) NOT NULL DEFAULT 0,
            ADD COLUMN IF NOT EXISTS monto_igv_unitario NUMERIC(18,6) NOT NULL DEFAULT 0,
            ADD COLUMN IF NOT EXISTS costo_total_unitario NUMERIC(18,6),
            ADD COLUMN IF NOT EXISTS costo_inventariable_unitario NUMERIC(18,6),
            ADD COLUMN IF NOT EXISTS subtotal_neto NUMERIC(18,2) NOT NULL DEFAULT 0,
            ADD COLUMN IF NOT EXISTS monto_igv NUMERIC(18,2) NOT NULL DEFAULT 0,
            ADD COLUMN IF NOT EXISTS total_costo_inventariable NUMERIC(18,2) NOT NULL DEFAULT 0,
            ADD COLUMN IF NOT EXISTS precio_venta_neto NUMERIC(18,6);

        UPDATE compra_detalles
        SET costo_neto_unitario = COALESCE(costo_neto_unitario, costo_unitario),
            porcentaje_igv = COALESCE(porcentaje_igv, 0),
            monto_igv_unitario = COALESCE(monto_igv_unitario, 0),
            costo_total_unitario = COALESCE(costo_total_unitario, costo_unitario),
            costo_inventariable_unitario = COALESCE(costo_inventariable_unitario, costo_unitario),
            subtotal_neto = CASE WHEN subtotal_neto = 0 THEN total ELSE subtotal_neto END,
            monto_igv = COALESCE(monto_igv, 0),
            total_costo_inventariable = CASE
                WHEN total_costo_inventariable = 0 THEN total
                ELSE total_costo_inventariable
            END,
            precio_venta_neto = COALESCE(precio_venta_neto, precio_venta);

        ALTER TABLE compra_detalles DROP CONSTRAINT IF EXISTS chk_compra_detalles_importes_tributarios;
        ALTER TABLE compra_detalles
            ADD CONSTRAINT chk_compra_detalles_importes_tributarios
            CHECK (
                costo_neto_unitario > 0
                AND porcentaje_igv BETWEEN 0 AND 100
                AND monto_igv_unitario >= 0
                AND costo_total_unitario > 0
                AND costo_inventariable_unitario > 0
                AND subtotal_neto >= 0
                AND monto_igv >= 0
                AND total_costo_inventariable >= 0
                AND total = subtotal_neto + monto_igv
            ) NOT VALID;
    END IF;
END $$;

DO $$
BEGIN
    IF to_regclass('venta_detalles') IS NOT NULL THEN
        ALTER TABLE venta_detalles
            ADD COLUMN IF NOT EXISTS costo_unitario_inventariable NUMERIC(18,6);

        CREATE INDEX IF NOT EXISTS idx_venta_detalles_fecha_fiscal
            ON venta_detalles (venta_id, tipo_afectacion_igv_codigo);
    END IF;
END $$;

DO $$
BEGIN
    IF to_regclass('notas_fiscales') IS NOT NULL THEN
        ALTER TABLE notas_fiscales
            ADD COLUMN IF NOT EXISTS base_imponible NUMERIC(18,2),
            ADD COLUMN IF NOT EXISTS monto_igv NUMERIC(18,2);

        CREATE INDEX IF NOT EXISTS idx_notas_fiscales_fecha_estado
            ON notas_fiscales (fecha_emision, tipo_documento, facturacion_estado);
    END IF;
END $$;
