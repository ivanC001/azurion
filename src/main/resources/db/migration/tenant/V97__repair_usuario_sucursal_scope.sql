-- Repara el alcance de sucursales de los usuarios que quedaron sin ninguna.
--
-- V21 y V43 enlazan usuarios con sucursales mediante un CROSS JOIN que se
-- ejecuta en el momento de la migracion. En un tenant nuevo el esquema se crea
-- ANTES de que exista ningun usuario, asi que ese backfill inserta cero filas y
-- los usuarios creados despues se quedan sin sucursal.
--
-- El efecto es que AuthorizationService.validarSucursal rechaza cualquier
-- operacion sobre una sucursal -- crear cotizaciones, registrar ventas -- y
-- ningun rol de tenant (ADMIN, ADMIN_EMPRESA, CRM_ADMIN...) se salta ese
-- control: solo lo hacen roles de plataforma que no existen dentro del tenant.
-- Resultado: en un tenant recien creado nadie podia cotizar.
--
-- Esta migracion solo anade lo que falta. No toca a los usuarios que ya tienen
-- un alcance asignado a proposito.

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = current_schema()
          AND table_name = 'usuario_sucursales'
    ) THEN
        RETURN;
    END IF;

    -- Sin ninguna sucursal el tenant no puede operar; se crea la sede base.
    IF NOT EXISTS (SELECT 1 FROM sucursales) THEN
        INSERT INTO sucursales (
            codigo, nombre, direccion, activo,
            ubigeo_codigo, departamento, provincia, distrito,
            igv_porcentaje, tipo_operacion_default_id,
            tipo_afectacion_default_id, tributo_default_id, porcentaje_igv_default
        )
        VALUES (
            'SUC-PRINCIPAL', 'Sucursal Principal', 'Generada automaticamente', TRUE,
            '150101', 'LIMA', 'LIMA', 'LIMA',
            18.00, '0101', '10', '1000', 18.00
        );
    END IF;

    -- Solo los usuarios que hoy no tienen ninguna sucursal asignada.
    INSERT INTO usuario_sucursales (usuario_id, sucursal_id)
    SELECT u.id, s.id
      FROM usuarios u
      CROSS JOIN sucursales s
     WHERE s.activo = TRUE
       AND NOT EXISTS (
           SELECT 1 FROM usuario_sucursales us WHERE us.usuario_id = u.id
       )
    ON CONFLICT (usuario_id, sucursal_id) DO NOTHING;
END $$;
