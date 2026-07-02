CREATE TABLE IF NOT EXISTS crm_pipelines (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    descripcion VARCHAR(300),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    es_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_crm_pipelines_default
    ON crm_pipelines(es_default)
    WHERE es_default = TRUE;

INSERT INTO crm_pipelines (nombre, descripcion, activo, es_default)
VALUES ('Pipeline comercial', 'Pipeline base de oportunidades CRM', TRUE, TRUE)
ON CONFLICT DO NOTHING;

ALTER TABLE crm_etapas_pipeline
    ADD COLUMN IF NOT EXISTS pipeline_id BIGINT REFERENCES crm_pipelines(id),
    ADD COLUMN IF NOT EXISTS descripcion VARCHAR(300),
    ADD COLUMN IF NOT EXISTS probabilidad_default INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS icono VARCHAR(80),
    ADD COLUMN IF NOT EXISTS requiere_validacion BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS modo_validacion VARCHAR(20) NOT NULL DEFAULT 'WARNING';

UPDATE crm_etapas_pipeline
SET pipeline_id = (SELECT id FROM crm_pipelines WHERE es_default = TRUE ORDER BY id LIMIT 1)
WHERE pipeline_id IS NULL;

UPDATE crm_etapas_pipeline
SET descripcion = CASE codigo
        WHEN 'NUEVO' THEN 'Oportunidad recien creada, pendiente de primera gestion.'
        WHEN 'CONTACTADO' THEN 'Cliente ya fue contactado y existe una primera respuesta.'
        WHEN 'INTERESADO' THEN 'Cliente mostro interes real y se califico la necesidad.'
        WHEN 'COTIZADO' THEN 'Se envio una propuesta o cotizacion formal.'
        WHEN 'NEGOCIACION' THEN 'Se negocian precio, condiciones, pago o cierre.'
        WHEN 'GANADO' THEN 'Venta aceptada o cierre comercial confirmado.'
        WHEN 'PERDIDO' THEN 'Oportunidad descartada con motivo registrado.'
        ELSE COALESCE(descripcion, 'Etapa comercial configurable.')
    END,
    probabilidad_default = CASE codigo
        WHEN 'NUEVO' THEN 10
        WHEN 'CONTACTADO' THEN 25
        WHEN 'INTERESADO' THEN 50
        WHEN 'COTIZADO' THEN 65
        WHEN 'NEGOCIACION' THEN 80
        WHEN 'GANADO' THEN 100
        WHEN 'PERDIDO' THEN 0
        ELSE probabilidad_default
    END,
    icono = CASE codigo
        WHEN 'NUEVO' THEN 'pi pi-plus-circle'
        WHEN 'CONTACTADO' THEN 'pi pi-phone'
        WHEN 'INTERESADO' THEN 'pi pi-star'
        WHEN 'COTIZADO' THEN 'pi pi-file-edit'
        WHEN 'NEGOCIACION' THEN 'pi pi-handshake'
        WHEN 'GANADO' THEN 'pi pi-trophy'
        WHEN 'PERDIDO' THEN 'pi pi-times-circle'
        ELSE COALESCE(icono, 'pi pi-briefcase')
    END,
    requiere_validacion = TRUE,
    modo_validacion = CASE codigo
        WHEN 'NUEVO' THEN 'FREE'
        WHEN 'CONTACTADO' THEN 'WARNING'
        WHEN 'INTERESADO' THEN 'WARNING'
        WHEN 'COTIZADO' THEN 'STRICT'
        WHEN 'NEGOCIACION' THEN 'WARNING'
        WHEN 'GANADO' THEN 'STRICT'
        WHEN 'PERDIDO' THEN 'STRICT'
        ELSE modo_validacion
    END,
    updated_at = now();

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_crm_etapas_pipeline_probabilidad'
    ) THEN
        ALTER TABLE crm_etapas_pipeline
            ADD CONSTRAINT chk_crm_etapas_pipeline_probabilidad
            CHECK (probabilidad_default BETWEEN 0 AND 100);
    END IF;
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_crm_etapas_pipeline_modo'
    ) THEN
        ALTER TABLE crm_etapas_pipeline
            ADD CONSTRAINT chk_crm_etapas_pipeline_modo
            CHECK (modo_validacion IN ('STRICT', 'WARNING', 'FREE'));
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS crm_pipeline_stage_checklist (
    id BIGSERIAL PRIMARY KEY,
    stage_id BIGINT NOT NULL REFERENCES crm_etapas_pipeline(id) ON DELETE CASCADE,
    codigo VARCHAR(80) NOT NULL,
    nombre VARCHAR(180) NOT NULL,
    descripcion VARCHAR(300),
    obligatorio BOOLEAN NOT NULL DEFAULT TRUE,
    orden INTEGER NOT NULL DEFAULT 1,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_crm_pipeline_checklist_orden CHECK (orden > 0),
    CONSTRAINT ux_crm_pipeline_checklist_stage_codigo UNIQUE(stage_id, codigo)
);

INSERT INTO crm_pipeline_stage_checklist (stage_id, codigo, nombre, descripcion, obligatorio, orden, activo)
SELECT e.id, seed.codigo, seed.nombre, seed.descripcion, seed.obligatorio, seed.orden, TRUE
FROM (
    VALUES
        ('NUEVO', 'REVISION_INFO', 'Revisar informacion del prospecto', 'Validar contacto, origen y producto de interes.', TRUE, 1),
        ('NUEVO', 'ASIGNAR_RESPONSABLE', 'Asignar responsable', 'Definir vendedor o asesor responsable del seguimiento.', TRUE, 2),
        ('NUEVO', 'PROGRAMAR_PRIMERA_ACTIVIDAD', 'Programar primera actividad', 'Registrar llamada, WhatsApp, correo o reunion inicial.', TRUE, 3),
        ('CONTACTADO', 'REGISTRAR_CONTACTO', 'Registrar primera llamada o WhatsApp', 'Guardar la primera interaccion realizada.', TRUE, 1),
        ('CONTACTADO', 'IDENTIFICAR_NECESIDAD', 'Identificar necesidad', 'Anotar que busca comprar, contratar o resolver el cliente.', TRUE, 2),
        ('CONTACTADO', 'PROXIMA_ACTIVIDAD', 'Registrar proxima actividad', 'Mantener una accion futura para no perder seguimiento.', TRUE, 3),
        ('INTERESADO', 'CONFIRMAR_OFERTA', 'Confirmar producto u oferta de interes', 'Relacionar catalogo CRM, servicio, curso, inmueble, vehiculo u oferta.', TRUE, 1),
        ('INTERESADO', 'REGISTRAR_PRESUPUESTO', 'Registrar presupuesto', 'Registrar monto estimado o rango de presupuesto.', FALSE, 2),
        ('INTERESADO', 'REGISTRAR_URGENCIA', 'Registrar urgencia', 'Indicar si compra ahora, este mes o mas adelante.', FALSE, 3),
        ('INTERESADO', 'TOMADOR_DECISION', 'Identificar tomador de decision', 'Saber quien aprueba la compra o contratacion.', FALSE, 4),
        ('COTIZADO', 'CREAR_COTIZACION', 'Crear cotizacion', 'Generar una propuesta desde la oportunidad.', TRUE, 1),
        ('COTIZADO', 'ENVIAR_COTIZACION', 'Enviar cotizacion', 'Confirmar envio por correo, WhatsApp o canal acordado.', TRUE, 2),
        ('COTIZADO', 'CONFIRMAR_RECEPCION', 'Confirmar recepcion', 'Registrar que el cliente recibio la propuesta.', FALSE, 3),
        ('COTIZADO', 'SEGUIMIENTO_COTIZACION', 'Programar seguimiento de cotizacion', 'Crear una actividad futura sobre la propuesta enviada.', TRUE, 4),
        ('NEGOCIACION', 'REGISTRAR_OBJECIONES', 'Registrar objeciones', 'Anotar barreras de precio, plazo, garantia o alcance.', TRUE, 1),
        ('NEGOCIACION', 'DESCUENTO_SOLICITADO', 'Registrar descuento solicitado', 'Guardar monto o condicion solicitada por el cliente.', FALSE, 2),
        ('NEGOCIACION', 'FORMA_PAGO', 'Definir forma de pago', 'Registrar contado, credito, cuotas, adelanto u otra condicion.', TRUE, 3),
        ('NEGOCIACION', 'FECHA_CIERRE', 'Registrar fecha probable de cierre', 'Definir fecha estimada para cierre.', TRUE, 4),
        ('GANADO', 'CONFIRMAR_ACEPTACION', 'Confirmar aceptacion del cliente', 'Registrar aceptacion o documento de cierre.', TRUE, 1),
        ('GANADO', 'CONVERTIR_CLIENTE_VENTA', 'Convertir en venta o cliente', 'Crear cliente, venta o documento asociado segun corresponda.', TRUE, 2),
        ('GANADO', 'DOCUMENTO_ASOCIADO', 'Registrar documento asociado', 'Relacionar comprobante, contrato, orden o venta.', FALSE, 3),
        ('PERDIDO', 'MOTIVO_PERDIDA', 'Registrar motivo de perdida', 'Seleccionar una razon concreta de perdida.', TRUE, 1),
        ('PERDIDO', 'OBSERVACION_FINAL', 'Registrar observacion final', 'Guardar aprendizaje comercial del caso.', FALSE, 2),
        ('PERDIDO', 'CERRAR_PENDIENTES', 'Cerrar actividades pendientes', 'Cancelar o cerrar tareas que ya no aplican.', TRUE, 3)
) AS seed(stage_code, codigo, nombre, descripcion, obligatorio, orden)
JOIN crm_etapas_pipeline e ON e.codigo = seed.stage_code
ON CONFLICT (stage_id, codigo) DO UPDATE
SET nombre = EXCLUDED.nombre,
    descripcion = EXCLUDED.descripcion,
    obligatorio = EXCLUDED.obligatorio,
    orden = EXCLUDED.orden,
    activo = TRUE,
    updated_at = now();

CREATE TABLE IF NOT EXISTS crm_motivos_perdida (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(60) NOT NULL UNIQUE,
    nombre VARCHAR(150) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

INSERT INTO crm_motivos_perdida (codigo, nombre, activo)
VALUES
    ('PRECIO', 'Precio', TRUE),
    ('COMPETENCIA', 'Competencia', TRUE),
    ('SIN_PRESUPUESTO', 'Sin presupuesto', TRUE),
    ('SIN_RESPUESTA', 'Sin respuesta', TRUE),
    ('NO_ENCAJA', 'No encaja con la necesidad', TRUE),
    ('POSTERGADO', 'Decision postergada', TRUE)
ON CONFLICT (codigo) DO UPDATE
SET nombre = EXCLUDED.nombre,
    activo = TRUE,
    updated_at = now();

ALTER TABLE crm_oportunidades
    ADD COLUMN IF NOT EXISTS pipeline_id BIGINT REFERENCES crm_pipelines(id),
    ADD COLUMN IF NOT EXISTS nivel_interes VARCHAR(20),
    ADD COLUMN IF NOT EXISTS presupuesto_cliente NUMERIC(18, 2),
    ADD COLUMN IF NOT EXISTS urgencia VARCHAR(30),
    ADD COLUMN IF NOT EXISTS tomador_decision VARCHAR(180),
    ADD COLUMN IF NOT EXISTS motivo_perdida_id BIGINT REFERENCES crm_motivos_perdida(id),
    ADD COLUMN IF NOT EXISTS observaciones_perdida VARCHAR(800);

UPDATE crm_oportunidades
SET pipeline_id = (SELECT id FROM crm_pipelines WHERE es_default = TRUE ORDER BY id LIMIT 1)
WHERE pipeline_id IS NULL;

UPDATE crm_oportunidades
SET nivel_interes = CASE
        WHEN COALESCE(probabilidad, 0) >= 70 THEN 'CALIENTE'
        WHEN COALESCE(probabilidad, 0) >= 40 THEN 'MEDIO'
        ELSE 'FRIO'
    END
WHERE nivel_interes IS NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_crm_oportunidades_nivel_interes'
    ) THEN
        ALTER TABLE crm_oportunidades
            ADD CONSTRAINT chk_crm_oportunidades_nivel_interes
            CHECK (nivel_interes IS NULL OR nivel_interes IN ('FRIO', 'MEDIO', 'CALIENTE'));
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS crm_oportunidad_stage_checks (
    id BIGSERIAL PRIMARY KEY,
    oportunidad_id BIGINT NOT NULL REFERENCES crm_oportunidades(id) ON DELETE CASCADE,
    checklist_id BIGINT NOT NULL REFERENCES crm_pipeline_stage_checklist(id) ON DELETE CASCADE,
    completado BOOLEAN NOT NULL DEFAULT FALSE,
    completado_por VARCHAR(80),
    completado_en TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ux_crm_oportunidad_stage_check UNIQUE(oportunidad_id, checklist_id)
);

CREATE INDEX IF NOT EXISTS idx_crm_pipeline_stage_checklist_stage
    ON crm_pipeline_stage_checklist(stage_id, activo, orden);

CREATE INDEX IF NOT EXISTS idx_crm_oportunidad_stage_checks_oportunidad
    ON crm_oportunidad_stage_checks(oportunidad_id, checklist_id);

CREATE INDEX IF NOT EXISTS idx_crm_oportunidades_pipeline_stage
    ON crm_oportunidades(pipeline_id, etapa_id, estado);

INSERT INTO permisos (codigo, nombre, descripcion, modulo, activo, sistema)
VALUES
    ('CRM_PIPELINE_VIEW', 'Ver pipeline CRM', 'Consultar pipeline profesional de oportunidades', 'CRM', TRUE, TRUE),
    ('CRM_PIPELINE_MANAGE', 'Configurar pipeline CRM', 'Administrar etapas, reglas y checklist del pipeline', 'CRM', TRUE, TRUE),
    ('CRM_OPPORTUNITY_MOVE_STAGE', 'Mover oportunidad de etapa', 'Mover oportunidades entre etapas del pipeline', 'CRM', TRUE, TRUE),
    ('CRM_OPPORTUNITY_FORCE_MOVE_STAGE', 'Forzar movimiento de etapa', 'Permitir movimientos con advertencias o requisitos pendientes', 'CRM', TRUE, TRUE),
    ('CRM_OPPORTUNITY_MARK_WON', 'Marcar oportunidad ganada', 'Cerrar oportunidades como ganadas', 'CRM', TRUE, TRUE),
    ('CRM_OPPORTUNITY_MARK_LOST', 'Marcar oportunidad perdida', 'Cerrar oportunidades como perdidas', 'CRM', TRUE, TRUE)
ON CONFLICT (codigo) DO UPDATE
SET nombre = EXCLUDED.nombre,
    descripcion = EXCLUDED.descripcion,
    modulo = EXCLUDED.modulo,
    activo = TRUE,
    sistema = TRUE,
    updated_at = now();

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo IN (
    'CRM_PIPELINE_VIEW',
    'CRM_PIPELINE_MANAGE',
    'CRM_OPPORTUNITY_MOVE_STAGE',
    'CRM_OPPORTUNITY_FORCE_MOVE_STAGE',
    'CRM_OPPORTUNITY_MARK_WON',
    'CRM_OPPORTUNITY_MARK_LOST'
)
WHERE r.codigo IN ('ADMIN_EMPRESA', 'CRM_ADMIN', 'GERENTE_COMERCIAL')
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo IN (
    'CRM_PIPELINE_VIEW',
    'CRM_OPPORTUNITY_MOVE_STAGE',
    'CRM_OPPORTUNITY_MARK_WON',
    'CRM_OPPORTUNITY_MARK_LOST'
)
WHERE r.codigo IN ('VENDEDOR', 'CRM_VENDEDOR', 'SUPERVISOR_SUCURSAL')
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo = 'CRM_PIPELINE_VIEW'
WHERE r.codigo IN ('AUDITOR', 'CONTADOR')
ON CONFLICT (rol_id, permiso_id) DO NOTHING;
