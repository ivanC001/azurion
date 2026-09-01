-- DIAGNOSTICO (solo lectura). Ejecutar ANTES de desplegar.
--
-- Contexto
-- --------
-- El arreglo que se aplico directamente en el servidor se llamo
-- V96__cotizaciones_productos_schema_alignment.sql. En el repositorio la
-- version 96 ya estaba ocupada por V96__whatsapp_message_template_snapshot.sql,
-- asi que el arreglo se renumero a V98__cotizaciones_productos_schema_alignment.sql.
--
-- Consecuencia: en los esquemas donde el servidor ya registro la version 96 con
-- la descripcion de "cotizaciones productos", Flyway encontrara que el script
-- 96 del classpath (whatsapp) no coincide en descripcion ni en checksum con lo
-- que dice el historial. TenantMigrationService llama a migrate() sin repair
-- automatico a proposito, asi que eso NO se corrige solo: aborta el arranque o
-- el alta del tenant con una ValidateException.
--
-- Esta consulta dice exactamente en que esquemas pasa. Si no devuelve filas, no
-- hay nada que reparar y se puede desplegar sin mas.

DO $$
DECLARE
    esquema         TEXT;
    fila            RECORD;
    encontrados     INT := 0;
BEGIN
    FOR esquema IN
        SELECT n.nspname
          FROM pg_namespace n
          JOIN pg_class c ON c.relnamespace = n.oid
         WHERE c.relname = 'flyway_schema_history'
           AND c.relkind = 'r'
         ORDER BY n.nspname
    LOOP
        FOR fila IN EXECUTE format(
            'SELECT installed_rank, version, description, success, installed_on
               FROM %I.flyway_schema_history
              WHERE version = ''96''',
            esquema
        )
        LOOP
            encontrados := encontrados + 1;

            IF fila.description ILIKE '%%cotizaciones%%' THEN
                RAISE NOTICE
                    '[COLISION] esquema=% rank=% descripcion="%" aplicada=% exito=%  -> hay que reparar',
                    esquema, fila.installed_rank, fila.description, fila.installed_on, fila.success;
            ELSE
                RAISE NOTICE
                    '[OK] esquema=% rank=% descripcion="%" aplicada=%',
                    esquema, fila.installed_rank, fila.description, fila.installed_on;
            END IF;
        END LOOP;
    END LOOP;

    IF encontrados = 0 THEN
        RAISE NOTICE 'Ningun esquema tiene registrada la version 96. Nada que reparar.';
    END IF;
END $$;
