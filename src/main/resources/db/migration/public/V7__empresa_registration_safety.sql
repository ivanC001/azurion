ALTER TABLE public.empresas
    ADD COLUMN IF NOT EXISTS logo_panel_url VARCHAR(500);

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
