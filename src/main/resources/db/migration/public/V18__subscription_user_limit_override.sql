ALTER TABLE public.suscripciones
    ADD COLUMN IF NOT EXISTS limite_usuarios INTEGER;

ALTER TABLE public.suscripciones
    DROP CONSTRAINT IF EXISTS ck_suscripciones_limite_usuarios;

ALTER TABLE public.suscripciones
    ADD CONSTRAINT ck_suscripciones_limite_usuarios
        CHECK (limite_usuarios IS NULL OR limite_usuarios >= 1);

CREATE INDEX IF NOT EXISTS idx_suscripciones_empresa_estado_actual
    ON public.suscripciones(empresa_id, estado, fecha_inicio DESC, id DESC);

WITH ranked_active AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY empresa_id
               ORDER BY fecha_inicio DESC, id DESC
           ) AS row_number
    FROM public.suscripciones
    WHERE UPPER(estado) = 'ACTIVA'
)
UPDATE public.suscripciones subscription
SET estado = 'SUSPENDIDA',
    fecha_fin = COALESCE(subscription.fecha_fin, CURRENT_DATE),
    updated_at = now()
FROM ranked_active ranked
WHERE subscription.id = ranked.id
  AND ranked.row_number > 1;

CREATE UNIQUE INDEX IF NOT EXISTS uk_suscripciones_empresa_activa
    ON public.suscripciones(empresa_id)
    WHERE UPPER(estado) = 'ACTIVA';
