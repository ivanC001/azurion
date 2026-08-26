ALTER TABLE usuarios
    ADD COLUMN IF NOT EXISTS apellidos VARCHAR(160),
    ADD COLUMN IF NOT EXISTS telefono VARCHAR(40),
    ADD COLUMN IF NOT EXISTS cargo VARCHAR(120),
    ADD COLUMN IF NOT EXISTS foto_perfil_url VARCHAR(500);

DO $migration$
BEGIN
    IF to_regclass('cotizaciones') IS NOT NULL THEN
        ALTER TABLE cotizaciones
            ADD COLUMN IF NOT EXISTS asesor_apellidos VARCHAR(160),
            ADD COLUMN IF NOT EXISTS asesor_telefono VARCHAR(40),
            ADD COLUMN IF NOT EXISTS asesor_email VARCHAR(180),
            ADD COLUMN IF NOT EXISTS asesor_cargo VARCHAR(120),
            ADD COLUMN IF NOT EXISTS asesor_foto_url VARCHAR(500);

        UPDATE cotizaciones cot
        SET asesor_apellidos = COALESCE(cot.asesor_apellidos, usr.apellidos),
            asesor_telefono = COALESCE(cot.asesor_telefono, usr.telefono),
            asesor_email = COALESCE(cot.asesor_email, usr.email),
            asesor_cargo = COALESCE(cot.asesor_cargo, usr.cargo),
            asesor_foto_url = COALESCE(cot.asesor_foto_url, usr.foto_perfil_url)
        FROM usuarios usr
        WHERE cot.usuario_id = CAST(usr.id AS VARCHAR)
           OR LOWER(cot.usuario_id) = LOWER(usr.username);
    END IF;
END
$migration$;
