CREATE INDEX IF NOT EXISTS idx_clientes_activo_id
    ON clientes(activo, id DESC);

CREATE INDEX IF NOT EXISTS idx_productos_almacen_nombre_id
    ON productos(almacen_id, nombre, id);

CREATE INDEX IF NOT EXISTS idx_notas_fiscales_tipo_fecha_id
    ON notas_fiscales(tipo_documento, fecha_emision DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_notas_fiscales_estado_fecha
    ON notas_fiscales(facturacion_estado, fecha_emision DESC);

CREATE INDEX IF NOT EXISTS idx_guias_remision_estado_fecha_id
    ON guias_remision(facturacion_estado, fecha_emision DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_guias_remision_origen_destino
    ON guias_remision(sucursal_origen_id, sucursal_destino_id, id DESC);
