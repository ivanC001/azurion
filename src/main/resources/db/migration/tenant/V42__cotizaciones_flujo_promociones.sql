ALTER TABLE cotizaciones
    ADD COLUMN IF NOT EXISTS fecha_envio TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS canal_envio VARCHAR(30),
    ADD COLUMN IF NOT EXISTS proximo_seguimiento_en TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS fecha_respuesta TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS motivo_rechazo VARCHAR(500),
    ADD COLUMN IF NOT EXISTS decision_siguiente VARCHAR(30);

ALTER TABLE cotizaciones
    DROP CONSTRAINT IF EXISTS chk_cotizaciones_estado;

ALTER TABLE cotizaciones
    ADD CONSTRAINT chk_cotizaciones_estado CHECK (
        estado IN ('BORRADOR', 'ENVIADA', 'EN_SEGUIMIENTO', 'ACEPTADA', 'RECHAZADA', 'NEGOCIACION', 'VENCIDA', 'CONVERTIDA')
    );

ALTER TABLE cotizaciones
    DROP CONSTRAINT IF EXISTS chk_cotizaciones_decision_siguiente;

ALTER TABLE cotizaciones
    ADD CONSTRAINT chk_cotizaciones_decision_siguiente CHECK (
        decision_siguiente IS NULL OR decision_siguiente IN ('NEGOCIACION', 'VENTA')
    );

CREATE TABLE IF NOT EXISTS promociones_cotizacion (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(40) NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(500),
    tipo_descuento VARCHAR(20) NOT NULL,
    valor NUMERIC(18, 2) NOT NULL,
    fecha_inicio DATE,
    fecha_fin DATE,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_promociones_cotizacion_codigo UNIQUE (codigo),
    CONSTRAINT chk_promociones_cotizacion_tipo CHECK (tipo_descuento IN ('MONTO', 'PORCENTAJE')),
    CONSTRAINT chk_promociones_cotizacion_valor CHECK (valor >= 0),
    CONSTRAINT chk_promociones_cotizacion_estado CHECK (estado IN ('ACTIVA', 'INACTIVA'))
);

ALTER TABLE cotizacion_detalles
    ADD COLUMN IF NOT EXISTS promocion_id BIGINT REFERENCES promociones_cotizacion(id),
    ADD COLUMN IF NOT EXISTS promocion_descuento NUMERIC(18, 2) NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_promociones_cotizacion_estado ON promociones_cotizacion(estado);
CREATE INDEX IF NOT EXISTS idx_cotizacion_detalles_promocion ON cotizacion_detalles(promocion_id);
CREATE INDEX IF NOT EXISTS idx_cotizaciones_crm_estado ON cotizaciones(crm_oportunidad_id, estado);
