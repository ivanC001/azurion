ALTER TABLE crm_prospectos
    ADD COLUMN IF NOT EXISTS nivel_interes VARCHAR(20);

UPDATE crm_prospectos
SET nivel_interes = CASE
        WHEN estado = 'INTERESADO' THEN 'CALIENTE'
        WHEN estado = 'CONTACTADO' THEN 'MEDIO'
        ELSE 'FRIO'
    END
WHERE nivel_interes IS NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_crm_prospectos_nivel_interes'
    ) THEN
        ALTER TABLE crm_prospectos
            ADD CONSTRAINT chk_crm_prospectos_nivel_interes
            CHECK (nivel_interes IS NULL OR nivel_interes IN ('FRIO', 'MEDIO', 'CALIENTE'));
    END IF;
END $$;

ALTER TABLE crm_actividades
    ADD COLUMN IF NOT EXISTS resultado_contacto VARCHAR(40),
    ADD COLUMN IF NOT EXISTS nivel_interes VARCHAR(20),
    ADD COLUMN IF NOT EXISTS estado_prospecto_resultado VARCHAR(30);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_crm_actividades_resultado_contacto'
    ) THEN
        ALTER TABLE crm_actividades
            ADD CONSTRAINT chk_crm_actividades_resultado_contacto
            CHECK (
                resultado_contacto IS NULL OR resultado_contacto IN (
                    'CONTACTADO', 'INTERESADO', 'REPROGRAMADO', 'SIN_RESPUESTA',
                    'NO_INTERESADO', 'DESCARTADO', 'COTIZACION_SOLICITADA'
                )
            );
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_crm_actividades_nivel_interes'
    ) THEN
        ALTER TABLE crm_actividades
            ADD CONSTRAINT chk_crm_actividades_nivel_interes
            CHECK (nivel_interes IS NULL OR nivel_interes IN ('FRIO', 'MEDIO', 'CALIENTE'));
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_crm_actividades_estado_prospecto_resultado'
    ) THEN
        ALTER TABLE crm_actividades
            ADD CONSTRAINT chk_crm_actividades_estado_prospecto_resultado
            CHECK (
                estado_prospecto_resultado IS NULL OR estado_prospecto_resultado IN (
                    'NUEVO', 'CONTACTADO', 'INTERESADO', 'NO_INTERESADO', 'CONVERTIDO', 'DESCARTADO'
                )
            );
    END IF;
END $$;
