ALTER TABLE public.venta_facturacion_outbox
    ADD COLUMN IF NOT EXISTS lease_owner VARCHAR(120),
    ADD COLUMN IF NOT EXISTS lease_until TIMESTAMP,
    ADD COLUMN IF NOT EXISTS heartbeat_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_venta_facturacion_outbox_lease
    ON public.venta_facturacion_outbox(status, lease_until)
    WHERE status = 'PROCESSING';

UPDATE public.venta_facturacion_outbox
   SET status = 'RETRY',
       next_attempt_at = CURRENT_TIMESTAMP,
       last_error = 'Tarea recuperada durante migracion de leases',
       lease_owner = NULL,
       lease_until = NULL,
       heartbeat_at = NULL
 WHERE status = 'PROCESSING';
