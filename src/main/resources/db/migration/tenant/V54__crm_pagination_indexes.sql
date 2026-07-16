CREATE INDEX IF NOT EXISTS idx_crm_prospectos_estado_created
    ON crm_prospectos(estado, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_crm_prospectos_responsable_estado_created
    ON crm_prospectos(responsable_id, estado, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_crm_prospectos_origen_canal_created
    ON crm_prospectos(origen, canal_ingreso, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_crm_prospectos_campania_created
    ON crm_prospectos(campania, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_crm_prospectos_fecha_interes
    ON crm_prospectos(fecha_interes DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_crm_actividades_usuario_estado_fecha
    ON crm_actividades(usuario_id, estado, fecha_programada ASC, id DESC);

CREATE INDEX IF NOT EXISTS idx_crm_actividades_estado_fecha
    ON crm_actividades(estado, fecha_programada ASC, id DESC);

CREATE INDEX IF NOT EXISTS idx_crm_actividades_tipo_fecha
    ON crm_actividades(tipo_actividad, fecha_programada ASC);

CREATE INDEX IF NOT EXISTS idx_crm_actividades_prospecto_fecha
    ON crm_actividades(prospecto_id, fecha_programada ASC);

CREATE INDEX IF NOT EXISTS idx_crm_actividades_oportunidad_fecha
    ON crm_actividades(oportunidad_id, fecha_programada ASC);

CREATE INDEX IF NOT EXISTS idx_crm_oportunidades_etapa_estado_updated
    ON crm_oportunidades(etapa_id, estado, fecha_ultima_actualizacion DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_crm_oportunidades_responsable_estado_updated
    ON crm_oportunidades(responsable_id, estado, fecha_ultima_actualizacion DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_crm_oportunidades_cierre_estimada
    ON crm_oportunidades(fecha_cierre_estimada DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_crm_oportunidades_estado_pago
    ON crm_oportunidades(estado, monto_real, monto_estimado);
