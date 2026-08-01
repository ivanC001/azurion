ALTER TABLE public.empresas
    ADD COLUMN IF NOT EXISTS facturador_status VARCHAR(30) NOT NULL DEFAULT 'NO_REQUERIDO',
    ADD COLUMN IF NOT EXISTS facturador_document_mode VARCHAR(30) NOT NULL DEFAULT 'TICKET_ONLY',
    ADD COLUMN IF NOT EXISTS facturador_fiscal_status VARCHAR(30) NOT NULL DEFAULT 'NOT_CONFIGURED',
    ADD COLUMN IF NOT EXISTS facturador_sunat_mode VARCHAR(20) NOT NULL DEFAULT 'DISABLED',
    ADD COLUMN IF NOT EXISTS facturador_last_error VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS facturador_provisioned_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS facturador_next_attempt_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS facturador_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS facturador_lease_owner VARCHAR(120),
    ADD COLUMN IF NOT EXISTS facturador_lease_until TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_empresas_facturador_queue
    ON public.empresas(facturador_status, facturador_next_attempt_at)
    WHERE facturador_status IN ('PENDIENTE', 'REINTENTO');

UPDATE public.empresas empresa
SET facturador_status = 'PENDIENTE',
    facturador_next_attempt_at = now()
WHERE empresa.activo = TRUE
  AND EXISTS (
      SELECT 1
      FROM public.empresa_modulos empresa_modulo
      JOIN public.modulos modulo ON modulo.id = empresa_modulo.modulo_id
      WHERE empresa_modulo.empresa_id = empresa.id
        AND empresa_modulo.activo = TRUE
        AND upper(coalesce(empresa_modulo.estado, 'ACTIVO')) = 'ACTIVO'
        AND upper(coalesce(modulo.estado, 'ACTIVO')) = 'ACTIVO'
        AND upper(modulo.codigo) = 'ERP'
        AND (empresa_modulo.fecha_inicio IS NULL OR empresa_modulo.fecha_inicio <= CURRENT_DATE)
        AND (empresa_modulo.fecha_fin IS NULL OR empresa_modulo.fecha_fin >= CURRENT_DATE)
  );
