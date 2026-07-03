ALTER TABLE crm_prospectos
    ADD COLUMN IF NOT EXISTS necesidad_identificada BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS interes_real VARCHAR(20) NOT NULL DEFAULT 'BAJO',
    ADD COLUMN IF NOT EXISTS presupuesto_definido VARCHAR(20) NOT NULL DEFAULT 'DESCONOCIDO',
    ADD COLUMN IF NOT EXISTS tomador_decision VARCHAR(20) NOT NULL DEFAULT 'DESCONOCIDO',
    ADD COLUMN IF NOT EXISTS fecha_estimada_compra VARCHAR(30) NOT NULL DEFAULT 'DESCONOCIDO',
    ADD COLUMN IF NOT EXISTS score_calificacion INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS temperatura VARCHAR(20) NOT NULL DEFAULT 'FRIO',
    ADD COLUMN IF NOT EXISTS motivo_espera VARCHAR(120),
    ADD COLUMN IF NOT EXISTS fecha_proximo_contacto TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS motivo_perdida VARCHAR(120),
    ADD COLUMN IF NOT EXISTS observacion_perdida VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS oportunidad_id BIGINT;

ALTER TABLE crm_prospectos DROP CONSTRAINT IF EXISTS chk_crm_prospectos_estado;
ALTER TABLE crm_prospectos
    ADD CONSTRAINT chk_crm_prospectos_estado
    CHECK (estado IN ('NUEVO', 'CONTACTADO', 'EN_ESPERA', 'INTERESADO', 'CALIFICADO', 'PERDIDO', 'CONVERTIDO', 'NO_INTERESADO', 'DESCARTADO'));

UPDATE crm_prospectos
SET estado = CASE
        WHEN estado = 'NO_INTERESADO' THEN 'PERDIDO'
        WHEN estado = 'DESCARTADO' THEN 'PERDIDO'
        ELSE estado
    END,
    interes_real = CASE
        WHEN nivel_interes = 'CALIENTE' THEN 'ALTO'
        WHEN nivel_interes = 'MEDIO' THEN 'MEDIO'
        ELSE COALESCE(interes_real, 'BAJO')
    END,
    temperatura = CASE
        WHEN nivel_interes = 'CALIENTE' THEN 'CALIENTE'
        WHEN nivel_interes = 'MEDIO' THEN 'TIBIO'
        ELSE COALESCE(temperatura, 'FRIO')
    END
WHERE estado IN ('NO_INTERESADO', 'DESCARTADO')
   OR interes_real IS NULL
   OR temperatura IS NULL;

UPDATE crm_prospectos
SET score_calificacion =
    (CASE WHEN necesidad_identificada THEN 30 ELSE 0 END) +
    (CASE interes_real WHEN 'ALTO' THEN 30 WHEN 'MEDIO' THEN 20 ELSE 0 END) +
    (CASE presupuesto_definido WHEN 'SI' THEN 20 ELSE 0 END) +
    (CASE tomador_decision WHEN 'SI' THEN 10 WHEN 'DEBE_CONSULTAR' THEN 5 ELSE 0 END) +
    (CASE fecha_estimada_compra WHEN 'INMEDIATO' THEN 10 WHEN 'TREINTA_DIAS' THEN 8 WHEN 'TRES_MESES' THEN 5 WHEN 'MAS_ADELANTE' THEN 2 ELSE 0 END);

UPDATE crm_prospectos
SET temperatura = CASE
        WHEN score_calificacion >= 70 THEN 'CALIENTE'
        WHEN score_calificacion >= 40 THEN 'TIBIO'
        ELSE 'FRIO'
    END,
    estado = CASE
        WHEN estado NOT IN ('CONVERTIDO', 'PERDIDO', 'EN_ESPERA')
             AND necesidad_identificada
             AND interes_real IN ('MEDIO', 'ALTO')
        THEN 'CALIFICADO'
        ELSE estado
    END;

ALTER TABLE crm_prospectos DROP CONSTRAINT IF EXISTS chk_crm_prospectos_estado;
ALTER TABLE crm_prospectos
    ADD CONSTRAINT chk_crm_prospectos_estado
    CHECK (estado IN ('NUEVO', 'CONTACTADO', 'EN_ESPERA', 'INTERESADO', 'CALIFICADO', 'PERDIDO', 'CONVERTIDO', 'NO_INTERESADO', 'DESCARTADO'));

ALTER TABLE crm_prospectos DROP CONSTRAINT IF EXISTS chk_crm_prospectos_nivel_interes;
ALTER TABLE crm_prospectos
    ADD CONSTRAINT chk_crm_prospectos_nivel_interes
    CHECK (nivel_interes IS NULL OR nivel_interes IN ('FRIO', 'TIBIO', 'MEDIO', 'CALIENTE'));

ALTER TABLE crm_prospectos
    ADD CONSTRAINT chk_crm_prospectos_interes_real
    CHECK (interes_real IN ('BAJO', 'MEDIO', 'ALTO'));

ALTER TABLE crm_prospectos
    ADD CONSTRAINT chk_crm_prospectos_presupuesto_definido
    CHECK (presupuesto_definido IN ('SI', 'NO', 'DESCONOCIDO'));

ALTER TABLE crm_prospectos
    ADD CONSTRAINT chk_crm_prospectos_tomador_decision
    CHECK (tomador_decision IN ('SI', 'NO', 'DEBE_CONSULTAR', 'DESCONOCIDO'));

ALTER TABLE crm_prospectos
    ADD CONSTRAINT chk_crm_prospectos_fecha_estimada_compra
    CHECK (fecha_estimada_compra IN ('INMEDIATO', 'TREINTA_DIAS', 'TRES_MESES', 'MAS_ADELANTE', 'DESCONOCIDO'));

ALTER TABLE crm_prospectos
    ADD CONSTRAINT chk_crm_prospectos_score_calificacion
    CHECK (score_calificacion BETWEEN 0 AND 100);

ALTER TABLE crm_prospectos
    ADD CONSTRAINT chk_crm_prospectos_temperatura
    CHECK (temperatura IN ('FRIO', 'TIBIO', 'CALIENTE'));

ALTER TABLE crm_actividades DROP CONSTRAINT IF EXISTS chk_crm_actividades_resultado_contacto;
ALTER TABLE crm_actividades
    ADD CONSTRAINT chk_crm_actividades_resultado_contacto
    CHECK (
        resultado_contacto IS NULL OR resultado_contacto IN (
            'CONTACTADO', 'INTERESADO', 'MUY_INTERESADO', 'REPROGRAMADO',
            'LLAMAR_DESPUES', 'EN_ESPERA', 'SIN_RESPUESTA', 'NO_RESPONDE',
            'NO_INTERESADO', 'PERDIDO', 'DESCARTADO', 'SOLICITA_PROPUESTA',
            'COTIZACION_SOLICITADA'
        )
    );

ALTER TABLE crm_actividades DROP CONSTRAINT IF EXISTS chk_crm_actividades_nivel_interes;
ALTER TABLE crm_actividades
    ADD CONSTRAINT chk_crm_actividades_nivel_interes
    CHECK (nivel_interes IS NULL OR nivel_interes IN ('BAJO', 'MEDIO', 'ALTO', 'FRIO', 'TIBIO', 'CALIENTE'));

ALTER TABLE crm_actividades DROP CONSTRAINT IF EXISTS chk_crm_actividades_estado_prospecto_resultado;
ALTER TABLE crm_actividades
    ADD CONSTRAINT chk_crm_actividades_estado_prospecto_resultado
    CHECK (
        estado_prospecto_resultado IS NULL OR estado_prospecto_resultado IN (
            'NUEVO', 'CONTACTADO', 'EN_ESPERA', 'INTERESADO', 'CALIFICADO',
            'PERDIDO', 'CONVERTIDO', 'NO_INTERESADO', 'DESCARTADO'
        )
    );

CREATE INDEX IF NOT EXISTS idx_crm_prospectos_calificacion ON crm_prospectos(estado, temperatura, score_calificacion);
CREATE INDEX IF NOT EXISTS idx_crm_prospectos_oportunidad ON crm_prospectos(oportunidad_id);
