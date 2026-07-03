-- Repairs tenants created before inventory/tax/CRM migrations were expanded.
-- Keep this migration idempotent: older tenants may be missing only some columns.

ALTER TABLE productos
    ADD COLUMN IF NOT EXISTS codigo VARCHAR(80),
    ADD COLUMN IF NOT EXISTS codigo_barras VARCHAR(80),
    ADD COLUMN IF NOT EXISTS descripcion VARCHAR(500),
    ADD COLUMN IF NOT EXISTS categoria_id BIGINT,
    ADD COLUMN IF NOT EXISTS marca_id BIGINT,
    ADD COLUMN IF NOT EXISTS unidad_medida_id BIGINT,
    ADD COLUMN IF NOT EXISTS tipo_producto VARCHAR(30) NOT NULL DEFAULT 'PRODUCTO',
    ADD COLUMN IF NOT EXISTS imagen_url TEXT,
    ADD COLUMN IF NOT EXISTS precio_compra_base NUMERIC(18,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS precio_venta_base NUMERIC(18,2),
    ADD COLUMN IF NOT EXISTS costo_promedio NUMERIC(18,6) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS afecto_igv BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS tipo_afectacion_igv_id VARCHAR(4),
    ADD COLUMN IF NOT EXISTS tributo_id VARCHAR(6),
    ADD COLUMN IF NOT EXISTS porcentaje_impuesto NUMERIC(5,2),
    ADD COLUMN IF NOT EXISTS usa_configuracion_empresa BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS maneja_stock BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS maneja_lotes BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS maneja_vencimiento BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS stock_minimo_global NUMERIC(18,4) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS stock_minimo NUMERIC(18,4) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS foto TEXT,
    ADD COLUMN IF NOT EXISTS estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    ADD COLUMN IF NOT EXISTS activo BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE productos
SET codigo = COALESCE(codigo, sku),
    precio_venta_base = COALESCE(precio_venta_base, precio),
    costo_promedio = COALESCE(costo_promedio, 0),
    afecto_igv = COALESCE(afecto_igv, TRUE),
    usa_configuracion_empresa = COALESCE(usa_configuracion_empresa, TRUE),
    maneja_stock = COALESCE(maneja_stock, TRUE),
    maneja_lotes = COALESCE(maneja_lotes, FALSE),
    maneja_vencimiento = COALESCE(maneja_vencimiento, FALSE),
    stock_minimo_global = COALESCE(stock_minimo_global, stock_minimo, 0),
    stock_minimo = COALESCE(stock_minimo, stock_minimo_global, 0),
    tipo_producto = COALESCE(NULLIF(tipo_producto, ''), 'PRODUCTO'),
    estado = COALESCE(NULLIF(estado, ''), CASE WHEN activo THEN 'ACTIVO' ELSE 'INACTIVO' END)
WHERE codigo IS NULL
   OR precio_venta_base IS NULL
   OR costo_promedio IS NULL
   OR tipo_producto IS NULL
   OR tipo_producto = ''
   OR estado IS NULL
   OR estado = ''
   OR stock_minimo IS NULL
   OR stock_minimo_global IS NULL;

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

UPDATE crm_prospectos
SET necesidad_identificada = COALESCE(necesidad_identificada, FALSE),
    interes_real = COALESCE(NULLIF(interes_real, ''), 'BAJO'),
    presupuesto_definido = COALESCE(NULLIF(presupuesto_definido, ''), 'DESCONOCIDO'),
    tomador_decision = COALESCE(NULLIF(tomador_decision, ''), 'DESCONOCIDO'),
    fecha_estimada_compra = COALESCE(NULLIF(fecha_estimada_compra, ''), 'DESCONOCIDO'),
    score_calificacion = COALESCE(score_calificacion, 0),
    temperatura = COALESCE(NULLIF(temperatura, ''), 'FRIO')
WHERE interes_real IS NULL
   OR interes_real = ''
   OR presupuesto_definido IS NULL
   OR presupuesto_definido = ''
   OR tomador_decision IS NULL
   OR tomador_decision = ''
   OR fecha_estimada_compra IS NULL
   OR fecha_estimada_compra = ''
   OR score_calificacion IS NULL
   OR temperatura IS NULL
   OR temperatura = '';

CREATE INDEX IF NOT EXISTS idx_crm_prospectos_oportunidad ON crm_prospectos(oportunidad_id);
