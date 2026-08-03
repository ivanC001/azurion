BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM tenant_interamericana_norte.usuarios
        WHERE id = 2 AND activo = true
    ) THEN
        RAISE EXCEPTION 'El usuario responsable 2 no existe o no esta activo';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM tenant_interamericana_norte.crm_catalogo_items
        WHERE id = 1 AND estado = 'ACTIVO'
    ) THEN
        RAISE EXCEPTION 'El item CRM 1 no existe o no esta activo';
    END IF;
END $$;

WITH seed (
    indice, tipo_persona, tipo_documento, numero_documento, nombre,
    razon_social, nombre_comercial, telefono, correo, direccion,
    origen, canal_ingreso, estado, nivel_interes, score_calificacion,
    presupuesto_estimado, temperatura, fecha_estimada_compra, interes_detalle
) AS (
    VALUES
        (1, 'NATURAL', 'DNI', '70001001', 'Ana Torres Medina', NULL, NULL, '900100001', 'prospecto.demo.001@example.test', 'Miraflores, Lima', 'INSTAGRAM', 'MANUAL', 'NUEVO', 'FRIO', 18, 1200.00, 'FRIO', 'MAS_ADELANTE', 'Solicito informacion general sobre el curso.'),
        (2, 'NATURAL', 'DNI', '70001002', 'Luis Mendoza Rojas', NULL, NULL, '900100002', 'prospecto.demo.002@example.test', 'San Miguel, Lima', 'FACEBOOK', 'FACEBOOK', 'NUEVO', 'BAJO', 22, 1500.00, 'FRIO', 'TRES_MESES', 'Llego desde una publicacion de Facebook.'),
        (3, 'NATURAL', 'DNI', '70001003', 'Carla Ruiz Navarro', NULL, NULL, '900100003', 'prospecto.demo.003@example.test', 'Surco, Lima', 'WEB', 'LANDING', 'CONTACTADO', 'MEDIO', 42, 1800.00, 'TIBIO', 'TREINTA_DIAS', 'Desea conocer horarios y modalidad.'),
        (4, 'NATURAL', 'DNI', '70001004', 'Diego Salazar Vega', NULL, NULL, '900100004', 'prospecto.demo.004@example.test', 'Los Olivos, Lima', 'WHATSAPP', 'WHATSAPP', 'CONTACTADO', 'TIBIO', 48, 1900.00, 'TIBIO', 'TREINTA_DIAS', 'Pidio una llamada de seguimiento.'),
        (5, 'NATURAL', 'DNI', '70001005', 'Sofia Castro Leon', NULL, NULL, '900100005', 'prospecto.demo.005@example.test', 'La Molina, Lima', 'REFERIDO', 'MANUAL', 'INTERESADO', 'ALTO', 68, 2200.00, 'CALIENTE', 'TREINTA_DIAS', 'Referida por un alumno actual.'),
        (6, 'NATURAL', 'DNI', '70001006', 'Marco Paredes Diaz', NULL, NULL, '900100006', 'prospecto.demo.006@example.test', 'Callao, Callao', 'LLAMADA', 'MANUAL', 'EN_ESPERA', 'MEDIO', 39, 1600.00, 'TIBIO', 'TRES_MESES', 'Debe confirmar disponibilidad de tiempo.'),
        (7, 'NATURAL', 'DNI', '70001007', 'Elena Vargas Soto', NULL, NULL, '900100007', 'prospecto.demo.007@example.test', 'Jesus Maria, Lima', 'VISITA', 'MANUAL', 'CALIFICADO', 'ALTO', 82, 2500.00, 'CALIENTE', 'INMEDIATO', 'Lista para recibir propuesta comercial.'),
        (8, 'NATURAL', 'DNI', '70001008', 'Jose Ramirez Pena', NULL, NULL, '900100008', 'prospecto.demo.008@example.test', 'Pueblo Libre, Lima', 'WEB', 'WEBHOOK', 'NUEVO', 'FRIO', 15, 1100.00, 'FRIO', 'DESCONOCIDO', 'Completo el formulario web de contacto.'),
        (9, 'NATURAL', 'DNI', '70001009', 'Valeria Flores Campos', NULL, NULL, '900100009', 'prospecto.demo.009@example.test', 'Barranco, Lima', 'INSTAGRAM', 'MANUAL', 'INTERESADO', 'TIBIO', 61, 2100.00, 'TIBIO', 'TREINTA_DIAS', 'Consulta por certificacion y beneficios.'),
        (10, 'NATURAL', 'DNI', '70001010', 'Renato Aguilar Nunez', NULL, NULL, '900100010', 'prospecto.demo.010@example.test', 'San Borja, Lima', 'OTRO', 'IMPORTADO', 'CONTACTADO', 'MEDIO', 45, 1750.00, 'TIBIO', 'TRES_MESES', 'Prospecto importado para seguimiento.'),
        (11, 'NATURAL', 'DNI', '70001011', 'Paula Cardenas Silva', NULL, NULL, '900100011', 'prospecto.demo.011@example.test', 'Magdalena, Lima', 'FACEBOOK', 'FACEBOOK', 'CALIFICADO', 'CALIENTE', 88, 2800.00, 'CALIENTE', 'INMEDIATO', 'Solicito enlace de pago y fecha de inicio.'),
        (12, 'NATURAL', 'DNI', '70001012', 'Andres Lozano Cruz', NULL, NULL, '900100012', 'prospecto.demo.012@example.test', 'Chorrillos, Lima', 'WHATSAPP', 'WHATSAPP', 'EN_ESPERA', 'MEDIO', 36, 1450.00, 'TIBIO', 'MAS_ADELANTE', 'Esperando confirmacion de presupuesto.'),
        (13, 'NATURAL', 'DNI', '70001013', 'Camila Herrera Mora', NULL, NULL, '900100013', 'prospecto.demo.013@example.test', 'Lince, Lima', 'REFERIDO', 'MANUAL', 'INTERESADO', 'ALTO', 72, 2400.00, 'CALIENTE', 'TREINTA_DIAS', 'Interesada en capacitacion especializada.'),
        (14, 'NATURAL', 'DNI', '70001014', 'Fernando Cabrera Ortiz', NULL, NULL, '900100014', 'prospecto.demo.014@example.test', 'Ate, Lima', 'LLAMADA', 'MANUAL', 'CONTACTADO', 'BAJO', 30, 1300.00, 'FRIO', 'TRES_MESES', 'Solicito que lo contacten por la tarde.'),
        (15, 'NATURAL', 'DNI', '70001015', 'Lucia Espinoza Reyes', NULL, NULL, '900100015', 'prospecto.demo.015@example.test', 'Rimac, Lima', 'VISITA', 'MANUAL', 'CALIFICADO', 'ALTO', 79, 2600.00, 'CALIENTE', 'INMEDIATO', 'Visito la sede y desea matricularse.'),
        (16, 'JURIDICA', 'RUC', '20690010012', 'Andrea Rios - Compras', 'COMERCIAL DEMO ANDINA S.A.C.', 'DEMO ANDINA', '900100016', 'prospecto.demo.016@example.test', 'San Isidro, Lima', 'WEB', 'LANDING', 'NUEVO', 'MEDIO', 34, 6500.00, 'TIBIO', 'TRES_MESES', 'Empresa interesada en capacitar a su equipo.'),
        (17, 'JURIDICA', 'RUC', '20690010021', 'Miguel Santos - Gerencia', 'INVERSIONES PRUEBA NORTE S.A.C.', 'PRUEBA NORTE', '900100017', 'prospecto.demo.017@example.test', 'Trujillo, La Libertad', 'REFERIDO', 'MANUAL', 'CONTACTADO', 'ALTO', 57, 8200.00, 'TIBIO', 'TREINTA_DIAS', 'Solicita propuesta para cinco colaboradores.'),
        (18, 'JURIDICA', 'RUC', '20690010039', 'Rosa Medina - RRHH', 'SERVICIOS EMPRESARIALES DEMO E.I.R.L.', 'SERVI DEMO', '900100018', 'prospecto.demo.018@example.test', 'Cercado de Lima, Lima', 'LINKEDIN', 'IMPORTADO', 'INTERESADO', 'ALTO', 70, 9600.00, 'CALIENTE', 'TREINTA_DIAS', 'Busca un programa corporativo a medida.'),
        (19, 'JURIDICA', 'RUC', '20690010047', 'Carlos Pena - Administracion', 'LOGISTICA FICTICIA DEL PACIFICO S.A.C.', 'LOGIFICT', '900100019', 'prospecto.demo.019@example.test', 'Callao, Callao', 'LLAMADA', 'MANUAL', 'EN_ESPERA', 'MEDIO', 41, 5400.00, 'TIBIO', 'TRES_MESES', 'Pendiente de aprobacion de administracion.'),
        (20, 'JURIDICA', 'RUC', '20690010055', 'Maria Vega - Compras', 'DISTRIBUIDORA DEMO SUR S.A.C.', 'DEMO SUR', '900100020', 'prospecto.demo.020@example.test', 'Arequipa, Arequipa', 'FACEBOOK', 'FACEBOOK', 'CALIFICADO', 'CALIENTE', 91, 12500.00, 'CALIENTE', 'INMEDIATO', 'Requiere cotizacion formal para diez vacantes.'),
        (21, 'JURIDICA', 'RUC', '20690010063', 'Jorge Luna - Operaciones', 'TECNOLOGIA DE PRUEBA PERU S.A.C.', 'TECH DEMO', '900100021', 'prospecto.demo.021@example.test', 'Miraflores, Lima', 'WEB', 'WEBHOOK', 'NUEVO', 'FRIO', 20, 7200.00, 'FRIO', 'DESCONOCIDO', 'Registro recibido desde integracion web.'),
        (22, 'JURIDICA', 'RUC', '20690010071', 'Patricia Solis - RRHH', 'CONSULTORES FICTICIOS ASOCIADOS S.A.C.', 'CONSULTORES DEMO', '900100022', 'prospecto.demo.022@example.test', 'Surco, Lima', 'WHATSAPP', 'WHATSAPP', 'INTERESADO', 'ALTO', 74, 10800.00, 'CALIENTE', 'TREINTA_DIAS', 'Solicita temario y condiciones corporativas.'),
        (23, 'JURIDICA', 'RUC', '20690010080', 'Alberto Reyes - Gerencia', 'INDUSTRIAS DEMOSTRACION S.A.C.', 'INDUSTRIAS DEMO', '900100023', 'prospecto.demo.023@example.test', 'Ate, Lima', 'VISITA', 'MANUAL', 'CALIFICADO', 'ALTO', 86, 14000.00, 'CALIENTE', 'INMEDIATO', 'Reunion presencial completada satisfactoriamente.'),
        (24, 'JURIDICA', 'RUC', '20690010098', 'Daniela Cruz - Compras', 'PROYECTOS SIMULADOS DEL NORTE S.A.C.', 'PROYECTOS DEMO', '900100024', 'prospecto.demo.024@example.test', 'Chiclayo, Lambayeque', 'REFERIDO', 'MANUAL', 'CONTACTADO', 'MEDIO', 52, 7800.00, 'TIBIO', 'TRES_MESES', 'Referido por un cliente empresarial.'),
        (25, 'JURIDICA', 'RUC', '20690010101', 'Ricardo Flores - Finanzas', 'NEGOCIOS DE ENSAYO PERU S.A.C.', 'ENSAYO PERU', '900100025', 'prospecto.demo.025@example.test', 'San Borja, Lima', 'OTRO', 'IMPORTADO', 'EN_ESPERA', 'BAJO', 33, 4900.00, 'FRIO', 'MAS_ADELANTE', 'Evaluara el presupuesto el siguiente trimestre.'),
        (26, 'JURIDICA', 'RUC', '20690010110', 'Laura Rojas - RRHH', 'SOLUCIONES CORPORATIVAS DEMO S.A.C.', 'SOLUCIONES DEMO', '900100026', 'prospecto.demo.026@example.test', 'La Molina, Lima', 'INSTAGRAM', 'MANUAL', 'INTERESADO', 'ALTO', 69, 9300.00, 'CALIENTE', 'TREINTA_DIAS', 'Interes en plan de formacion empresarial.'),
        (27, 'JURIDICA', 'RUC', '20690010128', 'Sergio Campos - Gerencia', 'GRUPO FICTICIO CENTRAL S.A.C.', 'GRUPO CENTRAL DEMO', '900100027', 'prospecto.demo.027@example.test', 'Huancayo, Junin', 'LLAMADA', 'MANUAL', 'CONTACTADO', 'MEDIO', 47, 6800.00, 'TIBIO', 'TRES_MESES', 'Primera llamada realizada con gerencia.'),
        (28, 'JURIDICA', 'RUC', '20690010136', 'Natalia Ortiz - Compras', 'IMPORTACIONES DE PRUEBA S.A.C.', 'IMPORTA DEMO', '900100028', 'prospecto.demo.028@example.test', 'Los Olivos, Lima', 'FACEBOOK', 'FACEBOOK', 'NUEVO', 'FRIO', 25, 5700.00, 'FRIO', 'DESCONOCIDO', 'Consulta inicial desde campaña empresarial.'),
        (29, 'JURIDICA', 'RUC', '20690010144', 'Victor Salas - Administracion', 'CONSTRUCTORA DEMO COSTA S.A.C.', 'CONSTRUCTORA DEMO', '900100029', 'prospecto.demo.029@example.test', 'Piura, Piura', 'WEB', 'LANDING', 'CALIFICADO', 'CALIENTE', 89, 15500.00, 'CALIENTE', 'INMEDIATO', 'Solicita propuesta y contrato de servicio.'),
        (30, 'JURIDICA', 'RUC', '20690010152', 'Monica Leon - RRHH', 'ALIMENTOS FICTICIOS DEL PERU S.A.C.', 'ALIMENTOS DEMO', '900100030', 'prospecto.demo.030@example.test', 'Villa El Salvador, Lima', 'WHATSAPP', 'WHATSAPP', 'INTERESADO', 'ALTO', 76, 11800.00, 'CALIENTE', 'TREINTA_DIAS', 'Desea reservar cupos para su personal.')
), normalized AS (
    SELECT
        seed.*,
        CASE WHEN origen = 'LINKEDIN' THEN 'OTRO' ELSE origen END AS origen_valido,
        CASE
            WHEN nivel_interes = 'ALTO' THEN 'CALIENTE'
            WHEN nivel_interes = 'BAJO' THEN 'FRIO'
            ELSE nivel_interes
        END AS nivel_interes_valido
    FROM seed
)
INSERT INTO tenant_interamericana_norte.crm_prospectos (
    tipo_persona, pais_codigo, tipo_documento, numero_documento, nombre,
    razon_social, nombre_comercial, telefono, correo, direccion, origen,
    canal_ingreso, campania, mensaje, tipo_interes, interes_principal,
    interes_detalle, presupuesto_estimado, fecha_interes, metadata_json,
    catalogo_item_id, producto_pendiente, estado, nivel_interes,
    necesidad_identificada, interes_real, presupuesto_definido,
    tomador_decision, fecha_estimada_compra, score_calificacion,
    temperatura, fecha_proximo_contacto, responsable_id, observacion,
    created_at, updated_at, version
)
SELECT
    s.tipo_persona,
    'PE',
    s.tipo_documento,
    s.numero_documento,
    s.nombre,
    s.razon_social,
    s.nombre_comercial,
    s.telefono,
    s.correo,
    s.direccion,
    s.origen_valido,
    s.canal_ingreso,
    'DEMO CRM AGOSTO 2026',
    'Prospecto ficticio generado para pruebas funcionales del CRM.',
    'CURSO',
    'Curso Intermedio Work',
    s.interes_detalle,
    s.presupuesto_estimado,
    CURRENT_DATE - ((s.indice % 12)::text || ' days')::interval,
    json_build_object('seed', 'DEMO_CRM_2026_08', 'indice', s.indice, 'ficticio', true)::text,
    1,
    false,
    s.estado,
    s.nivel_interes_valido,
    s.estado IN ('INTERESADO', 'CALIFICADO'),
    CASE WHEN s.score_calificacion >= 70 THEN 'ALTO' WHEN s.score_calificacion >= 40 THEN 'MEDIO' ELSE 'BAJO' END,
    CASE WHEN s.score_calificacion >= 60 THEN 'SI' WHEN s.score_calificacion >= 35 THEN 'NO' ELSE 'DESCONOCIDO' END,
    CASE WHEN s.tipo_persona = 'JURIDICA' THEN 'DEBE_CONSULTAR' ELSE 'SI' END,
    s.fecha_estimada_compra,
    s.score_calificacion,
    s.temperatura,
    CURRENT_TIMESTAMP + (((s.indice % 7) + 1)::text || ' days')::interval,
    '2',
    'DATO FICTICIO DE PRUEBA. No contactar fuera del entorno de demostracion.',
    CURRENT_TIMESTAMP - (s.indice::text || ' hours')::interval,
    CURRENT_TIMESTAMP - (s.indice::text || ' hours')::interval,
    0
FROM normalized s
WHERE NOT EXISTS (
    SELECT 1
    FROM tenant_interamericana_norte.crm_prospectos existing
    WHERE lower(existing.correo) = lower(s.correo)
);

COMMIT;

SELECT
    count(*) AS prospectos_demo,
    count(*) FILTER (WHERE tipo_persona = 'NATURAL') AS personas,
    count(*) FILTER (WHERE tipo_persona = 'JURIDICA') AS empresas
FROM tenant_interamericana_norte.crm_prospectos
WHERE metadata_json LIKE '%"seed" : "DEMO_CRM_2026_08"%'
   OR metadata_json LIKE '%"seed":"DEMO_CRM_2026_08"%';
