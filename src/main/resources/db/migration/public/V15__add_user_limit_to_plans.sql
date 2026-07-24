ALTER TABLE public.planes
    ADD COLUMN IF NOT EXISTS limite_usuarios INTEGER;

UPDATE public.planes
SET limite_usuarios = CASE UPPER(codigo)
    WHEN 'PLAN_CRM' THEN 10
    WHEN 'PLAN_COMPLETO' THEN 25
    ELSE 5
END
WHERE limite_usuarios IS NULL;

ALTER TABLE public.planes
    ALTER COLUMN limite_usuarios SET DEFAULT 5,
    ALTER COLUMN limite_usuarios SET NOT NULL;

ALTER TABLE public.planes
    DROP CONSTRAINT IF EXISTS ck_planes_limite_usuarios;

ALTER TABLE public.planes
    ADD CONSTRAINT ck_planes_limite_usuarios CHECK (limite_usuarios >= 1);
