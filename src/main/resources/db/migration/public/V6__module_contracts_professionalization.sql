ALTER TABLE public.planes
    ADD COLUMN IF NOT EXISTS descripcion VARCHAR(400);

CREATE TABLE IF NOT EXISTS public.plan_modulos (
    id BIGSERIAL PRIMARY KEY,
    plan_id BIGINT NOT NULL REFERENCES public.planes(id) ON DELETE CASCADE,
    modulo_id BIGINT NOT NULL REFERENCES public.modulos(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_plan_modulo UNIQUE (plan_id, modulo_id)
);

CREATE INDEX IF NOT EXISTS idx_plan_modulos_plan_id ON public.plan_modulos(plan_id);
CREATE INDEX IF NOT EXISTS idx_plan_modulos_modulo_id ON public.plan_modulos(modulo_id);

ALTER TABLE public.empresa_modulos
    ADD COLUMN IF NOT EXISTS estado VARCHAR(20);

UPDATE public.empresa_modulos
SET estado = CASE WHEN activo THEN 'ACTIVO' ELSE 'INACTIVO' END
WHERE estado IS NULL;

ALTER TABLE public.empresa_modulos
    ALTER COLUMN estado SET DEFAULT 'ACTIVO';

INSERT INTO public.modulos (codigo, nombre, descripcion, estado)
VALUES
    ('ERP', 'ERP', 'Core administrativo de la plataforma AZURION', 'ACTIVO'),
    ('INVENTARIO', 'Inventario', 'Gestion de productos, stock, kardex y almacenes', 'ACTIVO'),
    ('VENTAS', 'Ventas', 'Punto de venta, historial comercial y conversion operacional', 'ACTIVO'),
    ('CAJA', 'Caja', 'Apertura, movimientos, cierres y control de caja', 'ACTIVO'),
    ('COMPRAS', 'Compras', 'Registro de compras, ingresos y abastecimiento', 'ACTIVO'),
    ('CLIENTES', 'Clientes', 'Gestion comercial y financiera de clientes', 'ACTIVO'),
    ('FACTURACION', 'Facturacion', 'Emision y seguimiento documental del facturador', 'ACTIVO'),
    ('CRM', 'CRM', 'Captacion, prospectos, oportunidades y actividades comerciales', 'ACTIVO'),
    ('REPORTES', 'Reportes', 'Indicadores, reportes y analitica operacional', 'ACTIVO'),
    ('COTIZACIONES', 'Cotizaciones', 'Propuestas comerciales y conversion a venta', 'ACTIVO')
ON CONFLICT (codigo) DO UPDATE
SET nombre = EXCLUDED.nombre,
    descripcion = EXCLUDED.descripcion,
    estado = EXCLUDED.estado,
    updated_at = now();

INSERT INTO public.planes (nombre, codigo, descripcion, limite_mensual_bolsa, precio_mensual, estado)
VALUES
    ('Plan Inventario', 'PLAN_INVENTARIO', 'Inventario, compras, clientes y reportes para operacion comercial.', 0, 0, 'ACTIVO'),
    ('Plan Facturacion', 'PLAN_FACTURACION', 'Facturacion, clientes y reportes para emision y seguimiento.', 0, 0, 'ACTIVO'),
    ('Plan CRM', 'PLAN_CRM', 'CRM, clientes, cotizaciones y reportes para gestion comercial.', 0, 0, 'ACTIVO'),
    ('Plan Completo', 'PLAN_COMPLETO', 'Acceso a todos los modulos operativos de AZURION.', 0, 0, 'ACTIVO')
ON CONFLICT (codigo) DO UPDATE
SET nombre = EXCLUDED.nombre,
    descripcion = EXCLUDED.descripcion,
    estado = EXCLUDED.estado,
    updated_at = now();

INSERT INTO public.plan_modulos (plan_id, modulo_id)
SELECT p.id, m.id
FROM public.planes p
JOIN public.modulos m ON (
    (p.codigo = 'PLAN_INVENTARIO' AND m.codigo IN ('INVENTARIO', 'COMPRAS', 'CLIENTES', 'REPORTES')) OR
    (p.codigo = 'PLAN_FACTURACION' AND m.codigo IN ('FACTURACION', 'CLIENTES', 'REPORTES')) OR
    (p.codigo = 'PLAN_CRM' AND m.codigo IN ('CRM', 'CLIENTES', 'COTIZACIONES', 'REPORTES')) OR
    (p.codigo = 'PLAN_COMPLETO' AND m.codigo IN ('ERP', 'INVENTARIO', 'VENTAS', 'CAJA', 'COMPRAS', 'CLIENTES', 'FACTURACION', 'CRM', 'REPORTES', 'COTIZACIONES'))
)
ON CONFLICT (plan_id, modulo_id) DO NOTHING;

INSERT INTO public.empresa_modulos (empresa_id, modulo_id, activo, estado, fecha_inicio, fecha_fin, configuracion_extra)
SELECT DISTINCT s.empresa_id,
       pm.modulo_id,
       TRUE,
       'ACTIVO',
       COALESCE(s.fecha_inicio, CURRENT_DATE),
       s.fecha_fin,
       NULL::jsonb
FROM public.suscripciones s
JOIN public.plan_modulos pm ON pm.plan_id = s.plan_id
WHERE UPPER(COALESCE(s.estado, 'ACTIVA')) = 'ACTIVA'
ON CONFLICT (empresa_id, modulo_id) DO UPDATE
SET activo = TRUE,
    estado = 'ACTIVO',
    fecha_inicio = COALESCE(public.empresa_modulos.fecha_inicio, EXCLUDED.fecha_inicio),
    fecha_fin = EXCLUDED.fecha_fin,
    updated_at = now();
