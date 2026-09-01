-- REPARACION. Ejecutar solo si 01_diagnostico_flyway_v96.sql reporto [COLISION].
--
-- Que hace
-- --------
-- Borra del historial de Flyway la fila de la version 96 que corresponde al
-- arreglo aplicado a mano en el servidor ("cotizaciones productos schema
-- alignment"). No toca la fila 96 de whatsapp si ya existiera.
--
-- Por que es seguro borrar esa fila
-- ---------------------------------
-- El efecto de esa migracion (columnas almacen_id y precio_venta_modo en
-- productos, su FK y su indice) NO se revierte: sigue en la base. Lo unico que
-- se borra es el registro del historial.
--
-- Tras el borrado, en el proximo arranque:
--   * V96__whatsapp_message_template_snapshot.sql se aplica por primera vez,
--   * V98__cotizaciones_productos_schema_alignment.sql se aplica y no hace nada
--     porque usa ADD COLUMN IF NOT EXISTS / CREATE INDEX IF NOT EXISTS y solo
--     crea la FK si falta.
--
-- El orden no es un problema: TenantMigrationService configura Flyway con
-- outOfOrder(true), asi que aplicar la 96 despues de la 97 esta permitido.
--
-- Antes de ejecutar: respaldo del historial, que es lo unico que se modifica.
--   pg_dump -t '*.flyway_schema_history' <base> > flyway_history_backup.sql

BEGIN;

DO $$
DECLARE
    esquema   TEXT;
    borradas  INT;
    total     INT := 0;
BEGIN
    FOR esquema IN
        SELECT n.nspname
          FROM pg_namespace n
          JOIN pg_class c ON c.relnamespace = n.oid
         WHERE c.relname = 'flyway_schema_history'
           AND c.relkind = 'r'
         ORDER BY n.nspname
    LOOP
        EXECUTE format(
            'DELETE FROM %I.flyway_schema_history
              WHERE version = ''96''
                AND description ILIKE ''%%cotizaciones%%''',
            esquema
        );
        GET DIAGNOSTICS borradas = ROW_COUNT;

        IF borradas > 0 THEN
            total := total + borradas;
            RAISE NOTICE 'esquema=% -> % fila(s) de historial eliminadas', esquema, borradas;
        END IF;
    END LOOP;

    RAISE NOTICE 'Total de filas de historial eliminadas: %', total;
END $$;

-- Revisar los NOTICE antes de confirmar. Si algo no cuadra: ROLLBACK;
COMMIT;
