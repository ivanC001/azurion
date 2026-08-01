INSERT INTO crm_actividades (
    prospecto_id,
    oportunidad_id,
    cliente_id,
    tipo_actividad,
    asunto,
    descripcion,
    fecha_programada,
    estado,
    usuario_id
)
SELECT
    oportunidad.prospecto_id,
    oportunidad.id,
    oportunidad.cliente_id,
    'TAREA',
    'Definir siguiente accion comercial',
    'Actividad creada automaticamente para regularizar una oportunidad abierta sin siguiente paso.',
    now() + INTERVAL '1 day',
    'PENDIENTE',
    oportunidad.responsable_id
FROM crm_oportunidades oportunidad
WHERE oportunidad.estado = 'ABIERTA'
  AND NOT EXISTS (
      SELECT 1
      FROM crm_actividades actividad
      WHERE actividad.oportunidad_id = oportunidad.id
        AND actividad.estado = 'PENDIENTE'
  );
