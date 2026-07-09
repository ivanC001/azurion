UPDATE crm_etapas_pipeline
SET activo = FALSE,
    updated_at = now()
WHERE codigo IN ('NUEVO', 'CONTACTADO');

UPDATE crm_etapas_pipeline
SET nombre = seed.nombre,
    orden = seed.orden,
    color = seed.color,
    es_ganado = seed.es_ganado,
    es_perdido = seed.es_perdido,
    probabilidad_default = seed.probabilidad_default,
    icono = seed.icono,
    requiere_validacion = seed.requiere_validacion,
    modo_validacion = seed.modo_validacion,
    activo = TRUE,
    updated_at = now()
FROM (
    VALUES
        ('INTERESADO', 'Interesado', 1, '#2563eb', FALSE, FALSE, 50, 'pi pi-user', TRUE, 'WARNING'),
        ('COTIZADO', 'Cotizado', 2, '#64748b', FALSE, FALSE, 65, 'pi pi-file-edit', TRUE, 'STRICT'),
        ('NEGOCIACION', 'Negociacion', 3, '#64748b', FALSE, FALSE, 80, 'pi pi-handshake', TRUE, 'WARNING'),
        ('GANADO', 'Ganado', 4, '#64748b', TRUE, FALSE, 100, 'pi pi-trophy', TRUE, 'STRICT'),
        ('PERDIDO', 'Perdido', 5, '#64748b', FALSE, TRUE, 0, 'pi pi-times-circle', TRUE, 'STRICT')
) AS seed(codigo, nombre, orden, color, es_ganado, es_perdido, probabilidad_default, icono, requiere_validacion, modo_validacion)
WHERE crm_etapas_pipeline.codigo = seed.codigo;

INSERT INTO crm_etapas_pipeline (
    codigo,
    nombre,
    orden,
    color,
    es_ganado,
    es_perdido,
    activo,
    pipeline_id,
    descripcion,
    probabilidad_default,
    icono,
    requiere_validacion,
    modo_validacion
)
SELECT
    seed.codigo,
    seed.nombre,
    seed.orden,
    seed.color,
    seed.es_ganado,
    seed.es_perdido,
    TRUE,
    (SELECT id FROM crm_pipelines WHERE es_default = TRUE ORDER BY id LIMIT 1),
    seed.descripcion,
    seed.probabilidad_default,
    seed.icono,
    seed.requiere_validacion,
    seed.modo_validacion
FROM (
    VALUES
        ('INTERESADO', 'Interesado', 1, '#2563eb', FALSE, FALSE, 'Necesidad confirmada para preparar cotizacion.', 50, 'pi pi-user', TRUE, 'WARNING'),
        ('COTIZADO', 'Cotizado', 2, '#64748b', FALSE, FALSE, 'Cotizacion creada o enviada al cliente.', 65, 'pi pi-file-edit', TRUE, 'STRICT'),
        ('NEGOCIACION', 'Negociacion', 3, '#64748b', FALSE, FALSE, 'Cliente pidio ajuste de precio, alcance o condiciones.', 80, 'pi pi-handshake', TRUE, 'WARNING'),
        ('GANADO', 'Ganado', 4, '#64748b', TRUE, FALSE, 'Venta aceptada o cierre comercial confirmado.', 100, 'pi pi-trophy', TRUE, 'STRICT'),
        ('PERDIDO', 'Perdido', 5, '#64748b', FALSE, TRUE, 'Oportunidad cerrada con motivo de perdida.', 0, 'pi pi-times-circle', TRUE, 'STRICT')
) AS seed(codigo, nombre, orden, color, es_ganado, es_perdido, descripcion, probabilidad_default, icono, requiere_validacion, modo_validacion)
WHERE NOT EXISTS (
    SELECT 1
    FROM crm_etapas_pipeline etapa
    WHERE etapa.codigo = seed.codigo
);

UPDATE crm_oportunidades oportunidad
SET etapa = 'INTERESADO',
    etapa_id = etapa.id,
    probabilidad = GREATEST(COALESCE(oportunidad.probabilidad, 0), 50),
    fecha_ultima_actualizacion = now(),
    updated_at = now()
FROM crm_etapas_pipeline etapa
WHERE etapa.codigo = 'INTERESADO'
  AND oportunidad.etapa IN ('NUEVO', 'CONTACTADO');

DELETE FROM crm_pipeline_stage_checklist checklist
USING crm_etapas_pipeline etapa
WHERE checklist.stage_id = etapa.id
  AND etapa.codigo IN ('NUEVO', 'CONTACTADO');

INSERT INTO crm_pipeline_stage_checklist (stage_id, codigo, nombre, descripcion, obligatorio, orden, activo)
SELECT etapa.id, seed.codigo, seed.nombre, seed.descripcion, seed.obligatorio, seed.orden, TRUE
FROM (
    VALUES
        ('INTERESADO', 'CLIENTE_DEFINIDO', 'Cliente definido', 'La oportunidad debe estar asociada a un cliente o prospecto identificado.', TRUE, 1),
        ('INTERESADO', 'REQUERIMIENTO', 'Requerimiento registrado', 'Registrar curso, producto, servicio o paquete solicitado por el cliente.', TRUE, 2),
        ('INTERESADO', 'CANTIDAD_DEFINIDA', 'Vacantes o cantidad definidas', 'Indicar cantidad, participantes, unidades o alcance de la propuesta.', TRUE, 3),
        ('INTERESADO', 'VALOR_ESTIMADO', 'Valor estimado', 'Definir precio unitario o monto referencial para cotizar.', TRUE, 4),
        ('COTIZADO', 'CREAR_COTIZACION', 'Crear cotizacion', 'Generar una propuesta desde la oportunidad.', TRUE, 1),
        ('COTIZADO', 'ENVIAR_COTIZACION', 'Enviar cotizacion', 'Confirmar envio por correo, WhatsApp o canal acordado.', TRUE, 2),
        ('NEGOCIACION', 'REGISTRAR_OBJECIONES', 'Registrar objeciones', 'Anotar barreras de precio, plazo, garantia o alcance.', TRUE, 1),
        ('NEGOCIACION', 'FORMA_PAGO', 'Definir forma de pago', 'Registrar contado, credito, cuotas, adelanto u otra condicion.', TRUE, 2),
        ('GANADO', 'CONFIRMAR_ACEPTACION', 'Confirmar aceptacion del cliente', 'Registrar aceptacion o documento de cierre.', TRUE, 1),
        ('PERDIDO', 'MOTIVO_PERDIDA', 'Registrar motivo de perdida', 'Seleccionar una razon concreta de perdida.', TRUE, 1)
) AS seed(stage_code, codigo, nombre, descripcion, obligatorio, orden)
JOIN crm_etapas_pipeline etapa ON etapa.codigo = seed.stage_code
ON CONFLICT (stage_id, codigo) DO UPDATE
SET nombre = EXCLUDED.nombre,
    descripcion = EXCLUDED.descripcion,
    obligatorio = EXCLUDED.obligatorio,
    orden = EXCLUDED.orden,
    activo = TRUE,
    updated_at = now();
