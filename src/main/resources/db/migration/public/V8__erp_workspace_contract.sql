INSERT INTO public.plan_modulos (plan_id, modulo_id)
SELECT p.id, m.id
FROM public.planes p
JOIN public.modulos m ON m.codigo = 'ERP'
WHERE p.codigo IN ('PLAN_INVENTARIO', 'PLAN_FACTURACION', 'PLAN_COMPLETO')
ON CONFLICT (plan_id, modulo_id) DO NOTHING;

INSERT INTO public.empresa_modulos (
    empresa_id, modulo_id, activo, estado, fecha_inicio, fecha_fin, configuracion_extra
)
SELECT DISTINCT em.empresa_id,
       erp.id,
       TRUE,
       'ACTIVO',
       CURRENT_DATE,
       NULL::date,
       NULL::jsonb
FROM public.empresa_modulos em
JOIN public.modulos source_module ON source_module.id = em.modulo_id
JOIN public.modulos erp ON erp.codigo = 'ERP'
WHERE em.activo = TRUE
  AND UPPER(COALESCE(em.estado, 'ACTIVO')) = 'ACTIVO'
  AND source_module.codigo IN ('INVENTARIO', 'VENTAS', 'CAJA', 'COMPRAS', 'FACTURACION')
ON CONFLICT (empresa_id, modulo_id) DO UPDATE
SET activo = TRUE,
    estado = 'ACTIVO',
    updated_at = now();
