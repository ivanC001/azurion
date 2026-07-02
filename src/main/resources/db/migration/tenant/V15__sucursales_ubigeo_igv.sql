DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = current_schema()
          AND table_name = 'sucursales'
    ) THEN
        ALTER TABLE sucursales
            ADD COLUMN IF NOT EXISTS ubigeo_codigo VARCHAR(6),
            ADD COLUMN IF NOT EXISTS departamento VARCHAR(120),
            ADD COLUMN IF NOT EXISTS provincia VARCHAR(120),
            ADD COLUMN IF NOT EXISTS distrito VARCHAR(160),
            ADD COLUMN IF NOT EXISTS igv_porcentaje NUMERIC(5,2) NOT NULL DEFAULT 18.00;

        UPDATE sucursales
        SET ubigeo_codigo = COALESCE(ubigeo_codigo, '150101'),
            departamento = COALESCE(departamento, 'LIMA'),
            provincia = COALESCE(provincia, 'LIMA'),
            distrito = COALESCE(distrito, 'LIMA'),
            igv_porcentaje = COALESCE(igv_porcentaje, 18.00)
        WHERE ubigeo_codigo IS NULL
           OR departamento IS NULL
           OR provincia IS NULL
           OR distrito IS NULL
           OR igv_porcentaje IS NULL;

        ALTER TABLE sucursales
            ALTER COLUMN ubigeo_codigo SET NOT NULL,
            ALTER COLUMN departamento SET NOT NULL,
            ALTER COLUMN provincia SET NOT NULL,
            ALTER COLUMN distrito SET NOT NULL;

        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_sucursales_ubigeo_codigo ON sucursales(ubigeo_codigo)';
    END IF;
END $$;
