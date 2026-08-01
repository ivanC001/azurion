DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = current_schema()
          AND table_name = 'almacenes'
    ) AND EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'almacenes'
          AND column_name = 'sucursal_id'
    ) THEN
        WITH principales_duplicados AS (
            SELECT id,
                   row_number() OVER (
                       PARTITION BY sucursal_id
                       ORDER BY created_at NULLS LAST, id
                   ) AS posicion
            FROM almacenes
            WHERE activo = TRUE
              AND upper(tipo_almacen) = 'PRINCIPAL'
        )
        UPDATE almacenes almacen
        SET tipo_almacen = 'SECUNDARIO',
            updated_at = now(),
            version = version + 1
        FROM principales_duplicados duplicado
        WHERE almacen.id = duplicado.id
          AND duplicado.posicion > 1;

        EXECUTE 'CREATE UNIQUE INDEX IF NOT EXISTS uq_almacen_principal_activo_sucursal
            ON almacenes (sucursal_id)
            WHERE activo = TRUE AND upper(tipo_almacen) = ''PRINCIPAL''';
    END IF;
END $$;
